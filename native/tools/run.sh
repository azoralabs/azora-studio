#!/bin/bash
# Build and open a native development window.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-studio}"
if [[ "$MODE" != studio && "$MODE" != launcher ]] || [[ "$MODE" = launcher && $# -gt 1 ]] || [ "$#" -gt 2 ]; then
    echo "usage: run.sh [launcher | studio [project]]" >&2
    exit 1
fi
source "$ROOT/tools/toolchain-env.sh"
if [ "$MODE" = studio ]; then
    export AZORA_STUDIO_PROJECT="${2:-${AZORA_STUDIO_PROJECT:-}}"
    case "$AZORA_STUDIO_PROJECT" in /*|"") ;; *) echo "The project must be an absolute path" >&2; exit 1 ;; esac
    export AZORA_STUDIO_PREVIEW_PROJECT="${AZORA_STUDIO_PREVIEW_PROJECT:-$AZORA_STUDIO_PROJECT}"
fi
[ -f "$AZORA_ENGINE_HOME/runtime/build/libazora_runtime.dylib" ] || bash "$AZORA_ENGINE_HOME/runtime/build.sh"
bash "$ROOT/tools/build-window.sh" "$MODE"
if [[ "$MODE" = studio && -n "$AZORA_STUDIO_PROJECT" && ! -e "$AZORA_STUDIO_PROJECT" ]]; then
    TEMPLATE=game-ecs
    if [ "$AZORA_STUDIO_PROJECT" = "$ROOT/build/demo-project" ]; then TEMPLATE=dolomites-drive; fi
    bash "$ROOT/tools/build.sh" >/dev/null
    AZORA_STUDIO_ACTION=template AZORA_STUDIO_TEMPLATE="$AZORA_ENGINE_HOME/templates/$TEMPLATE" "$ROOT/build/azora-studio-services"
fi
if [[ "$MODE" = studio && "$AZORA_STUDIO_PROJECT" = "$ROOT/build/demo-project" ]]; then
    bash "$AZORA_ENGINE_HOME/tools/import-standard-materials.sh" "$AZORA_STUDIO_PROJECT"
fi
exec bash "$ROOT/tools/launch.sh" "$MODE"
