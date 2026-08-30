# GameNative technical documentation

This directory is a code-grounded guide to the settings that are difficult to understand from the app alone.

The documentation targets the current `y700/stable` branch. It deliberately separates:

- **Confirmed:** behavior visible in the current source tree.
- **Recommended:** a conservative operating choice for this fork.
- **Needs measurement:** a plausible claim that has not been demonstrated on a Lenovo Legion Y700 Gen 3.

Discord messages can identify useful questions, but they are not treated as the source of truth. When the code does not explain the intended difference between two options, this guide says so.

## Start here

- [Graphics stack and driver families](graphics/driver-stack.md)
- [Present modes and the three similarly named settings](graphics/present-modes.md)
- [Box64, FEXCore and Bionic containers](emulation/box64-and-fexcore.md)
- [Lenovo Legion Y700 Gen 3 guide](devices/lenovo-legion-y700-gen3.md)
- [Troubleshooting and useful evidence](troubleshooting/diagnostics.md)

## Scope

This is not a compatibility database or a collection of universal “best settings.” GameNative combines several layers—Wine/Proton, CPU emulation, DirectX translation, Vulkan drivers and Android presentation—and a change that helps one title may hurt another.

The safest workflow is therefore:

1. begin with the app defaults or a known configuration;
2. change one layer at a time;
3. record the exact game, build and setting changed;
4. revert if the result is not repeatably better.

## Contributing

Documentation changes should identify the evidence behind a claim:

- link to the relevant source file or upstream component documentation;
- state the tested device/build when reporting observed behavior;
- label untested performance claims as hypotheses;
- avoid turning one successful game configuration into a global recommendation.

The implementation and validation contract for this maintained fork lives in [`AGENTS.md`](../AGENTS.md), [`STATE.md`](../STATE.md) and [`.agents/validation.md`](../.agents/validation.md).
