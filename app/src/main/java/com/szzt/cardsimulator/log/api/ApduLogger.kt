package com.szzt.cardsimulator.log.api

import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import kotlinx.coroutines.flow.Flow

/**
 * In-memory APDU logger for real-time debugging.
 * Logs are NOT persisted — cleared on app process death.
 */
interface ApduLogger {

    /**
     * Log an APDU exchange.
     *
     * @param command     The incoming APDU command.
     * @param response    The generated APDU response.
     * @param profileName Name of the active card profile (for context).
     */
    suspend fun log(command: ApduCommand, response: ApduResponse, profileName: String)

    /**
     * Observe the log entries as a Flow.
     * Each new entry is emitted in real-time.
     */
    fun observe(): Flow<ApduLogEntry>

    /**
     * Clear all log entries.
     */
    suspend fun clear()
}

data class ApduLogEntry(
    val timestamp: Long,
    val direction: ApduDirection,
    val cla: Int,
    val ins: Int,
    val p1: Int,
    val p2: Int,
    val data: ByteArray?,
    val sw1: Int?,
    val sw2: Int?,
    val profileName: String,
    val hexDump: String
)

enum class ApduDirection { COMMAND, RESPONSE }
