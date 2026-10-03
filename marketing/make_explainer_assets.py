from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "marketing" / "explainer_assets"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 1080, 1920
BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"


def f(size, bold=True):
    return ImageFont.truetype(BOLD if bold else REGULAR, size)


def center(draw, text, y, size, color="white", bold=True):
    ft = f(size, bold)
    box = draw.textbbox((0, 0), text, font=ft)
    draw.text(((W - box[2] + box[0]) / 2, y), text, font=ft, fill=color)


def base(title, subtitle):
    im = Image.new("RGB", (W, H), (13, 13, 43))
    d = ImageDraw.Draw(im)
    for y in range(H):
        k = y / H
        d.line((0, y, W, y), fill=(int(11 + 22*k), int(12 + 7*k), int(42 + 34*k)))
    center(d, title, 125, 72)
    center(d, subtitle, 220, 36, (198, 198, 235), False)
    return im.convert("RGBA")


fire = Image.open(ROOT / "app/src/main/res/drawable/game_mascot_fire.png").convert("RGBA")
water = Image.open(ROOT / "app/src/main/res/drawable/game_mascot_water.png").convert("RGBA")

# AI-generated cinematic opening, with deterministic typography added afterward.
intro = Image.open(ROOT / "marketing/reel_assets/ai_intro_6x6.png").convert("RGB")
intro = intro.resize((W, H), Image.Resampling.LANCZOS).convert("RGBA")
shade = Image.new("RGBA", (W, 430), (0, 0, 0, 0))
sd = ImageDraw.Draw(shade)
for y in range(430):
    sd.line((0, y, W, y), fill=(7, 7, 31, int(220 * (1-y/430))))
intro.alpha_composite(shade, (0, 0))
draw = ImageDraw.Draw(intro)
center(draw, "SOLVEX", 100, 112)
center(draw, "TROUVEZ L'ÉQUILIBRE", 225, 45, (211, 211, 250))
intro.convert("RGB").save(OUT / "00_intro.jpg", quality=96)


def mascot(kind, size):
    src = (fire if kind == "F" else water).copy()
    src.thumbnail((size, size), Image.Resampling.LANCZOS)
    return src


def tile(im, kind, x, y, size=180, border=None):
    d = ImageDraw.Draw(im)
    fill = (70, 26, 22, 255) if kind == "F" else (12, 41, 82, 255) if kind == "W" else (225, 228, 242, 255)
    d.rounded_rectangle((x, y, x+size, y+size), radius=28, fill=fill, outline=border or (110, 111, 155, 255), width=5)
    if kind in ("F", "W"):
        m = mascot(kind, int(size * .88))
        im.alpha_composite(m, (x + (size-m.width)//2, y + (size-m.height)//2))


# Click cycle: exactly the app's EMPTY -> FIRE -> WATER -> EMPTY behavior.
im = base("COMMENT JOUER", "Touchez une case vide")
d = ImageDraw.Draw(im)
labels = [("1er CLIC", "F", "FEU"), ("2e CLIC", "W", "EAU"), ("3e CLIC", "E", "VIDE")]
for i, (top, kind, bottom) in enumerate(labels):
    x = 95 + i * 330
    d.rounded_rectangle((x, 470, x+280, 970), radius=38, fill=(245, 245, 255, 245))
    box = d.textbbox((0, 0), top, font=f(31))
    d.text((x+(280-box[2])/2, 510), top, font=f(31), fill=(82, 81, 145))
    tile(im, kind, x+50, 620, 180, (255, 112, 28, 255) if i == 0 else None)
    box = d.textbbox((0, 0), bottom, font=f(31))
    d.text((x+(280-box[2])/2, 850), bottom, font=f(31), fill=(25, 25, 60))
    if i < 2:
        d.text((x+287, 690), "→", font=f(42), fill=(255, 120, 32))
center(d, "FEU  →  EAU  →  VIDE", 1120, 54, (255, 184, 64))
center(d, "Chaque clic change la case", 1210, 37, (215, 215, 240), False)
im.convert("RGB").save(OUT / "01_clicks.jpg", quality=96)


# Fully valid 6×6 solution: balanced rows/columns, no triples.
im = base("MATRICE 6 × 6", "Même nombre de Feu et d'Eau")
d = ImageDraw.Draw(im)
matrix = ["FFWFWW", "FFWWFW", "WWFFWF", "FWFFWW", "WFWWFF", "WWFWFF"]
size, gap = 125, 11
x0, y0 = (W-(6*size+5*gap))//2, 395
for r, row in enumerate(matrix):
    for c, kind in enumerate(row):
        tile(im, kind, x0+c*(size+gap), y0+r*(size+gap), size)
center(d, "3 FEU  +  3 EAU", 1300, 55, (255, 185, 70))
center(d, "dans chaque ligne et chaque colonne", 1380, 36, (215, 215, 240), False)
im.convert("RGB").save(OUT / "02_balance.jpg", quality=96)


# No-three example, shown only with a correct sequence.
im = base("JAMAIS 3 IDENTIQUES", "Deux identiques imposent l'autre élément")
d = ImageDraw.Draw(im)
x0, y0, size, gap = 170, 610, 230, 22
for c, kind in enumerate("FFW"):
    tile(im, kind, x0+c*(size+gap), y0, size, (48, 213, 137, 255) if c == 2 else None)
center(d, "FEU  +  FEU  →  EAU", 1010, 58, (255, 184, 64))
center(d, "✓ Ligne correcte", 1110, 42, (72, 220, 145))
im.convert("RGB").save(OUT / "03_no_three.jpg", quality=96)


# Constraint examples are deliberately valid.
im = base("SUIVEZ LES SYMBOLES", "Les deux relations du plateau")
d = ImageDraw.Draw(im)
def pair(y, a, sign, b, caption, color):
    tile(im, a, 190, y, 220)
    d.ellipse((445, y+70, 635, y+150), fill=(245, 245, 255), outline=color, width=6)
    box = d.textbbox((0, 0), sign, font=f(54))
    d.text(((W-box[2])/2, y+76), sign, font=f(54), fill=color)
    tile(im, b, 670, y, 220)
    center(d, caption, y+255, 38, color)
pair(470, "F", "=", "F", "MÊME ÉLÉMENT", (255, 145, 45))
pair(1020, "F", "×", "W", "ÉLÉMENTS DIFFÉRENTS", (55, 195, 255))
im.convert("RGB").save(OUT / "04_symbols.jpg", quality=96)


# Clean final card.
im = base("SOLVEX", "Spark. Flow. Solve.")
d = ImageDraw.Draw(im)
fm, wm = mascot("F", 430), mascot("W", 430)
im.alpha_composite(fm, (105, 470))
im.alpha_composite(wm, (550, 480))
d.rounded_rectangle((145, 1190, 935, 1370), radius=62, fill=(255, 76, 25))
center(d, "JOUER MAINTENANT", 1240, 56)
center(d, "Le défi Feu & Eau", 1480, 45, (205, 205, 244), False)
im.convert("RGB").save(OUT / "05_cta.jpg", quality=96)
