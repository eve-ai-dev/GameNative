# Lenovo Legion Y700 Gen 3 guide

This page documents the maintained `y700/stable` fork. It is intentionally stricter than a generic “best settings” post.

## What this fork guarantees in code and CI

The maintained branch is designed to preserve:

- paired left/right Joy-Con ownership as one logical controller where the topology is unambiguous;
- controller reconnect and player-slot persistence;
- normal touch input alongside controller input;
- isolated application ID `app.gamenative.joycontest`;
- isolated app-private `evshim` shared-memory behavior;
- stable signing-certificate continuity;
- Legacy and Modern unit tests before the isolated Modern APK is produced.

CI proves compilation, tests, package identity and signing. It does **not** prove physical controller, display, audio, thermal or frame-pacing behavior on a Y700.

## Conservative starting point

1. Use the isolated Y700 build rather than installing over the upstream package.
2. Start from a known game configuration or the app defaults.
3. Pair both Joy-Con halves before launching the game.
4. Verify touch input and controller input independently.
5. Change one graphics/audio/emulation setting per test.
6. Keep a working configuration before experimenting.

No single graphics-driver family, emulator or present mode is documented here as universally fastest. That would be fake precision without a repeatable Y700 test set.

## Minimum hardware check after a new build

- both Joy-Con halves are discovered;
- they produce one intended logical player when paired;
- buttons, sticks and triggers map correctly;
- touch remains usable;
- disconnect/reconnect restores the expected player slot;
- at least one game launches and accepts input;
- the isolated app coexists with the normal GameNative package;
- updating the Y700 build does not trigger a signing mismatch.

Record the exact Git commit or workflow run used. “Latest” expires immediately.

## Current performance candidates

These upstream pull requests are interesting, but are not integrated merely because their description sounds good.

### DirectAudio — upstream PR #1806

Potential benefit: lower audio latency for a supported Bionic/ARM64EC path.

Before integration, a Y700 comparison should establish:

- whether the target Proton/ARM64EC setup actually selects the new driver;
- launch and playback stability across more than one game;
- latency versus PulseAudio using the same game and controller action;
- crackling, underruns, suspend/resume and Bluetooth/audio-route behavior;
- CPU cost and interaction with the existing low-latency PulseAudio option;
- provenance and reproducibility of any added native binary.

PR: <https://github.com/utkarshdalal/GameNative/pull/1806>

### Adaptive resolution — upstream PR #1759

Potential benefit: lower render cost or steadier performance on the Y700 display.

Before integration, a Y700 comparison should establish:

- actual internal and output resolution changes;
- frame pacing, not only average FPS;
- text/UI legibility and scaling artifacts;
- touch-coordinate and controller-overlay alignment;
- behavior during rotation, resume and resolution transitions;
- thermal/power behavior over a sustained run;
- whether generated or unrelated files can be removed from the integration delta.

PR: <https://github.com/utkarshdalal/GameNative/pull/1759>

## Related guides

- [Graphics stack and drivers](../graphics/driver-stack.md)
- [Present modes](../graphics/present-modes.md)
- [Box64 and FEXCore](../emulation/box64-and-fexcore.md)
- [Diagnostics](../troubleshooting/diagnostics.md)

## Project evidence

- Branch contract: [`AGENTS.md`](../../AGENTS.md)
- Current mutable state: [`STATE.md`](../../STATE.md)
- Validation contract: [`.agents/validation.md`](../../.agents/validation.md)
- Joy-Con regression tests: [`JoyConSupportTest.kt`](../../app/src/test/java/com/winlator/inputcontrols/JoyConSupportTest.kt)
