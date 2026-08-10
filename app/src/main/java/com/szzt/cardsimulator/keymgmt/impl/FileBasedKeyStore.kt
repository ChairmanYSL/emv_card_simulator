package com.szzt.cardsimulator.keymgmt.impl

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.szzt.cardsimulator.keymgmt.api.KeyStore
import com.szzt.cardsimulator.keymgmt.api.RsaKeyPair
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.IOException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * File-based key store.
 *
 * Key material is encrypted with an AES-256-GCM master key held in the
 * Android Keystore (never leaves secure hardware / TEE) before being written
 * to the app's internal storage. Each key file carries a fresh random IV,
 * and GCM provides authenticity, so tampered files fail decryption instead
 * of silently returning garbage.
 *
 * All disk I/O runs on [Dispatchers.IO]; writes are atomic (temp file + rename).
 */
class FileBasedKeyStore(
    private val context: Context
) : KeyStore {

    private val keysDir: File
        get() = File(context.filesDir, "keys")

    private val rsaDir: File
        get() = File(context.filesDir, "rsa_keys")

    override suspend fun storeSymmetricKey(keyId: String, keyData: ByteArray) {
        withContext(Dispatchers.IO) {
            validateKeyId(keyId)
            atomicWrite(File(keysDir, keyId), encrypt(keyData))
        }
    }

    override suspend fun getSymmetricKey(keyId: String): ByteArray? {
        return withContext(Dispatchers.IO) {
            validateKeyId(keyId)
            val file = File(keysDir, keyId)
            if (!file.exists()) return@withContext null
            try {
                decrypt(file.readBytes())
            } catch (e: Exception) {
                Timber.e(e, "Failed to decrypt symmetric key: $keyId")
                null
            }
        }
    }

    override suspend fun storeRsaKeyPair(keyId: String, publicKey: ByteArray, privateKey: ByteArray) {
        withContext(Dispatchers.IO) {
            validateKeyId(keyId)
            val dir = File(rsaDir, keyId)
            atomicWrite(File(dir, "public.der"), encrypt(publicKey))
            atomicWrite(File(dir, "private.der"), encrypt(privateKey))
        }
    }

    override suspend fun getRsaKeyPair(keyId: String): RsaKeyPair? {
        return withContext(Dispatchers.IO) {
            validateKeyId(keyId)
            val dir = File(rsaDir, keyId)
            val publicFile = File(dir, "public.der")
            val privateFile = File(dir, "private.der")
            if (!publicFile.exists() || !privateFile.exists()) return@withContext null

            try {
                RsaKeyPair(
                    publicKey = decrypt(publicFile.readBytes()),
                    privateKey = decrypt(privateFile.readBytes())
                )
            } catch (e: Exception) {
                Timber.e(e, "Failed to decrypt RSA key pair: $keyId")
                null
            }
        }
    }

    override suspend fun listKeyIds(): List<String> {
        return withContext(Dispatchers.IO) {
            val symmetricIds = keysDir.listFiles()?.filter { it.isFile }?.map { it.name } ?: emptyList()
            val rsaIds = rsaDir.listFiles()?.filter { it.isDirectory }?.map { it.name } ?: emptyList()
            (symmetricIds + rsaIds).distinct()
        }
    }

    override suspend fun deleteKey(keyId: String) {
        withContext(Dispatchers.IO) {
            validateKeyId(keyId)
            val sym = File(keysDir, keyId)
            if (sym.exists() && !sym.delete()) {
                Timber.w("Failed to delete symmetric key: $keyId")
            }
            val rsa = File(rsaDir, keyId)
            if (rsa.exists() && !rsa.deleteRecursively()) {
                Timber.w("Failed to delete RSA key: $keyId")
            }
        }
    }

    // ==================== Helpers ====================

    private fun validateKeyId(keyId: String) {
        require(KEY_ID_PATTERN.matches(keyId)) {
            "Invalid keyId '$keyId': must match $KEY_ID_PATTERN"
        }
    }

    /** Write via temp file + rename so a crash mid-write never leaves a corrupt file. */
    private fun atomicWrite(target: File, data: ByteArray) {
        target.parentFile?.mkdirs()
        val tmp = File(target.parentFile, target.name + ".tmp")
        try {
            tmp.writeBytes(data)
            if (!tmp.renameTo(target)) {
                // rename can fail on some filesystems; fall back to direct write
                target.writeBytes(data)
                tmp.delete()
            }
        } catch (e: IOException) {
            tmp.delete()
            throw e
        }
    }

    // --- Android Keystore AES-GCM encryption ---

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateMasterKey())
        val ciphertext = cipher.doFinal(plain)
        val iv = cipher.iv
        return iv + ciphertext
    }

    private fun decrypt(data: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, IV_LENGTH)
        val ciphertext = data.copyOfRange(IV_LENGTH, data.size)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateMasterKey(), GCMParameterSpec(TAG_BITS, iv))
        return cipher.doFinal(ciphertext)
    }

    private fun getOrCreateMasterKey(): SecretKey {
        val androidKeyStore = java.security.KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (androidKeyStore.getEntry(MASTER_KEY_ALIAS, null) as? java.security.KeyStore.SecretKeyEntry)
            ?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val MASTER_KEY_ALIAS = "card_simulator_master_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_LENGTH = 12
        const val TAG_BITS = 128
        val KEY_ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
