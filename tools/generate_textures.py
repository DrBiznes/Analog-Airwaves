"""
Generates Analog Airwaves' block and item textures.

Everything is built from Analog Audio's radio block textures so this mod's blocks read as the
same hardware family: the transmitter continues the radio's frame upward, and the portable radio
is a miniature of it. BORDER (#2C2C21) is the exact frame colour on every radio side face, and
the top/bottom ring is copied pixel-for-pixel from analogaudio:block/radio_top.

Run:  python tools/generate_textures.py
"""

import math
import os
import struct
import zlib

OUT_BLOCK = os.path.join("src", "main", "resources", "assets", "analogairwaves", "textures", "block")
OUT_ITEM = os.path.join("src", "main", "resources", "assets", "analogairwaves", "textures", "item")

# --- Analog Audio radio palette (sampled from analogaudio:block/radio_*) --------------------
BORDER = (0x2C, 0x2C, 0x21)      # side-face frame
BEVEL = (0x65, 0x43, 0x22)       # inner bevel / bottom rows of the side faces
BODY_DARK = (0x8B, 0x58, 0x2F)
BODY = (0xA8, 0x66, 0x34)
BODY_LIGHT = (0xB9, 0x6B, 0x30)  # the highlight row just under the top frame
GRILLE_DARK = (0x49, 0x2D, 0x10)
GRILLE = (0x66, 0x39, 0x14)
PANEL_DARK = (0x37, 0x29, 0x1B)
PANEL = (0x52, 0x3B, 0x2C)
DIAL = (0x76, 0x5F, 0x49)
METAL_DARK = (0x75, 0x69, 0x5F)
METAL_LIGHT = (0xB0, 0x9E, 0x90)

# Radio top/bottom ring, copied from analogaudio:block/radio_top. Rows 0-2 and 13-15 and
# columns 0-2 and 13-15 of this map are the frame; '4' is the plain body inside it.
RADIO_TOP_PALETTE = {
    "0": (0x2D, 0x2C, 0x22), "1": (0x3B, 0x33, 0x2F), "2": (0x66, 0x39, 0x14),
    "3": (0x65, 0x43, 0x22), "4": (0x8B, 0x58, 0x2F), "5": (0xA8, 0x66, 0x34),
}
RADIO_TOP_MAP = [
    "0011001111101000",
    "0234433333443320",
    "0345554334555431",
    "1354444444444531",
    "1354444444444541",
    "0354444444444541",
    "0354444444444541",
    "1454444444444531",
    "1454444444444531",
    "1454444444444531",
    "1354444444444540",
    "1354444444444540",
    "1354444444444541",
    "1345554334555431",
    "0234433344433320",
    "0001111111101000",
]

# Walkie-talkie aerial greys (analogaudio:item/walkie_talkie).
AERIAL = (0x89, 0x89, 0x87)
AERIAL_DARK = (0x6B, 0x6B, 0x6A)
AERIAL_SHADOW = (0x5D, 0x5D, 0x5D)

# Vanilla lightning-rod copper, so the transmitter aerial matches the rod it is crafted from.
COPPER_LIGHT = (0xE3, 0x82, 0x6C)
COPPER = (0xD6, 0x7B, 0x5B)
COPPER_MID = (0xC8, 0x74, 0x56)
COPPER_DARK = (0xA7, 0x5A, 0x40)
COPPER_SHADOW = (0x9A, 0x50, 0x38)

# Status indicator colours.
LED_OFF = (0x3A, 0x2A, 0x1C)
LED_ON = (0x6C, 0xD8, 0x5C)
LED_WARN = (0xE0, 0x8A, 0x33)

TRANSPARENT = None


def write_png(path, pixels):
    """Writes an RGBA PNG from a 2D list of (r,g,b) tuples or None for transparent."""
    height = len(pixels)
    width = len(pixels[0])
    raw = bytearray()
    for row in pixels:
        raw.append(0)
        for px in row:
            raw.extend((0, 0, 0, 0) if px is None else (px[0], px[1], px[2], 255))

    def chunk(tag, data):
        out = struct.pack(">I", len(data)) + tag + data
        return out + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = (b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header)
           + chunk(b"IDAT", zlib.compress(bytes(raw), 9)) + chunk(b"IEND", b""))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "wb") as handle:
        handle.write(png)
    print("wrote", path)


def blank(size=16, fill=TRANSPARENT):
    return [[fill for _ in range(size)] for _ in range(size)]


def rect(px, x0, y0, x1, y1, colour):
    """Fills columns x0..x1-1, rows y0..y1-1."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[y][x] = colour


def ring(px, x0, y0, x1, y1, colour):
    """Outlines the rectangle x0..x1-1, y0..y1-1."""
    for x in range(x0, x1):
        px[y0][x] = colour
        px[y1 - 1][x] = colour
    for y in range(y0, y1):
        px[y][x0] = colour
        px[y][x1 - 1] = colour


def grille(px, x0, y0, x1, y1):
    """The radio's speaker-grille checker."""
    for y in range(y0, y1):
        for x in range(x0, x1):
            px[y][x] = GRILLE_DARK if (x + y) % 2 == 0 else GRILLE


