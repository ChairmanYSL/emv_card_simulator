package com.szzt.cardsimulator.emv.api

/**
 * Cryptography engine for EMV operations.
 *
 * Handles:
 *  - ARQC/TC/AAC generation (3DES/AES)
 *  - ARPC verification
 *  - SDA signature data preparation
 *  - DDA dynamic signature generation
 *  - CDA combined signature generation
 *  - Session key derivation
 */
interface CryptoEngine {

    /**
     * Generate Application Cryptogram (ARQC / TC / AAC).
     *
     * @param cdol1Data  Data built from CDOL1 (CDOL1 values populated from card data).
     * @param keyId      Identifier for the symmetric card key (MDK/UDK) to use.
     * @param cryptogramType 0x80 = ARQC, 0x40 = TC, 0x00 = AAC (validated).
     * @return 8-byte cryptogram + ATC + CVR (Cryptogram Information Data)
     *
     * The ATC is read, incremented and persisted by the implementation, so it
     * never repeats across transactions.
     */
    suspend fun generateApplicationCryptogram(
        cdol1Data: ByteArray,
        keyId: String,
        cryptogramType: Int
    ): CryptogramResult

    /**
     * Prepare SDA signature data for the reader to verify.
     * Returns the Signed Static Application Data (SSAD).
     */
    suspend fun prepareSDA(iccPrivateKeyId: String, dataToSign: ByteArray): ByteArray

    /**
     * Generate DDA dynamic signature.
     */
    suspend fun generateDDA(iccPrivateKeyId: String, dynamicData: ByteArray): ByteArray

    /**
     * Generate CDA combined signature (covers transaction data as well).
     */
    suspend fun generateCDA(iccPrivateKeyId: String, dataToSign: ByteArray): ByteArray

    /**
     * Derive session key from MDK/UDK for a given ATC.
     */
    suspend fun deriveSessionKey(masterKeyId: String, atc: Int): ByteArray
}

data class CryptogramResult(
    val cryptogram: ByteArray,
    val atc: Int,
    val cvr: ByteArray,
    val applicationTransactionCounter: ByteArray,
    val unpredictableNumber: ByteArray
)
