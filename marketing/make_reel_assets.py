from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "marketing" / "reel_assets"
OUT.mkdir(parents=True, exist_ok=True)

W, H = 1080, 1920
BOLD = "/System/Library/Fonts/Supplemental/Arial Bold.ttf"
REGULAR = "/System/Library/Fonts/Supplemental/Arial.ttf"


def font(size, bold=True):
    return ImageFont.truetype(BOLD if bold else REGULAR, size)


def centered(draw, text, y, text_font, fill, stroke=0):
    box = draw.textbbox((0, 0), text, font=text_font, stroke_width=stroke)
    x = (W - (box[2] - box[0])) // 2
    draw.text((x, y), text, font=text_font, fill=fill, stroke_width=stroke, stroke_fill=(15, 15, 38, 230))


def overlay(name, headline, accent):
    image = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle((75, 105, 1005, 300), radius=42, fill=(15, 15, 40, 224), outline=accent, width=5)
    centered(draw, headline, 152, font(66), "white")
    image.save(OUT / name)


overlay("hook.png", "CAN YOU SOLVE IT?", (255, 111, 25, 255))
overlay("tap.png", "TAP. THINK. BALANCE.", (31, 187, 255, 255))
overlay("rules.png", "NO 3 IN A ROW", (255, 92, 43, 255))
overlay("win.png", "THE PERFECT BALANCE!", (255, 190, 30, 255))

# Final call-to-action card.
card = Image.new("RGB", (W, H), (10, 10, 35))
draw = ImageDraw.Draw(card)
for y in range(H):
    ratio = y / H
    draw.line((0, y, W, y), fill=(int(18 + 22 * ratio), int(16 + 5 * ratio), int(52 + 25 * ratio)))

fire = Image.open(ROOT / "app/src/main/res/drawable/game_mascot_fire.png").convert("RGBA")
water = Image.open(ROOT / "app/src/main/res/drawable/game_mascot_water.png").convert("RGBA")

def contain(subject, size):
    subject.thumbnail(size, Image.Resampling.LANCZOS)
    return subject

fire = contain(fire, (500, 620))
water = contain(water, (500, 620))

glow = Image.new("RGBA", card.size, (0, 0, 0, 0))
gd = ImageDraw.Draw(glow)
gd.ellipse((15, 325, 585, 1025), fill=(255, 86, 15, 85))
gd.ellipse((495, 325, 1065, 1025), fill=(0, 175, 255, 85))
glow = glow.filter(ImageFilter.GaussianBlur(75))
card = Image.alpha_composite(card.convert("RGBA"), glow)
card.alpha_composite(fire, (45, 380))
card.alpha_composite(water, (540, 390))
draw = ImageDraw.Draw(card)
centered(draw, "SOLVEX", 1050, font(126), "white")
centered(draw, "FIRE & WATER LOGIC", 1190, font(43), (183, 183, 255, 255))
draw.rounded_rectangle((150, 1375, 930, 1535), radius=55, fill=(255, 77, 22, 255))
centered(draw, "PLAY NOW", 1414, font(65), "white")
centered(draw, "Spark. Flow. Solve.", 1615, font(42, False), (220, 220, 250, 255))
card.convert("RGB").save(OUT / "end_card.jpg", quality=96)