def radio_top_ring(interior=BODY_DARK):
    """A 16x16 face using the radio's own top/bottom frame ring around a plain interior."""
    px = blank(16, interior)
    for y, row in enumerate(RADIO_TOP_MAP):
        for x, key in enumerate(row):
            if x < 3 or x > 12 or y < 3 or y > 12:
                px[y][x] = RADIO_TOP_PALETTE[key]
    return px


# --- transmitter -----------------------------------------------------------------------------

def transmitter_side(led):
    """
    Side faces of the transmitter slab. The model only samples rows 0-3: a BORDER frame all
    the way round, the radio's highlight row, one body row, and a small status lamp centred
    on each face. The rest of the texture is plain framed body so particles look right.
    """
    px = blank(16, BODY)
    ring(px, 0, 0, 16, 16, BORDER)
    rect(px, 1, 1, 15, 2, BODY_LIGHT)
    rect(px, 0, 3, 16, 4, BORDER)
    px[1][7] = led
    px[1][8] = led
    px[2][7] = led
    px[2][8] = led
    return px


def transmitter_top():
    """Top of the slab: the radio's top ring, with a socket for the aerial in the centre."""
    px = radio_top_ring()
    rect(px, 6, 6, 10, 10, BEVEL)
    rect(px, 7, 7, 9, 9, COPPER_SHADOW)
    return px


def transmitter_bottom():
    return radio_top_ring()


def transmitter_antenna():
    """
    Copper aerial laid out like vanilla's lightning rod: the 4x4 tip at uv (0,0)-(4,4) and the
    2-wide rod down the left edge at uv (0,4)-(2,16).
    """
    px = blank(16, TRANSPARENT)
    rect(px, 0, 0, 4, 4, COPPER)
    rect(px, 0, 0, 4, 1, COPPER_LIGHT)
    rect(px, 3, 0, 4, 4, COPPER_MID)
    rect(px, 0, 3, 4, 4, COPPER_DARK)
    for y in range(4, 16):
        px[y][0] = COPPER if y % 3 else COPPER_LIGHT
        px[y][1] = COPPER_DARK if y % 3 else COPPER_SHADOW
    return px


# --- portable radio --------------------------------------------------------------------------
# The block is 10 wide, 7 tall and 6 deep, drawn at the same uv regions the model samples:
#   front/back  cols 3-12, rows 9-15      sides  cols 5-10, rows 9-15
#   top/bottom  cols 3-12, rows 5-10

def radio_base():
    px = blank(16, BODY)
    ring(px, 0, 0, 16, 16, BORDER)
    return px


def portable_front(powered):
    """Front: frame, highlight row, speaker grille on the left, dial and lamp on the right."""
    px = radio_base()
    ring(px, 3, 9, 13, 16, BORDER)
    rect(px, 4, 10, 12, 11, BODY_LIGHT)
    grille(px, 4, 11, 9, 15)
    rect(px, 9, 11, 12, 13, PANEL_DARK)
    px[11][10] = DIAL
    px[13][10] = METAL_DARK
    px[14][10] = LED_ON if powered else LED_OFF
    px[14][11] = LED_ON if powered else LED_OFF
    # Carry handle, drawn in the row above the body where the handle element samples.
    rect(px, 4, 8, 9, 9, PANEL)
    return px


def portable_back():
    px = radio_base()
    ring(px, 3, 9, 13, 16, BORDER)
    rect(px, 4, 10, 12, 11, BODY_LIGHT)
    grille(px, 4, 11, 12, 15)
    rect(px, 4, 8, 9, 9, PANEL)
    return px


def portable_side():
    px = radio_base()
    ring(px, 5, 9, 11, 16, BORDER)
    rect(px, 6, 10, 10, 11, BODY_LIGHT)
    grille(px, 6, 11, 10, 15)
    rect(px, 6, 8, 10, 9, PANEL)
    return px


def portable_top():
    """Top: frame and bevel like the radio's top, plus the handle and the aerial socket."""
    px = radio_base()
    ring(px, 3, 5, 13, 11, BORDER)
    ring(px, 4, 6, 12, 10, BEVEL)
    rect(px, 5, 7, 11, 9, BODY_DARK)
    rect(px, 4, 6, 9, 10, PANEL_DARK)
    rect(px, 5, 7, 8, 9, PANEL)
    px[7][11] = AERIAL_SHADOW
    return px


def portable_bottom():
    px = radio_base()
    ring(px, 3, 5, 13, 11, BORDER)
    ring(px, 4, 6, 12, 10, BEVEL)
    rect(px, 5, 7, 11, 9, BODY_DARK)
    return px


def portable_antenna():
    """Grey aerial in the walkie-talkie's metal, sampled at uv (0,0)-(1,8)."""
    px = blank(16, TRANSPARENT)
    for y in range(8):
        px[y][0] = AERIAL_DARK if y % 3 == 2 else AERIAL
    px[0][0] = AERIAL_SHADOW
    px[0][1] = AERIAL_SHADOW
    return px


