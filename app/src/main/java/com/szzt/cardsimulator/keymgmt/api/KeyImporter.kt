package com.szzt.cardsimulator.keymgmt.api

/**
 * Imports key material from external sources (JSON file, etc.).
 */
interface KeyImporter {

    /**
     * Import keys from a JSON string.
     *
     * Expected JSON structure:
     * ```json
     * {
     *   "symmetric_keys": [
     *     { "id": "key1", "key_data": "base64..." }
     *   ],
     *   "rsa_keys": [
     *     { "id": "rsa1", "public_key": "base64...", "private_key": "base64..." }
     *   ],
     *   "certificates": {
     *     "ca": "base64...",
     *     "issuer": "base64...",
     *     "icc": "base64...",
     *     "icc_private_key": "base64..."
     *   }
     * }
     * ```
     *
     * @param json  JSON key material string.
     * @return Import result with counts.
     */
    suspend fun importFromJson(json: String): KeyImportResult
}

data class KeyImportResult(
    val symmetricKeysImported: Int,
    val rsaKeyPairsImported: Int,
    val certificatesImported: Boolean,
    val errors: List<String> = emptyList()
)
