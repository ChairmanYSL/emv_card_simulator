package com.szzt.cardsimulator.hce.impl

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.szzt.cardsimulator.hce.api.HceRouter
import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import com.szzt.cardsimulator.log.api.ApduLogger
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber

/**
 * Host-based Card Emulation service.
 *
 * Entry point for NFC communication. Receives APDU commands from the contactless
 * reader/POS and delegates to the [HceRouter] for processing.
 */
class CardEmulationService : HostApduService(), KoinComponent {

    private val router: HceRouter by lazy { get<HceRouter>() }
    private val logger: ApduLogger by lazy { get<ApduLogger>() }

    override fun onDeactivated(reason: Int) {
        Timber.d("NFC field deactivated, reason: $reason")
        router.reset()
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val command = parseCommand(commandApdu)
        Timber.d("CMD: CLA=${String.format("%02X", command.cla)} INS=${String.format("%02X", command.ins)} P1=${String.format("%02X", command.p1)} P2=${String.format("%02X", command.p2)}")

        val response = kotlinx.coroutines.runBlocking {
            router.route(command)
        }

        Timber.d("RSP: SW1=${String.format("%02X", response.sw1)} SW2=${String.format("%02X", response.sw2)} dataLen=${response.data?.size ?: 0}")

        // Log the exchange (fire-and-forget to avoid blocking NFC callback)
        kotlinx.coroutines.runBlocking {
            logger.log(command, response, "unknown") // TODO: inject profile name
        }

        return response.raw
    }

    private fun parseCommand(apdu: ByteArray): ApduCommand {
        require(apdu.size >= 4) { "APDU must have at least CLA, INS, P1, P2" }

        val cla = apdu[0].toInt() and 0xFF
        val ins = apdu[1].toInt() and 0xFF
        val p1 = apdu[2].toInt() and 0xFF
        val p2 = apdu[3].toInt() and 0xFF

        return when (apdu.size) {
            4 -> ApduCommand(cla, ins, p1, p2) // Case 1: no data, no Le
            5 -> {
                // Case 2: Le only
                ApduCommand(cla, ins, p1, p2, le = apdu[4].toInt() and 0xFF)
            }
            else -> {
                // Case 3 or 4
                val lc = apdu[4].toInt() and 0xFF
                val dataEnd = 5 + lc
                val data = apdu.copyOfRange(5, dataEnd)
                val le = if (apdu.size > dataEnd) apdu[dataEnd].toInt() and 0xFF else 0
                ApduCommand(cla, ins, p1, p2, lc = lc, data = data, le = le)
            }
        }
    }
}
