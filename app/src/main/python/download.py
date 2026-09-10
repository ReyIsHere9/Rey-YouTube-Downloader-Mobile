"""yt-dlp helper for the Rey YouTube Downloader Android app.

Functions here are called from Kotlin via Chaquopy. Downloads are written to
the app's private storage (os.environ["HOME"]) which survives until uninstall.
"""
import os
import sys
from os.path import join


def _work_dir():
    d = join(os.environ.get("HOME", "."), "ReyDownloads")
    os.makedirs(d, exist_ok=True)
    return d


def _ffmpeg():
    """Locate a bundled static ffmpeg binary if we shipped one.

    Android only allows executing binaries from the app's native library
    directory. We look there (via /proc/self/maps) for a file named `ffmpeg`
    or `libffmpeg.so`.
    """
    if sys.platform.startswith("android"):
        import re
        import glob
        dirs = set()
        try:
            with open("/proc/self/maps") as f:
                for line in f:
                    m = re.search(r"^(\\S+)\\s+\\S+\\s+\\S+\\s+\\S+\\s+\\S+\\s+(\\S+)", line)
                    if not m:
                        continue
                    path = m.group(2)
                    if "/lib/" in path and path.endswith(".so"):
                        dirs.add(path.rsplit("/", 1)[0])
        except Exception:
            pass
        # Chaquopy's own executable dir
        try:
            import os
            for d in os.environ.get("HOME", ""), "":
                pass
        except Exception:
            pass
        for d in dirs:
            for name in ("ffmpeg", "libffmpeg.so"):
                p = os.path.join(d, name)
                if os.path.exists(p):
                    return p
        return None
    import shutil
    return shutil.which("ffmpeg")


def preview(text):
    """Return a list of [title, url] pairs from a newline-separated link list."""
    import yt_dlp
    urls = [u.strip() for u in text.splitlines() if u.strip()]
    out = []
    try:
        with yt_dlp.YoutubeDL({"skip_download": True, "quiet": True,
                               "no_warnings": True, "extract_flat": "in_playlist"}) as y:
            for u in urls:
                info = y.extract_info(u, download=False)
                if info and info.get("entries"):
                    for e in info["entries"]:
                        if e:
                            ti = (e.get("title") or "") or ""
                            eu = (e.get("url") or "") or ""
                            if not eu and e.get("id"):
                                eu = "https://www.youtube.com/watch?v=" + e["id"]
                            if eu:
                                out.append([ti, eu])
                elif info:
                    ti = (info.get("title") or "") or ""
                    eu = (info.get("webpage_url") or u) or ""
                    out.append([ti, eu])
    except Exception as e:
        out.append(["ERROR: %s" % e, ""])
    return out


def _quality_sort(q):
    if not q or q == "Best":
        return []
    return ["res:%s" % q.rstrip("p")]


def download(url, mode="mp4", quality="Best", audio="mp3",
             subs=False, sub_lang="en", ffmpeg=""):
    """Download a single URL. Returns a status string."""
    import yt_dlp
    dd = _work_dir()
    outtmpl = join(dd, "%(title).120s [%(id)s].%(ext)s")
    o = {
        "outtmpl": outtmpl,
        "quiet": True, "no_warnings": True,
        "format_sort": _quality_sort(quality),
    }
    if ffmpeg:
        o["ffmpeg_location"] = ffmpeg
    else:
        ff = _ffmpeg()
        if ff:
            o["ffmpeg_location"] = ff

    if mode == "video":
        o["format"] = "bv*"
    elif mode == "audio":
        o["format"] = "bestaudio"
        if audio and audio != "original":
            o["postprocessors"] = [{"key": "FFmpegExtractAudio",
                                    "preferredcodec": audio}]
    else:
        # prefer a pre-muxed MP4; fall back to merge (needs ffmpeg)
        o["format"] = "b[ext=mp4]/bv*+ba/b"
        o["merge_output_format"] = "mp4"
    if subs:
        langs = ["all"] if sub_lang.lower() == "all" else [sub_lang]
        o["subtitleslangs"] = langs
        o["writesubtitles"] = True
        if mode == "mp4":
            o["embedsubs"] = True
            o["subformat"] = "srt"
    try:
        with yt_dlp.YoutubeDL(o) as y:
            y.download([url])
        return "Saved to %s" % dd
    except Exception as e:
        return "ERROR: %s" % e
