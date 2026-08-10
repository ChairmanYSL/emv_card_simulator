package com.szzt.cardsimulator.log.api

import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import kotlinx.coroutines.flow.Flow

/**
 * In-memory APDU logger for real-time debugging.
 * Logs are NOT persisted — cleared on app process death.
 *
 * Implementations must be safe to call from the NFC callback thread:
 * [log] is non-suspending and must never block on back-pressure.
 */
interface ApduLogger {

    /**
     * Log an APDU exchange (command + response pair).
     *
     * Non-suspending and non-blocking so it can be invoked from the NFC
     * callback thread without risking transaction timeouts.
     *
     * @param command     The incoming APDU command.
     * @param response    The generated APDU response.
     * @param profileName Name of the active card profile (for context).
     */
    fun log(command: ApduCommand, response: ApduResponse, profileName: String)

    /**
     * Observe the log entries as a Flow.
     * Emits the full list on every change; the latest value is replayed
     * to new collectors (so re-subscribing never loses history).
     */
    fun observe(): Flow<List<ApduLogEntry>>

    /**
     * Clear all log entries.
     */
    fun clear()
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
