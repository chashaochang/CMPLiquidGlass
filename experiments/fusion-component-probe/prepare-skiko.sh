#!/bin/sh
set -eu

if [ "$#" -ne 4 ]; then
    printf 'Usage: sh prepare-skiko.sh original.klib skia-m132-source-directory libjpeg-turbo-m132.tar.gz output.klib\n' >&2
    exit 2
fi
ORIGINAL=$1
SKIA=$2
JPEG=$3
OUTPUT=$4
NATIVE=${DEVECO_STUDIO_CONTENTS:-/Applications/DevEco-Studio.app/Contents}/sdk/default/openharmony/native
WORK=$(mktemp -d "${TMPDIR:-/private/tmp}/glass-jpeg-skiko.XXXXXX")
trap 'rm -rf "$WORK"' EXIT
unzip -q "$ORIGINAL" -d "$WORK/klib"
mkdir -p "$WORK/jpeg"
tar -xzf "$JPEG" -C "$WORK/jpeg"
for source in SkJpegEncoderImpl SkJPEGWriteUtility; do
    "$NATIVE/llvm/bin/clang++" --target=aarch64-linux-ohos --sysroot="$NATIVE/sysroot" \
    -std=c++17 -O2 -fPIC -fno-rtti -fno-exceptions -fvisibility=hidden \
    -fvisibility-inlines-hidden -ffunction-sections -fdata-sections \
    -DSK_ALLOW_STATIC_GLOBAL_INITIALIZERS=1 -DSK_FORCE_DISTANCE_FIELD_TEXT=0 \
    -DSK_GAMMA_APPLY_TO_A8 -DSK_GAMMA_SRGB -DSK_SCALAR_TO_FLOAT_EXCLUDED \
    -DSK_SUPPORT_GPU=1 -DSK_GANESH -DSK_GL -DSK_SHAPER_HARFBUZZ_AVAILABLE \
    -DSK_UNICODE_AVAILABLE -DSK_SHAPER_UNICODE_AVAILABLE -DSK_SUPPORT_OPENCL=0 \
    "-I$SKIA" "-I$WORK/jpeg" \
    -c "$SKIA/src/encode/$source.cpp" -o "$WORK/$source.o"
done
ARCHIVE=$WORK/klib/default/targets/ohos_arm64/included/libskia.a
"$NATIVE/llvm/bin/llvm-ar" r "$ARCHIVE" "$WORK/SkJpegEncoderImpl.o" \
    "$WORK/SkJPEGWriteUtility.o"
"$NATIVE/llvm/bin/llvm-nm" --defined-only "$ARCHIVE" | rg '_ZN13SkJpegEncoder6EncodeEP15GrDirectContext'
mkdir -p "$(dirname -- "$OUTPUT")"
OUTPUT=$(CDPATH= cd -- "$(dirname -- "$OUTPUT")" && pwd)/$(basename -- "$OUTPUT")
cd "$WORK/klib"
zip -qr "$WORK/jpeg.klib" .
cp "$WORK/jpeg.klib" "$OUTPUT"
printf 'Original m132 JPEG encoder restored: %s\n' "$OUTPUT"
