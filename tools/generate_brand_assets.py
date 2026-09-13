#!/usr/bin/env python3
"""Generate the two Play Store brand assets from the app's own brand sources.

The store icon is derived mechanically from res/drawable/ic_launcher_foreground.xml
rather than redrawn, so the listing icon and the launcher icon cannot drift apart.
The feature graphic follows the Syltech system: Signal Orange on Ink, geometric,
flat, uppercase and tracked, no mascot.

Usage:  python3 tools/generate_brand_assets.py [--out DIR]
Requires Pillow.
"""

import argparse
import os

from PIL import Image, ImageDraw, ImageFont

# ── Syltech tokens. Source of truth: ui/theme/Color.kt ────────────────
SIGNAL_ORANGE = (0xFF, 0x5C, 0x00)
AMBER = (0xFF, 0xA5, 0x66)
INK = (0x0B, 0x0B, 0x0C)
SLATE = (0x16, 0x16, 0x19)
LINE = (0x2A, 0x2A, 0x2F)
MIST = (0xCF, 0xCF, 0xD4)
ASH = (0x8F, 0x8F, 0x97)

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT_PATH = os.path.join(REPO, "app/src/main/res/font/archivo.ttf")

# ── The launcher mark, in its native 108dp viewport ───────────────────
# Mirrors ic_launcher_foreground.xml exactly. Change it there, re-run this.
VIEWPORT = 108.0
# Launchers mask an adaptive icon down to roughly the inner 72dp. The store
# icon carries no mask, so rendering the full 108 makes the identical mark look
# 1.5x smaller than it does on the home screen. Play rejected exactly this kind
# of listing/launcher mismatch, so the store icon is scaled by 108/72 to match
# the apparent size a user sees.
SAFE_ZONE = 72.0
CHEVRON = [(36.0, 32.0), (66.0, 54.0), (36.0, 76.0)]
CHEVRON_STROKE = 12.0
NODE_CENTER = (72.0, 34.0)
NODE_RADIUS = 6.0

SUPERSAMPLE = 4


def archivo(size, weight=600, width=100):
    font = ImageFont.truetype(FONT_PATH, size)
    try:
        font.set_variation_by_axes([weight, width])
    except Exception:
        pass  # Static build of the font; the default instance is fine.
    return font


def draw_mark(draw, cx, cy, span, stroke_scale=1.0):
    """Draw the RustSensei mark centred on (cx, cy), `span` px wide overall.

    Geometry comes from the 108dp viewport, so the proportions are identical
    to the launcher icon at any size.
    """
    k = span / VIEWPORT
    ox = cx - (VIEWPORT / 2.0) * k
    oy = cy - (VIEWPORT / 2.0) * k

    pts = [(ox + x * k, oy + y * k) for x, y in CHEVRON]
    w = CHEVRON_STROKE * k * stroke_scale
    r = w / 2.0

    draw.line(pts, fill=SIGNAL_ORANGE, width=int(round(w)), joint="curve")
    # Pillow has no round line cap, so cap each vertex with a disc. This also
    # rounds the elbow, matching strokeLineJoin="round".
    for x, y in pts:
        draw.ellipse([x - r, y - r, x + r, y + r], fill=SIGNAL_ORANGE)

    nx = ox + NODE_CENTER[0] * k
    ny = oy + NODE_CENTER[1] * k
    nr = NODE_RADIUS * k
    draw.ellipse([nx - nr, ny - nr, nx + nr, ny + nr], fill=AMBER)


def tracked_text(draw, xy, text, font, fill, tracking=0.0, anchor_left=True):
    """Draw text with letter-spacing. Returns the width consumed.

    Pillow cannot letter-space, and the brand is uppercase and tracked
    everywhere, so advance per glyph by hand.
    """
    widths = [draw.textlength(ch, font=font) for ch in text]
    total = sum(widths) + tracking * max(len(text) - 1, 0)
    x, y = xy
    if not anchor_left:
        x -= total
    for ch, w in zip(text, widths):
        draw.text((x, y), ch, font=font, fill=fill)
        x += w + tracking
    return total


def generate_icon(out_dir):
    """512x512, no alpha. Play applies its own corner mask."""
    size = 512
    s = size * SUPERSAMPLE
    img = Image.new("RGB", (s, s), INK)
    draw = ImageDraw.Draw(img)
    # Scale so the adaptive icon's safe zone, not its full canvas, fills the
    # tile. This is what makes the store icon and the launcher icon read as the
    # same icon rather than two sizes of the same mark.
    draw_mark(draw, s / 2.0, s / 2.0, s * (VIEWPORT / SAFE_ZONE))
    img = img.resize((size, size), Image.LANCZOS)
    path = os.path.join(out_dir, "app_icon_512x512.png")
    img.save(path, "PNG")
    return path


def generate_feature_graphic(out_dir):
    """1024x500, no alpha. Play crops this differently across surfaces, so
    everything meaningful stays inside a generous margin."""
    w, h = 1024, 500
    s = SUPERSAMPLE
    img = Image.new("RGB", (w * s, h * s), INK)
    draw = ImageDraw.Draw(img)

    margin = 72 * s

    # A single Slate band behind the mark, flat, no gradient.
    draw.rectangle([int(w * 0.60) * s, 0, w * s, h * s], fill=SLATE)
    draw.line([(int(w * 0.60) * s, 0), (int(w * 0.60) * s, h * s)],
              fill=LINE, width=2 * s)

    # Wordmark.
    wordmark_font = archivo(62 * s, weight=800, width=100)
    y = 130 * s
    tracked_text(draw, (margin, y), "RUSTSENSEI", wordmark_font, MIST,
                 tracking=5 * s)

    # Signal Orange rule, the brand's underline device.
    rule_y = y + 84 * s
    draw.rectangle([margin, rule_y, margin + 132 * s, rule_y + 5 * s],
                   fill=SIGNAL_ORANGE)

    # Eyebrow, matching the splash wording and treatment.
    eyebrow_font = archivo(19 * s, weight=600, width=100)
    tracked_text(draw, (margin, rule_y + 34 * s), "ON-DEVICE RUST TUTOR",
                 eyebrow_font, ASH, tracking=4 * s)

    # One plain sentence. No marketing adjectives, per the brand voice.
    body_font = archivo(23 * s, weight=500, width=100)
    draw.text((margin, rule_y + 76 * s),
              "A Rust book, 97 exercises, and an AI tutor",
              font=body_font, fill=MIST)
    draw.text((margin, rule_y + 110 * s),
              "that runs on your phone, offline.",
              font=body_font, fill=MIST)

    # Maker credit, matching the Settings card and the launch splash.
    credit_font = archivo(16 * s, weight=600, width=100)
    tracked_text(draw, (margin, h * s - margin + 8 * s), "A SYLTECH AI SYSTEM",
                 credit_font, ASH, tracking=3 * s)

    # The mark, on the Slate band.
    draw_mark(draw, int(w * 0.80) * s, (h // 2) * s, 272 * s)

    img = img.resize((w, h), Image.LANCZOS)
    path = os.path.join(out_dir, "feature_graphic_1024x500.png")
    img.save(path, "PNG")
    return path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", default=os.path.join(REPO, "play-store-assets"))
    args = ap.parse_args()
    os.makedirs(args.out, exist_ok=True)

    for path in (generate_icon(args.out), generate_feature_graphic(args.out)):
        with Image.open(path) as im:
            print(f"  {os.path.basename(path):32} {im.size[0]}x{im.size[1]}  {im.mode}")


if __name__ == "__main__":
    main()
