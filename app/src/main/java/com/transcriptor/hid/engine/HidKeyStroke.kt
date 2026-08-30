package com.transcriptor.hid.engine

/**
 * Representation of a single keystroke action consisting of a modifier mask
 * and a USB HID Usage ID (Page 0x07).
 */
data class HidKeyStroke(
    val modifierMask: Byte, // 0x00=NONE, 0x02=SHIFT, 0x40=ALT_GR, etc.
    val usageId: Byte       // HID Usage ID (Page 0x07)
) {
    /**
     * Converts this keystroke to an 8-byte HID key-down report.
     */
    fun toKeyDownReport(): HidReport =
        HidReport(
            modifier = modifierMask,
            reserved = 0,
            keyCodes = byteArrayOf(usageId, 0, 0, 0, 0, 0)
        )

    companion object {
        /**
         * Key release (all zeroes) report.
         */
        val RELEASE_REPORT: HidReport = HidReport.empty()
    }
}
