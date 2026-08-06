package com.szzt.cardsimulator.keymgmt.impl

import com.szzt.cardsimulator.keymgmt.api.KeyImporter
import com.szzt.cardsimulator.keymgmt.api.KeyImportResult
import com.szzt.cardsimulator.keymgmt.api.KeyStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * JSON key importer.
 * Parses a structured JSON file containing key material and imports it
 * into the [KeyStore].
 */
class JsonKeyImporter(
    private val keyStore: KeyStore,
    private val certificateProvider: com.szzt.cardsimulator.emv.api.CertificateProvider
) : KeyImporter {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun importFromJson(jsonString: String): KeyImportResult {
        val element = json.parseToJsonElement(jsonString)
        val obj = element.jsonObject
        val errors = mutableListOf<String>()
        var symmetricCount = 0
        var rsaCount = 0
        var certsImported = false

        // Import symmetric keys
        obj["symmetric_keys"]?.jsonArray?.forEach { keyObj ->
            try {
                val id = keyObj.jsonObject["id"]!!.jsonPrimitive.content
                val keyData = Base64.getDecoder().decode(
                    keyObj.jsonObject["key_data"]!!.jsonPrimitive.content
                )
                keyStore.storeSymmetricKey(id, keyData)
                symmetricCount++
            } catch (e: Exception) {
                errors.add("Failed to import symmetric key: ${e.message}")
            }
        }

        // Import RSA keys
        obj["rsa_keys"]?.jsonArray?.forEach { keyObj ->
            try {
                val id = keyObj.jsonObject["id"]!!.jsonPrimitive.content
                val publicKey = Base64.getDecoder().decode(
                    keyObj.jsonObject["public_key"]!!.jsonPrimitive.content
                )
                val privateKey = Base64.getDecoder().decode(
                    keyObj.jsonObject["private_key"]!!.jsonPrimitive.content
                )
                keyStore.storeRsaKeyPair(id, publicKey, privateKey)
                rsaCount++
            } catch (e: Exception) {
                errors.add("Failed to import RSA key: ${e.message}")
            }
        }

        // Import certificates
        obj["certificates"]?.jsonObject?.let { certs ->
            try {
                val certData = certs.toString()
                certificateProvider.importCertificateChain(certData.toByteArray())
                certsImported = true
            } catch (e: Exception) {
                errors.add("Failed to import certificates: ${e.message}")
            }
        }

        return KeyImportResult(
            symmetricKeysImported = symmetricCount,
            rsaKeyPairsImported = rsaCount,
            certificatesImported = certsImported,
            errors = errors
        )
    }
}
