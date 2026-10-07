#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLANG="${AZORA_CLANG:-/usr/bin/clang}"
ENGINE="${AZORA_ENGINE_DIR:-$(cd "$ROOT/../../azora-engine" && pwd)}"
COMPILER="${AZORA_NATIVE_COMPILER:-${AZORA_COMPILER_BIN:-}}"
if [ -z "$COMPILER" ]; then echo "Select AZORA_NATIVE_COMPILER or an explicit development AZORA_COMPILER_BIN" >&2; exit 1; fi
if [ -z "${AZORA_NATIVE_COMPILER:-}" ] && [ "${AZORA_STUDIO_DEVELOPMENT_BOOTSTRAP:-0}" != 1 ]; then
    echo "Native installed-toolchain qualification remains open; this is an explicit development build." >&2
    exit 1
fi
MODE="${1:-studio}"
case "$MODE" in
    studio) ENTRY=window ;;
    launcher) ENTRY=launcher ;;
    model-probe) ENTRY=model-probe ;;
    workspace-probe) ENTRY=workspace-probe ;;
    *) echo "usage: build-window.sh [studio|launcher|model-probe|workspace-probe]" >&2; exit 1 ;;
esac
BUILD="$ROOT/build/$MODE"
rm -rf "$BUILD/staged" "$BUILD/project/src"
mkdir -p "$BUILD/project/src" "$BUILD/staged"
cp "$ROOT/src/$ENTRY.az" "$BUILD/project/src/main.az"
cp "$ROOT/src/host.az" "$ROOT/src/editor.az" "$ROOT/src/intelligence.az" "$ROOT/src/theme.az" "$BUILD/project/src/"
case "$MODE" in
    studio)
        # The editor shell: state, commands, panels, viewport and code surfaces.
        cp "$ROOT/src/problems.az" "$ROOT/src/state.az" "$ROOT/src/ui.az" "$ROOT/src/fields.az" "$ROOT/src/panels.az" \
            "$ROOT/src/viewport.az" "$ROOT/src/code.az" "$ROOT/src/commands.az" "$ROOT/src/script.az" "$BUILD/project/src/" ;;
    launcher)
        # The launcher shares Studio's parts and its scripted input.
        cp "$ROOT/src/ui.az" "$ROOT/src/script.az" "$BUILD/project/src/" ;;
    workspace-probe|model-probe)
        cp "$ROOT/src/scene.az" "$ROOT/src/workspace.az" "$ROOT/src/problems.az" "$BUILD/project/src/" ;;
esac
python3 "$ENGINE/tools/azpm.py" resolve "$BUILD/project" > "$BUILD/resolve.tsv"
FLAGS=()
while IFS=$'\t' read -r kind source destination; do
    case "$kind" in
        STAGE) mkdir -p "$BUILD/staged/$destination"; cp "$source/"*.az "$BUILD/staged/$destination/" ;;
        FRAMEWORK) FLAGS+=(-framework "$source") ;;
        LIB) FLAGS+=("-l$source") ;;
    esac
done < "$BUILD/resolve.tsv"
cp "$BUILD/project/src/"*.az "$BUILD/staged/"
"$COMPILER" compile llvm "$BUILD/staged/main.az" > "$BUILD/studio.ll"
"$CLANG" -std=c11 -Wall -Wextra -Werror -Wno-override-module ${AZORA_STUDIO_OPTIMIZE:--O2} \
    ${AZORA_STUDIO_SANITIZER_FLAGS:-} "$BUILD/studio.ll" "$ROOT/host/azora_studio_host.c" \
    -L "$ENGINE/runtime/build" -lazora_runtime -Wl,-rpath,"$ENGINE/runtime/build" \
    ${FLAGS[@]+"${FLAGS[@]}"} -o "$BUILD/azora-$MODE"
APP="$BUILD/Azora $MODE Development.app"
mkdir -p "$APP/Contents/MacOS"
cp "$BUILD/azora-$MODE" "$APP/Contents/MacOS/azora-$MODE"
cat > "$APP/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>dev.azora.native.$MODE.development</string>
<key>CFBundleExecutable</key><string>azora-$MODE</string>
<key>CFBundleName</key><string>Azora $MODE Development</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleVersion</key><string>0.1.0</string>
</dict></plist>
EOF
echo "Native window executable: $BUILD/azora-$MODE"
