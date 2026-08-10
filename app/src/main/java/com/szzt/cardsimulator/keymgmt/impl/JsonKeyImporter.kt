package com.szzt.cardsimulator.keymgmt.impl

import com.szzt.cardsimulator.keymgmt.api.KeyImporter
import com.szzt.cardsimulator.keymgmt.api.KeyImportResult
import com.szzt.cardsimulator.keymgmt.api.KeyStore
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Base64

/**
 * JSON key importer.
 *
 * Parses a structured JSON file containing key material and imports it into
 * the [KeyStore]. Import is two-phase: the whole document is parsed and
 * validated first; only if every entry is well-formed are the keys written,
 * so a malformed document never leaves a partially imported state behind.
 */
class JsonKeyImporter(
    private val keyStore: KeyStore,
    private val certificateProvider: com.szzt.cardsimulator.emv.api.CertificateProvider
) : KeyImporter {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun importFromJson(jsonString: String): KeyImportResult {
        val root = parseRoot(jsonString) ?: return KeyImportResult(
            symmetricKeysImported = 0,
            rsaKeyPairsImported = 0,
            certificatesImported = false,
            errors = listOf("Invalid JSON: could not parse document")
        )

        val errors = mutableListOf<String>()
        val symmetric = mutableListOf<Pair<String, ByteArray>>()
        val rsa = mutableListOf<Triple<String, ByteArray, ByteArray>>()

        // --- Phase 1: parse & validate everything (no writes yet) ---
        parseSymmetricKeys(root, symmetric, errors)
        parseRsaKeys(root, rsa, errors)

        // Certificates are imported separately; a failure there is reported
        // but does not abort key imports.
        val certsImported = parseCertificates(root, errors)

        // --- Phase 2: write only if nothing failed validation ---
        if (errors.isNotEmpty()) {
            return KeyImportResult(0, 0, certsImported, errors)
        }

        symmetric.forEach { (id, data) -> keyStore.storeSymmetricKey(id, data) }
        rsa.forEach { (id, pub, priv) -> keyStore.storeRsaKeyPair(id, pub, priv) }

        return KeyImportResult(
            symmetricKeysImported = symmetric.size,
            rsaKeyPairsImported = rsa.size,
            certificatesImported = certsImported,
            errors = emptyList()
        )
    }

    private fun parseRoot(jsonString: String): JsonObject? {
        return try {
            json.parseToJsonElement(jsonString).jsonObject
        } catch (e: Exception) {
            null
        }
    }

    private fun parseSymmetricKeys(
        root: JsonObject,
        out: MutableList<Pair<String, ByteArray>>,
        errors: MutableList<String>
    ) {
        val element = root["symmetric_keys"] ?: return
        if (element !is JsonArray) {
            errors.add("'symmetric_keys' must be an array")
            return
        }
        element.forEachIndexed { index, keyObj ->
            if (keyObj !is JsonObject) {
                errors.add("symmetric_keys[$index]: expected object")
                return@forEachIndexed
            }
            val id = (keyObj["id"] as? JsonPrimitive)?.content
            val keyData = (keyObj["key_data"] as? JsonPrimitive)?.content
            if (id == null || keyData == null) {
                errors.add("symmetric_keys[$index]: missing 'id' or 'key_data'")
                return@forEachIndexed
            }
            if (!KEY_ID_PATTERN.matches(id)) {
                errors.add("symmetric_keys[$index]: invalid id '$id'")
                return@forEachIndexed
            }
            val decoded = try {
                Base64.getDecoder().decode(keyData)
            } catch (e: IllegalArgumentException) {
                errors.add("symmetric_keys[$index] ('$id'): key_data is not valid base64")
                return@forEachIndexed
            }
            out.add(id to decoded)
        }
    }

    private fun parseRsaKeys(
        root: JsonObject,
        out: MutableList<Triple<String, ByteArray, ByteArray>>,
        errors: MutableList<String>
    ) {
        val element = root["rsa_keys"] ?: return
        if (element !is JsonArray) {
            errors.add("'rsa_keys' must be an array")
            return
        }
        element.forEachIndexed { index, keyObj ->
            if (keyObj !is JsonObject) {
                errors.add("rsa_keys[$index]: expected object")
                return@forEachIndexed
            }
            val id = (keyObj["id"] as? JsonPrimitive)?.content
            val publicKey = (keyObj["public_key"] as? JsonPrimitive)?.content
            val privateKey = (keyObj["private_key"] as? JsonPrimitive)?.content
            if (id == null || publicKey == null || privateKey == null) {
                errors.add("rsa_keys[$index]: missing 'id', 'public_key' or 'private_key'")
                return@forEachIndexed
            }
            if (!KEY_ID_PATTERN.matches(id)) {
                errors.add("rsa_keys[$index]: invalid id '$id'")
                return@forEachIndexed
            }
            val publicBytes = try {
                Base64.getDecoder().decode(publicKey)
            } catch (e: IllegalArgumentException) {
                errors.add("rsa_keys[$index] ('$id'): public_key is not valid base64")
                return@forEachIndexed
            }
            val privateBytes = try {
                Base64.getDecoder().decode(privateKey)
            } catch (e: IllegalArgumentException) {
                errors.add("rsa_keys[$index] ('$id'): private_key is not valid base64")
                return@forEachIndexed
            }
            out.add(Triple(id, publicBytes, privateBytes))
        }
    }

    private suspend fun parseCertificates(root: JsonObject, errors: MutableList<String>): Boolean {
        val element = root["certificates"] ?: return false
        if (element !is JsonObject) {
            errors.add("'certificates' must be an object")
            return false
        }
        return try {
            certificateProvider.importCertificateChain(element.toString().toByteArray())
            true
        } catch (e: Exception) {
            errors.add("Failed to import certificates: ${e.message}")
            false
        }
    }

    private companion object {
        // Must stay consistent with FileBasedKeyStore.validateKeyId
        val KEY_ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,64}$")
    }
}
