package com.transcriptor.hid.motion

import com.google.common.truth.Truth.assertThat
import org.junit.Test

/**
 * Unit tests for [GyroAirMouseEngine] verifying ballistics acceleration and clamping.
 */
class GyroAirMouseEngineTest {

    @Test
    fun testBallisticDelta_deadbandZero() {
        val delta = GyroAirMouseEngine.calculateBallisticDelta(0.005f, 1.2f)
        assertThat(delta).isEqualTo(0.toByte())
    }

    @Test
    fun testBallisticDelta_fineMotion() {
        val delta = GyroAirMouseEngine.calculateBallisticDelta(0.1f, 1.0f)
        // 0.1 * 18 + 0.001 * 35 = 1.835 -> 1
        assertThat(delta.toInt()).isGreaterThan(0)
        assertThat(delta.toInt()).isLessThan(10)
    }

    @Test
    fun testBallisticDelta_fastSweepCubicBoost() {
        val slowDelta = GyroAirMouseEngine.calculateBallisticDelta(0.5f, 1.0f)
        val fastDelta = GyroAirMouseEngine.calculateBallisticDelta(2.0f, 1.0f)

        // Cubic component should cause disproportionately faster speed
        assertThat(fastDelta.toInt()).isGreaterThan(slowDelta.toInt() * 4)
    }

    @Test
    fun testBallisticDelta_clampsToSignedByteBounds() {
        val hugeDelta = GyroAirMouseEngine.calculateBallisticDelta(15.0f, 2.0f)
        assertThat(hugeDelta.toInt()).isEqualTo(127)

        val hugeNegativeDelta = GyroAirMouseEngine.calculateBallisticDelta(-15.0f, 2.0f)
        assertThat(hugeNegativeDelta.toInt()).isEqualTo(-127)
    }
}
