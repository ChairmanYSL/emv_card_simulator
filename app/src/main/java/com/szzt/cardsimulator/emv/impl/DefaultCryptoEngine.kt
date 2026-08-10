package com.szzt.cardsimulator.emv.impl

import com.szzt.cardsimulator.emv.api.CryptoEngine
import com.szzt.cardsimulator.emv.api.CryptogramResult
import com.szzt.cardsimulator.keymgmt.api.KeyStore
import timber.log.Timber
import java.security.KeyFactory
import java.security.Signature
import java.security.spec.PKCS8EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

/**
 * EMV 4.3 Book 2 compliant crypto engine.
 *
 * Implements:
 *  - Session key derivation (Annex A1.3.1 Common SKD, double-length 16-byte master key)
 *  - Application cryptogram ARQC/TC/AAC (ISO/IEC 9797-1 MAC Algorithm 3 / Retail MAC,
 *    padding method 2, 8-byte output)
 *  - RSA signatures for SDA/DDA/CDA (SHA-1 with RSA, per EMV ODA)
 *  - ARPC Method 1 verification (EMV Book 2 Section 8.2)
 *
 * The master key material is loaded from the [KeyStore] by key id. The ATC is
 * read/incremented/persisted through the [KeyStore] so that it never repeats
 * across transactions (EMV requires a monotonically increasing ATC).
 */
