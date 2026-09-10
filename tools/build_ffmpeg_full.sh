#!/usr/bin/env bash
# Cross-compile ffmpeg (with lame + opus + subtitle codecs) for Android.
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
export PATH="$TC/bin:$HOME/bin:$PATH"
CC="$TC/bin/${TC_PREFIX}${API}-clang"
CXX="$TC/bin/${TC_PREFIX}${API}-clang++"
AR="$TC/bin/llvm-ar"; NM="$TC/bin/llvm-nm"
RANLIB="$TC/bin/llvm-ranlib"; STRIP="$TC/bin/llvm-strip"
SYSROOT="$TC/sysroot"
PREFIX="$HOME/libs/$ARCH"
export PKG_PREFIX="$PREFIX"
PAGE="-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384"
mkdir -p "$PREFIX"

echo "=== lame ($ARCH) ==="
cd "$HOME/lame-3.100"
make distclean >/dev/null 2>&1 || true
./configure --host=${TC_PREFIX} --prefix="$PREFIX" \
  --enable-static --disable-shared --disable-frontend \
  --disable-decoder --disable-analyzer-hooks \
  CC="$CC" AR="$AR" RANLIB="$RANLIB" \
  CFLAGS="-fPIE -O2" LDFLAGS="$PAGE" > "$HOME/lame_$ARCH.log" 2>&1
make -j"$(nproc)" >> "$HOME/lame_$ARCH.log" 2>&1
make install >> "$HOME/lame_$ARCH.log" 2>&1

echo "=== opus ($ARCH) ==="
cd "$HOME/opus-1.4"
make distclean >/dev/null 2>&1 || true
./configure --host=${TC_PREFIX} --prefix="$PREFIX" \
  --enable-static --disable-shared --disable-doc --disable-extra-programs \
  CC="$CC" AR="$AR" RANLIB="$RANLIB" \
  CFLAGS="-fPIE -O2" LDFLAGS="$PAGE" > "$HOME/opus_$ARCH.log" 2>&1
make -j"$(nproc)" >> "$HOME/opus_$ARCH.log" 2>&1
make install >> "$HOME/opus_$ARCH.log" 2>&1

echo "=== ffmpeg ($ARCH) ==="
cd "$HOME/FFmpeg"
make distclean >/dev/null 2>&1 || true
./configure \
  --prefix="$HOME/ffout/$ARCH" \
  --target-os=android --arch=$ARCH \
  --enable-cross-compile --cc="$CC" --ld="$CC" --cxx="$CXX" \
  --ar="$AR" --nm="$NM" --ranlib="$RANLIB" --strip="$STRIP" \
  --sysroot="$SYSROOT" \
  --enable-static --disable-shared --enable-pic \
  --disable-autodetect --disable-network --disable-doc --disable-debug \
  --disable-x86asm --disable-ffprobe --disable-ffplay \
  --disable-everything \
  --enable-ffmpeg \
  --enable-protocol=file \
  --enable-demuxer=mov,matroska,mp3,aac,ogg,wav,flac,srt,ass,webvtt \
  --enable-muxer=mp4,mov,matroska,mp3,adts,wav,ogg,ipod,srt,ass,webvtt \
  --enable-encoder=aac,pcm_s16le,vorbis,libmp3lame,libopus,mov_text,ass,srt,webvtt \
  --enable-decoder=h264,hevc,vp8,vp9,aac,mp3,vorbis,opus,ass,srt,subrip,webvtt,mov_text \
  --enable-libmp3lame --enable-libopus \
  --extra-cflags="-fPIE -O2 -I$PREFIX/include" \
  --extra-ldflags="$PAGE -L$PREFIX/lib" \
  --extra-libs="-lm" \
  > "$HOME/ffconf_$ARCH.log" 2>&1
make -j"$(nproc)" > "$HOME/ffmake_$ARCH.log" 2>&1
ls -lh ffmpeg
file ffmpeg
