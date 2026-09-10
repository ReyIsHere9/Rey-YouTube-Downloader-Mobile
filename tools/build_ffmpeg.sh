#!/usr/bin/env bash
# Cross-compile a minimal static ffmpeg for Android (x86_64) with the NDK r27c.
set -e
ARCH=${1:-x86_64}
case "$ARCH" in
  x86_64) TC_PREFIX=x86_64-linux-android ;;
  arm64|aarch64) TC_PREFIX=aarch64-linux-android ;;
  *) echo "unknown arch $ARCH"; exit 1 ;;
esac
API=24
NDK="$HOME/android-ndk-r27c"
TC="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
export PATH="$TC/bin:$PATH"
CC="$TC/bin/${TC_PREFIX}${API}-clang"
SYSROOT="$TC/sysroot"
export CC
export AR="$TC/bin/llvm-ar"
export NM="$TC/bin/llvm-nm"
export RANLIB="$TC/bin/llvm-ranlib"
export STRIP="$TC/bin/llvm-strip"
export LD="$CC"

cd "$HOME/FFmpeg"
make distclean >/dev/null 2>&1 || true
./configure \
  --prefix="$HOME/ffout/$ARCH" \
  --target-os=android --arch=$ARCH \
  --enable-cross-compile --cc="$CC" --ld="$CC" \
  --ar="$AR" --nm="$NM" --ranlib="$RANLIB" --strip="$STRIP" \
  --sysroot="$SYSROOT" \
  --enable-static --disable-shared --enable-pic \
  --disable-autodetect --disable-network --disable-doc \
  --disable-debug --disable-x86asm --disable-ffprobe --disable-ffplay \
  --disable-everything \
  --enable-ffmpeg \
  --enable-protocol=file \
  --enable-demuxer=mov,matroska,mp3,aac,ogg,wav,flac \
  --enable-muxer=mp4,mov,matroska,mp3,adts,wav,ogg,ipod \
  --enable-encoder=aac,pcm_s16le,vorbis \
  --enable-decoder=h264,hevc,vp8,vp9,aac,mp3,vorbis,opus \
  --extra-cflags="-fPIE" \
  --extra-ldflags="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384" \
  > configure_$ARCH.log 2>&1
echo "configure exit=$?"
make -j"$(nproc)" > make_$ARCH.log 2>&1
echo "make exit=$?"
ls -lh ffmpeg
file ffmpeg
