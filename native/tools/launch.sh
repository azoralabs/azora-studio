#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-studio}"
case "$MODE" in
    studio) source "$ROOT/tools/toolchain-env.sh" ;;
    launcher) export AZORA_STUDIO_CATALOG="${AZORA_STUDIO_CATALOG:-$ROOT/build/catalog}" ;;
    *) echo "usage: launch.sh [studio|launcher]" >&2; exit 1 ;;
esac
BUILD="${AZORA_STUDIO_BUILD_DIR:-$ROOT/build/$MODE}"
APP="$BUILD/Azora $MODE Development.app/Contents"
if [ ! -s "$APP/Resources/shaders/scene.metallib" ] || [ ! -x "$APP/MacOS/azora-$MODE" ]; then
    echo "Build Azora $MODE first with native/tools/run.sh $MODE" >&2
    exit 1
fi
exec "$APP/MacOS/azora-$MODE"
