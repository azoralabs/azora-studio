#!/bin/bash
# Runs in a child process so network I/O and signature checks never block the UI.
set -euo pipefail

ACTION="$1"
URL="$2"
STAGE="$3"
VERSION="$4"
DIGEST="$5"
TEAM="$6"

fail() { printf '%s\n' "$*" >&2; exit 1; }

if [ "$TEAM" = - ]; then
    [[ "$URL" =~ ^http://127\.0\.0\.1:[0-9]+/ ]] || fail "Use a signed Azora Launcher to download public versions."
    TRANSPORT=(--proto '=http' --proto-redir '=http' --max-redirs 0)
else
    [[ "$TEAM" =~ ^[A-Z0-9]{10}$ ]] || fail "The Launcher release signature is misconfigured."
    [[ "$URL" = https://azorastudio.org/downloads/macos-arm64/* ]] || fail "Invalid Azora release address."
    TRANSPORT=(--proto '=https' --proto-redir '=https' --location --max-redirs 3)
fi

fetch() {
    /usr/bin/curl --disable --fail --silent --show-error --connect-timeout 10 \
        "${TRANSPORT[@]}" "$@"
}

case "$ACTION" in
    catalog)
        fetch --max-time 20 --max-filesize 65536 "$URL" || fail "Could not reach the Azora release catalog. Try Refresh."
        ;;
    install)
        [[ "$VERSION" =~ ^[0-9][a-zA-Z0-9.+-]{0,62}$ ]] || fail "Invalid Studio version."
        [[ "$DIGEST" =~ ^[0-9a-f]{64}$ ]] || fail "Invalid Studio archive checksum."
        test -d "$STAGE" || fail "Could not prepare the Studio installation directory."
        ZIP="$STAGE/studio.zip"
        fetch --max-time 900 --max-filesize 1073741824 --output "$ZIP" "$URL" ||
            fail "Could not download Studio. Try Install again."
        CHECKSUM="$(/usr/bin/shasum -a 256 "$ZIP")"
        [ "${CHECKSUM%% *}" = "$DIGEST" ] || fail "The Studio download failed its checksum. Try Install again."
        /usr/bin/tar -xf "$ZIP" -C "$STAGE" --no-same-owner --no-same-permissions ||
            fail "Could not extract the Studio package."
        APP="$STAGE/Azora Studio $VERSION.app"
        test -d "$APP" || fail "The Studio archive did not contain the selected version."
        if [ "$TEAM" = - ]; then
            /usr/bin/codesign --verify --deep --strict "$APP" || fail "The local Studio signature is invalid."
        else
            ID="com.azoralabs.studio.version-${VERSION//+/--}"
            REQUIREMENT="anchor apple generic and identifier \"$ID\" and certificate leaf[subject.OU] = \"$TEAM\" and certificate leaf[field.1.2.840.113635.100.6.1.13] exists"
            /usr/bin/codesign --verify --deep --strict "$APP" && /usr/bin/codesign --verify -R "$REQUIREMENT" "$APP" ||
                fail "The Studio package could not be verified as an Azora Labs release."
            /usr/sbin/spctl --assess --type execute "$APP" || fail "macOS could not verify the Studio notarization."
        fi
        ;;
    *) fail "Unknown Azora release operation." ;;
esac
