# Rey YouTube Downloader — Mobile (Android)

A single, installable **APK** that downloads YouTube videos, audio and
subtitles, using the same yt-dlp engine as the desktop app. Native **Kotlin**
UI + **Chaquopy** (embedded Python) running **yt-dlp**, with a bundled
**ffmpeg** so MP4 merging and audio conversion work on-device.

## Features

- Paste one or more links (with a **Paste** button); **Preview titles** shows
  what each link points to.
- Playlists/multi-video links appear as a tick list — untick what you don't want.
- **MP4** (video + audio), **Video only**, **Audio only**
  (mp3 / m4a / opus / wav / original).
- Quality cap (Best → 360p) and **subtitles** (download + embed; auto-captions).
- **Progress bar** in the app and a **notification progress bar** so you can
  switch apps while it keeps working in the background (foreground service).
- **Choose where files are saved**: public `Downloads/ReyYouTubeDownloader`
  (default, visible in Files) or any folder via the system folder picker (SAF).
- **Settings**: light / dark / system theme, save location, notification toggle.
- **History**: every download with **Open**, **Share**, **Re-download** and
  **Remove** buttons — so you can grab something again later.
- **Share** finished files to any other app.

## Install note (Play Protect)

This app is **self-signed** (not from Google Play) and contains an embedded
Python runtime, so **Google Play Protect may warn** ("unknown developer" /
"app may be harmful"). This is expected for sideloaded apps. Tap
**Install anyway** to continue. The full fix is publishing on Google Play.

## Layout

```
app/src/main/java/org/rey/reyytdlpdw/MainActivity.kt    main UI
app/src/main/java/org/rey/reyytdlpdw/DownloadService.kt foreground service
app/src/main/java/org/rey/reyytdlpdw/SettingsActivity.kt
app/src/main/java/org/rey/reyytdlpdw/HistoryActivity.kt
app/src/main/python/download.py                         yt-dlp logic (Chaquopy)
app/src/main/jniLibs/<abi>/libffmpeg.so                 bundled ffmpeg
tools/build_ffmpeg_full.sh                              cross-compile ffmpeg+lame+opus
tools/build_ffmpeg_only.sh                              cross-compile ffmpeg only
```

## Building the APK

Requires the Android SDK + JDK 17. From the project root:

```bash
gradle assembleDebug      # debug APK
gradle assembleRelease    # signed with keystore.properties (ReyDT)
```

## Bundled ffmpeg

`libffmpeg.so` is a **custom minimal ffmpeg cross-compiled for Android** with
the codecs yt-dlp needs: mp4/mkv/mp3/ogg/wav muxing + `aac`, `libmp3lame`,
`libopus`, `pcm`, and subtitle (`mov_text`/`srt`/`webvtt`) support. It must be:

- **PIE** (Android refuses non-PIE executables), and
- linked with **16 KB max page size** (Android 15+ / 16 KB devices).

Rebuild (needs the Android NDK r27 + lame/opus/ffmpeg sources on Linux/WSL):

```bash
tools/build_ffmpeg_full.sh x86_64
tools/build_ffmpeg_full.sh arm64
# copy each produced ./ffmpeg to app/src/main/jniLibs/<abi>/libffmpeg.so
```

## Signing

Release builds are signed with the keystore referenced by `keystore.properties`
(certificate: `CN=ReyDT`). Both the keystore and that properties file are
gitignored — keep them safe.
