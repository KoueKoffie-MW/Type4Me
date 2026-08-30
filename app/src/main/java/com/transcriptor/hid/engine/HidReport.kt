package com.transcriptor.hid.engine

/**
 * Standard 8-byte USB HID Keyboard Input Report.
 * Byte 0: Modifier mask (e.g. Shift 0x02, AltGr 0x40)
 * Byte 1: Reserved (0x00)
 * Bytes 2-7: Up to 6 simultaneous key usage codes (Page 0x07)
 */
data class HidReport(
    val modifier: Byte = 0,
    val reserved: Byte = 0,
    val keyCodes: ByteArray = ByteArray(6)
) {
    fun toByteArray(): ByteArray = byteArrayOf(modifier, reserved, *keyCodes)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as HidReport
        if (modifier != other.modifier) return false
        if (reserved != other.reserved) return false
        if (!keyCodes.contentEquals(other.keyCodes)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = modifier.toInt()
        result = 31 * result + reserved.toInt()
        result = 31 * result + keyCodes.contentHashCode()
        return result
    }

    companion object {
        fun empty(): HidReport = HidReport()

        fun fromKey(modifier: Byte, usageId: Byte): HidReport =
            HidReport(modifier = modifier, keyCodes = byteArrayOf(usageId, 0, 0, 0, 0, 0))
    }
}
