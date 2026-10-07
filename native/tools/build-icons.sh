#!/bin/bash
# Rasterises the Lucide icons resources/icons.azon lists into resources/icons.png
# (the atlas Studio and the Launcher draw from) and resources/icons-index.azon.
# Needs node and swift; the outputs are checked in, so building Studio does not.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LUCIDE="${LUCIDE_REACT:-$(cd "$ROOT/../.." && pwd)/azora-lang-website/node_modules/lucide-react}"
SVG="$ROOT/build/icons-svg"
node "$ROOT/tools/build-icons.mjs" "$ROOT/resources/icons.azon" "$LUCIDE" "$SVG"
xcrun swift "$ROOT/tools/build-icons.swift" "$ROOT/resources/icons.azon" "$SVG" "$ROOT/resources/icons.png" "$ROOT/resources/icons-index.azon"