class DefaultCryptoEngine(
    private val keyStore: KeyStore
) : CryptoEngine {

    override suspend fun generateApplicationCryptogram(
        cdol1Data: ByteArray,
        keyId: String,
        cryptogramType: Int
    ): CryptogramResult {
        require(cryptogramType == 0x80 || cryptogramType == 0x40 || cryptogramType == 0x00) {
            "Invalid cryptogram type: 0x${String.format("%02X", cryptogramType)}"
        }

        val masterKey = keyStore.getSymmetricKey(keyId)
            ?: throw IllegalStateException("No symmetric key found for keyId: $keyId")

        val atc = nextAtc(keyId)
        val sessionKey = deriveSessionKeyInternal(masterKey, atc)
        val unpredictableNumber = extractUnpredictableNumber(cdol1Data)

        val cryptogram = retailMac(sessionKey, cdol1Data) // 8-byte ARQC/TC/AAC
        val cvr = buildCvr(cryptogramType)                // 6-byte M/Chip-style CVR
        val atcBytes = byteArrayOf(
            ((atc shr 8) and 0xFF).toByte(),
            (atc and 0xFF).toByte()
        )

        return CryptogramResult(
            cryptogram = cryptogram,
            atc = atc,
            cvr = cvr,
            applicationTransactionCounter = atcBytes,
            unpredictableNumber = unpredictableNumber
        )
    }

    override suspend fun deriveSessionKey(masterKeyId: String, atc: Int): ByteArray {
        val masterKey = keyStore.getSymmetricKey(masterKeyId)
            ?: throw IllegalStateException("No symmetric key found for keyId: $masterKeyId")
        return deriveSessionKeyInternal(masterKey, atc)
    }

    override suspend fun prepareSDA(iccPrivateKeyId: String, dataToSign: ByteArray): ByteArray {
        return rsaSign(iccPrivateKeyId, dataToSign)
    }

    override suspend fun generateDDA(iccPrivateKeyId: String, dynamicData: ByteArray): ByteArray {
        return rsaSign(iccPrivateKeyId, dynamicData)
    }

    override suspend fun generateCDA(iccPrivateKeyId: String, dataToSign: ByteArray): ByteArray {
        return rsaSign(iccPrivateKeyId, dataToSign)
    }

    // ==================== EMV crypto primitives ====================

    /**
     * EMV 4.3 Book 2 Annex A1.3.1 Common Session Key Derivation (double-length key).
     *
     * SK = SK_A || SK_B where:
     *   R      = ATC(2 bytes) || 0x00 x 6
     *   R_A    = R with byte 2 set to 0xF0
     *   R_B    = R with byte 2 set to 0x0F
     *   SK_A   = 3DES-ECB(MK, R_A),  SK_B = 3DES-ECB(MK, R_B)
     * with DES odd-parity adjusted on the output.
     */
    private fun deriveSessionKeyInternal(masterKey: ByteArray, atc: Int): ByteArray {
        require(masterKey.size == 16) {
            "EMV 4.3 Common SKD requires a 16-byte (double-length) master key, got ${masterKey.size}"
        }

        val r = byteArrayOf(
            ((atc shr 8) and 0xFF).toByte(),
            (atc and 0xFF).toByte(),
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        )
        val rA = r.copyOf().also { it[2] = 0xF0.toByte() }
        val rB = r.copyOf().also { it[2] = 0x0F.toByte() }

        val cipher = Cipher.getInstance("DESede/ECB/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(masterKey, "DESede"))
        val encrypted = cipher.doFinal(rA + rB) // 16 bytes

        // Adjust DES odd parity on each key byte
        val sk = ByteArray(16)
        for (i in encrypted.indices) {
            sk[i] = adjustDesParity(encrypted[i])
        }
        return sk
    }

    /**
     * ISO/IEC 9797-1 MAC Algorithm 3 (Retail MAC) with padding method 2.
     *
     * Session key = 16 bytes: K1 = first 8 (used for CBC encryption),
     * K2 = last 8 (used for the final decrypt-then-encrypt step).
     * Output is the full 8-byte MAC (ARQC / TC / AAC).
     */
    private fun retailMac(sessionKey: ByteArray, data: ByteArray): ByteArray {
        require(sessionKey.size == 16) { "Retail MAC requires a 16-byte session key" }
        val k1 = SecretKeySpec(sessionKey.copyOfRange(0, 8), "DES")
        val k2 = SecretKeySpec(sessionKey.copyOfRange(8, 16), "DES")

        val padded = padIso9797Method2(data)
        val cipher = Cipher.getInstance("DES/ECB/NoPadding")

        var block = ByteArray(8) // IV = all zeros
        val blockCount = padded.size / 8

        // CBC over the first n-1 blocks with K1
        for (i in 0 until blockCount - 1) {
            block = xorBlocks(block, padded, i * 8)
            cipher.init(Cipher.ENCRYPT_MODE, k1)
            block = cipher.doFinal(block)
        }

        // Final block: decrypt with K2, then encrypt with K1
        block = xorBlocks(block, padded, (blockCount - 1) * 8)
        cipher.init(Cipher.DECRYPT_MODE, k2)
        block = cipher.doFinal(block)
        cipher.init(Cipher.ENCRYPT_MODE, k1)
        return cipher.doFinal(block)
    }

    /**
     * EMV Book 2 Section 8.2 ARPC Method 1 (kept for issuer-side verification):
     *   ARPC = 3DES-CBC(SK, IV=0) ( ARQC XOR (ARPC-RC || 0x00 x 6) )
     * Returns the 8-byte ARPC. Not currently invoked by the card-side kernel.
     */
    @Suppress("unused")
    private fun generateArpcMethod1(sessionKey: ByteArray, arqc: ByteArray, arpcRc: ByteArray): ByteArray {
        require(arqc.size == 8) { "ARQC must be 8 bytes" }
        require(arpcRc.size == 2) { "ARPC-RC must be 2 bytes" }

        val xorInput = ByteArray(8)
        arqc.copyInto(xorInput)
        for (i in 0 until 2) {
            xorInput[i] = (xorInput[i].toInt() xor arpcRc[i].toInt()).toByte()
        }

        val cipher = Cipher.getInstance("DESede/CBC/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(sessionKey, "DESede"), javax.crypto.spec.IvParameterSpec(ByteArray(8)))
        return cipher.doFinal(xorInput)
    }

    // ==================== Helpers ====================

    /** Read, increment and persist the ATC for a key id. */
    private suspend fun nextAtc(keyId: String): Int {
        val atcKeyId = "atc_$keyId"
        val current = keyStore.getSymmetricKey(atcKeyId)
        val currentAtc = if (current != null && current.size == 2) {
            ((current[0].toInt() and 0xFF) shl 8) or (current[1].toInt() and 0xFF)
        } else {
            0
        }
        val next = (currentAtc + 1) and 0xFFFF
        keyStore.storeSymmetricKey(
            atcKeyId,
            byteArrayOf(((next shr 8) and 0xFF).toByte(), (next and 0xFF).toByte())
        )
        return next
    }

    /**
     * Extract the Unpredictable Number (tag 9F37, 4 bytes) from the CDOL1 data.
     *
     * The CDOL1 data is the concatenation of the data elements listed in the
     * profile's CDOL1 template; 9F37 is the last element in the standard EMV
     * minimal CDOL1, so we take the last 4 bytes. If the data is shorter than
     * 4 bytes, a zero UN is used (the reader will have provided one).
     */
    private fun extractUnpredictableNumber(cdol1Data: ByteArray): ByteArray {
        if (cdol1Data.size >= 4) {
            return cdol1Data.copyOfRange(cdol1Data.size - 4, cdol1Data.size)
        }
        Timber.w("CDOL1 data too short (${cdol1Data.size} bytes) for UN; using zero UN")
        return ByteArray(4)
    }

    private fun buildCvr(cryptogramType: Int): ByteArray {
        // M/Chip-style 6-byte CVR. Byte 1 bits 8-7: 2nd GENERATE AC result
        // (00=AAC returned, 01=TC returned, 10=not requested). Bits 6-5: 1st
        // GENERATE AC result (00=AAC, 01=TC, 10=ARQC). Remaining bytes zeroed
        // for a basic card with no CDA/PIN counters.
        val firstAc: Int = when (cryptogramType) {
            0x80 -> 0b10 // ARQC
            0x40 -> 0b01 // TC
            else -> 0b00 // AAC
        }
        val byte1 = (0b10 shl 6) or (firstAc shl 4)
        return byteArrayOf(byte1.toByte(), 0x00, 0x00, 0x00, 0x00, 0x00)
    }

    private suspend fun rsaSign(iccPrivateKeyId: String, data: ByteArray): ByteArray {
        val keyPair = keyStore.getRsaKeyPair(iccPrivateKeyId)
            ?: throw IllegalStateException("No RSA key pair found for keyId: $iccPrivateKeyId")

        val keyFactory = KeyFactory.getInstance("RSA")
        val privateKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyPair.privateKey))
        val signature = Signature.getInstance("SHA1withRSA")
        signature.initSign(privateKey)
        signature.update(data)
        return signature.sign()
    }

    private fun padIso9797Method2(data: ByteArray): ByteArray {
        val paddedLength = (data.size / 8 + 1) * 8
        val padded = ByteArray(paddedLength)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        return padded
    }

    private fun xorBlocks(block: ByteArray, data: ByteArray, offset: Int): ByteArray {
        val out = ByteArray(8)
        for (i in 0 until 8) {
            out[i] = (block[i].toInt() xor data[offset + i].toInt()).toByte()
        }
        return out
    }

    private fun adjustDesParity(b: Byte): Byte {
        val x = b.toInt() and 0xFF
        // Bit 0 (LSB) is the parity bit; set it so the byte has odd parity.
        val parity = Integer.bitCount(x and 0xFE) and 1
        return ((x and 0xFE) or (parity xor 1)).toByte()
    }
}
