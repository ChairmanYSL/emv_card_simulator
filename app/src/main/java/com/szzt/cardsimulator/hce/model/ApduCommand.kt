package com.szzt.cardsimulator.hce.model

/**
 * Represents an APDU command received from the reader/POS.
 *
 * @param cla  Class byte
 * @param ins  Instruction byte
 * @param p1   Parameter 1
 * @param p2   Parameter 2
 * @param lc   Length of command data (0 if no data)
 * @param data Command data (null if no data)
 * @param le   Expected length of response (0 if default)
 */
data class ApduCommand(
    val cla: Int,
    val ins: Int,
    val p1: Int,
    val p2: Int,
    val lc: Int = 0,
    val data: ByteArray? = null,
    val le: Int = 0
) {
    /**
     * Convenience: full APDU bytes as received.
     */
    val raw: ByteArray by lazy {
        val builder = ByteArray(4 + lc + if (le > 0) 1 else 0)
        builder[0] = cla.toByte()
        builder[1] = ins.toByte()
        builder[2] = p1.toByte()
        builder[3] = p2.toByte()
        if (lc > 0) {
            builder[4] = lc.toByte()
            data?.copyInto(builder, 5)
        }
        if (le > 0) {
            builder[builder.size - 1] = le.toByte()
        }
        builder
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ApduCommand) return false
        return cla == other.cla && ins == other.ins &&
                p1 == other.p1 && p2 == other.p2 &&
                lc == other.lc && le == other.le &&
                (data?.contentEquals(other.data) ?: (other.data == null))
    }

    override fun hashCode(): Int {
        var result = cla
        result = 31 * result + ins
        result = 31 * result + p1
        result = 31 * result + p2
        result = 31 * result + lc
        result = 31 * result + (data?.contentHashCode() ?: 0)
        result = 31 * result + le
        return result
    }
}
