# Builds the face of the Genshin look, resource_han_rounded.ttf of core/ui-genshin, from the bold face of
# Resource Han Rounded (https://github.com/CyanoHao/Resource-Han-Rounded, SIL OFL 1.1, see OFL.txt):
#
#   * only the characters the app can show are kept: the Latin, Greek and Cyrillic letters, kana, the common
#     hanzi, kanji and hangul of GB 2312, JIS X 0208 and KS X 1001, and every character of the strings, the
#     marker data and the code, so 10 MB are left of 28;
#   * a line is 1.22 em high instead of 1.45, the layouts are made for it: two lines of a name fit an item;
#   * … sits on the baseline in every language, not in the middle of the line as in Chinese.
#
# Run it again when the strings or the marker data get a character out of these sets, it says which ones:
#
#   pip install fonttools
#   python3 tools/genshin-font/make_font.py ResourceHanRoundedSC-Bold.ttf
#
# ResourceHanRoundedSC-Bold.ttf is in RHR-TTF-0.990.7z of the v0.990 release.

import argparse
import glob
import os

from fontTools import subset
from fontTools.ttLib import TTFont

TOOL = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.dirname(os.path.dirname(TOOL))
OUTPUT = os.path.join(ROOT, 'core/ui-genshin/src/commonMain/composeResources/font/resource_han_rounded.ttf')

# The texts the Genshin look shows
SOURCES = [
    'feature/*/*/src/commonMain/composeResources/values*/strings.xml',
    'feature/map/logic-genshin-impl/src/commonMain/composeResources/files/**/*.json',
    'feature/*/*/src/commonMain/kotlin/**/*.kt',
    'core/*/src/commonMain/kotlin/**/*.kt',
]

# Whole blocks: Latin, combining marks, Greek, Cyrillic, punctuation, currency, letterlike symbols and arrows,
# enclosed numbers, shapes and symbols, CJK punctuation and kana, hangul jamo, full and half width forms
BLOCKS = [
    (0x0020, 0x024F), (0x0300, 0x036F), (0x0370, 0x03FF), (0x0400, 0x04FF), (0x2000, 0x206F), (0x20A0, 0x20CF),
    (0x2100, 0x21FF), (0x2460, 0x24FF), (0x25A0, 0x26FF), (0x3000, 0x30FF), (0x3130, 0x318F), (0xFF00, 0xFFEF),
]

# The common characters: the ones the national two byte encodings have
ENCODED = [('gb2312', 0x4E00, 0x9FFF), ('shift_jis', 0x4E00, 0x9FFF), ('euc_kr', 0xAC00, 0xD7A3)]

ASCENT, DESCENT = 916, 301


def two_byte(char, encoding):
    try:
        return len(char.encode(encoding)) == 2
    except UnicodeEncodeError:
        return False


def used_characters():
    chars = set()
    for pattern in SOURCES:
        for path in glob.glob(os.path.join(ROOT, pattern), recursive=True):
            with open(path, encoding='utf-8') as file:
                chars |= {ord(c) for c in file.read() if ord(c) >= 0x80}
    return chars


def baseline_ellipsis(font):
    """The glyph the pwid feature puts in place of …: the dots of Latin text."""
    gsub = font['GSUB'].table
    centered = font.getBestCmap()[0x2026]
    for record in gsub.FeatureList.FeatureRecord:
        if record.FeatureTag != 'pwid':
            continue
        for index in record.Feature.LookupListIndex:
            for table in gsub.LookupList.Lookup[index].SubTable:
                mapping = getattr(getattr(table, 'ExtSubTable', table), 'mapping', {})
                if centered in mapping:
                    return mapping[centered]
    raise SystemExit('No … for Latin text in the font')


def main():
    parser = argparse.ArgumentParser(description='Builds resource_han_rounded.ttf of core/ui-genshin')
    parser.add_argument('source', help='ResourceHanRoundedSC-Bold.ttf')
    font = TTFont(parser.parse_args().source)

    used = used_characters()
    kept = {c for start, end in BLOCKS for c in range(start, end + 1)}
    kept |= {c for encoding, start, end in ENCODED for c in range(start, end + 1) if two_byte(chr(c), encoding)}
    kept |= used

    ellipsis = baseline_ellipsis(font)
    for table in font['cmap'].tables:
        if 0x2026 in table.cmap:
            table.cmap[0x2026] = ellipsis

    options = subset.Options()
    options.layout_features = ['*']
    options.name_IDs = ['*']
    options.name_languages = ['*']
    options.notdef_outline = True
    subsetter = subset.Subsetter(options)
    subsetter.populate(unicodes=kept)
    subsetter.subset(font)

    font['hhea'].ascent, font['hhea'].descent, font['hhea'].lineGap = ASCENT, -DESCENT, 0
    font['OS/2'].usWinAscent, font['OS/2'].usWinDescent = ASCENT, DESCENT

    # The license goes with every copy of the font
    name = font['name']
    with open(os.path.join(TOOL, 'OFL.txt'), encoding='utf-8') as file:
        license_text = file.read().strip()
    name.removeNames(nameID=13)
    name.removeNames(nameID=14)
    name.setName(license_text, 13, 3, 1, 0x409)
    name.setName('https://openfontlicense.org', 14, 3, 1, 0x409)

    font.save(OUTPUT)

    cmap = font.getBestCmap()
    # Emoji and their variation selectors come from the font of the system
    missing = sorted(c for c in used if c not in cmap and not 0xFE00 <= c <= 0xFE0F and not 0x2600 <= c <= 0x27BF)
    print(f'{os.path.relpath(OUTPUT, ROOT)}: {len(cmap)} characters, {os.path.getsize(OUTPUT) / 1e6:.1f} MB')
    if missing:
        print('Not in Resource Han Rounded, the system font draws them:', ' '.join(f'{chr(c)} U+{c:04X}' for c in missing))


if __name__ == '__main__':
    main()
