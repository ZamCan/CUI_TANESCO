#!/usr/bin/env python3
"""
Build TANESCO launcher / in-app PNG assets from the authentic logo artwork.

The logo itself is NEVER redrawn, recoloured, stretched or re-proportioned by
this script. It only:
  * trims empty transparent margins,
  * places the unaltered artwork (aspect preserved) on launcher tiles,
  * exports Android density / adaptive-icon sizes.

Source artwork: docs/brand/source/tanesco_logo_transparent.png
See docs/brand/SOURCE.md for provenance. Replace that one file with a
higher-resolution official asset and re-run to regenerate everything.
"""
from PIL import Image, ImageDraw
import os

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC = os.path.join(ROOT, "docs", "brand", "source", "tanesco_logo_transparent.png")
RES = os.path.join(ROOT, "app", "src", "main", "res")
BRAND = os.path.join(ROOT, "docs", "brand")

DENS = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
FG = {"mdpi": 108, "hdpi": 162, "xhdpi": 216, "xxhdpi": 324, "xxxhdpi": 432}


def trimmed_logo():
    im = Image.open(SRC).convert("RGBA")
    bbox = im.getchannel("A").getbbox()
    return im.crop(bbox) if bbox else im


def place(canvas, logo, width_frac):
    """Paste logo centred at width_frac of canvas width, aspect preserved."""
    cw, ch = canvas.size
    tw = int(cw * width_frac)
    th = int(logo.size[1] * tw / logo.size[0])
    if th > ch * 0.9:
        th = int(ch * 0.9)
        tw = int(logo.size[0] * th / logo.size[1])
    resized = logo.resize((tw, th), Image.LANCZOS)
    canvas.alpha_composite(resized, ((cw - tw) // 2, (ch - th) // 2))
    return canvas


def launcher_tile(logo, size=1024):
    tile = Image.new("RGBA", (size, size), (247, 250, 241, 255))
    px = tile.load()
    top, bottom = (255, 255, 255), (232, 243, 222)
    for y in range(size):
        t = y / max(size - 1, 1)
        row = tuple(int(top[c] + (bottom[c] - top[c]) * t) for c in range(3)) + (255,)
        for x in range(size):
            px[x, y] = row
    place(tile, logo, 0.80)
    d = ImageDraw.Draw(tile)
    inset = int(size * 0.012)
    d.rounded_rectangle([inset, inset, size - inset, size - inset],
                        radius=int(size * 0.225), outline=(23, 122, 46, 90),
                        width=max(2, size // 256))
    mask = Image.new("L", (size, size), 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, size, size],
                                            radius=int(size * 0.235), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(tile, (0, 0), mask)
    return out


def circle(img):
    m = Image.new("L", img.size, 0)
    ImageDraw.Draw(m).ellipse([0, 0, img.size[0], img.size[1]], fill=255)
    o = Image.new("RGBA", img.size, (0, 0, 0, 0))
    o.paste(img, (0, 0), m)
    return o


def save(im, path):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    im.save(path, optimize=True)
    print(os.path.relpath(path, ROOT), im.size)


if __name__ == "__main__":
    logo = trimmed_logo()
    print("source (trimmed):", logo.size)

    # Authentic logo, transparent (display master + in-app lockup asset).
    wide = logo.resize((1200, int(logo.size[1] * 1200 / logo.size[0])), Image.LANCZOS)
    save(wide, os.path.join(BRAND, "tanesco_logo_official_transparent.png"))
    save(wide, os.path.join(RES, "drawable-nodpi", "brand_logo.png"))

    # Square transparent mark tile content for headers (logo unaltered).
    mark = place(Image.new("RGBA", (512, 512), (0, 0, 0, 0)), logo, 0.88)
    save(mark, os.path.join(RES, "drawable-nodpi", "brand_mark.png"))

    # Launcher set.
    tile = launcher_tile(logo)
    save(tile, os.path.join(BRAND, "tanesco_launcher_icon_1024.png"))
    round_tile = circle(tile)
    for dens, sz in DENS.items():
        d = os.path.join(RES, f"mipmap-{dens}")
        save(tile.resize((sz, sz), Image.LANCZOS), os.path.join(d, "ic_launcher.png"))
        save(round_tile.resize((sz, sz), Image.LANCZOS), os.path.join(d, "ic_launcher_round.png"))
    for dens, sz in FG.items():
        fg = place(Image.new("RGBA", (sz, sz), (0, 0, 0, 0)), logo, 0.70)
        save(fg, os.path.join(RES, f"mipmap-{dens}", "ic_launcher_foreground.png"))
