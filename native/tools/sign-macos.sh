#!/bin/bash
set -euo pipefail
IDENTITY="${AZORA_SIGN_IDENTITY:--}"
FLAGS=(--timestamp=none)
if [ "$IDENTITY" != - ]; then FLAGS=(--timestamp --options runtime); fi
for item in "$@"; do
    /usr/bin/codesign --force --sign "$IDENTITY" "${FLAGS[@]}" "$item"
done
