package com.szzt.cardsimulator.hce.impl

import android.nfc.cardemulation.HostApduService
import android.os.Bundle
import com.szzt.cardsimulator.emv.api.EmvKernel
import com.szzt.cardsimulator.hce.api.HceRouter
import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import com.szzt.cardsimulator.log.api.ApduLogger
import com.szzt.cardsimulator.profile.api.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.get
import timber.log.Timber

/**
 * Host-based Card Emulation service.
 *
 * Entry point for NFC communication. Receives APDU commands from the contactless
 * reader/POS and delegates to the [HceRouter] for processing.
 *
 * The active card profile is loaded from [ProfileRepository] and pushed into the
 * [EmvKernel] whenever it changes (e.g. user switches profile in the UI).
 */
class CardEmulationService : HostApduService(), KoinComponent {

    private val router: HceRouter by lazy { get<HceRouter>() }
    private val kernel: EmvKernel by lazy { get<EmvKernel>() }
    private val logger: ApduLogger by lazy { get<ApduLogger>() }
    private val profileRepository: ProfileRepository by lazy { get<ProfileRepository>() }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        // Keep the EMV kernel in sync with the active profile. Room Flow replays
        // the current value on subscription, so this also loads the profile at startup.
        serviceScope.launch {
            profileRepository.observeActive().collectLatest { profile ->
                if (profile != null) {
                    kernel.setProfile(profile)
                    Timber.d("HCE: active profile loaded: ${profile.name}")
                } else {
                    kernel.reset()
                }
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onDeactivated(reason: Int) {
        Timber.d("NFC field deactivated, reason: $reason")
        router.reset()
    }

    override fun processCommandApdu(commandApdu: ByteArray, extras: Bundle?): ByteArray {
        val command = parseCommand(commandApdu)
        Timber.d("CMD: CLA=${String.format("%02X", command.cla)} INS=${String.format("%02X", command.ins)} P1=${String.format("%02X", command.p1)} P2=${String.format("%02X", command.p2)}")

        // The NFC stack calls this synchronously and expects a response before
        // returning, so the (mostly CPU-bound) kernel processing must complete
        // inline. The logger is non-blocking (tryEmit), so logging cannot stall
        // the NFC callback.
        val response = kotlinx.coroutines.runBlocking {
            router.route(command)
        }

        Timber.d("RSP: SW1=${String.format("%02X", response.sw1)} SW2=${String.format("%02X", response.sw2)} dataLen=${response.data?.size ?: 0}")

        // Fire-and-forget log (non-suspending emit).
        logger.log(command, response, kernel.activeProfile?.name ?: "unknown")

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
