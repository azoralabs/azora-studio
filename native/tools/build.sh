#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLANG="${AZORA_CLANG:-/usr/bin/clang}"
BUILD="$ROOT/build"
COMPILER="${AZORA_NATIVE_COMPILER:-${AZORA_COMPILER_BIN:-}}"
if [ -z "$COMPILER" ] || [ ! -x "$COMPILER" ]; then
    echo "Select an executable native compiler with AZORA_NATIVE_COMPILER." >&2
    exit 1
fi
# Explicit bootstrap mode compiles development Azora application sources; it
# does not qualify the compiler, an installed product, or bundled services.
if [ -z "${AZORA_NATIVE_COMPILER:-}" ] && [ "${AZORA_STUDIO_DEVELOPMENT_BOOTSTRAP:-0}" != 1 ]; then
    echo "Native compiler/package qualification is still open. Set AZORA_STUDIO_DEVELOPMENT_BOOTSTRAP=1 for a development build." >&2
    exit 1
fi
mkdir -p "$BUILD/services-src"
cp "$ROOT/src/main.az" "$ROOT/src/host.az" "$ROOT/src/editor.az" "$ROOT/src/intelligence.az" "$BUILD/services-src/"
"$COMPILER" check "$BUILD/services-src/main.az"
"$COMPILER" compile llvm "$BUILD/services-src/main.az" > "$BUILD/studio.ll"
"$CLANG" -std=c11 -Wall -Wextra -Werror -Wno-override-module \
    ${AZORA_STUDIO_SANITIZER_FLAGS:-} "$BUILD/studio.ll" \
    "$ROOT/host/azora_studio_host.c" -o "$BUILD/azora-studio-services"
echo "Native service executable: $BUILD/azora-studio-services"
