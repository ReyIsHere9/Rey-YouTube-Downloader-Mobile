"""yt-dlp helper for the Rey YouTube Downloader Android app.

Functions here are called from Kotlin via Chaquopy. Downloads are written to
the app's private storage (os.environ["HOME"]) which survives until uninstall.
"""
import os
from os.path import join


def _work_dir():
    d = join(os.environ.get("HOME", "."), "ReyDownloads")
    os.makedirs(d, exist_ok=True)
    return d


def preview(urls):
    """Return a list of video titles that a URL / playlist resolves to."""
    import yt_dlp
    out = []
    try:
        with yt_dlp.YoutubeDL({"skip_download": True, "quiet": True,
                               "no_warnings": True, "extract_flat": "in_playlist"}) as y:
            for u in urls:
                info = y.extract_info(u, download=False)
                if info and info.get("entries"):
                    for e in info["entries"]:
                        if e:
                            out.append((e.get("title") or "") or "")
                elif info:
                    out.append((info.get("title") or "") or "")
    except Exception as e:
        out.append("ERROR: %s" % e)
    return out


def _quality_sort(q):
    if q and q != "Best":
        return [("res", q)]
    return []


def download(url, mode="mp4", quality="Best", audio="mp3",
             subs=False, sub_lang="en"):
    """Download a single URL. Returns a status string."""
    import yt_dlp
    dd = _work_dir()
    o = {
        "outtmpl": join(dd, "%(title).120s [%(id)s].%(ext)s"),
        "quiet": True, "no_warnings": True,
        "format_sort": _quality_sort(quality),
    }
    if mode == "video":
        o["format"] = "bv*"
    elif mode == "audio":
        o["format"] = "bestaudio"
        if audio and audio != "original":
            o["postprocessors"] = [{"key": "FFmpegExtractAudio",
                                    "preferredcodec": audio}]
    else:
        # prefer a pre-muxed MP4 (no ffmpeg needed); fall back to merge
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
