package com.winlator.inputcontrols;

import android.view.InputDevice;
import android.view.KeyEvent;

import java.util.Collection;

/** Compatibility helpers for Nintendo Switch Joy-Con halves exposed as separate Android devices. */
public final class JoyConSupport {
    public static final int NINTENDO_VENDOR_ID = 0x057e;
    public static final int JOY_CON_LEFT_PRODUCT_ID = 0x2006;
    public static final int JOY_CON_RIGHT_PRODUCT_ID = 0x2007;

    private JoyConSupport() {}

    public static boolean isLeftJoyCon(InputDevice device) {
        return device != null && isLeftJoyCon(device.getVendorId(), device.getProductId());
    }

    public static boolean isRightJoyCon(InputDevice device) {
        return device != null && isRightJoyCon(device.getVendorId(), device.getProductId());
    }

    public static boolean isJoyCon(InputDevice device) {
        return device != null && isJoyCon(device.getVendorId(), device.getProductId());
    }

    public static boolean isLeftJoyCon(int vendorId, int productId) {
        return vendorId == NINTENDO_VENDOR_ID && productId == JOY_CON_LEFT_PRODUCT_ID;
    }

    public static boolean isRightJoyCon(int vendorId, int productId) {
        return vendorId == NINTENDO_VENDOR_ID && productId == JOY_CON_RIGHT_PRODUCT_ID;
    }

    public static boolean isJoyCon(int vendorId, int productId) {
        return isLeftJoyCon(vendorId, productId) || isRightJoyCon(vendorId, productId);
    }

    public static boolean areComplementary(InputDevice first, InputDevice second) {
        return first != null && second != null && areComplementary(
                first.getVendorId(), first.getProductId(), second.getVendorId(), second.getProductId());
    }

    public static boolean areComplementary(
            int firstVendorId,
            int firstProductId,
            int secondVendorId,
            int secondProductId
    ) {
        return (isLeftJoyCon(firstVendorId, firstProductId) && isRightJoyCon(secondVendorId, secondProductId))
                || (isRightJoyCon(firstVendorId, firstProductId) && isLeftJoyCon(secondVendorId, secondProductId));
    }

    /**
     * Returns whether the supplied IDs describe exactly one left and one right Joy-Con.
     * Multiple same-side candidates are intentionally not paired because Android exposes no
     * stable relationship that lets us determine which physical set belongs together.
     */
    public static boolean isUnambiguousPair(Collection<int[]> vendorProductIds) {
        if (vendorProductIds == null) return false;
        int leftCount = 0;
        int rightCount = 0;
        for (int[] ids : vendorProductIds) {
            if (ids == null || ids.length < 2) continue;
            if (isLeftJoyCon(ids[0], ids[1])) leftCount++;
            if (isRightJoyCon(ids[0], ids[1])) rightCount++;
        }
        return leftCount == 1 && rightCount == 1;
    }

    /** A pair is fused only while exactly one of its halves owns a player slot. */
    public static boolean shouldFusePair(int firstDirectSlot, int secondDirectSlot) {
        return (firstDirectSlot >= 0) != (secondDirectSlot >= 0);
    }

    /** Returns the slot to retain when migrating a legacy split assignment, or -1 if not needed. */
    public static int getLegacyPairOwnerSlot(int firstDirectSlot, int secondDirectSlot) {
        return firstDirectSlot >= 0 && secondDirectSlot >= 0 && firstDirectSlot != secondDirectSlot
                ? Math.min(firstDirectSlot, secondDirectSlot)
                : -1;
    }

    /**
     * Android reports several Joy-Con buttons as unknown or with a generic layout. Translate the
     * Linux scan codes used by the Joy-Con key layouts into stable Android gamepad key codes.
     */
    public static int remapKeyCode(int vendorId, int productId, int scanCode, int fallbackKeyCode) {
        if (isLeftJoyCon(vendorId, productId)) {
            switch (scanCode) {
                case 544: return KeyEvent.KEYCODE_DPAD_UP;
                case 545: return KeyEvent.KEYCODE_DPAD_DOWN;
                case 546: return KeyEvent.KEYCODE_DPAD_LEFT;
                case 547: return KeyEvent.KEYCODE_DPAD_RIGHT;
                case 309: return KeyEvent.KEYCODE_BUTTON_MODE;
                case 310: return KeyEvent.KEYCODE_BUTTON_L1;
                case 312: return KeyEvent.KEYCODE_BUTTON_L2;
                case 314: return KeyEvent.KEYCODE_BUTTON_SELECT;
                case 317: return KeyEvent.KEYCODE_BUTTON_THUMBL;
                default: return fallbackKeyCode;
            }
        }

        if (isRightJoyCon(vendorId, productId)) {
            switch (scanCode) {
                case 304: return KeyEvent.KEYCODE_BUTTON_A;
                case 305: return KeyEvent.KEYCODE_BUTTON_B;
                case 307: return KeyEvent.KEYCODE_BUTTON_Y;
                case 308: return KeyEvent.KEYCODE_BUTTON_X;
                case 311: return KeyEvent.KEYCODE_BUTTON_R1;
                case 313: return KeyEvent.KEYCODE_BUTTON_R2;
                case 315: return KeyEvent.KEYCODE_BUTTON_START;
                case 316: return KeyEvent.KEYCODE_BUTTON_MODE;
                case 318: return KeyEvent.KEYCODE_BUTTON_THUMBR;
                default: return fallbackKeyCode;
            }
        }

        return fallbackKeyCode;
    }

    public static int remapKeyCode(InputDevice device, KeyEvent event) {
        if (device == null || event == null) return event != null ? event.getKeyCode() : KeyEvent.KEYCODE_UNKNOWN;
        return remapKeyCode(device.getVendorId(), device.getProductId(), event.getScanCode(), event.getKeyCode());
    }

}
