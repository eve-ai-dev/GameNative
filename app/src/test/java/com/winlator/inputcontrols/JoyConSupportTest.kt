package com.winlator.inputcontrols

import android.view.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JoyConSupportTest {
    @Test
    fun `combines retained state from both controller halves`() {
        val left = GamepadState().apply {
            thumbLX = -0.75f
            dpad[0] = true
            setPressed(ExternalController.IDX_BUTTON_L1.toInt(), true)
        }
        val right = GamepadState().apply {
            thumbRX = 0.6f
            triggerR = 1f
            setPressed(ExternalController.IDX_BUTTON_A.toInt(), true)
        }

        val combined = GamepadState.combine(listOf(left, right))

        assertEquals(-0.75f, combined.thumbLX, 0f)
        assertEquals(0.6f, combined.thumbRX, 0f)
        assertEquals(1f, combined.triggerR, 0f)
        assertTrue(combined.dpad[0])
        assertTrue(combined.isPressed(ExternalController.IDX_BUTTON_L1.toInt()))
        assertTrue(combined.isPressed(ExternalController.IDX_BUTTON_A.toInt()))
    }

    @Test
    fun `combination uses the strongest axis contribution`() {
        val first = GamepadState().apply { thumbLX = -0.8f }
        val second = GamepadState().apply { thumbLX = 0.25f }

        assertEquals(-0.8f, GamepadState.combine(listOf(first, second)).thumbLX, 0f)
    }

    @Test
    fun `recombination drops released and disconnected source state`() {
        val left = GamepadState().apply {
            thumbLX = -0.8f
            setPressed(ExternalController.IDX_BUTTON_L1.toInt(), true)
        }
        val right = GamepadState().apply {
            setPressed(ExternalController.IDX_BUTTON_A.toInt(), true)
        }

        left.thumbLX = 0f
        left.setPressed(ExternalController.IDX_BUTTON_L1.toInt(), false)
        val afterRelease = GamepadState.combine(listOf(left, right))
        assertEquals(0f, afterRelease.thumbLX, 0f)
        assertFalse(afterRelease.isPressed(ExternalController.IDX_BUTTON_L1.toInt()))
        assertTrue(afterRelease.isPressed(ExternalController.IDX_BUTTON_A.toInt()))

        val afterDisconnect = GamepadState.combine(emptyList())
        assertEquals(0f, afterDisconnect.thumbLX, 0f)
        assertEquals(0, afterDisconnect.buttons.toInt())
    }

    @Test
    fun `identifies complementary Nintendo Joy-Cons`() {
        assertTrue(JoyConSupport.isJoyCon(JoyConSupport.NINTENDO_VENDOR_ID, JoyConSupport.JOY_CON_LEFT_PRODUCT_ID))
        assertTrue(JoyConSupport.isJoyCon(JoyConSupport.NINTENDO_VENDOR_ID, JoyConSupport.JOY_CON_RIGHT_PRODUCT_ID))
        assertTrue(JoyConSupport.areComplementary(
            JoyConSupport.NINTENDO_VENDOR_ID,
            JoyConSupport.JOY_CON_LEFT_PRODUCT_ID,
            JoyConSupport.NINTENDO_VENDOR_ID,
            JoyConSupport.JOY_CON_RIGHT_PRODUCT_ID,
        ))
        assertFalse(JoyConSupport.areComplementary(
            JoyConSupport.NINTENDO_VENDOR_ID,
            JoyConSupport.JOY_CON_LEFT_PRODUCT_ID,
            JoyConSupport.NINTENDO_VENDOR_ID,
            JoyConSupport.JOY_CON_LEFT_PRODUCT_ID,
        ))
    }

    @Test
    fun `pairs only one unambiguous left and right set`() {
        val left = intArrayOf(JoyConSupport.NINTENDO_VENDOR_ID, JoyConSupport.JOY_CON_LEFT_PRODUCT_ID)
        val right = intArrayOf(JoyConSupport.NINTENDO_VENDOR_ID, JoyConSupport.JOY_CON_RIGHT_PRODUCT_ID)

        assertTrue(JoyConSupport.isUnambiguousPair(listOf(left, right)))
        assertFalse(JoyConSupport.isUnambiguousPair(listOf(left, left, right)))
        assertFalse(JoyConSupport.isUnambiguousPair(listOf(left, right, right)))
        assertTrue(JoyConSupport.shouldFusePair(0, -1))
        assertTrue(JoyConSupport.shouldFusePair(-1, 2))
        assertFalse(JoyConSupport.shouldFusePair(0, 1))
        assertFalse(JoyConSupport.shouldFusePair(-1, -1))
        assertEquals(0, JoyConSupport.getLegacyPairOwnerSlot(0, 1))
        assertEquals(1, JoyConSupport.getLegacyPairOwnerSlot(3, 1))
        assertEquals(-1, JoyConSupport.getLegacyPairOwnerSlot(0, -1))
        assertEquals(-1, JoyConSupport.getLegacyPairOwnerSlot(2, 2))
    }

    @Test
    fun `surviving half recovers persisted pair slot without fusing ambiguous topology`() {
        assertEquals(2, JoyConSupport.resolveLogicalSlot(true, -1, -1, 2, 1))
        assertEquals(-1, JoyConSupport.resolveLogicalSlot(true, -1, -1, 2, 4))
        assertEquals(1, JoyConSupport.resolveLogicalSlot(true, -1, 1, 2, 2))
        assertEquals(-1, JoyConSupport.resolveLogicalSlot(false, -1, -1, 2, 1))
    }

    @Test
    fun `non-owner half moves the logical pair owner when claiming player one`() {
        assertEquals("left-owner", JoyConSupport.resolveClaimOwnerIdentifier("left-owner", "right-half"))
        assertEquals("ordinary-controller", JoyConSupport.resolveClaimOwnerIdentifier(null, "ordinary-controller"))
    }

    @Test
    fun `moving direct pair owner preserves both remembered members`() {
        val pairSlots = linkedMapOf("left" to 1, "right" to 1)

        assertTrue(JoyConSupport.shouldMoveRememberedPair(1, 0, 1))
        JoyConSupport.moveRememberedPairSlot(pairSlots, 1, 0)

        assertEquals(linkedMapOf("left" to 0, "right" to 0), pairSlots)
        assertFalse(JoyConSupport.shouldMoveRememberedPair(1, 0, -1))
    }

    @Test
    fun `remembered lone non-owner becomes direct slot owner`() {
        assertTrue(JoyConSupport.shouldPromoteRememberedLoneHalf(true, -1, 2, 1, false))
        assertFalse(JoyConSupport.shouldPromoteRememberedLoneHalf(true, -1, 2, 2, false))
        assertFalse(JoyConSupport.shouldPromoteRememberedLoneHalf(true, -1, 2, 1, true))
    }

    @Test
    fun `cached source state requires matching descriptor`() {
        assertTrue(JoyConSupport.canReuseSourceController("descriptor-a", "descriptor-a"))
        assertFalse(JoyConSupport.canReuseSourceController("descriptor-a", "descriptor-b"))
        assertFalse(JoyConSupport.canReuseSourceController(null, "descriptor-a"))
    }

    @Test
    fun `maps left Joy-Con Linux scan codes to Android controls`() {
        assertEquals(KeyEvent.KEYCODE_DPAD_UP, JoyConSupport.remapKeyCode(
            JoyConSupport.NINTENDO_VENDOR_ID, JoyConSupport.JOY_CON_LEFT_PRODUCT_ID, 544, KeyEvent.KEYCODE_UNKNOWN,
        ))
        assertEquals(KeyEvent.KEYCODE_DPAD_LEFT, JoyConSupport.remapKeyCode(
            JoyConSupport.NINTENDO_VENDOR_ID, JoyConSupport.JOY_CON_LEFT_PRODUCT_ID, 546, KeyEvent.KEYCODE_UNKNOWN,
        ))
        assertEquals(KeyEvent.KEYCODE_BUTTON_L2, JoyConSupport.remapKeyCode(
            JoyConSupport.NINTENDO_VENDOR_ID, JoyConSupport.JOY_CON_LEFT_PRODUCT_ID, 312, KeyEvent.KEYCODE_UNKNOWN,
        ))
    }

    @Test
    fun `maps right Joy-Con by XInput position plus shoulders and system buttons`() {
        val expectedMappings = mapOf(
            304 to KeyEvent.KEYCODE_BUTTON_A,      // Nintendo B: south
            305 to KeyEvent.KEYCODE_BUTTON_B,      // Nintendo A: east
            307 to KeyEvent.KEYCODE_BUTTON_Y,      // Nintendo X: north
            308 to KeyEvent.KEYCODE_BUTTON_X,      // Nintendo Y: west
            311 to KeyEvent.KEYCODE_BUTTON_R1,
            313 to KeyEvent.KEYCODE_BUTTON_R2,
            315 to KeyEvent.KEYCODE_BUTTON_START,
            316 to KeyEvent.KEYCODE_BUTTON_MODE,
            318 to KeyEvent.KEYCODE_BUTTON_THUMBR,
        )

        expectedMappings.forEach { (scanCode, expectedKeyCode) ->
            assertEquals(expectedKeyCode, JoyConSupport.remapKeyCode(
                JoyConSupport.NINTENDO_VENDOR_ID,
                JoyConSupport.JOY_CON_RIGHT_PRODUCT_ID,
                scanCode,
                KeyEvent.KEYCODE_UNKNOWN,
            ))
        }
    }

    @Test
    fun `preserves normal controller key codes`() {
        assertEquals(KeyEvent.KEYCODE_BUTTON_X, JoyConSupport.remapKeyCode(
            0x045e, 0x0b13, 308, KeyEvent.KEYCODE_BUTTON_X,
        ))
    }
}
