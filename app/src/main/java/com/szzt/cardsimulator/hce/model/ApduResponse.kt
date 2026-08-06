package com.szzt.cardsimulator.hce.model

/**
 * Represents an APDU response to be sent back to the reader/POS.
 *
 * @param sw1  Status word byte 1
 * @param sw2  Status word byte 2
 * @param data Response data (null if no data)
 */
data class ApduResponse(
    val sw1: Int,
    val sw2: Int,
    val data: ByteArray? = null
) {
    /**
     * Full response bytes including trailing status word.
     */
    val raw: ByteArray by lazy {
        val dataLen = data?.size ?: 0
        val total = dataLen + 2
        ByteArray(total).apply {
            data?.copyInto(this, 0)
            this[dataLen] = sw1.toByte()
            this[dataLen + 1] = sw2.toByte()
        }
    }

    val isSuccess: Boolean
        get() = sw1 == 0x90 && sw2 == 0x00

    companion object {
        val SUCCESS = ApduResponse(sw1 = 0x90, sw2 = 0x00)
        val FILE_NOT_FOUND = ApduResponse(sw1 = 0x6A, sw2 = 0x82)
        val CONDITIONS_NOT_SATISFIED = ApduResponse(sw1 = 0x69, sw2 = 0x85)
        val CLA_NOT_SUPPORTED = ApduResponse(sw1 = 0x6E, sw2 = 0x00)
        val INS_NOT_SUPPORTED = ApduResponse(sw1 = 0x6D, sw2 = 0x00)
        val WRONG_LENGTH = ApduResponse(sw1 = 0x67, sw2 = 0x00)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApduResponse) return false
        return sw1 == other.sw1 && sw2 == other.sw2 &&
                (data?.contentEquals(other.data) ?: (other.data == null))
    }

    override fun hashCode(): Int {
        var result = sw1
        result = 31 * result + sw2
        result = 31 * result + (data?.contentHashCode() ?: 0)
        return result
    }
}
