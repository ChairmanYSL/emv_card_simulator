package com.szzt.cardsimulator.emv.model

/**
 * BER-TLV data object as found in EMV data.
 */
data class TlvObject(
    val tag: Int,
    val length: Int,
    val value: ByteArray,
    val constructed: Boolean
) {
    /** Tag bytes for multi-byte tags. */
    val tagBytes: Int = tag

    /**
     * Convenience: encode this TLV back to bytes.
     */
    fun encode(): ByteArray {
        val tagEnc = encodeTag()
        val lenEnc = encodeLength()
        return tagEnc + lenEnc + value
    }

    private fun encodeTag(): ByteArray {
        // Simple: assume tag fits in 1-2 bytes
        return if (tag > 0xFF) {
            byteArrayOf(
                ((tag shr 8) and 0xFF).toByte(),
                (tag and 0xFF).toByte()
            )
        } else {
            byteArrayOf((tag and 0xFF).toByte())
        }
    }

    private fun encodeLength(): ByteArray {
        val len = value.size
        return when {
            len < 0x80 -> byteArrayOf(len.toByte())
            len <= 0xFF -> byteArrayOf(0x81.toByte(), len.toByte())
            else -> byteArrayOf(0x82.toByte(), ((len shr 8) and 0xFF).toByte(), (len and 0xFF).toByte())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TlvObject) return false
        return tag == other.tag && length == other.length &&
                constructed == other.constructed && value.contentEquals(other.value)
    }

    override fun hashCode(): Int {
        var result = tag
        result = 31 * result + length
        result = 31 * result + constructed.hashCode()
        result = 31 * result + value.contentHashCode()
        return result
    }
}
