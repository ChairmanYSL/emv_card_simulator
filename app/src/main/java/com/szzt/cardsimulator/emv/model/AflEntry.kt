package com.szzt.cardsimulator.emv.model

/**
 * Application File Locator entry.
 *
 * Each AFL entry specifies:
 *  - SFI (Short File Identifier)
 *  - First record
 *  - Last record
 *  - Number of records involved in offline data authentication
 */
data class AflEntry(
    /** Short File Identifier (bits 8-3 of byte 1). */
    val sfi: Int,
    /** First record to read (byte 2). */
    val firstRecord: Int,
    /** Last record to read (byte 3). */
    val lastRecord: Int,
    /** Number of records involved in offline data auth (byte 4). */
    val recordsInvolvedInOda: Int
) {
    companion object {
        /**
         * Parse AFL bytes (4×N bytes) into a list of entries.
         */
        fun parse(aflBytes: ByteArray): List<AflEntry> {
            val entries = mutableListOf<AflEntry>()
            for (i in aflBytes.indices step 4) {
                if (i + 3 >= aflBytes.size) break
                entries.add(
                    AflEntry(
                        sfi = (aflBytes[i].toInt() and 0xFF) shr 3,
                        firstRecord = aflBytes[i + 1].toInt() and 0xFF,
                        lastRecord = aflBytes[i + 2].toInt() and 0xFF,
                        recordsInvolvedInOda = aflBytes[i + 3].toInt() and 0xFF
                    )
                )
            }
            return entries
        }
    }
}
