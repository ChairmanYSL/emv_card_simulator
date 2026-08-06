package com.szzt.cardsimulator.emv.impl

import com.szzt.cardsimulator.emv.api.CertificateProvider
import com.szzt.cardsimulator.emv.model.Certificates

/**
 * Default certificate provider.
 * Supports loading from KeyStore and generating self-signed test certificates.
 */
class DefaultCertificateProvider : CertificateProvider {

    override suspend fun loadCertificateChain(profileId: String): Certificates {
        // TODO: Load from KeyStore by profileId
        throw UnsupportedOperationException("Loading certificates from KeyStore not yet implemented")
    }

    override suspend fun generateTestCertificateChain(pan: String, caKeyId: String?): Certificates {
        // TODO: Generate a self-signed test certificate chain using Bouncy Castle or JCE
        // For now, return placeholder dummy data
        return Certificates(
            caPublicKey = ByteArray(128), // 1024-bit RSA placeholder
            issuerCertificate = ByteArray(128),
            iccCertificate = ByteArray(128),
            iccPrivateKey = ByteArray(128),
            pan = pan
        )
    }

    override suspend fun importCertificateChain(certificateData: ByteArray): Certificates {
        // TODO: Parse BER/DER certificate data
        throw UnsupportedOperationException("Certificate import not yet implemented")
    }
}
