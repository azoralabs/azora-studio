# Native Studio qualification — 2026-10-06

Executed on macOS 27 arm64 with Apple Clang 21.0.0 and the native compiler built
from `azora-lang` 0.1.0-dev (`compiler/build/bin/macosArm64/{debug,release}Executable/azora.kexe`).
No JVM participates in any Studio, Launcher, service or tool run below.

## Passing

- **Windowed Studio runs.** `tools/build-window.sh studio` links the retained
  workspace; it opens, renders and shuts down cleanly. 150 frames under Apple
  ASan/UBSan report nothing. Release frame interval is 9–10 ms, peak resident
  memory about 100 MB.
- **Headless workspace probe** (`src/workspace-probe.az`) passes under ASan/UBSan:
  constructor tree, keyed identity, skipped recomposition, layout, toolbar hit
  test, console dock width and disposal with no live host handles.
- **Scripted interaction.** `AZORA_STUDIO_SCRIPT` drives the window through the
  same paths as a user (actions, clicks, text, keys, ⌘-chords) and `shot <name>`
  captures the window itself into `AZORA_STUDIO_SHOTS`. Exercised under ASan:
  open source, type UTF-8 text, Enter with indentation, arrows, Home/End, ⌘A
  selection, typing over a selection, ⌘Z undo, Problems navigation.
- **Diagnostics and navigation.** Studio runs the native compiler's `inspect`
  on open and after every save. It lists each diagnostic (severity, line:column,
  code, message), underlines the exact UTF-8 span in the editor, and opens the
  source at the reported position when a problem is clicked. Spans are marked
  stale after an edit until the next check.
- **Run app** builds and runs the actual project through the native compiler.
  Output streams into the console (`headless ecs passed` for the demo project).
- **Launcher** renders the template gallery and creates a project from the
  selected Engine template, then opens Studio on it.
- `tests/qualify.sh` passes with the native release compiler: host probes under
  ASan/UBSan, create/edit/undo/redo/save/reopen, structural spans, inspect/build/
  play and template create/build/play of the Engine ECS project.
- Engine gates: `qualify-native.sh headless` and `qualify-native.sh game` pass.
  The shipping game template builds and runs its bounded frames natively.

## Compiler repairs this qualification depended on

All in `azora-lang`, each with a regression test in
`FoundationArgumentOwnershipExecTest` that runs interpreter, LLVM debug and
LLVM release under ASan/UBSan. Full compiler suite: 2,849 tests, the same 6
failures as the untouched baseline (ABI timeouts from the Homebrew clang ASan
deadlock, Wasm clone, GpuStdlib, two SemanticFacts tests).

1. A named `Copy` value stored into a container, a field, or a parameter that
   an erased generic keeps was shared, not copied. Scope cleanup then freed it
   under its keeper. This was the Studio's startup crash in `storageInsert`.
2. `.clone()` on an erased `T: Clone` returned the original pointer. Inline
   `<T: Clone>` bounds were parsed and dropped. They now mean `where T: Clone`,
   and a `Clone` witness dispatches the concrete copy.
3. Returning a place the function does not own (a global such as
   `Entity::invalid`, a parameter, a field) by value handed the caller an alias
   it then freed.
4. Exclusive borrows (`x!`) never wrote back in native code: `bump(n: Int!)`,
   array growth and assignment were lost. LLVM now passes such scalars, strings
   and collections by the address of the caller's storage. The interpreter
   writes back fields and elements, and constant propagation treats borrowed
   arguments as written.
5. `VariadicMonomorphizer` used the JVM-only `putIfAbsent`, so the native
   compiler did not build.

## Open

- `qualify-native.sh foundation`: `owned-ecs` passes every insert/replace/
  remove/despawn check but a `World` leaving scope does not destroy the
  components it registered (recursive field destruction, F09).
- A call to a bounded generic with explicit type arguments specialises it; an
  inline `store(pass)` argument inside such a call then folds `T.typeName` to
  `"T"`. The Engine's paint code names its storages explicitly until the
  resolver infers through specialised parameters.
- Free extension members (`func (self: Modifier&).card()`) do not resolve from
  another module, so the Launcher repeats the Studio's few theme helpers.
- A program function named like an LLVM runtime helper (`isDigit`) collides at
  link time.
- Scene format is still the fixed Cube/Camera/Light set; arbitrary entities,
  components, assets, project catalogue and installed packaging remain open, as
  do completion/hover/rename and multi-file projects.
