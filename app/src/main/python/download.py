"""yt-dlp helper for the Rey YouTube Downloader Android app (Chaquopy)."""
import json
import os
import sys
import time
from os.path import join


def _work_dir():
    d = join(os.environ.get("HOME", "."), "ReyDownloads")
    os.makedirs(d, exist_ok=True)
    return d


def _ffmpeg():
    if sys.platform.startswith("android"):
        import re
        dirs = set()
        try:
            with open("/proc/self/maps") as f:
                for line in f:
                    m = re.search(r"^\S+\s+\S+\s+\S+\s+\S+\s+\S+\s+(\S+)", line)
                    if not m:
                        continue
                    path = m.group(1)
                    if "/lib/" in path and path.endswith(".so"):
                        dirs.add(path.rsplit("/", 1)[0])
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
             subs=False, sub_lang="en", ffmpeg="", callback=None):
    """Download one URL. Calls callback.onProgress(pct, text) if given.
    Returns JSON: {ok, files:[...], dir, title, error?}"""
    import yt_dlp
    dd = _work_dir()
    outtmpl = join(dd, "%(title).120s [%(id)s].%(ext)s")
    title = ""
    t0 = time.time()

    def hook(d):
        if callback is None:
            return
        try:
            if d.get("status") == "downloading":
                tot = d.get("total_bytes") or d.get("total_bytes_estimate") or 0
                done = d.get("downloaded_bytes") or 0
                pct = int(done * 100 / tot) if tot else 0
                callback.onProgress(pct, os.path.basename(d.get("filename", "") or ""))
            elif d.get("status") == "finished":
                callback.onProgress(100, "processing")
        except Exception:
            pass

    o = {
        "outtmpl": outtmpl,
        "quiet": True, "no_warnings": True,
        "format_sort": _quality_sort(quality),
        "progress_hooks": [hook],
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
        o["format"] = "b[ext=mp4]/bv*+ba/b"
        o["merge_output_format"] = "mp4"
    if subs:
        langs = ["all"] if sub_lang.lower() == "all" else [sub_lang]
        o["subtitleslangs"] = langs
        o["writesubtitles"] = True
        o["writeautomaticsub"] = True
        if mode == "mp4":
            o["embedsubs"] = True
            o["subformat"] = "srt"

    try:
        with yt_dlp.YoutubeDL(o) as y:
            info = y.extract_info(url, download=True)
        title = (info or {}).get("title") or ""
        files = [rd.get("filepath") for rd in (info or {}).get("requested_downloads", [])
                 if rd.get("filepath") and os.path.exists(rd["filepath"])]
        # include side files (separate subtitles, etc.) produced just now
        try:
            for f in os.listdir(dd):
                fp = join(dd, f)
                if os.path.isfile(fp) and os.path.getmtime(fp) >= t0 - 1:
                    if fp not in files:
                        files.append(fp)
        except OSError:
            pass
        return json.dumps({"ok": True, "files": files, "dir": dd, "title": title})
    except Exception as e:
        return json.dumps({"ok": False, "error": str(e), "files": [], "dir": dd,
                           "title": title})
