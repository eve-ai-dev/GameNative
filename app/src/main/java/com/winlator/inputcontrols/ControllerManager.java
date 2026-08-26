package com.winlator.inputcontrols;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.input.InputManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.preference.PreferenceManager;
import android.util.SparseArray;
import android.view.InputDevice;
import android.view.KeyEvent;
import android.view.MotionEvent;

import app.gamenative.PrefManager;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

public class ControllerManager {
    private static final String TAG = "ControllerManager";
    private static final int MAX_SLOTS = 4;

    @SuppressLint("StaticFieldLeak")
    private static ControllerManager instance;


    public static synchronized ControllerManager getInstance() {
        if (instance == null) {
            instance = new ControllerManager();
        }
        return instance;
    }

    private ControllerManager() {
        // Private constructor to prevent direct instantiation.
    }

    // --- Core Properties ---
    private Context context;
    private SharedPreferences preferences;
    private InputManager inputManager;

    // This list will hold all physical game controllers detected by Android.
    private final List<InputDevice> detectedDevices = new ArrayList<>();
    private final SparseArray<String> knownDeviceIdentifiers = new SparseArray<>();
    private final Map<String, Integer> knownVendorIdsByIdentifier = new HashMap<>();
    private final Map<String, Integer> knownProductIdsByIdentifier = new HashMap<>();

    // This maps a player slot (0-3) to the unique identifier of the physical device.
    // e.g., key=0, value="vendor_123_product_456"
    private final SparseArray<String> slotAssignments = new SparseArray<>();
    private final Map<String, Integer> lastKnownSlotByIdentifier = new HashMap<>();
    private final Map<String, Integer> pairedJoyConSlotByIdentifier = new HashMap<>();

    // This tracks which of the 4 player slots are enabled by the user.
    private final boolean[] enabledSlots = new boolean[MAX_SLOTS];
    private final List<OnSlotsChangedListener> slotListeners = new CopyOnWriteArrayList<>();
    private final ArrayDeque<Integer> recentlyFreedSlots = new ArrayDeque<>();

    private static final long ASSIGN_SETTLE_MS = 300L;
    private final Map<String, Long> firstSeenByIdentifier = new HashMap<>();
    private final Handler settleHandler = new Handler(Looper.getMainLooper());
    private final Runnable settleAssignRunnable = this::autoAssignConnectedDevices;
    private final Set<String> sessionActiveIdentifiers = new HashSet<>();
    private boolean sessionUsedExternalController;

    public interface OnSlotsChangedListener {
        void onSlotsChanged();
    }

    public static final String PREF_PLAYER_SLOT_PREFIX = "controller_slot_";
    public static final String PREF_ENABLED_SLOTS_PREFIX = "enabled_slot_";
    private static final String PREF_JOY_CON_PAIR_MEMBERS_PREFIX = "joy_con_pair_members_";


    /**
     * Initializes the manager. This must be called once from the main application context.
     * @param context The application context.
     */
    public void init(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(this.context);
        this.inputManager = (InputManager) this.context.getSystemService(Context.INPUT_SERVICE);

        // On startup, we load saved settings and scan for connected devices.
        loadAssignments();
        autoAssignConnectedDevices();
    }




    /**
     * Scans for all physically connected game controllers and updates the internal list.
     */
    public void scanForDevices() {
        detectedDevices.clear();
        int[] deviceIds = inputManager.getInputDeviceIds();
        Set<String> present = new HashSet<>();
        for (int deviceId : deviceIds) {
            InputDevice device = inputManager.getInputDevice(deviceId);
            // Some handhelds expose built-in controls as virtual devices, so
            // accept any device that reports a real gamepad/joystick shape.
            if (device != null && isGameController(device)) {
                detectedDevices.add(device);
                String ident = getDeviceIdentifier(device);
                knownDeviceIdentifiers.put(deviceId, ident);
                if (ident != null) {
                    present.add(ident);
                    knownVendorIdsByIdentifier.put(ident, device.getVendorId());
                    knownProductIdsByIdentifier.put(ident, device.getProductId());
                }
            }
        }
        long now = SystemClock.elapsedRealtime();
        for (String ident : present) {
            if (!firstSeenByIdentifier.containsKey(ident)) {
                firstSeenByIdentifier.put(ident, now);
            }
        }
        firstSeenByIdentifier.keySet().retainAll(present);
    }

