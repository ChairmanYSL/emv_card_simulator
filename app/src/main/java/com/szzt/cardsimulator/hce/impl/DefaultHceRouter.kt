package com.szzt.cardsimulator.hce.impl

import com.szzt.cardsimulator.emv.api.EmvKernel
import com.szzt.cardsimulator.hce.api.HceRouter
import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse
import timber.log.Timber

/**
 * Default HCE router.
 * Routes APDU commands to the configured EMV kernel.
 */
class DefaultHceRouter(
    private val emvKernel: EmvKernel
) : HceRouter {

    override suspend fun route(command: ApduCommand): ApduResponse {
        return try {
            emvKernel.processApdu(command)
        } catch (e: Exception) {
            Timber.e(e, "Error processing APDU")
            ApduResponse(sw1 = 0x6F, sw2 = 0x00) // Internal error
        }
    }

    override fun reset() {
        emvKernel.reset()
    }
}
