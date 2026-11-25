#!/bin/bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CLANG="${AZORA_CLANG:-/usr/bin/clang}"
ENGINE="${AZORA_ENGINE_DIR:-${AZORA_ENGINE_HOME:-$(cd "$ROOT/../../azora-engine" && pwd)}}"
RUNTIME="${AZORA_ENGINE_RUNTIME_DIR:-$ENGINE/runtime/build}"
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
# AZORA_STUDIO_BUILD_DIR builds elsewhere, e.g. while the usual Studio is running.
BUILD="${AZORA_STUDIO_BUILD_DIR:-$ROOT/build/$MODE}"
rm -rf "$BUILD/staged" "$BUILD/project/src"
mkdir -p "$BUILD/project/src" "$BUILD/staged"
cp "$ROOT/src/$ENTRY.az" "$BUILD/project/src/main.az"
cp "$ROOT/src/host.az" "$ROOT/src/editor.az" "$ROOT/src/intelligence.az" "$ROOT/src/theme.az" "$BUILD/project/src/"
if [[ "$MODE" = studio || "$MODE" = launcher ]]; then
    cp "$ROOT/src/startup.az" "$ROOT/src/script.az" "$BUILD/project/src/"
fi
case "$MODE" in
    studio)
        # The editor shell: state, commands, panels, viewport and code surfaces.
        cp "$ROOT/src/problems.az" "$ROOT/src/state.az" "$ROOT/src/ui.az" "$ROOT/src/fields.az" "$ROOT/src/panels.az" \
            "$ROOT/src/viewport.az" "$ROOT/src/code.az" "$ROOT/src/commands.az" "$ROOT/src/projectbrowser.az" \
            "$ROOT/src/landscape.az" "$ROOT/src/pluginapi.az" "$ROOT/src/worldtools.az" "$ROOT/src/foliagetool.az" "$ROOT/src/os.az" "$ROOT/src/materials.az" "$ROOT/src/plugins.az" "$ROOT/src/menubar.az" "$ROOT/src/play.az" "$BUILD/project/src/" ;;
    workspace-probe|model-probe)
        cp "$ROOT/src/scene.az" "$ROOT/src/workspace.az" "$ROOT/src/problems.az" "$BUILD/project/src/" ;;
esac
if [ "$MODE" = studio ]; then
    PREVIEW_PROJECT="${AZORA_STUDIO_PREVIEW_PROJECT:-${AZORA_STUDIO_PROJECT:-}}"
    if [ -n "$PREVIEW_PROJECT" ]; then
        python3 "$ROOT/tools/stage-project-preview.py" "$ENGINE/tools" "$PREVIEW_PROJECT" "$BUILD/project/src" > "$BUILD/preview-imports.az"
        if [ -s "$BUILD/preview-imports.az" ]; then
            cat "$BUILD/preview-imports.az" "$BUILD/project/src/main.az" > "$BUILD/preview-main.az"
            mv "$BUILD/preview-main.az" "$BUILD/project/src/main.az"
        fi
    fi
fi
python3 "$ENGINE/tools/azpm.py" resolve "$BUILD/project" > "$BUILD/resolve.tsv"
FLAGS=()
while IFS=$'\t' read -r kind source destination; do
    case "$kind" in
        STAGE) mkdir -p "$BUILD/staged/$destination"; cp "$source/"*.az "$BUILD/staged/$destination/" ;;
        FRAMEWORK) FLAGS+=(-framework "$source") ;;
        LIB) FLAGS+=("-l$source") ;;
    esac
done < "$BUILD/resolve.tsv"
cp -R "$BUILD/project/src/." "$BUILD/staged/"
# Studio exports the services its plugins call (studio.plugins); a plugin
# library resolves them against this executable when it is loaded.
EXPORTS=()
[ "$MODE" = studio ] && EXPORTS=(--export=studioPluginReply,studioPluginMesh,studioPluginObjectMesh,studioPluginObjectMeshLod,studioPluginReadFile,studioPluginWriteFile,studioMenuChosen)
"$COMPILER" compile llvm ${EXPORTS[@]+"${EXPORTS[@]}"} "$BUILD/staged/main.az" > "$BUILD/studio.ll"
"$CLANG" -std=c11 -Wall -Wextra -Werror -Wno-override-module ${AZORA_STUDIO_OPTIMIZE:--O2} \
    ${AZORA_STUDIO_SANITIZER_FLAGS:-} "$BUILD/studio.ll" "$ROOT/host/azora_studio_host.c" \
    -L "$RUNTIME" -lazora_runtime -Wl,-rpath,@executable_path \
    ${FLAGS[@]+"${FLAGS[@]}"} -o "$BUILD/azora-$MODE.next"
APP="$BUILD/Azora $MODE Development.app"
mkdir -p "$APP/Contents/MacOS" "$APP/Contents/Resources"
cp "$RUNTIME/libazora_runtime.dylib" "$BUILD/"
cp "$BUILD/libazora_runtime.dylib" "$APP/Contents/MacOS/"
cat > "$APP/Contents/Info.plist" <<EOF
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0"><dict>
<key>CFBundleIdentifier</key><string>dev.azora.native.$MODE.development</string>
<key>CFBundleExecutable</key><string>azora-$MODE</string>
<key>CFBundleName</key><string>Azora $MODE Development</string>
<key>CFBundleIconFile</key><string>AppIcon</string>
<key>CFBundlePackageType</key><string>APPL</string>
<key>CFBundleVersion</key><string>0.1.0</string>
</dict></plist>
EOF
if [[ "$MODE" = studio || "$MODE" = launcher ]]; then
    ICON="$ROOT/resources/AppIcon.icns"
    if [ "$MODE" = launcher ]; then ICON="$ROOT/resources/LauncherIcon.icns"; fi
    cp "$ICON" "$APP/Contents/Resources/AppIcon.icns"
    AZORA_COMPILER_BIN="$COMPILER" AZORA_SHADER_SCOPE=engine \
        bash "$ENGINE/tools/shader-cook.sh" "$BUILD/project" "$BUILD/shaders" metallib
    test -s "$BUILD/shaders/scene.metallib"
    mkdir -p "$APP/Contents/Resources/shaders"
    cp "$BUILD/shaders/"*.metallib "$APP/Contents/Resources/shaders/"
fi
if [ "$MODE" = launcher ]; then
    cp "$ROOT/tools/install-release.sh" "$APP/Contents/Resources/"
    cp "$ROOT/resources/azora-logo.png" "$APP/Contents/Resources/"
fi
mv "$BUILD/azora-$MODE.next" "$BUILD/azora-$MODE"
cp "$BUILD/azora-$MODE" "$APP/Contents/MacOS/azora-$MODE"
echo "Native window executable: $BUILD/azora-$MODE"
