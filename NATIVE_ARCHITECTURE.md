# Native Azora Studio architecture contract

Decision: 2026-10-04. Studio and Launcher application code must be written in
azora-lang and compiled by LLVM to native executables. JAR/JVM integration is
excluded from the product architecture.

## Required implementation

- Native entry points, window and input handling, renderer, docking panels,
  project browser, inspector, scene editor, undo/redo and asset tools in Azora.
- Engine and editor packages compiled together against a versioned native ABI.
  Platform graphics/window/file APIs are reached through the Engine native host;
  they must not use Java bindings or a JVM process.
- Project creation, load/save, build/play and compiler diagnostics exposed through
  native Azora services. The current Kotlin AZLS JAR bridge is a legacy prototype
  mechanism; native Studio needs an Azora language-service implementation or a
  native protocol endpoint. Invoking the existing JVM compiler is not sufficient
  to qualify a JVM-free installed Studio.
- Native serialization and persistence; migration of Room-backed project data.
- Azora plugin interfaces with explicit ABI versioning and ownership rules.
- Packaged matching native compiler, runtime, standard library and Engine tooling.

The existing Kotlin compiler may serve as a development bootstrap while Azora's
native toolchain is implemented. It is not an accepted runtime dependency of the
shipped Studio or its bundled build/language-service tools.

## Acceptance gates

1. Build and run a windowed Azora editor using LLVM output; inspect its linked
   dependencies and package contents to establish that no JVM or JAR is required.
2. Create → edit → build → play → save → reopen a real Engine project in that
   installation, including diagnostics and source navigation.
3. Exercise native renderer/input callbacks, owned resource replacement,
   cancellation, shutdown, editor undo and reload under available sanitizers.
4. Reproduce clean builds on each declared supported desktop platform; measure
   compile time, frame time and memory before adding platforms.

## Current status

Updated 2026-10-06. The windowed Studio and the Launcher run as native LLVM
executables written in Azora. The retained constructor workspace renders through
an Engine paint list (recorded once per layout, replayed each frame) with the
Engine's SDF rounded rectangles, borders, hover, text styles and a rasterised
text cache. Studio frame interval is 9–10 ms with about 100 MB resident. Source
editing has UTF-8 input, selection, clipboard and bounded coalesced undo. The
native compiler's semantic diagnostics are listed, underlined at their exact
spans and navigable. Build and run use the native compiler through the C host's
process boundary.

Qualification under Apple ASan/UBSan covers the headless workspace probe, the
windowed Studio and scripted interaction sessions (`AZORA_STUDIO_SCRIPT`, with
self-captured screenshots), in addition to the service, host and scene probes.
The compiler repairs this needed (copy-on-keep for named `Copy` values, `Clone`
witnesses for erased generics, by-value returns of borrowed places, and
exclusive borrows passed by storage address in native code) are in
`native/NATIVE_QUALIFICATION_2026_10_06.md`.

Still open: an installed matching compiler/Engine bundle, completion/hover/
rename and multi-file projects, arbitrary scene/component persistence, asset
workflows, the project catalogue and Room migration, plugin ABI, recursive
destruction of a world's registered components (Engine `foundation` gate),
performance baselines beyond this machine, and additional platforms. The
Kotlin/Compose prototype is preserved as a behaviour reference; its tests cannot
close native gates.
