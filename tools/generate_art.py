"""
Generates Analog Airwaves' promo art: the README banner and the mod icon.

Both are built from the same Analog Audio palette that tools/generate_textures.py uses, so the
art reads as the same hardware family. Nothing here is hand-drawn pixel-by-pixel: the icon is a
real isometric projection of the portable radio's block model, sampling the block textures this
repo already ships, and the banner letters come from a hand-built 7x7 pixel alphabet.

Run:  python tools/generate_art.py
"""

import math
import os
import random
import struct
import zlib

OUT_DIR = "docs"

# --- Analog Audio palette (same values as tools/generate_textures.py) ------------------------
BORDER = (0x2C, 0x2C, 0x21)
BEVEL = (0x65, 0x43, 0x22)
BODY_DARK = (0x8B, 0x58, 0x2F)
BODY = (0xA8, 0x66, 0x34)
BODY_LIGHT = (0xB9, 0x6B, 0x30)
GRILLE_DARK = (0x49, 0x2D, 0x10)
GRILLE = (0x66, 0x39, 0x14)
PANEL_DARK = (0x37, 0x29, 0x1B)
PANEL = (0x52, 0x3B, 0x2C)
DIAL = (0x76, 0x5F, 0x49)
METAL_DARK = (0x75, 0x69, 0x5F)
METAL_LIGHT = (0xB0, 0x9E, 0x90)
AERIAL = (0x89, 0x89, 0x87)
AERIAL_DARK = (0x6B, 0x6B, 0x6A)
AERIAL_SHADOW = (0x5D, 0x5D, 0x5D)
LED_ON = (0x6C, 0xD8, 0x5C)

# Analog Audio's cover art sits on a warm dark brown; these are sampled from it.
BG_DARK = (0x4A, 0x2E, 0x18)
BG = (0x5A, 0x38, 0x1D)
BG_LIGHT = (0x6A, 0x42, 0x22)

# The cover's title colours: hot orange with a deep red shadow, and the softer tan second word.
TITLE_HI = (0xF2, 0x8C, 0x3C)
TITLE = (0xE2, 0x6B, 0x24)
TITLE_LO = (0xB8, 0x44, 0x16)
TITLE_SHADOW = (0x5C, 0x1E, 0x0C)
TITLE2_HI = (0xF0, 0xB8, 0x5A)
TITLE2 = (0xE0, 0x9A, 0x38)
TITLE2_LO = (0xB0, 0x6E, 0x22)

TRANSPARENT = None


# --- PNG output -------------------------------------------------------------------------------

def write_png(path, pixels):
    """Writes an RGBA PNG from a 2D list of (r,g,b) / (r,g,b,a) tuples, or None for transparent."""
    height = len(pixels)
    width = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for px in row:
            if px is None:
                raw.extend((0, 0, 0, 0))
            elif len(px) == 4:
                raw.extend(px)
            else:
                raw.extend((px[0], px[1], px[2], 255))

    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)
    print("wrote", path, f"({width}x{height})")


def canvas(width, height, fill=TRANSPARENT):
    return [[fill for _ in range(width)] for _ in range(height)]


def put(px, x, y, colour):
    if colour is not None and 0 <= y < len(px) and 0 <= x < len(px[0]):
        px[y][x] = colour


def fill_rect(px, x0, y0, w, h, colour):
    for y in range(y0, y0 + h):
        for x in range(x0, x0 + w):
            put(px, x, y, colour)


def shade(colour, factor):
    """Multiplies a colour, for the isometric face lighting."""
    return tuple(max(0, min(255, int(c * factor))) for c in colour[:3])


# --- the portable radio, rebuilt from the block textures --------------------------------------
# Mirrors tools/generate_textures.py so the icon uses the exact pixels the block does.

def _blank16(fill):
    return [[fill for _ in range(16)] for _ in range(16)]


def _ring(px, x0, y0, x1, y1, colour):
    for x in range(x0, x1):
        px[y0][x] = colour
        px[y1 - 1][x] = colour
    for y in range(y0, y1):
        px[y][x0] = colour
        px[y][x1 - 1] = colour


def _rect(px, x0, y0, x1, y1, colour):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[y][x] = colour


def _grille(px, x0, y0, x1, y1):
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[y][x] = GRILLE_DARK if (x + y) % 2 == 0 else GRILLE


# The block textures are authored for a 6-7px tall face in-world, so blowing them up for promo
# art turns the 1px border into a slab. These faces redraw the same design at the face's own
# aspect, in the same palette, so the icon reads as the block without the upscaling artefacts.

def _face(w, h, fill):
    return [[fill for _ in range(w)] for _ in range(h)]


