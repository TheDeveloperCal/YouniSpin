#!/usr/bin/env python3
"""Generate YouniSpin launcher layers from the developer-approved artwork."""
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[1] / "app" / "src" / "main" / "res"
SRC = ROOT / "drawable-nodpi" / "younispin_brand_source.png"

DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}


def main() -> None:
    source = Image.open(SRC).convert("RGB")
    if source.width != source.height:
        raise ValueError(f"Expected square brand artwork, got {source.size}")

    image = source.resize((512, 512), Image.Resampling.LANCZOS)
    image.save(ROOT / "drawable-nodpi" / "younispin_brand.png", optimize=True)
    image.save(ROOT / "drawable-nodpi" / "younispin_icon_background.png", optimize=True)
    splash_size = 460
    splash_mark = image.resize((splash_size, splash_size), Image.Resampling.LANCZOS).convert("RGBA")
    mask_scale = 4
    splash_mask = Image.new("L", (splash_size * mask_scale, splash_size * mask_scale), 0)
    ImageDraw.Draw(splash_mask).rounded_rectangle(
        (0, 0, (splash_size - 1) * mask_scale, (splash_size - 1) * mask_scale),
        radius=118 * mask_scale,
        fill=255,
    )
    splash_mark.putalpha(splash_mask.resize((splash_size, splash_size), Image.Resampling.LANCZOS))
    splash = Image.new("RGBA", (512, 512), (0, 0, 0, 0))
    splash.alpha_composite(splash_mark, ((512 - splash_size) // 2, (512 - splash_size) // 2))
    splash.save(ROOT / "drawable-nodpi" / "younispin_splash.png", optimize=True)

    pixels = image.load()
    foreground = Image.new("RGBA", image.size, (0, 0, 0, 0))
    foreground_pixels = foreground.load()
    monochrome = Image.new("RGBA", image.size, (255, 255, 255, 0))
    monochrome_pixels = monochrome.load()
    for y in range(image.height):
        for x in range(image.width):
            red, green, blue = pixels[x, y]
            in_play = 150 <= x <= 360 and 135 <= y <= 335
            in_bars = 125 <= x <= 385 and 340 <= y <= 465
            near_white = (
                min(red, green, blue) >= 205
                and max(red, green, blue) - min(red, green, blue) <= 55
            )
            bright_bar = (
                red >= 125
                and green >= 175
                and blue >= 205
                and max(red, green, blue) - min(red, green, blue) <= 110
            )
            if (in_play and near_white) or (in_bars and (near_white or bright_bar)):
                foreground_pixels[x, y] = (red, green, blue, 255)
                monochrome_pixels[x, y] = (255, 255, 255, 255)

    foreground.save(ROOT / "drawable-nodpi" / "younispin_icon_foreground.png", optimize=True)
    monochrome.save(ROOT / "drawable-nodpi" / "younispin_icon_monochrome.png", optimize=True)

    for folder, size in DENSITIES.items():
        out_dir = ROOT / folder
        out_dir.mkdir(parents=True, exist_ok=True)
        resized = image.resize((size, size), Image.Resampling.LANCZOS)
        resized.save(out_dir / "ic_launcher.png", optimize=True)
        resized.save(out_dir / "ic_launcher_round.png", optimize=True)
        print(f"wrote {out_dir} {size}px")


if __name__ == "__main__":
    main()
