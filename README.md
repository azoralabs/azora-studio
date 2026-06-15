# Azora Studio

A Kotlin Multiplatform game engine and visual editor built with Compose Multiplatform. Azora provides a modular architecture for building cross-platform applications with a focus on visual editing, docking layouts, and extensibility through plugins.

The project ships **two products**, each with its own per-platform apps:

- **Launcher** - creates, opens, and manages Azora projects.
- **Studio** - the editor workspace (docking panels, canvas, project tooling).

## Architecture

```
azora-studio/
├── launcherApp/             # Launcher product
│   ├── shared/              #   shared launcher UI (KMP: Android/iOS/Desktop)
│   ├── androidApp/          #   Android entry point
│   ├── desktopApp/          #   Desktop (JVM) entry point  →  dev.azora.launcher.MainKt
│   └── iosApp/              #   iOS entry point (Xcode project)
├── studioApp/               # Studio product
│   ├── shared/              #   shared studio UI/logic (KMP)
│   ├── androidApp/          #   Android entry point (scaffold)
│   ├── desktopApp/          #   Desktop (JVM) entry point  →  dev.azora.studio.MainKt
│   └── iosApp/              #   iOS entry point (scaffold, Xcode project)
├── azora-local/             # Local persistence (Room)
├── azora-sdk/               # SDK feature modules (canvas, color, docking)
├── azora-sdk-core/          # Core SDK (component, data, domain, io, presentation, project, theme, util)
├── azora-sdk-plugin/        # Plugin system (core, domain, presentation)
├── azora-shared/            # Shared utilities
├── build-config/            # Generated build configuration
└── build-logic/             # Gradle convention plugins
```

All code lives under the `dev.azora.*` package namespace.

## Modules

### launcherApp
The launcher product. `shared` holds the launcher UI (rendered on all platforms); `androidApp`, `desktopApp`, and `iosApp` are thin per-platform entry points.

### studioApp
The Studio editor product, featuring window-state management, a project manager, and an editor workspace with docking panels. Most code lives in `studioApp/shared`; `desktopApp` is the primary entry point, with `androidApp`/`iosApp` scaffolded for future mobile support.

### azora-local
Local database module (`:azora-local:database`) using **Room** (KMP) for persistent storage.

### azora-sdk
SDK feature modules:
- **docking** - Professional docking system with split panels, tab groups, floating windows, and drag-and-drop (`data`/`domain`/`presentation`).
- **canvas** - Node-based visual editor canvas with links, ports, and reroute points (`domain`/`presentation`).
- **color** - Color picker with triangle wheel and ARGB slider modes (`presentation`).

### azora-sdk-core
Core SDK components:
- **component** - Reusable design-system UI components and debug utilities
- **data** - Data layer utilities and state management
- **domain** - Domain models and business logic
- **io** - File I/O and serialization
- **presentation** - Presentation-layer utilities (camera, permissions, navigation, lifecycle, undo/redo)
- **theme** - Azora design system, typography, and color palettes
- **util** - Common utilities
- **project** - Project model, settings, and repositories (`data`/`domain`/`presentation`)

### azora-sdk-plugin
Plugin system for extending Azora functionality (`core`/`domain`/`presentation`).

### azora-shared
Shared code and utilities used across modules.

## Target Platforms

- **Desktop (JVM)** - primary target
- **Android**
- **iOS**

## Build & Run

### Desktop

```shell
# Launcher
./gradlew :launcherApp:desktopApp:run

# Studio
./gradlew :studioApp:desktopApp:run
```

### Android / iOS

Use the bundled Android Studio run configurations: `launcherAndroidApp`, `studioAndroidApp`, `launcherIosApp`, `studioIosApp` (iOS configs require Xcode).

## Tech Stack

- **Kotlin Multiplatform** 2.4.0 - cross-platform development
- **Compose Multiplatform** 1.11.1 - declarative UI framework
- **Gradle** 9.4.1 with **AGP** 9.2.1 and Gradle **convention plugins** (`build-logic`)
- **Koin** - dependency injection
- **Room** - type-safe local persistence
- **KSP** - annotation processing (Room)
- **Kotlinx Serialization** - JSON serialization
- **Kotlinx Coroutines** - asynchronous programming
- **LWJGL (Vulkan/Shaderc)** - desktop rendering backend (Studio)

## Project Size

| Module                | Files | Lines  |
|-----------------------|-------|--------|
| launcherApp           | 4     | 711    |
| studioApp             | 76    | 13,217 |
| azora-local           | 15    | 435    |
| azora-sdk             | 104   | 13,181 |
| ├─ canvas             | 37    | 4,661  |
| ├─ color              | 13    | 1,146  |
| └─ docking            | 54    | 7,374  |
| azora-sdk-core        | 150   | 9,257  |
| ├─ component          | 14    | 1,419  |
| ├─ data               | 22    | 1,400  |
| ├─ domain             | 17    | 1,029  |
| ├─ io                 | 9     | 584    |
| ├─ presentation       | 58    | 2,521  |
| ├─ project            | 14    | 697    |
| ├─ theme              | 12    | 1,224  |
| └─ util               | 4     | 383    |
| azora-sdk-plugin      | 23    | 995    |
| azora-shared          | 23    | 620    |
| build-config          | 3     | 20     |
| build-logic           | 12    | 639    |
| **Total**             | **410** | **39,075** |

## Status

- AzScript / azora-lang language integration is **temporarily disabled** while the `azora-lang` toolchain is brought up to date; the editor runs with the feature off.
- `studioApp` and `launcherApp` mobile (Android/iOS) entries are scaffolded; desktop is the fully-supported target.

## Future Work

- **Fullstack web & backend** - a **Kobweb** web frontend and a **Ktor** backend service, sharing the SDK models for a browser-based launcher/editor and cloud project storage.
- Re-enable AzScript with the updated azora-lang compiler (lexer/parser/codegen) and in-editor diagnostics.
- Native code generation backends (LLVM, WASM, C#).
- Plugin marketplace and hot-reload support.
- Collaborative editing and real-time project sharing.
- Asset pipeline integration (textures, audio, 3D models).
- Profiling and debugging tools within the editor.

## License

Proprietary - DoubleGArts
