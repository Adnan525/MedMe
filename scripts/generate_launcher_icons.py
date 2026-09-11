#!/usr/bin/env python3
"""
Regenerates the MedMe launcher icon (legacy + adaptive, all densities) and the
Play Store hi-res icon from a single square source PNG.

Usage:
    python3 scripts/generate_launcher_icons.py [path/to/logo.png]

Defaults to store_assets/medme_logo_source.png. Run this after replacing that
file with a new logo, then re-sync/rebuild in Android Studio.

Requires Pillow: pip install pillow
"""
import sys
import os
from PIL import Image

REPO_ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(REPO_ROOT, "app", "src", "main", "res")
STORE_ASSETS = os.path.join(REPO_ROOT, "store_assets")

LEGACY_SIZES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}
ADAPTIVE_SIZES = {
    "mipmap-mdpi": 108,
    "mipmap-hdpi": 162,
    "mipmap-xhdpi": 216,
    "mipmap-xxhdpi": 324,
    "mipmap-xxxhdpi": 432,
}
# How much of the 108dp adaptive canvas the artwork occupies, centered. Lower
# this if important details (text, icons) are getting clipped by circular
# launcher masks; raise it if the icon looks too small/zoomed out.
FOREGROUND_SCALE = 0.78

BACKGROUND_XML = os.path.join(RES, "drawable", "ic_launcher_background.xml")


def sample_gradient_colors(src):
    """Best-effort sample of two opposite-corner colors from the painted
    (non-transparent) area, for the adaptive icon's background gradient."""
    bbox = src.getbbox()
    if bbox is None:
        return "#2EC4B7", "#0D55A6"

    def first_solid(points):
        for x, y in points:
            p = src.getpixel((x, y))
            if p[3] > 250:
                return p
        return None

    w0, h0, w1, h1 = bbox
    tl_candidates = [(int(w0 + f * (w1 - w0)), int(h0 + f * (h1 - h0))) for f in (0.15, 0.2, 0.25, 0.3)]
    br_candidates = [(int(w0 + f * (w1 - w0)), int(h0 + f * (h1 - h0))) for f in (0.85, 0.8, 0.75, 0.7)]
    tl = first_solid(tl_candidates) or (46, 196, 183, 255)
    br = first_solid(br_candidates) or (13, 85, 166, 255)

    def to_hex(p):
        return "#{:02X}{:02X}{:02X}".format(p[0], p[1], p[2])

    return to_hex(tl), to_hex(br)


def write_background_gradient(start_hex, end_hex):
    content = f'''<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:aapt="http://schemas.android.com/aapt"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <path android:pathData="M0,0h108v108h-108z">
        <aapt:attr name="android:fillColor">
            <gradient
                android:type="linear"
                android:startX="0"
                android:startY="0"
                android:endX="108"
                android:endY="108">
                <item android:offset="0" android:color="{start_hex}" />
                <item android:offset="1" android:color="{end_hex}" />
            </gradient>
        </aapt:attr>
    </path>
</vector>
'''
    with open(BACKGROUND_XML, "w") as f:
        f.write(content)


def main():
    src_path = sys.argv[1] if len(sys.argv) > 1 else os.path.join(STORE_ASSETS, "medme_logo_source.png")
    if not os.path.isfile(src_path):
        print(f"Source image not found: {src_path}")
        sys.exit(1)

    src = Image.open(src_path).convert("RGBA")
    if src.width != src.height:
        print(f"Warning: source is {src.width}x{src.height}, not square — it will be stretched.")

    for folder, size in LEGACY_SIZES.items():
        d = os.path.join(RES, folder)
        os.makedirs(d, exist_ok=True)
        resized = src.resize((size, size), Image.LANCZOS)
        resized.save(os.path.join(d, "ic_launcher.png"))
        resized.save(os.path.join(d, "ic_launcher_round.png"))

    for folder, canvas in ADAPTIVE_SIZES.items():
        d = os.path.join(RES, folder)
        os.makedirs(d, exist_ok=True)
        content_size = int(canvas * FOREGROUND_SCALE)
        resized = src.resize((content_size, content_size), Image.LANCZOS)
        fg = Image.new("RGBA", (canvas, canvas), (0, 0, 0, 0))
        offset = ((canvas - content_size) // 2, (canvas - content_size) // 2)
        fg.paste(resized, offset, resized)
        fg.save(os.path.join(d, "ic_launcher_foreground.png"))

    os.makedirs(STORE_ASSETS, exist_ok=True)
    src.resize((512, 512), Image.LANCZOS).save(os.path.join(STORE_ASSETS, "ic_launcher_hires.png"))

    start_hex, end_hex = sample_gradient_colors(src)
    write_background_gradient(start_hex, end_hex)

    print("Regenerated launcher icons at all densities.")
    print(f"Adaptive background gradient: {start_hex} -> {end_hex}")
    print("Re-sync/rebuild in Android Studio to see the change.")


if __name__ == "__main__":
    main()
