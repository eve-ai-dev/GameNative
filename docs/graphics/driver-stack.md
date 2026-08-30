# Graphics stack and driver families

## The short version

A GameNative graphics configuration is a chain, not one “driver”:

```text
Windows game
  → Wine / Proton
  → DirectX translation (DXVK or VKD3D)
  → Vulkan-facing graphics driver or wrapper
  → Android GPU driver
  → GameNative display renderer
  → Android display
```

Changing two layers at once makes a result hard to diagnose. Treat the DirectX wrapper, graphics-driver family/version and presentation settings as separate controls.

## DirectX translation

The `DX Wrapper` setting selects the translation layer used by the Windows graphics API:

- **DXVK** is the configured path for Direct3D 8/9/10/11 workloads.
- **VKD3D** is the configured path for Direct3D 12 workloads.

The selected component has its own version and options. It is distinct from the Bionic graphics-driver choices called `Wrapper`, `Wrapper-v2`, and similar names.

**Recommended:** use a known game configuration when available. Do not change DXVK/VKD3D and the graphics driver in the same test.

## Bionic graphics-driver families

The current Bionic UI exposes these component families:

- `Wrapper`
- `Wrapper-v2`
- `Wrapper-gamenative`
- `Wrapper-leegao`
- `Wrapper-legacy`

The family name and the driver **version/package** are separate selections. At launch, GameNative installs the selected family as a content component and resolves the selected driver package from the container configuration.

### What is confirmed

- The names above are distinct downloadable/installable component IDs.
- Selecting a different family causes GameNative to apply that component on the next relevant launch.
- Driver versions may come from the manifest or already-installed content.
- The runtime configures the selected wrapper through `WRAPPER_*`, Mesa and Vulkan environment variables.
- `Wrapper-gamenative` exposes additional BCn transcoder and texture-quality controls.

### What is not established by this repository

The source tree does **not** contain an authoritative design note explaining the implementation history, compatibility target or performance trade-off of each wrapper family. Their names alone are not enough to claim that `Wrapper-v2` is always faster, that `legacy` is always safer, or that one family is globally preferred.

**Recommended:** keep the default/known configuration unless a specific game issue or measured comparison justifies changing the family. If a family fixes a game, record the exact family **and version**.

## Driver family versus driver package

These settings answer different questions:

- **Graphics Driver:** which wrapper integration is installed and invoked.
- **Graphics Driver Version:** which underlying package/version that integration resolves.
- **Use Adrenotools Turnip:** whether the configured external Turnip path is enabled for the selected setup.

A report saying only “Turnip works” or “Wrapper-v2 is broken” is incomplete. Include all three fields plus the DXVK/VKD3D version.

## Other graphics controls

The Bionic graphics tab also exposes Vulkan extensions, reported device memory, resource type, BCn emulation, sharpening and presentation controls. These are advanced compatibility knobs, not a menu of linear upgrades.

In particular:

- limiting reported device memory can alter game behavior, but is not extra physical memory;
- blacklisting Vulkan extensions can work around compatibility problems, but can also hide capabilities;
- BCn emulation trades compatibility, conversion work and potentially memory/cache behavior;
- `auto` is the safest starting point when there is no title-specific evidence.

## Source map

- Bionic graphics UI: [`GraphicsTab.kt`](../../app/src/main/java/app/gamenative/ui/component/dialog/GraphicsTab.kt)
- Driver component inventory: [`graphics_driver_download.json`](../../app/src/main/assets/graphics_driver_download.json)
- Default graphics configuration: [`Container.java`](../../app/src/main/java/com/winlator/container/Container.java)
- Launch-time driver extraction and environment: [`XServerScreen.kt`](../../app/src/main/java/app/gamenative/ui/screen/xserver/XServerScreen.kt)
- Available UI values: [`arrays.xml`](../../app/src/main/res/values/arrays.xml)
