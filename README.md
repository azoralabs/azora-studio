# Azora Studio

Azora Studio and Launcher must be written entirely in **azora-lang** and compiled
through **LLVM** to native executables. The installed product must have no
JAR/JVM dependency, including its bundled build tools and language services.

- **Launcher** creates, opens and manages Azora projects.
- **Studio** provides the editor workspace: docking panels, scene editing,
  project and asset tools, source intelligence, build/play and undo/redo.

## Architecture

Application code, UI, editor state and project services belong in Azora packages.
The Engine provides native rendering, window/input and platform integration.
Compiler and language services communicate through native APIs or a native
protocol endpoint. Installed tools must match the Engine/runtime/stdlib versions.

The [native architecture contract](NATIVE_ARCHITECTURE.md) records the required
implementation and acceptance gates.

## Current status

**Native Studio is in development and is not ready for installation.** The
[`native/` development slice](native/README.md) contains real `.az` application
sources, native editor persistence/history, an Engine ECS scene model and a
retained constructor UI for Studio and Launcher. Services now use the actual
Kotlin/Native compiler for semantic diagnostics and native build/play. Service
and scene probes pass under Apple ASan/UBSan, including real Engine template
creation/build/play. The original native window is visible; the richer retained
workspace and installed toolchain qualification remain open.

Preserve the Kotlin/Compose prototype as behavior/UI reference for the port;
its successful builds do not qualify the native Studio.

The [foundation inventory](../azora-lang/ROADMAPs/FOUNDATION_REPAIR_2026_10_04.md)
tracks the remaining ownership, ABI and Engine compatibility repairs. The native
compiler exists; installed distribution and complete editor language services
remain acceptance gates. Slice evidence is in
[native qualification](native/NATIVE_QUALIFICATION_2026_10_05.md).

Existing prototype architecture and build instructions are documented separately
in [the legacy prototype reference](docs/LEGACY_PROTOTYPE.md).

## Native acceptance

A matching installed native toolchain must create, edit, build, play, save and
reopen a real Engine project, with diagnostics and source navigation. Qualification
includes dependency inspection, ownership/resource shutdown, callback behavior,
sanitizer checks and reproducible builds on each supported platform.

## License

Proprietary - DoubleGArts
