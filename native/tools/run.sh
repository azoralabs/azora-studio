#!/bin/bash
# Builds and opens the development Studio or Launcher against this checkout's
# toolchain: the native compiler from ../azora-lang and the Engine beside it.
#
#   bash native/tools/run.sh                     # Studio on build/demo-project
#   bash native/tools/run.sh studio /abs/project # Studio on a project
#   bash native/tools/run.sh launcher /abs/new   # Launcher creating a project there
#
# Every variable can be overridden from the environment.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-studio}"
case "$MODE" in studio|launcher) ;; *) echo "usage: run.sh [studio|launcher] [project]" >&2; exit 1 ;; esac
LANG_HOME="${AZORA_LANG_HOME:-$(cd "$ROOT/../../azora-lang" && pwd)}"
RELEASE="$LANG_HOME/compiler/build/bin/macosArm64/releaseExecutable/azora.kexe"
DEBUG="$LANG_HOME/compiler/build/bin/macosArm64/debugExecutable/azora.kexe"
if [ -z "${AZORA_NATIVE_COMPILER:-}" ]; then
    if [ -x "$RELEASE" ]; then AZORA_NATIVE_COMPILER="$RELEASE"; else AZORA_NATIVE_COMPILER="$DEBUG"; fi
fi
if [ ! -x "$AZORA_NATIVE_COMPILER" ]; then
    echo "No native compiler. Build one with: (cd $LANG_HOME && ./gradlew :compiler:linkReleaseExecutableMacosArm64)" >&2
    exit 1
fi
export AZORA_NATIVE_COMPILER
export AZORA_STDLIB="${AZORA_STDLIB:-$LANG_HOME/std}"
export AZORA_ENGINE_HOME="${AZORA_ENGINE_HOME:-$(cd "$ROOT/../../azora-engine" && pwd)}"
export AZORA_STUDIO_RESOURCES="${AZORA_STUDIO_RESOURCES:-$ROOT/resources}"
export AZORA_STUDIO_PROJECT="${2:-${AZORA_STUDIO_PROJECT:-$ROOT/build/demo-project}}"
case "$AZORA_STUDIO_PROJECT" in /*) ;; *) echo "The project must be an absolute path" >&2; exit 1 ;; esac

[ -f "$AZORA_ENGINE_HOME/runtime/build/libazora_runtime.dylib" ] || bash "$AZORA_ENGINE_HOME/runtime/build.sh"
bash "$ROOT/tools/build-window.sh" studio
if [ "$MODE" = launcher ]; then
    bash "$ROOT/tools/build-window.sh" launcher
    export AZORA_STUDIO_EXECUTABLE="${AZORA_STUDIO_EXECUTABLE:-$ROOT/build/studio/azora-studio}"
elif [ ! -d "$AZORA_STUDIO_PROJECT" ]; then
    # A Studio needs a project to open; start one from the Engine's ECS template.
    bash "$ROOT/tools/build.sh" >/dev/null
    AZORA_STUDIO_ACTION=template AZORA_STUDIO_TEMPLATE="$AZORA_ENGINE_HOME/templates/game-ecs" "$ROOT/build/azora-studio-services"
fi
exec "$ROOT/build/$MODE/azora-$MODE"
