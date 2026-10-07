#!/bin/bash
# Puts "Azora Studio" and "Azora Launcher" apps on the Desktop (or in $1) that
# open this checkout's development builds with their toolchain: the native
# compiler from ../azora-lang, its std, and the Engine beside it. Each app runs
# the window last built by run.sh / build-window.sh, so it opens at once, and
# builds one first only when none exists.
#
#   bash native/tools/desktop-app.sh                 # on the Desktop
#   bash native/tools/desktop-app.sh /Applications   # somewhere else
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LANG_HOME="${AZORA_LANG_HOME:-$(cd "$ROOT/../../azora-lang" && pwd)}"
ENGINE_HOME="${AZORA_ENGINE_HOME:-$(cd "$ROOT/../../azora-engine" && pwd)}"
DEST="${1:-$HOME/Desktop}"

# The toolchain both apps start from.
ENVIRONMENT="ROOT=\"$ROOT\"
LANG_HOME=\"$LANG_HOME\"
RELEASE=\"\$LANG_HOME/compiler/build/bin/macosArm64/releaseExecutable/azora.kexe\"
DEBUG=\"\$LANG_HOME/compiler/build/bin/macosArm64/debugExecutable/azora.kexe\"
if [ -x \"\$RELEASE\" ]; then export AZORA_NATIVE_COMPILER=\"\$RELEASE\"; else export AZORA_NATIVE_COMPILER=\"\$DEBUG\"; fi
export AZORA_STDLIB=\"\$LANG_HOME/std\"
export AZORA_ENGINE_HOME=\"$ENGINE_HOME\"
export AZORA_STUDIO_RESOURCES=\"\$ROOT/resources\"
export AZORA_STUDIO_EXECUTABLE=\"\$ROOT/build/studio/azora-studio\"
LOG=\"\$HOME/Library/Logs/Azora.log\"
mkdir -p \"\$(dirname \"\$LOG\")\""

# make_app <name> <bundle id suffix> <executable> <launch script body>
make_app() {
    local app="$DEST/$1.app"
    rm -rf "$app"
    mkdir -p "$app/Contents/MacOS" "$app/Contents/Resources"
    cp "$ROOT/resources/AppIcon.icns" "$app/Contents/Resources/AppIcon.icns"
    cat > "$app/Contents/Info.plist" <<PLIST
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleName</key><string>$1</string>
    <key>CFBundleDisplayName</key><string>$1</string>
    <key>CFBundleIdentifier</key><string>com.azoralabs.$2.development</string>
    <key>CFBundleExecutable</key><string>$3</string>
    <key>CFBundleIconFile</key><string>AppIcon</string>
    <key>CFBundlePackageType</key><string>APPL</string>
    <key>CFBundleShortVersionString</key><string>0.1.0-dev</string>
    <key>LSMinimumSystemVersion</key><string>13.0</string>
    <key>NSHighResolutionCapable</key><true/>
</dict>
</plist>
PLIST
    printf '#!/bin/bash\n%s\n%s\n' "$ENVIRONMENT" "$4" > "$app/Contents/MacOS/$3"
    chmod +x "$app/Contents/MacOS/$3"
    touch "$app"
    echo "$app"
}

make_app "Azora Studio" studio azora-studio '
export AZORA_STUDIO_PROJECT="${AZORA_STUDIO_PROJECT:-$ROOT/build/demo-project}"
if [ ! -x "$AZORA_STUDIO_EXECUTABLE" ] || [ ! -d "$AZORA_STUDIO_PROJECT" ]; then
    exec bash "$ROOT/tools/run.sh" studio "$AZORA_STUDIO_PROJECT" >> "$LOG" 2>&1
fi
exec "$AZORA_STUDIO_EXECUTABLE" >> "$LOG" 2>&1'

# The Launcher starts a new project from a template, in the first free
# "Project N" under ~/Azora Projects, then opens Studio on it.
make_app "Azora Launcher" launcher azora-launcher '
PROJECTS="$HOME/Azora Projects"
mkdir -p "$PROJECTS"
N=1
while [ -e "$PROJECTS/Project $N" ]; do N=$((N + 1)); done
export AZORA_STUDIO_PROJECT="$PROJECTS/Project $N"
if [ ! -x "$ROOT/build/launcher/azora-launcher" ] || [ ! -x "$AZORA_STUDIO_EXECUTABLE" ]; then
    exec bash "$ROOT/tools/run.sh" launcher "$AZORA_STUDIO_PROJECT" >> "$LOG" 2>&1
fi
exec "$ROOT/build/launcher/azora-launcher" >> "$LOG" 2>&1'
