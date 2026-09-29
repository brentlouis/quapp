"""Draws the three component textures in res/drawable-nodpi (DESIGN.md section 7).

    python tools/make_grain.py

Each is a seamless 256 x 256 PNG, mostly transparent, tiled by Grain.java over a component's fill:

  grain_stock.png   card stock: short dark fibres and fine tooth, for light fills
                    (cards, fields, chips, tonal buttons, the nav bar)
  grain_ink.png     ink on paper: pale specks where the ink didn't take, for dark fills
                    (filled buttons, the selected chip, the FAB)
  grain_ticket.png  the espresso ticket: pale fibres and a soft mottle, for the spotlight

Every mark is drawn at its wrapped positions too, so the tile has no seams.
Needs Pillow and NumPy.
"""
import os

import numpy as np
from PIL import Image, ImageDraw

SIZE = 256
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
INK_MUTED = (0x4F, 0x4A, 0x40)
ON_SPOTLIGHT = (0xF6, 0xEF, 0xE6)


def blurred_noise(rng, sigma):
    """Seamless soft noise in 0..1: white noise blurred in frequency space, so it wraps."""
    white = rng.standard_normal((SIZE, SIZE))
    freq = np.fft.fftfreq(SIZE)
    fy, fx = np.meshgrid(freq, freq, indexing='ij')
    soft = np.real(np.fft.ifft2(np.fft.fft2(white) * np.exp(-2 * (np.pi * sigma) ** 2 * (fx ** 2 + fy ** 2))))
    return (soft - soft.min()) / (soft.max() - soft.min())


def fibres(rng, count, length, alpha):
    """Short straight strokes at random angles, drawn into an alpha layer."""
    layer = Image.new('L', (SIZE, SIZE), 0)
    draw = ImageDraw.Draw(layer)
    for _ in range(count):
        x, y = rng.uniform(0, SIZE, 2)
        angle = rng.uniform(0, np.pi)
        n = rng.uniform(*length)
        dx, dy = np.cos(angle) * n / 2, np.sin(angle) * n / 2
        a = int(rng.uniform(*alpha))
        for ox in (-SIZE, 0, SIZE):
            for oy in (-SIZE, 0, SIZE):
                draw.line([(x - dx + ox, y - dy + oy), (x + dx + ox, y + dy + oy)], fill=a, width=1)
    return np.array(layer, dtype=np.float64)


def specks(rng, count, alpha):
    layer = np.zeros((SIZE, SIZE))
    ys, xs = rng.integers(0, SIZE, count), rng.integers(0, SIZE, count)
    layer[ys, xs] = rng.uniform(*alpha, count)
    return layer


def save(name, rgb, alpha):
    alpha = np.clip(alpha, 0, 255).astype(np.uint8)
    img = np.zeros((SIZE, SIZE, 4), dtype=np.uint8)
    img[..., :3] = rgb
    img[..., 3] = alpha
    Image.fromarray(img).save(os.path.join(OUT, name), optimize=True)
    print('%-17s mean alpha %.1f / 255' % (name, alpha.mean()))


def main():
    rng = np.random.default_rng(7)

    # Card stock: fine tooth everywhere, then fibres on top
    tooth = np.clip(blurred_noise(rng, 0.8) - 0.45, 0, None) * 30
    save('grain_stock.png', INK_MUTED, tooth + fibres(rng, 260, (5, 14), (14, 30)))

    # Ink: a soft uneven lay-down, then pale specks
    mottle = blurred_noise(rng, 7) * 10
    save('grain_ink.png', ON_SPOTLIGHT, mottle + specks(rng, 900, (30, 60)))

    # Ticket: mottle, pale fibres, a few specks
    mottle = blurred_noise(rng, 10) * 12
    save('grain_ticket.png', ON_SPOTLIGHT,
         mottle + fibres(rng, 200, (6, 18), (12, 26)) + specks(rng, 300, (20, 40)))


if __name__ == '__main__':
    main()
