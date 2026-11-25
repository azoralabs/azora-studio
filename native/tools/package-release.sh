#!/bin/bash
# Assembles the native Apple silicon Launcher DMG, Studio ZIP and server catalog.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BUILD_NUMBER="${1:?usage: package-release.sh VERSION_CODE OUTPUT_DIRECTORY}"
OUT="${2:?usage: package-release.sh VERSION_CODE OUTPUT_DIRECTORY}"
MODE="${3:---local}"
if [[ "$MODE" != --local && "$MODE" != --public ]]; then echo "Use --local or --public." >&2; exit 1; fi
[[ "$BUILD_NUMBER" =~ ^[1-9][0-9]{0,8}$ ]] || { echo "Invalid release build number." >&2; exit 1; }
[[ "$(uname -sm)" = 'Darwin arm64' ]] || { echo "Build this release on an Apple silicon Mac." >&2; exit 1; }
export AZORA_STUDIO_BUILD_NUMBER="$BUILD_NUMBER"
export MACOSX_DEPLOYMENT_TARGET="${MACOSX_DEPLOYMENT_TARGET:-15.0}"
VERSION=0.1.0-dev
LABEL=0.1-dev
LAUNCHER_FILE="Azora-Launcher-$LABEL-build-$BUILD_NUMBER-macos-arm64.dmg"
STUDIO_FILE="Azora-Studio-$LABEL-build-$BUILD_NUMBER-macos-arm64.zip"
PUBLIC=0
TEAM=""
if [ "$MODE" = --public ]; then
    [[ "${AZORA_SIGN_IDENTITY:-}" = 'Developer ID Application:'* && "${AZORA_SIGN_TEAM:-}" =~ ^[A-Z0-9]{10}$ && -n "${AZORA_NOTARY_PROFILE:-}" ]] ||
        { echo "Set a Developer ID Application identity, its Team ID, and a Keychain notarytool profile." >&2; exit 1; }
    PUBLIC=1
    TEAM="$AZORA_SIGN_TEAM"
else
    unset AZORA_SIGN_IDENTITY AZORA_NOTARY_PROFILE AZORA_SIGN_TEAM
fi
source "$ROOT/tools/toolchain-env.sh"
mkdir -p "$OUT"
OUT="$(cd "$OUT" && pwd)"
for file in "$LAUNCHER_FILE" "$STUDIO_FILE" catalog.json; do
    if [ -e "$OUT/$file" ]; then echo "Release output already exists: $OUT/$file" >&2; exit 1; fi
done
STAGE="$(mktemp -d "$OUT/.package-release.XXXXXX")"
trap 'rm -rf "$STAGE"' EXIT
export AZORA_ENGINE_RUNTIME_DIR="$STAGE/runtime"
bash "$AZORA_ENGINE_HOME/runtime/build.sh" "$AZORA_ENGINE_RUNTIME_DIR"
AZORA_STUDIO_BUILD_DIR="$STAGE/launcher-build" bash "$ROOT/tools/build-window.sh" launcher
mkdir "$STAGE/disk"
LAUNCHER="$STAGE/disk/Azora Launcher.app"
mv "$STAGE/launcher-build/Azora launcher Development.app" "$LAUNCHER"
CONTENT="$LAUNCHER/Contents"
PLIST="$CONTENT/Info.plist"
/usr/libexec/PlistBuddy \
    -c 'Set :CFBundleName Azora Launcher' \
    -c 'Set :CFBundleIdentifier com.azoralabs.launcher' \
    -c "Set :CFBundleVersion $BUILD_NUMBER" \
    -c 'Add :CFBundleShortVersionString string 0.1.0' \
    -c "Add :LSMinimumSystemVersion string $MACOSX_DEPLOYMENT_TARGET" "$PLIST"
if (( PUBLIC )); then /usr/libexec/PlistBuddy -c "Add :AzoraReleaseTeam string $TEAM" "$PLIST"; fi
bash "$ROOT/tools/sign-macos.sh" "$CONTENT/MacOS/libazora_runtime.dylib" "$LAUNCHER"
bash "$ROOT/tools/package-studio.sh" "$VERSION" "$STAGE/studio"
STUDIO="$STAGE/studio/Azora Studio $VERSION.app"

notarize_app() {
    local app="$1"
    local upload="$STAGE/notary-upload.zip"
    /usr/bin/ditto -c -k --keepParent "$app" "$upload"
    xcrun notarytool submit "$upload" --keychain-profile "$AZORA_NOTARY_PROFILE" --wait
    xcrun stapler staple "$app"
    xcrun stapler validate "$app"
    /usr/sbin/spctl --assess --type execute "$app"
    rm "$upload"
}
if (( PUBLIC )); then
    notarize_app "$STUDIO"
    notarize_app "$LAUNCHER"
fi
/usr/bin/tar -c --format=zip --disable-copyfile -f "$STAGE/$STUDIO_FILE" -C "$STAGE/studio" "Azora Studio $VERSION.app"
ln -s /Applications "$STAGE/disk/Applications"
/usr/bin/hdiutil create -volname "Azora Launcher" -srcfolder "$STAGE/disk" -format UDZO "$STAGE/$LAUNCHER_FILE"
if (( PUBLIC )); then
    /usr/bin/codesign --force --sign "$AZORA_SIGN_IDENTITY" --timestamp "$STAGE/$LAUNCHER_FILE"
    xcrun notarytool submit "$STAGE/$LAUNCHER_FILE" --keychain-profile "$AZORA_NOTARY_PROFILE" --wait
    xcrun stapler staple "$STAGE/$LAUNCHER_FILE"
    xcrun stapler validate "$STAGE/$LAUNCHER_FILE"
fi
python3 - "$STAGE" "$VERSION" "$LABEL" "$BUILD_NUMBER" "$MACOSX_DEPLOYMENT_TARGET" "$PUBLIC" "$LAUNCHER_FILE" "$STUDIO_FILE" <<'PYTHON'
import hashlib
import json
from pathlib import Path
import sys

directory, version, label, build, minimum, public, launcher, studio = sys.argv[1:]
directory = Path(directory)

def asset(name):
    path = directory / name
    digest = hashlib.sha256()
    with path.open("rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)
    return {"file": name, "bytes": path.stat().st_size, "sha256": digest.hexdigest()}

catalog = {
    "schema": 1,
    "distribution": "public" if public == "1" else "local",
    "platform": "macos-arm64",
    "minimumMacOS": minimum,
    "launcher": {**asset(launcher), "label": label},
    "studio": [{**asset(studio), "version": version, "label": label, "versionCode": int(build)}],
}
(directory / "catalog.json").write_text(json.dumps(catalog, indent=2) + "\n", encoding="utf-8")
PYTHON
mv "$STAGE/$LAUNCHER_FILE" "$STAGE/$STUDIO_FILE" "$OUT/"
mv "$STAGE/catalog.json" "$OUT/"
printf 'Release assets: %s\n' "$OUT"
