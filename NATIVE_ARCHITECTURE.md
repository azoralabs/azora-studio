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

The native implementation in `native/` now includes Azora source/history state,
atomic persistence, structural spans, an actual Engine ECS scene model and a
retained constructor tree for Studio and Launcher. Contextual constructors such
as `Column`, `Button`, `SceneViewport` and `PropertyInspector` return Engine
entities. A C host supplies checked byte-buffer handles, confined file access and
cancellable native process execution; it contains no editor or compiler logic.

The existing semantic compiler pipeline has been compiled with Kotlin/Native as
an actual macOS arm64 executable, with no JVM runtime. `AZORA_NATIVE_COMPILER`
selects it for Studio build/play/inspect. Native service qualification passes
create/edit/undo/redo/save/reopen, exact structural spans, native compiler semantic
JSON diagnostics and real build/play. Native template creation and build/play of
the actual Engine ECS fixture also pass through the Studio service endpoint.

The Engine ECS scene probe passes native selection, transform edits, undo,
transactional save/reopen, invalid-file rollback, isolated play/pause/stop and
destruction under Apple ASan/UBSan. The original native source window was launched
and visibly inspected. The richer constructor workspace typechecks and its
retained-tree probe exposed Engine component snapshot ownership faults; graphical
linking exposed an early-return defer capture fault in GPU initialization. Those
repairs and the resulting richer graphical execution remain under qualification.
An attempted CUA Save interaction was not executed because automatic approval
review reached a usage limit, so user interaction gates are still open.

Studio services use Apple Clang by default. The installed Homebrew Clang ASan
runtime deadlocks before main on this macOS host; this was isolated independently
of application code. Apple ASan/UBSan successfully runs the host and scene probes.
Unsanitized console services link `libSystem`; sanitizers add their native runtime.

The development build has not established an installed matching compiler/Engine
bundle, complete asynchronous semantic editor features, source navigation,
Unicode native input, arbitrary scene/component persistence, asset workflows,
project catalogue/Room migration, plugin ABI, performance baselines or additional
platforms. These remain open acceptance gates. The Kotlin/Compose prototype is
preserved as a behavior reference; its tests cannot close native gates. Detailed
slice evidence is in `native/NATIVE_QUALIFICATION_2026_10_05.md`.
