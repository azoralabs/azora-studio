# Native Studio

Studio and Launcher are authored in Azora and compiled to native LLVM executables.
The native compiler is the existing semantic pipeline compiled with Kotlin/Native;
it needs no JVM at runtime. This remains a development build with open product
qualification gates, recorded in `NATIVE_ARCHITECTURE.md`.

`src/editor.az` owns source buffers, undo/redo and atomic save/reopen.
`src/intelligence.az` implements structural spans in Azora. `src/scene.az` owns an
Engine ECS scene with selection, transform edits, undo, transactional persistence
and isolated play/pause/stop preview. `src/workspace.az` constructs the retained
Engine entity tree using contextual constructors returning `Entity`; it supplies
hierarchy/assets docks, scene/source tabs, inspector controls and console.
`src/window.az` drives input, retained composition, layout and render systems.
Launcher uses the same constructor DSL. The C host contains only OS/raw-storage
operations; its checked-handle ABI is in `host/azora_studio_host.h`.

Select the actual native compiler:

```sh
export AZORA_NATIVE_COMPILER=/absolute/path/to/azora.kexe
export AZORA_ENGINE_HOME=/absolute/path/to/azora-engine
bash native/tests/qualify.sh
bash native/tools/build-window.sh model-probe
bash native/tools/build-window.sh workspace-probe
bash native/tools/build-window.sh studio
bash native/tools/build-window.sh launcher
```

Apple `/usr/bin/clang` is selected by default; `AZORA_CLANG` overrides it. The
installed Homebrew Clang 21.1.4 ASan runtime deadlocks during initialization on
this macOS 27 host, before application code. Apple Clang 21.0.0 passes the
sanitizer probes. Bootstrap development remains explicitly available through
`AZORA_COMPILER_BIN` and `AZORA_STUDIO_DEVELOPMENT_BOOTSTRAP=1`; it cannot substitute
for native compiler qualification.

The console service uses environment-selected project/action values:

```sh
AZORA_STUDIO_PROJECT=/absolute/new/project \
AZORA_STUDIO_ACTION=create native/build/azora-studio-services

AZORA_STUDIO_PROJECT=/absolute/project \
AZORA_STUDIO_ACTION=inspect native/build/azora-studio-services

AZORA_STUDIO_PROJECT=/absolute/project \
AZORA_STUDIO_ACTION=edit AZORA_STUDIO_SOURCE='func main() { }' \
native/build/azora-studio-services

AZORA_STUDIO_PROJECT=/absolute/project native/build/studio/azora-studio
```

`create` refuses existing directories. `template` creates a project from the
selected `AZORA_STUDIO_TEMPLATE` directory; Launcher reuses the matching Engine
`templates/game` and `templates/app` sources. `probe` checks create/edit/undo/redo/save/
reopen and zero live host handles. `structure` emits TSV records with UTF-8 byte
spans and one-based byte line/column coordinates. `inspect` invokes the selected
native compiler and emits its versioned JSON semantic diagnostic protocol; when
no native compiler is selected it uses structural inspection. `build`/`play` pass
project root and action as argv to the selected native compiler without a shell.
Output is bounded and continues draining after truncation; cancellation kills
and joins owned children. Script/JAR entry points are rejected. A Mach-O header
alone does not prove a selected tool's entire dependency graph is native.

Studio distinguishes scene preview from `Run app`, which builds/runs the actual
project through the native compiler. Native Inspect currently displays compiler
protocol output in the console; diagnostic navigation, asynchronous document
versions, completion/hover/rename, UTF-16 integration and Unicode input remain
open. Source editing has one undo/redo snapshot and preserves UTF-8 file bytes.

The scene format `AZORA-SCENE 1` persists the initial Cube/Camera/Light transforms
in `scene.azscene`. Reopen validates the entire input before applying any change;
invalid files preserve the live scene. Arbitrary entity/component schemas,
asset import and scene-template migration remain open.

Launcher receives `AZORA_STUDIO_PROJECT` and `AZORA_STUDIO_EXECUTABLE`. It owns its
child Studio session, so closing Launcher cancels Studio. Project picking,
catalogue persistence, detached installed sessions and unsaved-close prompts
remain product work.

Qualification on 2026-10-05: the actual native compiler builds Studio services;
Apple ASan/UBSan host probes and native create/edit/undo/redo/save/reopen pass.
Native compiler inspect/build/play and precise ASCII semantic diagnostics pass
through the emitted Studio service boundary. Actual Engine ECS template creation,
native build and play also pass through that endpoint. The actual Engine ECS scene probe
passes edit/undo/save/reopen/invalid-file rollback/play/pause/stop/destruction under
Apple ASan/UBSan. The original native source window was launched and visibly
inspected. The richer constructor tree passes typecheck but exposed retained ECS
ownership and early-return defer emission faults under native qualification;
those remain under repair. UI interactions are unqualified because automatic
approval review rejected a CUA click after reaching its usage limit. The working
visible window is preserved until a repaired replacement is verified.

These slice results do not close installed compiler/package selection, full
language intelligence, Room migration, plugin ABI, packaging or platform gates.
