# Rey YouTube Downloader — Mobile (Android)

A single, installable **APK** that downloads YouTube videos, audio and
subtitles, using the same yt-dlp engine as the desktop app. Native **Kotlin**
UI + **Chaquopy** (embedded Python) running **yt-dlp**, with a bundled
**ffmpeg** so MP4 merging and audio conversion work on-device.

## Features

- Paste one or more links; **Preview titles** shows what each link points to.
- Playlists/multi-video links appear as a tick list — untick what you don't want.
- **MP4** (video + audio), **Video only**, **Audio only** (mp3/m4a/opus/wav).
- Quality cap, subtitle download/embed, live progress + activity log.
- **ffmpeg is bundled** (`jniLibs/<abi>/libffmpeg.so`) so downloads merge/convert
  on the device — no extra installs.

## Layout

```
app/src/main/java/org/rey/reyytdlpdw/MainActivity.kt   Kotlin UI
app/src/main/python/download.py                        yt-dlp logic (Chaquopy)
app/src/main/jniLibs/<abi>/libffmpeg.so                bundled ffmpeg (static)
tools/build_ffmpeg.sh                                  cross-compile script
```

## Building the APK

Requires the Android SDK + JDK 17 and an Android Gradle plugin (see
`build.gradle.kts`). From the project root:

```bash
gradle assembleDebug      # unsigned debug APK
gradle assembleRelease    # signed with keystore.properties (ReyDT)
```

## Bundled ffmpeg

The `libffmpeg.so` binaries are a **minimal ffmpeg cross-compiled for Android**
(demuxers/muxers for mp4/mkv/mp3 + aac/wav encoders) so yt-dlp can merge and
convert. They are built with the Android NDK and must be:

- **PIE** (Android refuses non-PIE executables), and
- linked with **16 KB max page size** (Android 15+ / 16 KB devices).

Rebuild them with:

```bash
# needs the Android NDK r27 (Linux/WSL) and FFmpeg source
tools/build_ffmpeg.sh x86_64
tools/build_ffmpeg.sh arm64
# then copy each produced ./ffmpeg to app/src/main/jniLibs/<abi>/libffmpeg.so
```

## Signing

Release builds are signed with the keystore referenced by `keystore.properties`
(certificate: `CN=ReyDT`). Both the keystore and that properties file are
gitignored — keep them safe.