    private boolean isSettled(String identifier) {
        Long t = firstSeenByIdentifier.get(identifier);
        return t != null && (SystemClock.elapsedRealtime() - t) >= ASSIGN_SETTLE_MS;
    }

    private void scheduleSettleAssign() {
        settleHandler.removeCallbacks(settleAssignRunnable);
        settleHandler.postDelayed(settleAssignRunnable, ASSIGN_SETTLE_MS + 20L);
    }

    /**
     * Loads the saved player slot assignments and enabled states from SharedPreferences.
     */
    private void loadAssignments() {
        slotAssignments.clear();
        lastKnownSlotByIdentifier.clear();
        pairedJoyConSlotByIdentifier.clear();
        for (int i = 0; i < MAX_SLOTS; i++) {
            // Load which device is assigned to this slot
            String prefKey = PREF_PLAYER_SLOT_PREFIX + i;
            String deviceIdentifier = preferences.getString(prefKey, null);
            if (deviceIdentifier != null) {
                slotAssignments.put(i, deviceIdentifier);
                lastKnownSlotByIdentifier.put(deviceIdentifier, i);
            }
            for (String pairMember : preferences.getStringSet(
                    PREF_JOY_CON_PAIR_MEMBERS_PREFIX + i, java.util.Collections.emptySet())) {
                pairedJoyConSlotByIdentifier.put(pairMember, i);
                lastKnownSlotByIdentifier.put(pairMember, i);
            }

            // Load whether this slot is enabled. Default P1=true, P2-4=false.
            String enabledKey = PREF_ENABLED_SLOTS_PREFIX + i;
            enabledSlots[i] = preferences.getBoolean(enabledKey, i == 0);
        }
    }

    /**
     * Saves the current player slot assignments and enabled states to SharedPreferences.
     */
    public void saveAssignments() {
        SharedPreferences.Editor editor = preferences.edit();
        for (int i = 0; i < MAX_SLOTS; i++) {
            // Save the assigned device identifier
            String deviceIdentifier = slotAssignments.get(i);
            String prefKey = PREF_PLAYER_SLOT_PREFIX + i;
            if (deviceIdentifier != null) {
                editor.putString(prefKey, deviceIdentifier);
            } else {
                editor.remove(prefKey);
            }

            Set<String> pairMembers = new HashSet<>();
            for (Map.Entry<String, Integer> entry : pairedJoyConSlotByIdentifier.entrySet()) {
                if (entry.getValue() == i) pairMembers.add(entry.getKey());
            }
            String pairMembersKey = PREF_JOY_CON_PAIR_MEMBERS_PREFIX + i;
            if (pairMembers.isEmpty()) {
                editor.remove(pairMembersKey);
            } else {
                editor.putStringSet(pairMembersKey, pairMembers);
            }

            // Save the enabled state
            String enabledKey = PREF_ENABLED_SLOTS_PREFIX + i;
            editor.putBoolean(enabledKey, enabledSlots[i]);
        }
        editor.apply();
    }

// --- Helper & Getter Methods ---

    /**
     * Checks if a device is a gamepad or joystick.
     * @param device The InputDevice to check.
     * @return True if the device is a game controller.
     */
    public static boolean isGameController(InputDevice device) {
        if (device == null) return false;

        boolean isGamepad = device.supportsSource(InputDevice.SOURCE_GAMEPAD);
        boolean isJoystick = device.supportsSource(InputDevice.SOURCE_JOYSTICK);

        boolean hasAxes =
                device.getMotionRange(android.view.MotionEvent.AXIS_X) != null ||
                        device.getMotionRange(android.view.MotionEvent.AXIS_Y) != null ||
                        device.getMotionRange(android.view.MotionEvent.AXIS_Z) != null ||
                        device.getMotionRange(android.view.MotionEvent.AXIS_RZ) != null;

        boolean[] hasGamepadKeysArray = device.hasKeys(
                KeyEvent.KEYCODE_BUTTON_A,
                KeyEvent.KEYCODE_BUTTON_B,
                KeyEvent.KEYCODE_BUTTON_X,
                KeyEvent.KEYCODE_BUTTON_Y
        );

        boolean hasGamepadKeys = false;
        for (boolean hasKey : hasGamepadKeysArray) {
            if (hasKey) {
                hasGamepadKeys = true;
                break;
            }
        }

        return JoyConSupport.isJoyCon(device) ||
                (isGamepad && hasGamepadKeys) ||
                (isJoystick && hasAxes);
    }

