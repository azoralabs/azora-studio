# Native Studio

Studio and Launcher are authored in Azora and compiled to native LLVM executables.
The native compiler is the existing semantic pipeline compiled with Kotlin/Native;
it needs no JVM at runtime. This remains a development build with open product
qualification gates, recorded in `NATIVE_ARCHITECTURE.md`.

## Run it

```sh
bash native/tools/run.sh                         # Studio Project Browser
bash native/tools/run.sh studio /abs/path/project
bash native/tools/package-studio.sh 0.1.0-dev     # add a package to the local catalog
bash native/tools/run.sh launcher                # Studio Library
```

`run.sh` selects `../azora-lang`'s release native compiler (falling back to the
debug one), the standard library and the Engine beside this checkout, builds the
window, and creates a project from the Engine's ECS template when an explicit
new project directory is supplied. Override the defaults with
`AZORA_NATIVE_COMPILER`, `AZORA_STDLIB`, `AZORA_ENGINE_HOME` or `AZORA_STUDIO_PROJECT`.

Choose **Install** in the Launcher to copy a catalog package into
`~/Library/Application Support/Azora Studio/Versions`. **Install from file…**
selects a Studio `.app` package from another folder. **Launch** opens that
version's Project Browser. **Projects** watches `~/Documents/Azora` for
projects and reads the display names from `azora.azon`. A `thumbnail.png`,
`thumbnail.jpg` or `thumbnail.jpeg` in a project's root supplies its image.
You can also open a recent project, browse for a directory or `azora.azon` file,
or create a project from a template.
The development catalog is `native/build/catalog`. Set `AZORA_STUDIO_CATALOG`
or `AZORA_STUDIO_LIBRARY` to use different directories.

Packages contain the corresponding native compiler, Engine, stdlib, runtime
and shaders for macOS on Apple silicon. Building and running projects uses the
macOS developer tools and Python 3.

## Apple silicon releases

The public Launcher reads `https://azorastudio.org/downloads/macos-arm64/catalog.json` at startup, on **Refresh**, and once a minute while open. **Update** replaces the installed Studio bundle atomically when the catalog offers a higher `versionCode` for the same Studio version. Studio must be closed during the replacement. Projects stay in their own folders, with `~/Documents/Azora` as the default.

```sh
bash native/tools/package-release.sh 1 native/build/macos-test --local
```

`--local` creates a DMG and Studio archive for loopback testing. Serve the output directory on `127.0.0.1` and set `AZORA_STUDIO_CATALOG_URL` to its `catalog.json` URL when launching the test app. `--public` signs the apps and notarizes the distribution through Apple. Set `AZORA_SIGN_IDENTITY` to the Developer ID Application identity, `AZORA_SIGN_TEAM` to its Team ID, and `AZORA_NOTARY_PROFILE` to a configured Keychain profile.

To publish, attach `catalog.json`, the Launcher DMG and the Studio ZIP to the public `macos-v0.1-dev` release in `azoralabs/azora-studio`. For updates, increment `versionCode`, upload the new archives, then replace `catalog.json`. Run the `azora-studio-website` deployment to publish the verified files at `azorastudio.org`. The website displays the download after the public catalog is served.

## Project previews and materials

Set `project.editorPreviewSources` in `azora.azon` to the project-relative
`.az` modules to include in the development Studio build. Run `run.sh studio`
with that project after changing those modules. Preview systems declare
`@System(editor: true)`. Also list the other project modules they import
unless they already come from an Engine package.

**Preferences → Scene Viewport → Simulations** pauses the editor's simulation
clock and resumes from the displayed frame. Startup and render systems still
prepare and draw the scene. Update systems that synchronize authored inputs
without advancing time can also declare `editorWhilePaused: true`.

The Assets panel creates material shaders as `.az` files with an
`@Execution("GPU")` module. Saving a shader requests a Metal build and reloads
the compiled material in the viewport.

## What is in it

- `src/window.az` — the Studio window: frame loop, input, the custom-drawn source
  editor (Menlo, syntax colours, line gutter, selection, caret), scene viewport
  (grid, entity glyphs, selection gizmo, drag to move) and console.
