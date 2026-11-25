#!/bin/bash
# Build a relocatable, side-by-side Studio package for the Launcher library.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
VERSION="${1:?usage: package-studio.sh VERSION [catalog-directory]}"
CATALOG="${2:-$ROOT/build/catalog}"
if [[ ! "$VERSION" =~ ^[0-9][a-zA-Z0-9.+-]{0,62}$ ]]; then
    echo "Invalid Studio version: $VERSION" >&2
    exit 1
fi
source "$ROOT/tools/toolchain-env.sh"
mkdir -p "$CATALOG"
CATALOG="$(cd "$CATALOG" && pwd)"
APP="$CATALOG/Azora Studio $VERSION.app"
if [ -e "$APP" ]; then
    echo "A Studio package is already present at $APP" >&2
    exit 1
fi
TEMP="$(mktemp -d "$CATALOG/.studio-package.XXXXXX")"
trap 'rm -rf "$TEMP"' EXIT
export AZORA_STUDIO_BUILD_DIR="$TEMP/build"
unset AZORA_STUDIO_PROJECT AZORA_STUDIO_PREVIEW_PROJECT
[ -f "$AZORA_ENGINE_HOME/runtime/build/libazora_runtime.dylib" ] || bash "$AZORA_ENGINE_HOME/runtime/build.sh"
bash "$ROOT/tools/build-window.sh" studio
BUNDLE="$TEMP/Azora Studio $VERSION.app"
mv "$AZORA_STUDIO_BUILD_DIR/Azora studio Development.app" "$BUNDLE"
CONTENTS="$BUNDLE/Contents"
RESOURCES="$CONTENTS/Resources"
TOOLCHAIN="$RESOURCES/toolchain"
ENGINE="$TOOLCHAIN/engine"
mkdir -p "$RESOURCES/studio" "$TOOLCHAIN/std" "$ENGINE/native/macos" "$ENGINE/tools"
rsync -a --exclude='.*' --exclude=build "$AZORA_STUDIO_RESOURCES/" "$RESOURCES/studio/"
rsync -a --exclude='.*' --exclude=build "$AZORA_STDLIB/" "$TOOLCHAIN/std/"
cp "$AZORA_NATIVE_COMPILER" "$TOOLCHAIN/azora"
cp "$AZORA_ENGINE_HOME/workspace.azon" "$AZORA_ENGINE_HOME/library.json" "$ENGINE/"
for section in packages templates materials; do
    mkdir -p "$ENGINE/$section"
    rsync -a --exclude='.*' --exclude=build "$AZORA_ENGINE_HOME/$section/" "$ENGINE/$section/"
done
cp "$AZORA_ENGINE_HOME"/tools/{azon.py,azpm.py,build.sh,shader-cook.sh,import-standard-materials.sh} "$ENGINE/tools/"
cp "$CONTENTS/MacOS/libazora_runtime.dylib" "$ENGINE/native/macos/"
cp "$AZORA_STUDIO_RESOURCES/AppIcon.icns" "$RESOURCES/"
printf '%s\n' "$VERSION" > "$RESOURCES/studio-version"
/usr/libexec/PlistBuddy \
    -c "Set :CFBundleName Azora Studio $VERSION" \
    -c "Set :CFBundleIdentifier com.azoralabs.studio.version-${VERSION//+/--}" \
    -c "Add :CFBundleShortVersionString string $VERSION" \
    -c "Add :CFBundleIconFile string AppIcon" "$CONTENTS/Info.plist"
codesign --force --sign - "$BUNDLE"
# Publish beside the staging directory without replacing an existing version.
mv -n "$BUNDLE" "$APP"
if [ -e "$BUNDLE" ]; then
    echo "A Studio package is already present at $APP" >&2
    exit 1
fi
echo "$APP"
