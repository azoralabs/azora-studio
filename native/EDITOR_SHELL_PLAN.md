# Studio editor shell — design and plan (2026-10-06)

Goal: Azora Studio looks and works like a real engine editor (reference: the
Bevy editor and launcher concepts), built on the Engine's reactive constructors
and ECS, with nothing hardcoded — panels, components, menus, templates and theme
tokens come from reflection or data files.

## Decisions (agreed)

- Approach A, ECS-native: the dock tree, editor state and the edited scene are
  ECS entities/components; panels are `@Panel` packs found by
  `reflect<*>.withAnnot<Panel>`; input becomes events, systems apply them, and a
  window recomposes only when a revision it reads changes.
- Inspector edits Engine built-in components first (`@Component` packs reflected
  with `reflect<C>.fields`); project components come later by compiling Studio
  with the project.
- Docking includes floating OS windows (multi-window platform layer).
- Menus live in the OS menu bar (macOS `NSMenu` now; Linux global menu with the
  Linux backend, in-window fallback).
- AZLS pastel palette (`azora-lang-code-website/src/index.css`
  `--color-pastel-*`, IDE `AzoraPalette.kt`) for Studio and the editor.
- Azora style: `when` over `if` chains; receivers as `func Type&.m()`.

## Compiler fixes (azora-lang, done first)

- [x] `inline for` over `reflect<*>.withAnnot<D>` expands inside lambdas, `using`,
      `defer`, `try`, `effect` (generic `AstMapper`).
- [x] `inline for` over `reflect<T>.fields` expands in free functions and tests;
      `f.name` / `f.typeName` fold to strings.
- [x] Compile-time folding reaches lambda bodies (`inline for` over ranges inside
      a trailing lambda).
- [x] A nested lambda that calls an inline callable parameter captures it by
      reference (resolver + LLVM capture scan) — no more undefined `@content`.

## Engine packages

| Package | Contents |
|---|---|
| `azora-ui-docking` (new, `engine.ui.docking`) | `@Panel(title, icon, area, order)`; components `DockNode`/`DockSplit`/`DockTabs`/`DockTab`/`DockWindow`; `DockSpace` ctor; splitter + tab drag, drop zones, tear-out; `DockOp` events; AZON layout persistence |
| `azora-gizmos` (new, `engine.gizmos`) | world-space line/circle/arrow/box/grid/frustum drawing; translate/rotate/scale handles with axis+plane picking and snapping; orientation widget; `EditorCamera` (orbit/pan/zoom/fly/focus, 2D pan/zoom) |
| `azora-scene` (new, `engine.scene`) | built-in `@Component`s (`Name`, `Parent`, `Transform`, `Visibility`, `MeshRenderer`, `Material`, `Camera`, `Light`), reflection AZON (de)serializer, scene file v2 |
| `azora-platform` | multi-window; menu bar model → `NSMenu`; folder picker; open URL |
| `azora-gpu` / `azora-render` | rotated AA lines; drawing into a rectangle (viewport remap + scissor); orthographic camera; sphere/plane meshes |
| `azora-image` | PNG/JPEG via ImageIO |
| `azora-ui` | `Icon` (Lucide outlines rasterized by CoreGraphics), `Image`, text field, drag-number field, clip |

## Phases (each ends with a Studio launch)

1. Pastel theme tokens from `theme.azon`, applied across Studio and the editor.
2. `azora-ui-docking`; Studio shell rebuilt on it (panels discovered by reflection).
3. Scene v2 + reflection inspector (Add/Remove Component, scene tree with hierarchy).
4. Viewport: 3D in the panel rect + 2D mode, `EditorCamera`.
5. `azora-gizmos`: transform handles, orientation widget, grid.
6. Menu bar (`NSMenu`), multi-window + floating panels.
7. Launcher redesign (hero banner, sidebar, projects table, search, open/new).
8. Icons (Lucide) throughout.

## Verification

- Compiler: focused unit tests + full `:compiler:desktopTest` compared by identity.
- Engine: `tests/qualify-native.sh`; new packages get headless tests.
- Studio: `AZORA_STUDIO_SCRIPT` sessions with screenshots after every phase; ASan
  build before calling a phase done.
