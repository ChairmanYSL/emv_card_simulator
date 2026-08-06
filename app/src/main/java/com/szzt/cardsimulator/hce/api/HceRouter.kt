package com.szzt.cardsimulator.hce.api

import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse

/**
 * Routes an incoming APDU command to the appropriate EMV kernel handler.
 * Implementations may select the kernel based on AID selection state.
 */
interface HceRouter {

    /**
     * Process an incoming APDU command and return the response.
     * The router maintains selection state internally.
     */
    suspend fun route(command: ApduCommand): ApduResponse

    /**
     * Reset internal state (e.g., on NFC field removal).
     */
    fun reset()
}
