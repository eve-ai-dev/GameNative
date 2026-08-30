# Box64, FEXCore and Bionic containers

## Why both names appear

A Bionic ARM64EC container can involve separate translation choices for 64-bit and 32-bit Windows code.

The current UI/runtime model is:

| Wine/Proton build | 64-bit path | 32-bit/WoW64 path |
|---|---|---|
| `arm64ec` | FEXCore, fixed by the UI | FEXCore or Box64, selectable |
| `x86_64` | Box64, fixed by the UI | Box64, fixed by the UI |

For ARM64EC, selecting FEXCore for the 32-bit path causes the launcher to use `libwow64fex.dll`; selecting Box64 causes it to use `wowbox64.dll`.

This is why “FEXCore is the only available 64-bit emulator” and “a Box64 version is selectable” can both be true: the Box64 selector can configure the optional 32-bit/WoW64 path.

## Why the Box64 version selector is always visible

The current Compose UI renders the Box64 version and preset controls outside the condition that decides whether Box64 is active. It therefore remains visible when ARM64EC uses FEXCore for both the fixed 64-bit path and the selected 32-bit path.

The Bionic launcher also prepares the FEXCore and WoWBox64 components for ARM64EC. The saved Box64 version can therefore describe the available fallback even when it is not the currently selected 32-bit emulator.

**Confirmed UI limitation:** visibility does not indicate that the selected Box64 version is active. Check `64-bit emulator`, `32-bit emulator`, and the Wine/Proton build together.

## Which should be preferred?

This repository does not contain enough device/game benchmark evidence for a universal FEXCore-versus-Box64 winner.

**Recommended defaults:**

- keep the emulator implied by the selected Wine/Proton build;
- for ARM64EC, keep the known configuration for the 32-bit path unless a game requires the alternative;
- change emulator and version separately so a result can be attributed;
- do not interpret a newer version number as guaranteed compatibility or performance.

## Version fields

- **FEXCore Version** controls the FEXCore content used by ARM64EC.
- **Box64 Version** controls normal Box64 for x86_64 Wine or WoWBox64 for the ARM64EC 32-bit path.
- **FEXCore Preset** and **Box64 Preset** tune different emulators; an inactive emulator's preset does not prove it is participating in the launch.

Manifest entries can add installable versions beyond the static arrays bundled in the app. A muted entry in the selector can represent content available for download but not yet installed.

## How to report an emulator issue

Include:

- container variant (`bionic` or `glibc`);
- exact Wine/Proton build (`arm64ec` or `x86_64` matters);
- 64-bit and 32-bit emulator values shown by the app;
- FEXCore and Box64/WoWBox64 versions;
- presets;
- whether the failing executable is 32-bit or 64-bit, if known;
- game debug log and latest crash log.

## Source map

- Emulation UI and visibility: [`EmulationTab.kt`](../../app/src/main/java/app/gamenative/ui/component/dialog/EmulationTab.kt)
- Emulator defaults derived from Wine build: [`ContainerConfigDialog.kt`](../../app/src/main/java/app/gamenative/ui/component/dialog/ContainerConfigDialog.kt)
- Runtime launcher selection and component extraction: [`BionicProgramLauncherComponent.java`](../../app/src/main/java/com/winlator/xenvironment/components/BionicProgramLauncherComponent.java)
- Container model: [`ContainerData.kt`](../../app/src/main/java/com/winlator/container/ContainerData.kt)
- Static options: [`arrays.xml`](../../app/src/main/res/values/arrays.xml)
- Downloadable component manifest: [`manifest.json`](../../manifest.json)
