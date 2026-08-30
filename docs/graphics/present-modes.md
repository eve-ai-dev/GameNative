# Present modes

GameNative currently exposes similarly named controls at different layers. They should not be assumed to be aliases.

## 1. Graphics `Present Mode`

This is stored inside `graphicsDriverConfig` as `presentMode`.

For the Bionic wrapper path, the current launcher writes that value to:

```text
MESA_VK_WSI_PRESENT_MODE
```

When the selected value contains `immediate`, it also sets:

```text
WRAPPER_MAX_IMAGE_COUNT=1
```

The UI offers `Never`, `mailbox`, `Normal`, `fifo`, `Always`, `immediate`, and `relaxed`. Some entries are wrapper policy labels while others resemble Vulkan/Mesa modes; they should not be treated as a clean list of interchangeable Vulkan constants.

The branch default is `mailbox`.

## 2. `Renderer Present Mode`

This is stored separately as `rendererPresentMode`. The UI offers `fifo` and `mailbox`, and the persisted default is `fifo`.

When GameNative uses its Vulkan display renderer, launch code converts the value to a Vulkan present-mode number and passes it through JNI to the native compositor. The compositor requests that mode for its Android swapchain and falls back to FIFO when the surface does not advertise it.

This controls GameNative's final Android-side compositor. It is a different queue from the game/wrapper/Mesa presentation controlled by Graphics `Present Mode`, so the values do not need to match. When the legacy GL renderer is active, this Vulkan compositor setting is not applied.

## 3. Manual `MESA_VK_WSI_PRESENT_MODE`

The same variable can appear in the free-form environment-variable list. The default container environment includes `MESA_VK_WSI_PRESENT_MODE=mailbox`.

For the Bionic wrapper path, driver setup first derives this variable from Graphics `Present Mode`; later, the free-form container environment is merged over the generated environment. A manual value therefore wins. The default environment currently already contains `MESA_VK_WSI_PRESENT_MODE=mailbox`, which can override a different dropdown selection.

**Recommended:** keep a single authority. Set Graphics `Present Mode` in the UI and remove the duplicate manual `MESA_VK_WSI_PRESENT_MODE` entry when testing another value. Until the duplicate default is fixed in code, changing only the dropdown may not change the effective Mesa value.

## Practical meaning of common modes

These definitions follow the Vulkan API reference; they are useful general expectations, not Y700 benchmark results.[1]

- **FIFO:** waits for vertical blanking, preserves queue order and does not tear. It is the only core present mode required to be supported.[1]
- **Mailbox:** waits for vertical blanking without tearing, but replaces an older pending frame when a newer one arrives.[1]
- **Immediate:** updates without waiting for vertical blanking, so it may tear.[1]
- **Relaxed FIFO:** normally waits like FIFO, but may present without another wait when the application has missed the previous blanking interval; this can reduce occasional stutter but may tear when late.[1]

## Should values be the same?

No blanket rule is justified:

- Graphics `Present Mode` feeds the Bionic wrapper/Mesa path, subject to a later manual environment override.
- `Renderer Present Mode` controls GameNative's Vulkan compositor swapchain.
- A manual Mesa variable duplicates and overrides the first control rather than coordinating a third stage.

Start with the defaults. Change only Graphics `Present Mode` when testing presentation behavior, and measure frame pacing and latency rather than FPS alone.

## Useful test record

For a meaningful comparison, record:

- display refresh rate;
- game FPS cap and whether frame generation is active;
- Graphics `Present Mode`;
- manual `MESA_VK_WSI_PRESENT_MODE`, if any;
- average FPS plus visible tearing, stutter and input response;
- whether the behavior repeats after a clean game restart.

## Source map

- Graphics controls: [`GraphicsTab.kt`](../../app/src/main/java/app/gamenative/ui/component/dialog/GraphicsTab.kt)
- UI values: [`arrays.xml`](../../app/src/main/res/values/arrays.xml)
- defaults and persistence: [`Container.java`](../../app/src/main/java/com/winlator/container/Container.java)
- launch-time environment assignment and compositor mode mapping: [`XServerScreen.kt`](../../app/src/main/java/app/gamenative/ui/screen/xserver/XServerScreen.kt)
- renderer-mode UI state: [`ContainerConfigDialog.kt`](../../app/src/main/java/app/gamenative/ui/component/dialog/ContainerConfigDialog.kt)
- Java/JNI compositor control: [`VulkanRenderer.java`](../../app/src/main/java/com/winlator/renderer/VulkanRenderer.java), [`vulkan_jni.cpp`](../../app/src/main/cpp/winlator/vulkan_jni.cpp)
- native swapchain selection and FIFO fallback: [`VulkanRendererContext.cpp`](../../app/src/main/cpp/winlator/VulkanRendererContext.cpp)

## Sources

[1] https://docs.vulkan.org/refpages/latest/refpages/source/VkPresentModeKHR.html — VkPresentModeKHR — Vulkan API Reference