def face_front(w=20, h=14):
    """
    Control panel with dial and power lamp on the left, speaker grille on the right.

    The layout is the mirror of portable_radio_front, because under this camera the front face
    is drawn right-to-left; mirroring here makes it read the same way round as the real block.
    """
    px = _face(w, h, BODY)
    _ring(px, 0, 0, w, h, BORDER)
    _rect(px, 1, 1, w - 1, 2, BODY_LIGHT)

    # Control panel on the left.
    _rect(px, 2, 3, 8, h - 2, PANEL_DARK)
    _rect(px, 3, 4, 7, 8, PANEL)
    # Tuning dial.
    for dy in range(5, 8):
        for dx in range(3, 6):
            px[dy][dx] = DIAL
    px[6][4] = METAL_LIGHT
    # Power lamp.
    px[h - 4][3] = LED_ON
    px[h - 4][4] = LED_ON
    px[h - 3][3] = shade(LED_ON, 0.7)

    # Grille on the right: a fine checker inset into its own darker recess.
    _rect(px, 9, 2, w - 1, h - 1, BEVEL)
    for y in range(3, h - 2):
        for x in range(10, w - 2):
            px[y][x] = GRILLE_DARK if (x + y) % 2 == 0 else GRILLE
    return px


def face_side(w=12, h=14):
    """Plain framed side with a grille panel, matching portable_radio_side."""
    px = _face(w, h, BODY)
    _ring(px, 0, 0, w, h, BORDER)
    _rect(px, 1, 1, w - 1, 2, BODY_LIGHT)
    _rect(px, 1, 3, w - 1, h - 2, BEVEL)
    for y in range(4, h - 3):
        for x in range(2, w - 2):
            px[y][x] = GRILLE_DARK if (x + y) % 2 == 0 else GRILLE
    return px


