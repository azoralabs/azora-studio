#!/bin/bash
# Reuse Studio's matching Engine shaders when building a game from the installed toolchain.
set -euo pipefail
TOOLS="$(cd "$(dirname "$0")" && pwd)"
SOURCE="$TOOLS/shader-cook-source.sh"
case "${1:-}" in --studio|'') exec bash "$SOURCE" "$@" ;; esac
if [ "${3:-${AZORA_SHADER_TARGETS:-metallib spv dxil}}" != metallib ]; then
    exec bash "$SOURCE" "$@"
fi
SHADERS="$TOOLS/../../../shaders"
test -s "$SHADERS/scene.metallib"
OUTPUT="${2:?Select a shader output directory.}"
mkdir -p "$OUTPUT"
STAGE="$(mktemp -d "$OUTPUT/.prebuilt-shaders.XXXXXX")"
trap 'rm -rf "$STAGE"' EXIT
for shader in "$SHADERS/"*.metallib; do
    name="$(basename "$shader")"
    if ! cmp -s "$shader" "$OUTPUT/$name"; then
        cp "$shader" "$STAGE/$name"
        mv "$STAGE/$name" "$OUTPUT/$name"
    fi
done
AZORA_SHADER_SCOPE=project bash "$SOURCE" "$@"
