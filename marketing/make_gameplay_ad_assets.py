from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "marketing" / "gameplay_ad_assets"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 1080, 1920
BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"


def make_overlay(filename, title, subtitle, accent):
    image = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((65, 95, 1015, 325), radius=44, fill=(12, 12, 39, 235), outline=accent, width=6)
    title_font = ImageFont.truetype(BOLD, 62)
    sub_font = ImageFont.truetype(BOLD, 31)
    tb = draw.textbbox((0, 0), title, font=title_font)
    sb = draw.textbbox((0, 0), subtitle, font=sub_font)
    draw.text(((W-(tb[2]-tb[0]))/2, 132), title, font=title_font, fill="white")
    draw.text(((W-(sb[2]-sb[0]))/2, 230), subtitle, font=sub_font, fill=(220, 220, 246))
    image.save(OUT / filename)


make_overlay("01_start.png", "À VOUS DE JOUER", "Remplissez la matrice 6 × 6", (255, 108, 25, 255))
make_overlay("02_click.png", "1 CLIC = FEU", "2e clic = Eau · 3e clic = Vide", (255, 145, 40, 255))
make_overlay("03_balance.png", "3 FEUX + 3 EAUX", "dans chaque ligne et colonne", (30, 187, 255, 255))
make_overlay("04_think.png", "OBSERVEZ. DÉDUISEZ.", "Jamais trois identiques", (255, 90, 45, 255))
make_overlay("05_finish.png", "PLUS QU'UNE CASE…", "Trouvez l'équilibre", (38, 205, 145, 255))
make_overlay("06_win.png", "ÉQUILIBRE ATTEINT !", "Pouvez-vous réussir ?", (255, 190, 30, 255))