def portable_item():
    """
    16x16 inventory sprite in the same plain style as Analog Audio's cassette tape: the radio
    front-on, a couple of pixels of margin, and a one-pixel aerial.
    """
    px = blank(16, TRANSPARENT)
    for y in range(1, 6):
        px[y][12] = AERIAL_DARK if y == 4 else AERIAL
    px[1][12] = AERIAL_SHADOW

    rect(px, 2, 6, 14, 15, BODY)
    ring(px, 2, 6, 14, 15, BORDER)
    rect(px, 3, 7, 13, 8, BODY_LIGHT)
    grille(px, 3, 8, 9, 14)
    rect(px, 10, 8, 13, 10, PANEL_DARK)
    px[8][11] = DIAL
    rect(px, 10, 11, 12, 13, METAL_DARK)
    px[13][12] = LED_ON
    return px


# --- broadcast waves ---------------------------------------------------------------------------
# The billboard a playing placed radio draws above its aerial. Three concentric arcs that expand
# and fade outward, as a 16x48 vertical strip of three 16x16 frames for Minecraft's .mcmeta
# animation to cycle.

WAVE_BRIGHT = (0xF2, 0x8C, 0x3C)
WAVE_MID = (0xE2, 0x6B, 0x24)
WAVE_DIM = (0xB8, 0x44, 0x16)


def wave_frame(phase):
    """
    One animation frame. `phase` in 0..2 slides each arc outward by one ring, so cycling the
    frames reads as waves travelling away from the aerial rather than merely blinking.
    """
    px = blank(16, TRANSPARENT)
    # The aerial tip sits at the bottom centre of the billboard. A 16px frame's centre line falls
    # between columns 7 and 8, so each arc is mirrored across that boundary below rather than
    # centred on a whole column, which would sit half a pixel right of the aerial.
    origin_y = 13

    # Three arcs at increasing radii; the phase offset rotates which radius is brightest.
    for index, radius in enumerate((2, 5, 8)):
        r = radius + phase
        if r > 12:
            continue
        colour = (WAVE_BRIGHT, WAVE_MID, WAVE_DIM)[(index + phase) % 3]
        # Walk the arc in fine angular steps and snap to the pixel grid, so the curve stays
        # chunky and symmetric instead of anti-aliased.
        # Plot the right half only, then mirror it across the centre line. Deriving the left
        # half from the right guarantees the arc is exactly centred on the 7/8 boundary; letting
        # both halves round independently is what left it a half pixel off the aerial.
        # Sweep from the apex outward. Stepping one texel of arc length at a time keeps the curve
        # unbroken; sparser steps leave gaps at the top where the two mirrored halves meet.
        steps = max(24, r * 12)
        for step in range(steps + 1):
            # 0 is the arc's outer tip, pi/2 its apex. Sweeping the full quarter turn closes the
            # seam at the top; stopping short of it is what left a notch where the halves meet.
            angle = math.pi * 0.5 * (step / steps)
            dx = int(round(math.cos(angle) * r))
            dy = int(round(math.sin(angle) * r))
            y = origin_y - dy
            if not (0 <= y < 16):
                continue
            for x in (8 + dx, 7 - dx):
                if 0 <= x < 16:
                    px[y][x] = colour
    return px


def broadcast_waves():
    """Stacks the three frames into one 16x48 strip."""
    strip = []
    for phase in range(3):
        strip.extend(wave_frame(phase))
    return strip


def main():
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    os.chdir(root)

    for name, led in (("idle", LED_OFF), ("broadcasting", LED_ON), ("interference", LED_WARN)):
        write_png(os.path.join(OUT_BLOCK, f"transmitter_{name}.png"), transmitter_side(led))
    write_png(os.path.join(OUT_BLOCK, "transmitter_top.png"), transmitter_top())
    write_png(os.path.join(OUT_BLOCK, "transmitter_bottom.png"), transmitter_bottom())
    write_png(os.path.join(OUT_BLOCK, "transmitter_antenna.png"), transmitter_antenna())

    write_png(os.path.join(OUT_BLOCK, "portable_radio_front.png"), portable_front(True))
    write_png(os.path.join(OUT_BLOCK, "portable_radio_front_off.png"), portable_front(False))
    write_png(os.path.join(OUT_BLOCK, "portable_radio_back.png"), portable_back())
    write_png(os.path.join(OUT_BLOCK, "portable_radio_side.png"), portable_side())
    write_png(os.path.join(OUT_BLOCK, "portable_radio_top.png"), portable_top())
    write_png(os.path.join(OUT_BLOCK, "portable_radio_bottom.png"), portable_bottom())
    write_png(os.path.join(OUT_BLOCK, "portable_radio_antenna.png"), portable_antenna())
    write_png(os.path.join(OUT_ITEM, "portable_radio.png"), portable_item())
    write_png(os.path.join(OUT_BLOCK, "broadcast_waves.png"), broadcast_waves())


if __name__ == "__main__":
    main()