- `src/workspace.az` — the retained Engine constructor tree: toolbar, hierarchy,
  project browser, dock tabs, inspector with steppers, console/problems dock and
  status bar, plus the Azora design tokens as modifier links.
- `src/problems.az` — reads the native compiler's `inspect` protocol into
  diagnostics with UTF-8 line/byte spans.
- `src/editor.az` — source buffers with bounded undo/redo (typing within a word
  is one step), splice, atomic save and transactional reopen.
- `src/scene.az` — the Engine ECS scene: selection, transform edits, drag, undo,
  `AZORA-SCENE 1` persistence with whole-file validation, isolated preview.
- `src/launcher.az` — version library, installation and Studio launch.
- `src/projectbrowser.az` — Studio's templates, recent projects and file picker.
- `src/intelligence.az` — structural spans; `src/main.az` the console services.
- `host/` — the C host: checked byte-buffer handles, confined project files and
  cancellable child processes. No editor or compiler logic.

Shortcuts: ⌘S save (then re-check), ⌘Z / ⇧⌘Z undo/redo, ⌘B build, ⌘R run,
⌘A / ⌘C / ⌘X / ⌘V on the source; Shift extends the selection with arrows,
Home/End and clicks; Enter keeps indentation; Tab inserts four spaces.

## Scripted sessions

`AZORA_STUDIO_SCRIPT` replays input through the same code paths as a user, so a
session can be exercised and inspected without anyone at the keyboard:

```sh
AZORA_STUDIO_SHOTS=/tmp/shots AZORA_STUDIO_SCRIPT="wait 120; action 31; shot problems; \
action 300; shot source; meta 65; shot selected; type hello; meta 90; quit" \
AZORA_STUDIO_PROJECT=/abs/project build/studio/azora-studio
```

`wait N` skips frames, `action N` fires a control by its action code, `click X Y`
hit-tests a point, `type TEXT` enters text, `key N` presses an Engine key code,
`meta N` presses it with ⌘, `shot NAME` captures the window to
`$AZORA_STUDIO_SHOTS/NAME.png` (the script waits for the capture), `quit` exits.

## Services and qualification

```sh
export AZORA_NATIVE_COMPILER=/absolute/path/to/azora.kexe
export AZORA_ENGINE_HOME=/absolute/path/to/azora-engine
bash native/tests/qualify.sh
bash native/tools/build-window.sh [studio|launcher|model-probe|workspace-probe]
```

Apple `/usr/bin/clang` is selected by default; `AZORA_CLANG` overrides it. The
installed Homebrew Clang 21.1.4 ASan runtime deadlocks during initialization on
this macOS 27 host, before application code; Apple Clang passes the sanitizer
probes. Build any window with `AZORA_STUDIO_SANITIZER_FLAGS="-fsanitize=address,undefined -g"`.

The console service uses environment-selected project/action values:

```sh
AZORA_STUDIO_PROJECT=/absolute/new/project AZORA_STUDIO_ACTION=create native/build/azora-studio-services
AZORA_STUDIO_PROJECT=/absolute/project AZORA_STUDIO_ACTION=inspect native/build/azora-studio-services
```

`create` refuses existing directories. `template` creates a project from the
selected `AZORA_STUDIO_TEMPLATE` directory. `probe` checks create/edit/undo/redo/
save/reopen and zero live host handles. `structure` emits TSV records with UTF-8
byte spans. `inspect` invokes the selected native compiler and emits its
versioned JSON diagnostic protocol. `build`/`play` pass project root and action
as argv to the native compiler without a shell; output is bounded and drained,
and cancellation kills and joins owned children. Script/JAR entry points are
rejected.

Latest evidence: [NATIVE_QUALIFICATION_2026_10_06.md](NATIVE_QUALIFICATION_2026_10_06.md)
(previous: [2026-10-05](NATIVE_QUALIFICATION_2026_10_05.md)). Installed compiler/
package selection, completion/hover/rename, arbitrary scene schemas, assets, Room
migration, plugin ABI, packaging and other platforms remain open.
