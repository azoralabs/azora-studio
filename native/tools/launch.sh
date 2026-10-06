#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-studio}"
case "$MODE" in studio|launcher) ;; *) echo "usage: launch.sh [studio|launcher]" >&2; exit 1 ;; esac
: "${AZORA_STUDIO_PROJECT:?Select an absolute project directory}"
: "${AZORA_NATIVE_COMPILER:?Select the matching native compiler executable}"
export AZORA_ENGINE_HOME="${AZORA_ENGINE_HOME:-$(cd "$ROOT/../../azora-engine" && pwd)}"
export AZORA_STUDIO_EXECUTABLE="${AZORA_STUDIO_EXECUTABLE:-$ROOT/build/studio/Azora studio Development.app/Contents/MacOS/azora-studio}"
exec "$ROOT/build/$MODE/Azora $MODE Development.app/Contents/MacOS/azora-$MODE"
