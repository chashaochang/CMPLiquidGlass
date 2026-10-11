#!/bin/sh
set -eu

PROBE_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
ROOT_DIR=$(CDPATH= cd -- "$PROBE_DIR/../.." && pwd)
DEVECO_DIR=${DEVECO_STUDIO_CONTENTS:-/Applications/DevEco-Studio.app/Contents}
HOST_DIR=$(mktemp -d "${TMPDIR:-/private/tmp}/glass-fusion-component-probe.XXXXXX")

"$ROOT_DIR/gradlew" -p "$PROBE_DIR" linkDebugSharedOhosArm64 --console=plain
"$ROOT_DIR/gradlew" -p "$PROBE_DIR/executor" linkDebugSharedOhosArm64 --console=plain

rsync -a --exclude=build --exclude=oh_modules --exclude=.hvigor --exclude=.cxx \
    --exclude='libs/arm64-v8a' --exclude=composeResources \
    "$ROOT_DIR/ohosApp/" "$HOST_DIR/"
rsync -a "$ROOT_DIR/ohosApp/oh_modules/" "$HOST_DIR/oh_modules/"
rsync -a "$ROOT_DIR/ohosApp/entry/oh_modules/" "$HOST_DIR/entry/oh_modules/"

mkdir -p "$HOST_DIR/entry/libs/arm64-v8a"
cp "$PROBE_DIR/build/bin/ohosArm64/debugShared/libkn.so" "$HOST_DIR/entry/libs/arm64-v8a/"
cp "$PROBE_DIR/executor/build/bin/ohosArm64/debugShared/libglass_component.so" \
    "$HOST_DIR/entry/libs/arm64-v8a/"
cp "$PROBE_DIR/build/bin/ohosArm64/debugShared/libkn_api.h" \
    "$HOST_DIR/entry/src/main/cpp/include/arm64-v8a/"
cp "$ROOT_DIR/experiments/fusion-material-probe/host/napi_init.cpp" "$HOST_DIR/entry/src/main/cpp/napi_init.cpp"
cp "$ROOT_DIR/experiments/fusion-material-probe/host/Index.ets" "$HOST_DIR/entry/src/main/ets/pages/Index.ets"

cd "$HOST_DIR"
DEVECO_SDK_HOME="$DEVECO_DIR/sdk" "$DEVECO_DIR/tools/node/bin/node" \
    "$DEVECO_DIR/tools/hvigor/bin/hvigorw.js" --mode module \
    -p module=entry@default -p product=default -p buildMode=debug assembleHap --no-daemon

printf '\nComponent probe HAP (not installed): %s\n' \
    "$HOST_DIR/entry/build/default/outputs/default/entry-default-signed.hap"
