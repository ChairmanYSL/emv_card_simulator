package com.szzt.cardsimulator.emv.impl

import com.szzt.cardsimulator.emv.api.CertificateProvider
import com.szzt.cardsimulator.emv.model.Certificates
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import timber.log.Timber
import java.util.Base64

/**
 * Default certificate provider.
 *
 * Certificate generation/import is not yet fully implemented; instead of
 * throwing [UnsupportedOperationException] (which would crash the EMV kernel
 * when offline data authentication runs), every operation returns an empty
 * [Certificates] and logs a warning. Callers must check for empty certificate
 * data and treat the card as "no ODA" rather than failing the transaction.
 */
class DefaultCertificateProvider : CertificateProvider {

    override suspend fun loadCertificateChain(profileId: String): Certificates {
        Timber.w("loadCertificateChain: certificate storage not implemented for profile '$profileId'; returning empty chain")
        return EMPTY_CERTIFICATES
    }

    override suspend fun generateTestCertificateChain(pan: String, caKeyId: String?): Certificates {
        Timber.w("generateTestCertificateChain: certificate generation not implemented; returning empty chain")
        return EMPTY_CERTIFICATES.copy(pan = pan)
    }

    override suspend fun importCertificateChain(certificateData: ByteArray): Certificates {
        // Attempt to parse a JSON certificate bundle:
        //   { "ca": "base64", "issuer": "base64", "icc": "base64",
        //     "icc_private_key": "base64", "pan": "optional" }
        // On any parse error, degrade to an empty chain instead of throwing.
        return try {
            val obj = Json { ignoreUnknownKeys = true }
                .parseToJsonElement(String(certificateData, Charsets.UTF_8))
                .jsonObject

            Certificates(
                caPublicKey = decodeBase64(obj["ca"]?.jsonPrimitive?.content),
                issuerCertificate = decodeBase64(obj["issuer"]?.jsonPrimitive?.content),
                iccCertificate = decodeBase64(obj["icc"]?.jsonPrimitive?.content),
                iccPrivateKey = decodeBase64(obj["icc_private_key"]?.jsonPrimitive?.content),
                pan = obj["pan"]?.jsonPrimitive?.content
            )
        } catch (e: Exception) {
            Timber.e(e, "importCertificateChain: failed to parse certificate data; returning empty chain")
            EMPTY_CERTIFICATES
        }
    }

    private fun decodeBase64(value: String?): ByteArray {
        if (value.isNullOrBlank()) return ByteArray(0)
        return try {
            Base64.getDecoder().decode(value)
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Invalid base64 in certificate bundle")
            ByteArray(0)
        }
    }

    private companion object {
        val EMPTY_CERTIFICATES = Certificates(
            caPublicKey = ByteArray(0),
            issuerCertificate = ByteArray(0),
            iccCertificate = ByteArray(0),
            iccPrivateKey = ByteArray(0),
            pan = null
        )
    }
}