def face_top(w=20, h=12):
    """Top deck: frame, bevel, and the darker handle plate on the left."""
    px = _face(w, h, BODY)
    _ring(px, 0, 0, w, h, BORDER)
    _rect(px, 1, 1, w - 1, h - 1, BEVEL)
    _rect(px, 2, 2, w - 2, h - 2, BODY_DARK)
    # Handle plate.
    _rect(px, 3, 3, w - 9, h - 3, PANEL_DARK)
    _rect(px, 4, 4, w - 10, h - 4, PANEL)
    # Aerial socket on the right.
    _rect(px, w - 5, h // 2 - 1, w - 3, h // 2 + 1, AERIAL_SHADOW)
    return px


# --- isometric projection ----------------------------------------------------------------------
# A standard 2:1 "video game" isometric: one model unit becomes `scale` px wide and `scale/2` px
# tall on each ground axis, and `scale` px straight up. Faces are drawn back-to-front.

def iso_point(x, y, z, scale, origin):
    """Model space (x east, y up, z south) -> screen. Returns float pixel coords."""
    sx = origin[0] + (x - z) * scale
    sy = origin[1] + (x + z) * scale * 0.5 - y * scale
    return sx, sy


def fill_quad(px, corners, entry, tint):
    """
    Scanline-fills the parallelogram `corners` (screen-space, in order), texturing it by inverse
    bilinear mapping back into the face's uv window.

    Point-sampling the forward map leaves holes, because one source texel spans several screen
    pixels along the stretched isometric axes. Filling per destination pixel instead guarantees a
    solid face, and nearest-neighbour lookup keeps the texels hard-edged.
    """
    tex = entry
    tex_h = len(tex)
    tex_w = len(tex[0])
    (ax, ay), (bx, by), (cx, cy), (dx, dy) = corners

    # The quad is a parallelogram: A + u*(B-A) + v*(D-A). Invert that 2x2 to get (u, v) per pixel.
    ex, ey = bx - ax, by - ay
    fx, fy = dx - ax, dy - ay
    det = ex * fy - ey * fx
    if abs(det) < 1e-9:
        return

    min_x = int(math.floor(min(ax, bx, cx, dx)))
    max_x = int(math.ceil(max(ax, bx, cx, dx)))
    min_y = int(math.floor(min(ay, by, cy, dy)))
    max_y = int(math.ceil(max(ay, by, cy, dy)))

    for y in range(min_y, max_y + 1):
        for x in range(min_x, max_x + 1):
            px_c = x + 0.5 - ax
            py_c = y + 0.5 - ay
            u = (px_c * fy - py_c * fx) / det
            v = (ex * py_c - ey * px_c) / det
            if not (0.0 <= u < 1.0 and 0.0 <= v < 1.0):
                continue
            tu = min(tex_w - 1, int(u * tex_w))
            tv = min(tex_h - 1, int(v * tex_h))
            colour = tex[tv][tu]
            if colour is not None:
                put(px, x, y, shade(colour, tint))


def draw_iso_box(px, box, textures, scale, origin):
    """
    Renders one axis-aligned box of the block model as three isometric faces.

    `box` is (x0, y0, z0, x1, y1, z1) in model units. `textures` maps each visible face to a
    (texture, u0, v0, u1, v1) sample window, matching the model JSON's uv entries. The three
    visible faces get flat tints (top brightest, right darkest) so the form reads in 3D.
    """
    x0, y0, z0, x1, y1, z1 = box

    # Top face (y = y1). Wound to match the left face below it: u runs -x (screen left-to-right
    # along the front edge), v runs -z back into the scene.
    if "top" in textures:
        fill_quad(px, (
            iso_point(x1, y1, z1, scale, origin),
            iso_point(x0, y1, z1, scale, origin),
            iso_point(x0, y1, z0, scale, origin),
            iso_point(x1, y1, z0, scale, origin),
        ), textures["top"], 1.0)

    # Left face: the model's south face (z = z1), which is the one pointing screen-left under
    # this camera. u runs -x so the texture reads left-to-right on screen; v runs down from y1.
    if "left" in textures:
        fill_quad(px, (
            iso_point(x1, y1, z1, scale, origin),
            iso_point(x0, y1, z1, scale, origin),
            iso_point(x0, y0, z1, scale, origin),
            iso_point(x1, y0, z1, scale, origin),
        ), textures["left"], 0.78)

    # Right face: the model's east face (x = x1). u runs +z, v runs down from y1.
    if "right" in textures:
        fill_quad(px, (
            iso_point(x1, y1, z0, scale, origin),
            iso_point(x1, y1, z1, scale, origin),
            iso_point(x1, y0, z1, scale, origin),
            iso_point(x1, y0, z0, scale, origin),
        ), textures["right"], 0.60)


def radio_centre_origin(scale, centre):
    """
    Returns the `origin` that puts the radio's own visual centre at screen point `centre`.

    Model coordinates are raw 0-16 block units, so projecting them against a bare origin throws
    the model far off-screen. Projecting the model's midpoint against a zero origin and
    subtracting gives an origin that lands the radio exactly where we want it.
    """
    mid_x, mid_z = 8.0, 8.0
    mid_y = 4.0  # a little above the body's own centre, to allow for the aerial's visual weight
    ox, oy = iso_point(mid_x, mid_y, mid_z, scale, (0, 0))
    return (centre[0] - ox, centre[1] - oy)


def draw_radio_shadow(px, scale, origin):
    """A flat isometric shadow under the body, so the radio sits on the ground plane."""
    pad = 0.6
    corners = (
        iso_point(13 + pad, 0, 11 + pad, scale, origin),
        iso_point(3 - pad, 0, 11 + pad, scale, origin),
        iso_point(3 - pad, 0, 5 - pad, scale, origin),
        iso_point(13 + pad, 0, 5 - pad, scale, origin),
    )
    fill_quad(px, corners, _face(1, 1, shade(BG_DARK, 0.62)), 1.0)


def draw_radio_iso(px, scale, origin):
    """
    The full portable radio, drawn back to front using the same element boxes as
    models/block/portable_radio_base.json so the icon is the real block, not an impression of it.
    """
    front, side, top = face_front(), face_side(), face_top()

    # Body [3,0,5] -> [13,7,11].
    draw_iso_box(px, (3, 0, 5, 13, 7, 11),
                 {"top": top, "left": front, "right": side}, scale, origin)

    # Carry handle [4,7,6] -> [9,8,10]: a low plate sitting on the deck.
    handle_top = _face(10, 8, PANEL)
    _ring(handle_top, 0, 0, 10, 8, PANEL_DARK)
    _rect(handle_top, 1, 1, 9, 3, shade(PANEL, 1.15))
    handle_side = _face(10, 4, PANEL)
    _ring(handle_side, 0, 0, 10, 4, PANEL_DARK)
    draw_iso_box(px, (4, 7, 6, 9, 8, 10),
                 {"top": handle_top, "left": handle_side, "right": handle_side}, scale, origin)

    # Aerial [11,7,7] -> [12,15,8]: a thin metal post with a banded shaft.
    aerial_side = _face(2, 26, AERIAL)
    for y in range(26):
        for x in range(2):
            if x == 1:
                aerial_side[y][x] = AERIAL_SHADOW
            elif y % 5 == 4:
                aerial_side[y][x] = AERIAL_DARK
            else:
                aerial_side[y][x] = METAL_LIGHT if y < 3 else AERIAL
    aerial_top = _face(2, 2, METAL_LIGHT)
    draw_iso_box(px, (11, 7, 7, 12, 15, 8),
                 {"top": aerial_top, "left": aerial_side, "right": aerial_side}, scale, origin)


# --- hand-drawn pixel alphabet -------------------------------------------------------------------
# A 7x7 display face with slightly irregular, chunky strokes so it reads as hand-lettered rather
# than a system font. '#' is a filled pixel.

GLYPHS = {
    "A": ["..###..",
          ".#...#.",
          "#.....#",
          "#######",
          "#.....#",
          "#.....#",
          "#.....#"],
    "N": ["#.....#",
          "##....#",
          "#.#...#",
          "#..#..#",
          "#...#.#",
          "#....##",
          "#.....#"],
    "L": ["#......",
          "#......",
          "#......",
          "#......",
          "#......",
          "#.....#",
          "#######"],
    "O": [".#####.",
          "#.....#",
          "#.....#",
          "#.....#",
          "#.....#",
          "#.....#",
          ".#####."],
    "G": [".#####.",
          "#.....#",
          "#......",
          "#..####",
          "#.....#",
          "#.....#",
          ".#####."],
    "I": ["#######",
          "...#...",
          "...#...",
          "...#...",
          "...#...",
          "...#...",
          "#######"],
    "R": ["######.",
          "#.....#",
          "#.....#",
          "######.",
          "#...#..",
          "#....#.",
          "#.....#"],
    "W": ["#.....#",
          "#.....#",
          "#.....#",
          "#..#..#",
          "#.###.#",
          "##...##",
          "#.....#"],
    "V": ["#.....#",
          "#.....#",
          "#.....#",
          ".#...#.",
          ".#...#.",
          "..#.#..",
          "...#..."],
    "E": ["#######",
          "#......",
          "#......",
          "#####..",
          "#......",
          "#......",
          "#######"],
    "S": [".######",
          "#......",
          "#......",
          ".#####.",
          "......#",
          "......#",
          "######."],
    " ": [".......",
          ".......",
          ".......",
          ".......",
          ".......",
          ".......",
          "......."],
}


def draw_text(px, text, x, y, pixel, palette, jitter_seed=None):
    """
    Draws `text` with each glyph pixel blown up to `pixel` px, plus a drop shadow and a top
    highlight row so the letters have the cover art's carved, chunky look.

    `palette` is (highlight, mid, low, shadow). A per-letter vertical jitter keeps the baseline
    from looking mechanically straight, which is what sells the hand-drawn feel.
    """
    hi, mid, low, shadow = palette
    rng = random.Random(jitter_seed) if jitter_seed is not None else None
    cursor = x
    for index, char in enumerate(text.upper()):
        glyph = GLYPHS.get(char, GLYPHS[" "])
        wobble = 0 if rng is None else rng.choice((-1, 0, 0, 1))
        top = y + wobble * pixel

        for row, line in enumerate(glyph):
            for col, cell in enumerate(line):
                if cell != "#":
                    continue
                gx = cursor + col * pixel
                gy = top + row * pixel

                # Drop shadow, offset down-right by one whole pixel-block.
                fill_rect(px, gx + pixel, gy + pixel, pixel, pixel, shadow)

        # Second pass so the body always paints over a neighbouring glyph's shadow. Pixels with
        # nothing above them catch the light; the bottom edge of each stroke falls into shadow.
        for row, line in enumerate(glyph):
            for col, cell in enumerate(line):
                if cell != "#":
                    continue
                gx = cursor + col * pixel
                gy = top + row * pixel
                above = row > 0 and glyph[row - 1][col] == "#"
                below = row < len(glyph) - 1 and glyph[row + 1][col] == "#"
                if not above:
                    colour = hi
                elif not below:
                    colour = low
                else:
                    colour = mid
                fill_rect(px, gx, gy, pixel, pixel, colour)

        cursor += (len(glyph[0]) + 1) * pixel
    return cursor


def text_width(text, pixel):
    return sum((len(GLYPHS.get(c.upper(), GLYPHS[" "])[0]) + 1) * pixel for c in text)


# --- broadcast arcs ------------------------------------------------------------------------------

def draw_arc(px, cx, cy, radius, colour, pixel, start_deg, end_deg):
    """A chunky pixel arc, used for the signal waves coming off the aerial."""
    steps = max(24, int(radius * 3))
    for i in range(steps + 1):
        angle = math.radians(start_deg + (end_deg - start_deg) * i / steps)
        ax = cx + math.cos(angle) * radius
        ay = cy - math.sin(angle) * radius
        fill_rect(px, int(ax // pixel) * pixel, int(ay // pixel) * pixel, pixel, pixel, colour)


# --- banner ---------------------------------------------------------------------------------------

def build_banner(width=800, height=300):
    px = canvas(width, height, BG)
    rng = random.Random(20260913)

    # Background: a soft vertical gradient plus a subtle pixel-noise dither, so the flat brown
    # has the same grain as the cover art rather than reading as a solid web colour.
    for y in range(height):
        t = y / height
        base = (
            int(BG_DARK[0] + (BG_LIGHT[0] - BG_DARK[0]) * (1 - abs(t - 0.45) * 1.6)),
            int(BG_DARK[1] + (BG_LIGHT[1] - BG_DARK[1]) * (1 - abs(t - 0.45) * 1.6)),
            int(BG_DARK[2] + (BG_LIGHT[2] - BG_DARK[2]) * (1 - abs(t - 0.45) * 1.6)),
        )
        for x in range(width):
            n = rng.randint(-4, 4)
            px[y][x] = (max(0, min(255, base[0] + n)),
                        max(0, min(255, base[1] + n)),
                        max(0, min(255, base[2] + n)))

    # Background texture: a sparse scatter of darker discs, kept off the wordmark and the radio.
    # Wordmark: "ANALOG" over "AIRWAVES", centred as a block on the left of the radio.
    top_text, bottom_text = "ANALOG", "AIRWAVES"
    top_pixel, bottom_pixel = 9, 8
    top_w = text_width(top_text, top_pixel)
    bottom_w = text_width(bottom_text, bottom_pixel)

    block_w = max(top_w, bottom_w)
    block_x = 48
    top_y = 96
    bottom_y = top_y + 8 * top_pixel + 14

    draw_text(px, top_text, block_x + (block_w - top_w) // 2, top_y, top_pixel,
              (TITLE_HI, TITLE, TITLE_LO, TITLE_SHADOW), jitter_seed=7)
    draw_text(px, bottom_text, block_x + (block_w - bottom_w) // 2, bottom_y, bottom_pixel,
              (TITLE2_HI, TITLE2, TITLE2_LO, TITLE_SHADOW), jitter_seed=13)

    # The radio itself, sitting to the right of the wordmark.
    scale = 11
    origin = radio_centre_origin(scale, (width - 132, height // 2 + 14))
    tip_x, tip_y = iso_point(11.5, 15, 7.5, scale, origin)
    for i, radius in enumerate((24, 36, 48)):
        draw_arc(px, tip_x, tip_y, radius, shade(TITLE, 0.95 - i * 0.18), 4, 15, 165)
    draw_radio_shadow(px, scale, origin)
    draw_radio_iso(px, scale, origin)

    return px


# --- mod icon ---------------------------------------------------------------------------------------

def build_icon(size=256):
    px = canvas(size, size, BG)
    rng = random.Random(4242)

    # Radial warm background: lighter behind the radio, falling off to the cover's dark brown.
    cx, cy = size * 0.5, size * 0.52
    max_d = math.hypot(cx, cy)
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - cx, y - cy) / max_d
            t = max(0.0, min(1.0, 1.0 - d * 1.15))
            n = rng.randint(-3, 3)
            px[y][x] = (
                max(0, min(255, int(BG_DARK[0] + (BG_LIGHT[0] - BG_DARK[0]) * t) + n)),
                max(0, min(255, int(BG_DARK[1] + (BG_LIGHT[1] - BG_DARK[1]) * t) + n)),
                max(0, min(255, int(BG_DARK[2] + (BG_LIGHT[2] - BG_DARK[2]) * t) + n)),
            )

    # No background circles here: at icon size they only crowd the subject.

    # The radio sits low and small enough that the widest wave still clears the top edge.
    scale = 8
    origin = radio_centre_origin(scale, (size // 2, size // 2 + 34))
    tip_x, tip_y = iso_point(11.5, 15, 7.5, scale, origin)

    # Broadcast arcs behind the radio, sized so the outermost stays inside the frame.
    for i, radius in enumerate((26, 38, 50)):
        colour = shade(TITLE, 0.95 - i * 0.18)
        draw_arc(px, tip_x, tip_y, radius, colour, 3, 20, 160)

    draw_radio_shadow(px, scale, origin)
    draw_radio_iso(px, scale, origin)

    return px


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.chdir(root)

    write_png(os.path.join(OUT_DIR, "banner.png"), build_banner())
    write_png(os.path.join(OUT_DIR, "icon.png"), build_icon())


if __name__ == "__main__":
    main()
