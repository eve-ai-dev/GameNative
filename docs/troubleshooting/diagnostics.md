# Troubleshooting and useful evidence

A useful report identifies the failed layer and preserves enough context to reproduce it. “It crashes with Wrapper-v2” is not enough.

## Before changing settings

Record:

- GameNative build commit or workflow run;
- device and Android version;
- game/store and exact point of failure;
- container variant and Wine/Proton version;
- graphics-driver family and version;
- DXVK/VKD3D version;
- FEXCore/Box64 selections and versions;
- screen size, refresh rate, FPS cap and present mode;
- controller/audio route when relevant.

Export or screenshot the working configuration before experimentation. Change one layer at a time.

## In-app diagnostics

The current Settings → Debug surface includes:

- Wine debug-channel selection;
- Wine debug logging to a file;
- Box86/Box64 logging to a file;
- latest crash-log viewer;
- latest game debug-log viewer.

The game menu also provides **Play with diagnostics** and, once a diagnostic file exists, **Share diagnostics**. This produces a per-game wrapper diagnostic file rather than relying on a screenshot of an error.

For controller problems, enable **Show Controller Debug Menu** in the container's Controller tab. The in-game overlay exposes player slots, Android device IDs/input sources and the guest input path, which is much more useful than “the controller connected.”

Enable verbose logs only while reproducing the issue. Logging can change timing and produce large files, so it is evidence collection—not a performance setting.

## Symptom routing

### Game does not launch

Check, in order:

1. Wine/Proton build matches the intended container path.
2. Required FEXCore or Box64/WoWBox64 content is installed.
3. DXVK/VKD3D choice matches the game's DirectX generation.
4. The graphics-driver family **and version** are available.
5. Latest crash and game logs identify the failing component.

### Black screen or graphics corruption

- Return graphics, DX wrapper and presentation settings to the known/default configuration.
- Change only one of: DXVK/VKD3D version, graphics-driver family/version, exposed extensions, BCn mode, present mode.
- Record whether audio/input continues; that distinguishes a rendering failure from a full process failure.

### Stutter, tearing or poor latency

- Separate average FPS from frame pacing.
- Record display refresh rate and game FPS cap.
- Remove the duplicate manual `MESA_VK_WSI_PRESENT_MODE` entry before testing a different UI value; the manual environment is merged later and wins.
- Test a single present-mode change through a full game restart.
- Do not change emulator, graphics driver and present mode in one comparison.

### Joy-Con halves do not behave as one controller

- Confirm both halves are connected before game launch.
- Record Android device names/IDs shown for each half if available.
- Test touch input separately.
- Reproduce disconnect/reconnect and note the player slot before and after.
- Include the exact Y700 build SHA; controller behavior is branch-specific.

### Audio latency or crackling

- Record output route: speakers, USB, wired or Bluetooth.
- Record the audio driver and low-latency setting.
- Compare with the same game scene and route.
- Treat Bluetooth codec latency separately from the GameNative audio driver.

## A compact issue template

```text
Build/SHA:
Device + Android:
Game/store:
Container + Wine/Proton:
Graphics driver + version:
DX wrapper + version:
64-bit / 32-bit emulator + versions:
Screen refresh / FPS cap / present mode:
Controller and audio route:
Expected:
Observed:
Single setting changed:
Reproduction steps:
Crash/game log attached:
```

## Source map

- Debug labels and available actions: [`strings.xml`](../../app/src/main/res/values/strings.xml)
- Per-game diagnostic artifact: [`DiagnosticsLog.kt`](../../app/src/main/java/app/gamenative/utils/DiagnosticsLog.kt)
- Controller diagnostic toggle: [`ControllerTab.kt`](../../app/src/main/java/app/gamenative/ui/component/dialog/ControllerTab.kt)
- Project validation and hardware gate: [`.agents/validation.md`](../../.agents/validation.md)
- Presentation guidance: [`present-modes.md`](../graphics/present-modes.md)
- Emulator guidance: [`box64-and-fexcore.md`](../emulation/box64-and-fexcore.md)