    /**
     * Creates a stable, unique identifier string for a given device.
     * This is used for saving and loading assignments.
     * @param device The InputDevice.
     * @return A unique identifier string.
     */
    public static String getDeviceIdentifier(InputDevice device) {
        if (device == null) return null;
        // The descriptor is the most reliable unique ID for a device.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return device.getDescriptor();
        }
        // Fallback for older Android versions
        return "vendor_" + device.getVendorId() + "_product_" + device.getProductId();
    }

    /**
     * Returns the list of all detected physical game controllers.
     */
    public List<InputDevice> getDetectedDevices() {
        return detectedDevices;
    }

    public void addOnSlotsChangedListener(OnSlotsChangedListener listener) {
        if (listener != null && !slotListeners.contains(listener)) {
            slotListeners.add(listener);
        }
    }

    public void removeOnSlotsChangedListener(OnSlotsChangedListener listener) {
        slotListeners.remove(listener);
    }

    private void notifySlotsChanged() {
        for (OnSlotsChangedListener listener : slotListeners) {
            listener.onSlotsChanged();
        }
    }

    /**
     * Returns the number of player slots the user has enabled.
     */
    public int getEnabledPlayerCount() {
        int count = 0;
        for (boolean enabled : enabledSlots) {
            if (enabled) {
                count++;
            }
        }
        return count;
    }

    /**
     * Assigns a physical device to a specific player slot.
     * This method handles un-assigning the device from any other slot it might have been in.
     * @param slotIndex The player slot to assign to (0-3).
     * @param device The physical InputDevice to assign.
     */
    public void assignDeviceToSlot(int slotIndex, InputDevice device) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return;

        String newDeviceIdentifier = getDeviceIdentifier(device);
        if (newDeviceIdentifier == null) return;

        assignDeviceIdentifierToSlot(slotIndex, newDeviceIdentifier);
        saveAssignments();
        notifySlotsChanged();
    }

    private void assignDeviceIdentifierToSlot(int slotIndex, String newDeviceIdentifier) {
        // First, remove the new device from any slot it might already be in.
        for (int i = 0; i < MAX_SLOTS; i++) {
            if (newDeviceIdentifier.equals(slotAssignments.get(i))) {
                slotAssignments.remove(i);
            }
        }

        // A different physical controller taking this slot invalidates stale Joy-Con pair memory.
        if (!Integer.valueOf(slotIndex).equals(pairedJoyConSlotByIdentifier.get(newDeviceIdentifier))) {
            pairedJoyConSlotByIdentifier.entrySet().removeIf(entry -> entry.getValue() == slotIndex);
        }

        // Assign the new device to the target slot.
        slotAssignments.put(slotIndex, newDeviceIdentifier);
        lastKnownSlotByIdentifier.put(newDeviceIdentifier, slotIndex);
        recentlyFreedSlots.remove(slotIndex);
    }

    /**
     * Clears any device assignment for the given player slot.
     * @param slotIndex The player slot to un-assign (0-3).
     */
    public void unassignSlot(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return;
        String deviceIdentifier = slotAssignments.get(slotIndex);
        if (deviceIdentifier != null) {
            lastKnownSlotByIdentifier.put(deviceIdentifier, slotIndex);
        }
        slotAssignments.remove(slotIndex);
        pairedJoyConSlotByIdentifier.entrySet().removeIf(entry -> entry.getValue() == slotIndex);
        markSlotRecentlyFreed(slotIndex);
        saveAssignments();
        notifySlotsChanged();
    }

    /**
     * Finds which player slot a given device is assigned to.
     * @param deviceId The ID of the physical device.
     * @return The player slot index (0-3), or -1 if the device is not assigned.
     */
    public int getSlotForDevice(int deviceId) {
        InputDevice device = inputManager.getInputDevice(deviceId);
        String deviceIdentifier = getDeviceIdentifierForDeviceId(deviceId);
        if (deviceIdentifier == null) return -1;

        int directSlot = getSlotForIdentifier(deviceIdentifier);
        InputDevice complementaryJoyCon = findComplementaryJoyCon(device);
        int complementaryDirectSlot = complementaryJoyCon == null
                ? -1
                : getSlotForIdentifier(getDeviceIdentifier(complementaryJoyCon));
        return JoyConSupport.resolveLogicalSlot(
                JoyConSupport.isJoyCon(device),
                directSlot,
                complementaryDirectSlot,
                pairedJoyConSlotByIdentifier.getOrDefault(deviceIdentifier, -1),
                getConnectedJoyConCount());
    }

    public boolean isPairedJoyCon(int deviceId) {
        InputDevice device = inputManager.getInputDevice(deviceId);
        InputDevice complementaryJoyCon = findComplementaryJoyCon(device);
        if (complementaryJoyCon == null) return false;
        return JoyConSupport.shouldFusePair(
                getSlotForIdentifier(getDeviceIdentifier(device)),
                getSlotForIdentifier(getDeviceIdentifier(complementaryJoyCon)));
    }

    private InputDevice findComplementaryJoyCon(InputDevice device) {
        if (!JoyConSupport.isJoyCon(device)) return null;
        List<int[]> joyConIds = new ArrayList<>();
        for (InputDevice candidate : detectedDevices) {
            if (JoyConSupport.isJoyCon(candidate)) {
                joyConIds.add(new int[]{candidate.getVendorId(), candidate.getProductId()});
            }
        }
        if (!JoyConSupport.isUnambiguousPair(joyConIds)) return null;

        InputDevice match = null;
        for (InputDevice candidate : detectedDevices) {
            if (candidate.getId() != device.getId() && JoyConSupport.areComplementary(device, candidate)) {
                match = candidate;
            }
        }
        return match;
    }

    private int getConnectedJoyConCount() {
        int count = 0;
        for (InputDevice device : detectedDevices) {
            if (JoyConSupport.isJoyCon(device)) count++;
        }
        return count;
    }

    private boolean rememberJoyConPairSlot(InputDevice device, int slot) {
        InputDevice complement = findComplementaryJoyCon(device);
        if (slot < 0 || complement == null) return false;
        String identifier = getDeviceIdentifier(device);
        String complementIdentifier = getDeviceIdentifier(complement);
        if (identifier == null || complementIdentifier == null) return false;
        boolean changed = !Integer.valueOf(slot).equals(pairedJoyConSlotByIdentifier.get(identifier))
                || !Integer.valueOf(slot).equals(pairedJoyConSlotByIdentifier.get(complementIdentifier));
        pairedJoyConSlotByIdentifier.put(identifier, slot);
        pairedJoyConSlotByIdentifier.put(complementIdentifier, slot);
        lastKnownSlotByIdentifier.put(identifier, slot);
        lastKnownSlotByIdentifier.put(complementIdentifier, slot);
        return changed;
    }

    /**
     * Older versions assigned each half of a single Joy-Con set to a different player. Collapse
     * that persisted layout to the lower player slot so existing users receive pairing without
     * having to clear controller settings. Multiple sets remain untouched because they are
     * ambiguous without an explicit pairing relationship from Android.
     */
    private boolean collapseLegacyJoyConPairAssignments() {
        List<InputDevice> joyCons = new ArrayList<>();
        List<int[]> joyConIds = new ArrayList<>();
        for (InputDevice device : detectedDevices) {
            if (JoyConSupport.isJoyCon(device)) {
                joyCons.add(device);
                joyConIds.add(new int[]{device.getVendorId(), device.getProductId()});
            }
        }
        if (!JoyConSupport.isUnambiguousPair(joyConIds)) return false;

        InputDevice first = joyCons.get(0);
        InputDevice second = joyCons.get(1);
        String firstIdentifier = getDeviceIdentifier(first);
        String secondIdentifier = getDeviceIdentifier(second);
        int firstSlot = getSlotForIdentifier(firstIdentifier);
        int secondSlot = getSlotForIdentifier(secondIdentifier);
        int ownerSlot = JoyConSupport.getLegacyPairOwnerSlot(firstSlot, secondSlot);
        if (ownerSlot < 0) return false;
        int releasedSlot = Math.max(firstSlot, secondSlot);
        String ownerIdentifier = firstSlot == ownerSlot ? firstIdentifier : secondIdentifier;
        slotAssignments.put(ownerSlot, ownerIdentifier);
        slotAssignments.remove(releasedSlot);
        lastKnownSlotByIdentifier.put(firstIdentifier, ownerSlot);
        lastKnownSlotByIdentifier.put(secondIdentifier, ownerSlot);
        pairedJoyConSlotByIdentifier.put(firstIdentifier, ownerSlot);
        pairedJoyConSlotByIdentifier.put(secondIdentifier, ownerSlot);
        markSlotRecentlyFreed(releasedSlot);
        Log.i(TAG, "Collapsed legacy split Joy-Con assignments into Player " + (ownerSlot + 1));
        return true;
    }

    /** Returns every connected physical device contributing to a player slot. */
    public List<InputDevice> getDevicesForSlot(int slotIndex) {
        List<InputDevice> devices = new ArrayList<>();
        for (InputDevice device : detectedDevices) {
            if (getSlotForDevice(device.getId()) == slotIndex) devices.add(device);
        }
        return devices;
    }

    private int getSlotForIdentifier(String deviceIdentifier) {
        if (deviceIdentifier == null) return -1;

        // Correctly loop through the sparse array to find the key for our value.
        for (int i = 0; i < slotAssignments.size(); i++) {
            int key = slotAssignments.keyAt(i);
            String value = slotAssignments.valueAt(i);
            if (deviceIdentifier.equals(value)) {
                return key; // Return the key (the slot index), not the internal index!
            }
        }

        return -1; // Not found
    }

    /** Returns the directly assigned identifier that owns this device's logical slot. */
    private String getSlotOwnerIdentifier(int deviceId) {
        int slot = getSlotForDevice(deviceId);
        return slot >= 0 ? slotAssignments.get(slot) : null;
    }

    private String getDeviceIdentifierForDeviceId(int deviceId) {
        InputDevice device = inputManager.getInputDevice(deviceId);
        String deviceIdentifier = getDeviceIdentifier(device);
        if (deviceIdentifier != null) {
            knownDeviceIdentifiers.put(deviceId, deviceIdentifier);
            return deviceIdentifier;
        }
        return knownDeviceIdentifiers.get(deviceId);
    }


    /**
     * Gets the InputDevice object that is currently assigned to a specific player slot.
     * @param slotIndex The player slot (0-3).
     * @return The assigned InputDevice, or null if no device is assigned or if the device is not currently connected.
     */
    public InputDevice getAssignedDeviceForSlot(int slotIndex) {
        String assignedIdentifier = slotAssignments.get(slotIndex);
        if (assignedIdentifier == null) return null;

        // Search our current list of connected devices for one that matches the saved identifier.
        for (InputDevice device : detectedDevices) {
            if (assignedIdentifier.equals(getDeviceIdentifier(device))) {
                return device; // Found it.
            }
        }

        return null; // The assigned device is not currently connected.
    }

    /**
     * Sets whether a player slot is enabled ("Connected").
     * @param slotIndex The player slot (0-3).
     * @param isEnabled The new enabled state.
     */
    public void setSlotEnabled(int slotIndex, boolean isEnabled) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return;
        enabledSlots[slotIndex] = isEnabled;
        saveAssignments();
        notifySlotsChanged();
    }

    public boolean isSlotEnabled(int slotIndex) {
        if (slotIndex < 0 || slotIndex >= MAX_SLOTS) return false;
        return enabledSlots[slotIndex];
    }

    public void onDeviceConnected(int deviceId) {
        InputDevice device = inputManager.getInputDevice(deviceId);
        if (device == null || !isGameController(device)) {
            return;
        }

        String deviceIdentifier = getDeviceIdentifier(device);
        if (deviceIdentifier == null) {
            return;
        }

        knownDeviceIdentifiers.put(deviceId, deviceIdentifier);
        scanForDevices();
        if (collapseLegacyJoyConPairAssignments()) {
            saveAssignments();
            notifySlotsChanged();
        }
        int existing = getSlotForDevice(deviceId);
        if (existing >= 0) {
            if (rememberJoyConPairSlot(device, existing)) {
                saveAssignments();
            }
            return;
        }

        if (!isSettled(deviceIdentifier)) {
            scheduleSettleAssign();
            return;
        }

        int slot = getPreferredFreeSlot(deviceIdentifier);
        if (slot >= 0) {
            enabledSlots[slot] = true;
            assignDeviceIdentifierToSlot(slot, deviceIdentifier);
            saveAssignments();
            notifySlotsChanged();
            Log.i(TAG, "Auto-assigned deviceId=" + deviceId + " to Player " + (slot + 1));
            return;
        }
        Log.i(TAG, "No free controller slot for deviceId=" + deviceId);
    }

    /**
     * Assigns any currently connected controller that is not already bound to a player slot.
     * Built-in controllers can be present before Android dispatches any hot-plug callback,
     * so callers should run this after a device scan during startup/session refresh.
     */
    public void autoAssignConnectedDevices() {
        scanForDevices();
        boolean changed = collapseLegacyJoyConPairAssignments();
        for (InputDevice device : detectedDevices) {
            String deviceIdentifier = getDeviceIdentifier(device);
            int logicalSlot = getSlotForDevice(device.getId());
            if (deviceIdentifier == null) {
                continue;
            }
            if (logicalSlot >= 0) {
                changed |= rememberJoyConPairSlot(device, logicalSlot);
                continue;
            }

            if (!isSettled(deviceIdentifier)) {
                scheduleSettleAssign();
                continue;
            }

            int slot = getPreferredFreeSlot(deviceIdentifier);
            if (slot < 0) {
                Log.i(TAG, "No free controller slot for connected deviceId=" + device.getId());
                break;
            }

            enabledSlots[slot] = true;
            assignDeviceIdentifierToSlot(slot, deviceIdentifier);
            knownDeviceIdentifiers.put(device.getId(), deviceIdentifier);
            changed = true;
            Log.i(TAG, "Auto-assigned connected deviceId=" + device.getId()
                    + " to Player " + (slot + 1));
        }

        if (changed) {
            saveAssignments();
            notifySlotsChanged();
        }
    }

    public void onDeviceDisconnected(int deviceId) {
        String deviceIdentifier = getDeviceIdentifierForDeviceId(deviceId);
        int slot = getSlotForIdentifier(deviceIdentifier);
        Integer disconnectedVendorId = knownVendorIdsByIdentifier.get(deviceIdentifier);
        Integer disconnectedProductId = knownProductIdsByIdentifier.get(deviceIdentifier);
        String replacementIdentifier = null;
        int complementaryCount = 0;
        if (slot >= 0 && disconnectedVendorId != null && disconnectedProductId != null) {
            for (InputDevice device : detectedDevices) {
                if (device.getId() != deviceId && JoyConSupport.areComplementary(
                        disconnectedVendorId,
                        disconnectedProductId,
                        device.getVendorId(),
                        device.getProductId())) {
                    complementaryCount++;
                    String candidateIdentifier = getDeviceIdentifier(device);
                    if (getSlotForIdentifier(candidateIdentifier) < 0) {
                        replacementIdentifier = candidateIdentifier;
                    }
                }
            }
            if (complementaryCount != 1) replacementIdentifier = null;
        }
        knownDeviceIdentifiers.remove(deviceId);
        scanForDevices();
        if (slot >= 0) {
            InputDevice replacement = null;
            if (replacementIdentifier != null) {
                for (InputDevice device : detectedDevices) {
                    if (replacementIdentifier.equals(getDeviceIdentifier(device))) {
                        replacement = device;
                        break;
                    }
                }
            }
            slotAssignments.remove(slot);
            if (deviceIdentifier != null) {
                lastKnownSlotByIdentifier.put(deviceIdentifier, slot);
            }
            if (replacement != null) {
                assignDeviceIdentifierToSlot(slot, getDeviceIdentifier(replacement));
                Log.i(TAG, "Promoted remaining Joy-Con deviceId=" + replacement.getId()
                        + " to Player " + (slot + 1));
            } else {
                markSlotRecentlyFreed(slot);
            }
            saveAssignments();
            notifySlotsChanged();
            if (replacement == null) {
                Log.i(TAG, "Unassigned disconnected deviceId=" + deviceId + " from Player " + (slot + 1));
            }
        }
    }

    private void markSlotRecentlyFreed(int slot) {
        if (slot < 0 || slot >= MAX_SLOTS) {
            return;
        }
        recentlyFreedSlots.remove(slot);
        recentlyFreedSlots.addLast(slot);
    }

    public void resetSessionActivity() {
        sessionActiveIdentifiers.clear();
        sessionUsedExternalController = false;
    }

    public int getSessionUsedControllerCount() {
        return sessionActiveIdentifiers.size();
    }

    public boolean getSessionUsedExternalController() {
        return sessionUsedExternalController;
    }

    public void noteGamepadActivity(MotionEvent event) {
        if (isRealGamepadMotion(event)) markActive(event.getDeviceId());
    }

    public boolean noteGamepadButton(int deviceId) {
        markActive(deviceId);
        int slot = getSlotForDevice(deviceId);
        if (slot == 0) return false;
        InputDevice occupant = getAssignedDeviceForSlot(0);
        if (occupant == null) {
            String deviceIdentifier = getDeviceIdentifierForDeviceId(deviceId);
            if (deviceIdentifier == null) return false;
            assignDeviceIdentifierToSlot(0, deviceIdentifier);
            saveAssignments();
            notifySlotsChanged();
            Log.i(TAG, "deviceId=" + deviceId + " claimed empty Player 1");
            return true;
        }
        String occupantIdentifier = getDeviceIdentifier(occupant);
        if (occupantIdentifier == null || sessionActiveIdentifiers.contains(occupantIdentifier)) return false;
        String deviceIdentifier = getSlotOwnerIdentifier(deviceId);
        if (deviceIdentifier == null) return false;
        assignDeviceIdentifierToSlot(0, deviceIdentifier);
        if (slot > 0) {
            assignDeviceIdentifierToSlot(slot, occupantIdentifier);
        }
        saveAssignments();
        notifySlotsChanged();
        Log.i(TAG, "deviceId=" + deviceId + " displaced idle Player 1");
        return true;
    }

    private void markActive(int deviceId) {
        String identifier = getSlotOwnerIdentifier(deviceId);
        if (identifier == null) {
            identifier = getDeviceIdentifierForDeviceId(deviceId);
        }
        if (identifier == null || !sessionActiveIdentifiers.add(identifier)) return;
        InputDevice device = inputManager.getInputDevice(deviceId);
        if (device != null &&
                (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q || device.isExternal())) {
            sessionUsedExternalController = true;
        }
    }

    private static boolean isRealGamepadMotion(MotionEvent event) {
        return Math.abs(event.getAxisValue(MotionEvent.AXIS_X)) > 0.25f
                || Math.abs(event.getAxisValue(MotionEvent.AXIS_Y)) > 0.25f
                || Math.abs(event.getAxisValue(MotionEvent.AXIS_Z)) > 0.25f
                || Math.abs(event.getAxisValue(MotionEvent.AXIS_RZ)) > 0.25f
                || Math.abs(event.getAxisValue(MotionEvent.AXIS_LTRIGGER)) > 0.25f
                || Math.abs(event.getAxisValue(MotionEvent.AXIS_RTRIGGER)) > 0.25f
                || Math.abs(event.getAxisValue(MotionEvent.AXIS_HAT_X)) > 0.25f
                || Math.abs(event.getAxisValue(MotionEvent.AXIS_HAT_Y)) > 0.25f;
    }

    private boolean isSlotAvailable(int slot) {
        return slot >= 0 && slot < MAX_SLOTS && getAssignedDeviceForSlot(slot) == null;
    }

    private int getPreferredFreeSlot(String deviceIdentifier) {
        Integer previousSlot = lastKnownSlotByIdentifier.get(deviceIdentifier);
        if (previousSlot != null && isSlotAvailable(previousSlot)) {
            recentlyFreedSlots.remove(previousSlot);
            return previousSlot;
        }

        for (int slot = 0; slot < MAX_SLOTS; slot++) {
            if (isSlotAvailable(slot)) {
                return slot;
            }
        }
        return -1;
    }
}
