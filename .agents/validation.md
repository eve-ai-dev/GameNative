# Project validation

This file is the sole authority for executable project commands. Run from the repository root. Declare results honestly: missing Java, SDK, signing material, network dependencies, or fresh evidence blocks the relevant gate rather than turning it green by vibes.

## Prerequisites

- Java: 17.
- Android SDK: compile/target SDK 36 and Build Tools containing `apksigner`.
- Android NDK: `27.3.13750724`.
- Native release bridge: SDL `release-2.32.10` headers available at `app/build/SDL/include` before building `evshim`.
- Signing: `app/keystores/joycontest.p12` and `app/keystores/joycontest.properties`, supplied only through approved secret handling and never committed.

## Core commands

- Y700 fork contract: `python3 scripts/verify_y700_contract.py .`
- Y700 contract regressions: `python3 scripts/test_verify_y700_contract.py`
- Setup/toolchain smoke: `./gradlew --version`
- Targeted Joy-Con test: `./gradlew :app:testLegacyDebugUnitTest :app:testModernDebugUnitTest --tests 'com.winlator.inputcontrols.JoyConSupportTest' --no-daemon --max-workers=2`
- Full test: `./gradlew :app:testLegacyDebugUnitTest :app:testModernDebugUnitTest --no-daemon --max-workers=2`
- Lint: `not_applicable` — no lint gate is currently proven in CI; do not invent one from plugin names.
- Type-check: `not_applicable` — Kotlin/Java compilation is exercised by tests/build.
- Local isolated Modern release build: `test -n "${JOYCON_VERSION_CODE:?set JOYCON_VERSION_CODE}" && ./gradlew :app:assembleModernRelease -PjoyConVersionCode="$JOYCON_VERSION_CODE" --no-daemon --max-workers=2`
- Diff integrity: `git diff --check`

## Native bridge gate

```bash
NDK_DIR="$ANDROID_SDK_ROOT/ndk/27.3.13750724"
cmake -S app/src/main/cpp/evshim -B app/build/evshim \
  -DCMAKE_TOOLCHAIN_FILE="$NDK_DIR/build/cmake/android.toolchain.cmake" \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-29 \
  -DSDL2_INCLUDE_DIR="$PWD/app/build/SDL/include" \
  -DCMAKE_BUILD_TYPE=Release
cmake --build app/build/evshim --parallel 2
install -m 0755 app/build/evshim/libevshim.so app/src/main/jniLibs/arm64-v8a/libevshim.so
nm -D app/src/main/jniLibs/arm64-v8a/libevshim.so | grep -q Java_com_winlator_winhandler_WinHandler_initializeSharedMemory
```

## APK verification

```bash
APK=$(find app/build/outputs/apk/modern/release -name '*.apk' -type f -print -quit)
test -n "$APK"
APKSIGNER=$(find "$ANDROID_SDK_ROOT/build-tools" -name apksigner -type f | sort -V | tail -1)
SIGNATURE_OUTPUT=$("$APKSIGNER" verify --verbose --print-certs "$APK")
printf '%s\n' "$SIGNATURE_OUTPUT"
printf '%s\n' "$SIGNATURE_OUTPUT" | grep -Fq 'certificate SHA-256 digest: eb2a3fac589273c9b1b38a1c6199c5f6fb13d11f382c48ca14ac51bb714310a9'
AAPT=$(find "$ANDROID_SDK_ROOT/build-tools" -name aapt -type f | sort -V | tail -1)
test -n "$AAPT"
PACKAGE=$($AAPT dump badging "$APK" | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
test "$PACKAGE" = "app.gamenative.joycontest"
sha256sum "$APK"
```

Expected identity/controls:

- package/application ID: `app.gamenative.joycontest`;
- version-name suffix: `-joycon-test`;
- signing verification: success with stable Y700 certificate SHA-256 `eb2a3fac589273c9b1b38a1c6199c5f6fb13d11f382c48ca14ac51bb714310a9`, not merely any valid/newly generated key;
- storage bridge: app-private `evshim`, no hardcoded base-package shared-memory path.

## Remote CI gates

- PR behavior gate: `.github/workflows/pluvia-pr-check.yml` on a PR to `master` runs Legacy + Modern unit tests.
- Protected Y700 integration gate: `.github/workflows/y700-protected-integration.yml` runs the Y700 contract, native bridge build, Legacy+Modern tests, isolated unsigned candidate build, and package/native-symbol inspection for every PR to `y700/stable`.
- Authorized full delivery gate: `gh workflow run pluvia-pr-check.yml --repo eve-ai-dev/GameNative --ref y700/stable`
- Inspect a run: `test -n "${RUN_ID:?set RUN_ID}" && gh run view "$RUN_ID" --repo eve-ai-dev/GameNative --json status,conclusion,headSha,jobs,url`
- Download artifacts only after matching the run `headSha` to the intended branch head: `test -n "${RUN_ID:?set RUN_ID}" && gh run download "$RUN_ID" --repo eve-ai-dev/GameNative --dir "outputs/ci/$RUN_ID"`

Triggering CI or downloading an artifact does not authorize publication. The `workflow_dispatch` lane runs the native bridge gate and Legacy+Modern tests before building, signing, verifying, and uploading the isolated APK, so one successful run can provide same-head code and artifact evidence.
Signed delivery dispatches are accepted only from `y700/stable`; feature refs must merge through the protected internal-PR lane first. Delivery artifacts remain candidates until the manual Lenovo Y700 hardware journey passes for that exact SHA.

## Specialized gates

- Browser/E2E: `not_applicable`.
- Visual/accessibility: `not_applicable` unless a selected PR changes UI behavior; then the active plan must add concrete evidence before integration.
- Performance/profile: `not_applicable` unless a selected PR makes a performance claim; then require a device-relevant before/after measurement.
- Hardware: manual Lenovo Legion Y700 Gen 3 journey for controller discovery, paired Joy-Con input, normal touch input, reconnect, player-slot persistence, and at least one game launch. Record device/build SHA and observations.
- Build worker budget: `--max-workers=2`, copied from the successful release workflow. Change only with a new successful measurement.

## Deterministic changed-code measurement

- Policy: `.agents/quality.yaml`.
- Policy validation: `python "${BUILDER_ASSURANCE_DIR:?set BUILDER_ASSURANCE_DIR}/scripts/validate_quality_policy.py" .agents/quality.yaml`
- Canonical report: `python "${BUILDER_ASSURANCE_DIR:?set BUILDER_ASSURANCE_DIR}/scripts/quality_report.py" --repo . --base fork/master --scope all --level standard --config .agents/quality.yaml --format json --output outputs/quality-report.json`
- External metrics: `not_applicable`; the installed reporter has no Java/Kotlin/C/C++ complexity adapter. `core_source_v1` provides deterministic changed-file/scope evidence only.

## Evidence contract

For each applicable gate, record command, exit code, head SHA, and artifact/observation. Keep behavioral tests, deterministic changed-code measurement, APK/package/signature checks, CI status, independent review, and hardware validation as separate evidence classes. No single green check substitutes for the rest.

## Maintenance

Update this file whenever project tooling or CI changes. Fix or remove stale commands before claiming validation passed.
