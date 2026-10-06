#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLANG="${AZORA_CLANG:-/usr/bin/clang}"
TEMP_ROOT="$(mktemp -d "${TMPDIR:-/private/tmp}/azora-studio-native.XXXXXX")"
trap 'rm -rf "$TEMP_ROOT"' EXIT
AZORA_STUDIO_SANITIZER_FLAGS="-fsanitize=address,undefined" bash "$ROOT/tools/build.sh"
"$CLANG" -std=c11 -Wall -Wextra -Werror -fsanitize=address,undefined \
    "$ROOT/host/azora_studio_host.c" "$ROOT/tests/host_test.c" -o "$TEMP_ROOT/host-test"
"$CLANG" -std=c11 -Wall -Wextra -Werror "$ROOT/tests/process_fixture.c" -o "$TEMP_ROOT/process-fixture"
"$TEMP_ROOT/host-test" "$TEMP_ROOT" "$TEMP_ROOT/process-fixture"
AZORA_STUDIO_ACTION=probe AZORA_STUDIO_PROJECT="$TEMP_ROOT/workflow" "$ROOT/build/azora-studio-services"
python3 "$ROOT/tests/intelligence_test.py" "$ROOT/build/azora-studio-services" "$TEMP_ROOT"
if [ -n "${AZORA_NATIVE_COMPILER:-}" ]; then
    python3 "$ROOT/tests/native_tool_test.py" "$ROOT/build/azora-studio-services" "$TEMP_ROOT" "$AZORA_NATIVE_COMPILER"
fi
if command -v otool >/dev/null 2>&1; then otool -L "$ROOT/build/azora-studio-services"; fi
