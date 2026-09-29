"""Draws the paper textures in res/drawable-nodpi (DESIGN.md section 7).

    python tools/make_grain.py

The screen ground, tiled by bg_paper.xml as the window background:

  paper_aged.png    aged paper: soft uneven warm blotches, fibres and a few tiny age spots
                    (512 x 512, so the blotches don't visibly repeat)

The component tiles, 256 x 256, tiled by Grain.java over a component's fill:

  grain_stock.png   card stock: short dark fibres and fine tooth, for light fills
                    (cards, fields, chips, tonal buttons, the nav bar)
  grain_ink.png     ink on paper: pale specks where the ink didn't take, for dark fills
                    (filled buttons, the selected chip, the FAB)
  grain_ticket.png  the espresso ticket: pale fibres and a soft mottle, for the spotlight

Every mark is drawn at its wrapped positions too, so the tile has no seams. STRENGTH scales the
component tiles; 2.4 is what Brent picked on the texture canvas ("loudest").
Needs Pillow and NumPy.
"""
import os

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

SIZE = 256
STRENGTH = 2.4
OUT = os.path.join(os.path.dirname(__file__), '..', 'app', 'src', 'main', 'res', 'drawable-nodpi')
INK_MUTED = (0x4F, 0x4A, 0x40)
AGED_BROWN = (0x6B, 0x4A, 0x2F)
ON_SPOTLIGHT = (0xF6, 0xEF, 0xE6)


def blurred_noise(rng, sigma, size=SIZE):
    """Seamless soft noise in 0..1: white noise blurred in frequency space, so it wraps."""
    white = rng.standard_normal((size, size))
    freq = np.fft.fftfreq(size)
    fy, fx = np.meshgrid(freq, freq, indexing='ij')
    soft = np.real(np.fft.ifft2(np.fft.fft2(white) * np.exp(-2 * (np.pi * sigma) ** 2 * (fx ** 2 + fy ** 2))))
    return (soft - soft.min()) / (soft.max() - soft.min())


def fibres(rng, count, length, alpha, size=SIZE):
    """Short straight strokes at random angles, drawn into an alpha layer."""
    layer = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(layer)
    for _ in range(count):
        x, y = rng.uniform(0, size, 2)
        angle = rng.uniform(0, np.pi)
        n = rng.uniform(*length)
        dx, dy = np.cos(angle) * n / 2, np.sin(angle) * n / 2
        a = int(rng.uniform(*alpha))
        for ox in (-size, 0, size):
            for oy in (-size, 0, size):
                draw.line([(x - dx + ox, y - dy + oy), (x + dx + ox, y + dy + oy)], fill=a, width=1)
    return np.array(layer, dtype=np.float64)


def specks(rng, count, alpha):
    layer = np.zeros((SIZE, SIZE))
    ys, xs = rng.integers(0, SIZE, count), rng.integers(0, SIZE, count)
    layer[ys, xs] = rng.uniform(*alpha, count)
    return layer


def spots(rng, count, radius, alpha, blur, size):
    """Round age spots (foxing), softened, drawn wrapped like the fibres."""
    layer = Image.new('L', (size, size), 0)
    draw = ImageDraw.Draw(layer)
    for _ in range(count):
        x, y = rng.uniform(0, size, 2)
        r = rng.uniform(*radius)
        a = int(rng.uniform(*alpha))
        for ox in (-size, 0, size):
            for oy in (-size, 0, size):
                draw.ellipse([x - r + ox, y - r + oy, x + r + ox, y + r + oy], fill=a)
    return np.array(layer.filter(ImageFilter.GaussianBlur(blur)), dtype=np.float64)


def save(name, rgb, alpha):
    alpha = np.clip(alpha, 0, 255).astype(np.uint8)
    size = alpha.shape[0]
    img = np.zeros((size, size, 4), dtype=np.uint8)
    img[..., :3] = rgb
    img[..., 3] = alpha
    Image.fromarray(img).save(os.path.join(OUT, name), optimize=True)
    print('%-17s mean alpha %.1f / 255' % (name, alpha.mean()))


def main():
    rng = np.random.default_rng(7)

    # The ground: aged paper (Brent picked it over vintage, which had stains and was too much)
    big = 512
    rng_ground = np.random.default_rng(11)
    tooth = np.clip(blurred_noise(rng_ground, 0.7, big) - 0.45, 0, None) * 24
    blotches = blurred_noise(rng_ground, 28, big) ** 2 * 20
    mottle = blurred_noise(rng_ground, 9, big) * 6
    save('paper_aged.png', AGED_BROWN,
         blotches + mottle + tooth + fibres(rng_ground, 380, (10, 24), (10, 24), big)
         + spots(rng_ground, 18, (1, 2.5), (25, 45), 1.2, big))

    # Card stock: fine tooth everywhere, then fibres on top
    tooth = np.clip(blurred_noise(rng, 0.8) - 0.45, 0, None) * 30
    save('grain_stock.png', INK_MUTED, STRENGTH * (tooth + fibres(rng, 260, (5, 14), (14, 30))))

    # Ink: a soft uneven lay-down, then pale specks
    mottle = blurred_noise(rng, 7) * 10
    save('grain_ink.png', ON_SPOTLIGHT, STRENGTH * (mottle + specks(rng, 900, (30, 60))))

    # Ticket: mottle, pale fibres, a few specks
    mottle = blurred_noise(rng, 10) * 12
    save('grain_ticket.png', ON_SPOTLIGHT,
         STRENGTH * (mottle + fibres(rng, 200, (6, 18), (12, 26)) + specks(rng, 300, (20, 40))))


if __name__ == '__main__':
    main()
