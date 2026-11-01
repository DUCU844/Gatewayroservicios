"""Renders evidence/*.txt (real command output captured by capture.sh on Windows / Git Bash) as PNG
screenshots styled like the Windows Terminal window where they were run.

Usage: python evidence/tools/render.py [name ...]     (no names = every .txt in evidence/)
Requires Pillow.
"""
import os
import socket
import getpass
import sys
import textwrap

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
FONTS = r'C:\Windows\Fonts'
MONO = [os.path.join(FONTS, 'CascadiaMono.ttf'), os.path.join(FONTS, 'consola.ttf')]
MONO_BOLD = [os.path.join(FONTS, 'CascadiaMono.ttf'), os.path.join(FONTS, 'consolab.ttf')]
UI = [os.path.join(FONTS, 'segoeui.ttf')]
ICONS = [os.path.join(FONTS, 'SegoeIcons.ttf'), os.path.join(FONTS, 'segmdl2.ttf')]

SIZE, LINE, PAD, COLS = 15, 20, 12, 130
BG, TITLEBAR, TAB, FG = (12, 12, 12), (32, 32, 32), (12, 12, 12), (204, 204, 204)
GREEN, PURPLE, YELLOW, CYAN = (19, 161, 14), (136, 23, 152), (193, 156, 0), (58, 150, 221)
ERR, STATUS = (231, 72, 86), (249, 241, 165)

USER = getpass.getuser()
HOST = socket.gethostname()
CWD = '/c/Users/' + USER + '/Downloads/Gatewayroservicios'


def font(paths, size):
    for p in paths:
        if os.path.exists(p):
            return ImageFont.truetype(p, size)
    return ImageFont.load_default()


def build_rows(raw):
    """Turns the captured text into colored terminal rows (list of segments per row)."""
    rows = []
    for line in raw:
        if line.startswith('# '):          # capture metadata (title/date): kept in the .txt only
            continue
        if line.startswith('$ '):          # Git Bash prompt + command
            rows.append([])
            rows.append([(USER + '@' + HOST + ' ', GREEN), ('MINGW64 ', PURPLE), (CWD + ' ', YELLOW), ('(main)', CYAN)])
            for i, part in enumerate(textwrap.wrap(line[2:], COLS - 2) or ['']):
                rows.append([('$ ' if i == 0 else '  ', FG), (part, FG)])
            continue
        color = FG
        if line.startswith(('HTTP/', '< HTTP/')):
            color = STATUS
        elif '[ERROR]' in line or 'Exception:' in line or 'FAILURE' in line:
            color = ERR
        for part in textwrap.wrap(line, COLS, replace_whitespace=False, drop_whitespace=False) or ['']:
            rows.append([(part, color)])
    rows.append([])
    rows.append([(USER + '@' + HOST + ' ', GREEN), ('MINGW64 ', PURPLE), (CWD + ' ', YELLOW), ('(main)', CYAN)])
    rows.append([('$ ', FG), ('\u2588', FG)])
    return rows


def render(name):
    with open(os.path.join(ROOT, name + '.txt'), encoding='utf-8', errors='replace') as f:
        raw = f.read().rstrip('\n').expandtabs(4).split('\n')

    mono, ui, icons = font(MONO, SIZE), font(UI, 12), font(ICONS, 10)
    rows = build_rows(raw)
    char_w = mono.getlength('M')
    width = int(PAD * 2 + char_w * COLS)
    bar_h = 40
    height = bar_h + PAD * 2 + LINE * len(rows)

    img = Image.new('RGB', (width, height), BG)
    d = ImageDraw.Draw(img)

    # Windows Terminal title bar with one active tab and the caption buttons.
    d.rectangle([0, 0, width, bar_h], fill=TITLEBAR)
    d.rounded_rectangle([8, 6, 300, bar_h + 6], radius=8, fill=TAB)
    d.rectangle([20, 15, 34, 29], outline=(150, 150, 150))            # tab icon
    d.text((26, 14), '>', fill=(220, 220, 220), font=font(MONO, 11), anchor='ma')
    d.text((44, 12), 'MINGW64:' + CWD[:24] + '...', fill=(255, 255, 255), font=ui)
    d.text((282, 13), '\uE711', fill=(200, 200, 200), font=icons)     # tab close
    d.text((314, 13), '\uE710', fill=(200, 200, 200), font=icons)     # new tab
    d.text((340, 13), '\uE70D', fill=(200, 200, 200), font=icons)     # dropdown
    for i, glyph in enumerate(['\uE921', '\uE922', '\uE8BB']):       # minimize, maximize, close
        d.text((width - 138 + i * 46 + 18, 14), glyph, fill=(230, 230, 230), font=icons)

    y = bar_h + PAD
    for segments in rows:
        x = PAD
        for text, color in segments:
            d.text((x, y), text, fill=color, font=mono)
            x += mono.getlength(text)
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
