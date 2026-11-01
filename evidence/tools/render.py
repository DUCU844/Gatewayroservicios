"""Renders evidence/*.txt (real command output captured by capture.sh) as terminal-style PNG screenshots.

Usage: python evidence/tools/render.py [name ...]     (no names = every .txt in evidence/)
Requires Pillow.
"""
import os
import sys
import textwrap

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
FONT_PATHS = [r'C:\Windows\Fonts\consola.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf']
BOLD_PATHS = [r'C:\Windows\Fonts\consolab.ttf', '/usr/share/fonts/truetype/dejavu/DejaVuSansMono-Bold.ttf']
UI_PATHS = [r'C:\Windows\Fonts\segoeui.ttf'] + FONT_PATHS

SIZE, LINE, PAD, COLS = 15, 20, 16, 130
BG, BAR, FG = (30, 30, 30), (50, 50, 51), (212, 212, 212)
CMD, META, STATUS, ERR = (78, 201, 176), (106, 153, 85), (220, 220, 170), (244, 135, 113)


def font(paths, size):
    for p in paths:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def color_of(line):
    if line.startswith('$ '):
        return CMD, True
    if line.startswith('# '):
        return META, False
    if line.startswith(('HTTP/', '< HTTP/')):
        return STATUS, True
    if 'ERROR' in line or 'Exception' in line or 'FAIL' in line:
        return ERR, False
    return FG, False


def render(name):
    with open(os.path.join(ROOT, name + '.txt'), encoding='utf-8', errors='replace') as f:
        raw = f.read().rstrip('\n').expandtabs(4).split('\n')
    title = raw[0][2:] if raw and raw[0].startswith('# ') else name

    rows = []  # (text, color, bold)
    for line in raw:
        color, bold = color_of(line)
        for part in textwrap.wrap(line, COLS, replace_whitespace=False, drop_whitespace=False) or ['']:
            rows.append((part, color, bold))

    regular, bold_font, ui = font(FONT_PATHS, SIZE), font(BOLD_PATHS, SIZE), font(UI_PATHS, 14)
    char_w = regular.getlength('M')
    width = int(PAD * 2 + char_w * COLS)
    bar_h = 34
    height = bar_h + PAD * 2 + LINE * len(rows)

    img = Image.new('RGB', (width, height), BG)
    draw = ImageDraw.Draw(img)
    draw.rectangle([0, 0, width, bar_h], fill=BAR)
    for i, c in enumerate([(255, 95, 87), (254, 188, 46), (40, 200, 64)]):
        x = 14 + i * 20
        draw.ellipse([x, 11, x + 12, 23], fill=c)
    draw.text((84, 8), title, fill=(204, 204, 204), font=ui)

    y = bar_h + PAD
    for text, color, bold in rows:
        draw.text((PAD, y), text, fill=color, font=bold_font if bold else regular)
        y += LINE

    out = os.path.join(ROOT, 'screenshots', name + '.png')
    os.makedirs(os.path.dirname(out), exist_ok=True)
    img.save(out, optimize=True)
    print('rendered', os.path.relpath(out, ROOT))


def main():
    names = sys.argv[1:] or sorted(f[:-4] for f in os.listdir(ROOT) if f.endswith('.txt'))
    for name in names:
        render(name)


if __name__ == '__main__':
    main()
