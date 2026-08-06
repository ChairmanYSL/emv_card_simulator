package com.szzt.cardsimulator.emv.api

import com.szzt.cardsimulator.emv.model.Certificates

/**
 * Provides the certificate chain and private key for EMV offline data authentication (SDA/DDA/CDA).
 *
 * Certificate chain hierarchy:
 *   CA Public Key → Issuer Public Key Certificate → ICC Public Key Certificate → ICC Private Key
 */
interface CertificateProvider {

    /**
     * Load the certificate chain for the active card profile.
     *
     * @param profileId  Identifier linking to the profile's certificate material.
     * @return The full certificate chain.
     */
    suspend fun loadCertificateChain(profileId: String): Certificates

    /**
     * Generate a self-signed test certificate chain.
     *
     * @param pan     PAN to embed in the certificate data.
     * @param caKeyId Optional CA key identifier.
     * @return Generated certificates.
     */
    suspend fun generateTestCertificateChain(pan: String, caKeyId: String? = null): Certificates

    /**
     * Import a certificate chain from external data.
     *
     * @param certificateData JSON or BER-encoded certificate data.
     * @return Parsed certificates.
     */
    suspend fun importCertificateChain(certificateData: ByteArray): Certificates
}
