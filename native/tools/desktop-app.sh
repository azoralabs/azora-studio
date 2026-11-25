#!/bin/bash
# Puts "Azora Studio" and "Azora Launcher" apps on the Desktop (or in $1) that
# open this checkout's development builds. Each shortcut uses the last build
# and builds its window on first launch if needed.
#
#   bash native/tools/desktop-app.sh                 # on the Desktop
#   bash native/tools/desktop-app.sh /Applications   # somewhere else
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
LANG_HOME="${AZORA_LANG_HOME:-$(cd "$ROOT/../../azora-lang" && pwd)}"
ENGINE_HOME="${AZORA_ENGINE_HOME:-$(cd "$ROOT/../../azora-engine" && pwd)}"
DEST="${1:-$HOME/Desktop}"

# make_app <name> <mode>
make_app() {
    local app="$DEST/$1.app" executable="azora-$2"
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
    <key>CFBundleExecutable</key><string>$executable</string>
    <key>CFBundleIconFile</key><string>AppIcon</string>
    <key>CFBundlePackageType</key><string>APPL</string>
    <key>CFBundleShortVersionString</key><string>0.1.0-dev</string>
    <key>LSMinimumSystemVersion</key><string>13.0</string>
    <key>NSHighResolutionCapable</key><true/>
</dict>
</plist>
PLIST
    {
        printf '#!/bin/bash\nROOT=%q\nexport AZORA_LANG_HOME=%q\nexport AZORA_ENGINE_HOME=%q\nMODE=%q\n' \
            "$ROOT" "$LANG_HOME" "$ENGINE_HOME" "$2"
        cat <<'SCRIPT'
LOG="$HOME/Library/Logs/Azora.log"
mkdir -p "$(dirname "$LOG")"
APP="$ROOT/build/$MODE/Azora $MODE Development.app/Contents"
if [ ! -x "$APP/MacOS/azora-$MODE" ] || [ ! -s "$APP/Resources/shaders/scene.metallib" ]; then
    exec bash "$ROOT/tools/run.sh" "$MODE" >> "$LOG" 2>&1
fi
exec bash "$ROOT/tools/launch.sh" "$MODE" >> "$LOG" 2>&1
SCRIPT
    } > "$app/Contents/MacOS/$executable.next"
    chmod +x "$app/Contents/MacOS/$executable.next"
    mv "$app/Contents/MacOS/$executable.next" "$app/Contents/MacOS/$executable"
    touch "$app"
    echo "$app"
}

make_app "Azora Studio" studio
make_app "Azora Launcher" launcher
