# Sourced by the native development and packaging scripts.
LANG_HOME="${AZORA_LANG_HOME:-$(cd "$ROOT/../../azora-lang" && pwd)}"
RELEASE="$LANG_HOME/compiler/build/bin/macosArm64/releaseExecutable/azora.kexe"
DEBUG="$LANG_HOME/compiler/build/bin/macosArm64/debugExecutable/azora.kexe"
if [ -z "${AZORA_NATIVE_COMPILER:-}" ]; then
    if [ -x "$RELEASE" ]; then AZORA_NATIVE_COMPILER="$RELEASE"; else AZORA_NATIVE_COMPILER="$DEBUG"; fi
fi
if [ ! -x "$AZORA_NATIVE_COMPILER" ]; then
    echo "No native compiler at $AZORA_NATIVE_COMPILER. Build :compiler:linkReleaseExecutableMacosArm64 in $LANG_HOME." >&2
    return 1
fi
export AZORA_NATIVE_COMPILER
export AZORA_STDLIB="${AZORA_STDLIB:-$LANG_HOME/std}"
export AZORA_ENGINE_HOME="${AZORA_ENGINE_DIR:-${AZORA_ENGINE_HOME:-$(cd "$ROOT/../../azora-engine" && pwd)}}"
export AZORA_STUDIO_RESOURCES="${AZORA_STUDIO_RESOURCES:-$ROOT/resources}"
