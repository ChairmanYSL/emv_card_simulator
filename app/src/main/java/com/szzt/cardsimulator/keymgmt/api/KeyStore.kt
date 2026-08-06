package com.szzt.cardsimulator.keymgmt.api

/**
 * Secure-ish key store for symmetric and asymmetric keys used in EMV operations.
 *
 * Keys are stored on-disk with basic obfuscation — this is a test tool,
 * not a production payment application.
 */
interface KeyStore {

    /**
     * Store a symmetric key (3DES/AES).
     *
     * @param keyId   Unique key identifier.
     * @param keyData Raw key bytes.
     */
    suspend fun storeSymmetricKey(keyId: String, keyData: ByteArray)

    /**
     * Retrieve a symmetric key.
     */
    suspend fun getSymmetricKey(keyId: String): ByteArray?

    /**
     * Store an RSA key pair.
     *
     * @param keyId      Unique key identifier.
     * @param publicKey  Public key bytes (DER-encoded).
     * @param privateKey Private key bytes (DER-encoded).
     */
    suspend fun storeRsaKeyPair(keyId: String, publicKey: ByteArray, privateKey: ByteArray)

    /**
     * Retrieve an RSA key pair.
     */
    suspend fun getRsaKeyPair(keyId: String): RsaKeyPair?

    /**
     * List all stored key IDs.
     */
    suspend fun listKeyIds(): List<String>

    /**
     * Delete a key by ID.
     */
    suspend fun deleteKey(keyId: String)
}

data class RsaKeyPair(
    val publicKey: ByteArray,
    val privateKey: ByteArray
)
