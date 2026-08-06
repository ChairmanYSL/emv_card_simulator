package com.szzt.cardsimulator.log.impl

import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import com.szzt.cardsimulator.log.api.ApduDirection
import com.szzt.cardsimulator.log.api.ApduLogEntry
import com.szzt.cardsimulator.log.api.ApduLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * In-memory APDU logger using SharedFlow for real-time observation.
 */
class InMemoryApduLogger : ApduLogger {

    private val _logs = MutableSharedFlow<ApduLogEntry>(replay = 200)

    override fun observe(): Flow<ApduLogEntry> = _logs.asSharedFlow()

    override suspend fun log(command: ApduCommand, response: ApduResponse, profileName: String) {
        val now = System.currentTimeMillis()

        // Emit command entry
        _logs.emit(
            ApduLogEntry(
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
        )

        // Emit response entry
        _logs.emit(
            ApduLogEntry(
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
        )
    }

    override suspend fun clear() {
        // SharedFlow does not support clearing replay cache directly.
        // A new instance is created on DI module refresh.
    }

    private fun formatHex(data: ByteArray): String {
        return data.joinToString(" ") { "%02X".format(it) }
    }
}
