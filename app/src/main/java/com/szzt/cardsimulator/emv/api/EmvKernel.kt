package com.szzt.cardsimulator.emv.api

import com.szzt.cardsimulator.hce.model.ApduCommand
import com.szzt.cardsimulator.hce.model.ApduResponse

/**
 * EMV transaction kernel.
 *
 * Maintains a state machine over the EMV transaction lifecycle:
 *   IDLE → SELECTED → GPO_DONE → DATA_READ → AC_GENERATED → COMPLETE
 *
 * Implementations are expected to be driven by the active [CardProfile] configuration.
 */
interface EmvKernel {

    /**
     * Current transaction state.
     */
    val state: EmvKernelState

    /**
     * The AID currently selected, if any.
     */
    val selectedAid: ByteArray?

    /**
     * Process a single APDU command and return the response.
     * This drives the internal state machine forward.
     */
    suspend fun processApdu(command: ApduCommand): ApduResponse

    /**
     * Reset the kernel to IDLE state (e.g., on field removal or profile switch).
     */
    fun reset()
}

/**
 * EMV kernel transaction state.
 */
enum class EmvKernelState {
    /** No application selected yet. */
    IDLE,

    /** PPSE or AID selected, awaiting GPO. */
    SELECTED,

    /** GPO processed, awaiting READ RECORD. */
    GPO_DONE,

    /** All records read, awaiting GENERATE AC. */
    DATA_READ,

    /** AC generated; transaction ready for completion. */
    AC_GENERATED,

    /** Transaction complete (ready for next command or field-off). */
    COMPLETE
}
