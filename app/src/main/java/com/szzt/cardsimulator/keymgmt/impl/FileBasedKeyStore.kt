package com.szzt.cardsimulator.keymgmt.impl

import android.content.Context
import com.szzt.cardsimulator.keymgmt.api.KeyStore
import com.szzt.cardsimulator.keymgmt.api.RsaKeyPair
import java.io.File
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * File-based key store with basic obfuscation.
 *
 * Keys are stored as obfuscated files in the app's internal storage.
 * This is NOT a secure key store — it is suitable only for
 * test environments.
 */
class FileBasedKeyStore(
    private val context: Context
) : KeyStore {

    private val keysDir: File
        get() = File(context.filesDir, "keys").also { it.mkdirs() }

    private val rsaDir: File
        get() = File(context.filesDir, "rsa_keys").also { it.mkdirs() }

    override suspend fun storeSymmetricKey(keyId: String, keyData: ByteArray) {
        val obfuscated = obfuscate(keyData)
        File(keysDir, keyId).writeBytes(obfuscated)
    }

    override suspend fun getSymmetricKey(keyId: String): ByteArray? {
        val file = File(keysDir, keyId)
        if (!file.exists()) return null
        return deobfuscate(file.readBytes())
    }

    override suspend fun storeRsaKeyPair(keyId: String, publicKey: ByteArray, privateKey: ByteArray) {
        val dir = File(rsaDir, keyId).also { it.mkdirs() }
        File(dir, "public.der").writeBytes(obfuscate(publicKey))
        File(dir, "private.der").writeBytes(obfuscate(privateKey))
    }

    override suspend fun getRsaKeyPair(keyId: String): RsaKeyPair? {
        val dir = File(rsaDir, keyId)
        if (!dir.exists()) return null
        val publicFile = File(dir, "public.der")
        val privateFile = File(dir, "private.der")
        if (!publicFile.exists() || !privateFile.exists()) return null

        return RsaKeyPair(
            publicKey = deobfuscate(publicFile.readBytes()),
            privateKey = deobfuscate(privateFile.readBytes())
        )
    }

    override suspend fun listKeyIds(): List<String> {
        val symmetricIds = keysDir.listFiles()?.map { it.name } ?: emptyList()
        val rsaIds = rsaDir.listFiles()?.map { it.name } ?: emptyList()
        return symmetricIds + rsaIds
    }

    override suspend fun deleteKey(keyId: String) {
        File(keysDir, keyId).delete()
        File(rsaDir, keyId).deleteRecursively()
    }

    // Basic XOR obfuscation (NOT secure, just prevents casual inspection)
    private val obfuscationKey = "CardSimulator2026".toByteArray()

    private fun obfuscate(data: ByteArray): ByteArray {
        return ByteArray(data.size) { i ->
            (data[i].toInt() xor obfuscationKey[i % obfuscationKey.size].toInt()).toByte()
        }
    }

    private fun deobfuscate(data: ByteArray): ByteArray = obfuscate(data) // XOR is symmetric
}
