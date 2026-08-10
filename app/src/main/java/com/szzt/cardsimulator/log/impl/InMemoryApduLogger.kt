package com.szzt.cardsimulator.log.impl

import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import com.szzt.cardsimulator.log.api.ApduDirection
import com.szzt.cardsimulator.log.api.ApduLogEntry
import com.szzt.cardsimulator.log.api.ApduLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * In-memory APDU logger.
 *
 * Holds the log as an immutable list in a [MutableStateFlow] so that:
 *  - [clear] actually works (replaces the list with an empty one);
 *  - new collectors replay the full current history (no 200-entry rollback);
 *  - [log] is non-suspending and never blocks, so it is safe on the NFC
 *    callback thread.
 *
 * The log is capped at [MAX_ENTRIES] to bound memory growth.
 */
class InMemoryApduLogger : ApduLogger {

    private val _logs = MutableStateFlow<List<ApduLogEntry>>(emptyList())

    override fun observe(): Flow<List<ApduLogEntry>> = _logs.asStateFlow()

    override fun log(command: ApduCommand, response: ApduResponse, profileName: String) {
        val now = System.currentTimeMillis()

        val commandEntry = ApduLogEntry(
            timestamp = now,
            direction = ApduDirection.COMMAND,
            cla = command.cla,
            ins = command.ins,
            p1 = command.p1,
            p2 = command.p2,
            data = command.data,
            sw1 = null,
            sw2 = null,
            profileName = profileName,
            hexDump = formatHex(command.raw)
        )

        val responseEntry = ApduLogEntry(
            timestamp = now,
            direction = ApduDirection.RESPONSE,
            cla = command.cla,
            ins = command.ins,
            p1 = command.p1,
            p2 = command.p2,
            data = response.data,
            sw1 = response.sw1,
            sw2 = response.sw2,
            profileName = profileName,
            hexDump = formatHex(response.raw)
        )

        _logs.update { current ->
            (current + commandEntry + responseEntry).takeLast(MAX_ENTRIES)
        }
    }

    override fun clear() {
        _logs.value = emptyList()
    }

    private fun formatHex(data: ByteArray): String {
        return data.joinToString(" ") { "%02X".format(it) }
    }

    private companion object {
        const val MAX_ENTRIES = 1000
    }
}
