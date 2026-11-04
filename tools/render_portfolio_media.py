"""Render an illustrative README preview and short product walkthrough."""

from __future__ import annotations

from pathlib import Path

import imageio.v2 as imageio
import numpy as np
from PIL import Image, ImageDraw, ImageFont, ImageOps


ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "docs" / "media"
HERO = ROOT / "android" / "app" / "src" / "main" / "res" / "drawable" / "basketguard_hero.webp"

FOREST = "#174b3d"
FOREST_DARK = "#12392f"
MINT = "#dce de0".replace(" ", "")
WARM = "#f7f6f0"
INK = "#1e2924"
MUTED = "#748078"
WHITE = "#ffffff"
RESCUE = "#fff0e4"
ORANGE = "#ba6433"

FONT_DIR = Path("C:/Windows/Fonts")


def font(size: int, bold: bool = False) -> ImageFont.FreeTypeFont:
    candidates = [
        FONT_DIR / ("segoeuib.ttf" if bold else "segoeui.ttf"),
        FONT_DIR / ("arialbd.ttf" if bold else "arial.ttf"),
    ]
    for path in candidates:
        if path.exists():
            return ImageFont.truetype(str(path), size=size)
    return ImageFont.load_default()


def rounded(draw: ImageDraw.ImageDraw, box: tuple[int, int, int, int], fill: str, radius: int = 18, outline: str | None = None, width: int = 1):
    draw.rounded_rectangle(box, radius=radius, fill=fill, outline=outline, width=width)


def line_text(draw: ImageDraw.ImageDraw, xy: tuple[int, int], value: str, size: int, fill: str = INK, bold: bool = False):
    draw.text(xy, value, font=font(size, bold), fill=fill)


def fitted_photo(size: tuple[int, int], anchor: tuple[float, float] = (0.5, 0.5)) -> Image.Image:
    source = Image.open(HERO).convert("RGB")
    return ImageOps.fit(source, size, method=Image.Resampling.LANCZOS, centering=anchor)


def phone_base() -> tuple[Image.Image, ImageDraw.ImageDraw]:
    image = Image.new("RGB", (420, 900), "#17211e")
    draw = ImageDraw.Draw(image)
    rounded(draw, (10, 10, 410, 890), "#0e1512", 48)
    rounded(draw, (22, 22, 398, 878), WARM, 38)
    rounded(draw, (164, 28, 256, 42), "#0e1512", 8)
    line_text(draw, (42, 52), "9:41", 14, INK, True)
    line_text(draw, (340, 52), "●  ▰  100%", 12, INK, True)
    return image, draw


def storefront() -> Image.Image:
    image, draw = phone_base()
    line_text(draw, (42, 82), "DELIVERING TO", 11, MUTED, True)
    line_text(draw, (42, 100), "Indiranagar · 6 min", 17, INK, True)
    line_text(draw, (328, 94), "BAG", 12, FOREST, True)
    rounded(draw, (38, 138, 382, 186), WHITE, 16)
    line_text(draw, (54, 151), "Search avocado, coffee, eggs...", 14, MUTED)
    photo = fitted_photo((344, 154), (0.62, 0.57))
    image.paste(photo, (38, 204))
    overlay = Image.new("RGBA", (344, 154), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    od.rounded_rectangle((0, 0, 344, 154), radius=22, fill=(14, 49, 39, 115))
    image.paste(overlay, (38, 204), overlay)
    draw = ImageDraw.Draw(image)
    line_text(draw, (56, 222), "THE BASKETGUARD PROMISE", 10, "#dbf0dd", True)
    line_text(draw, (56, 247), "Your basket.", 22, WHITE, True)
    line_text(draw, (56, 273), "Your call on swaps.", 22, WHITE, True)
    line_text(draw, (56, 307), "Set a price cap. Choose every replacement.", 11, "#e2eee6")
    rounded(draw, (38, 374, 382, 455), WHITE, 20)
    rounded(draw, (52, 390, 98, 436), MINT, 15)
    line_text(draw, (64, 399), "AI", 17, FOREST, True)
    line_text(draw, (112, 389), "AI Basket Planner", 16, INK, True)
    line_text(draw, (112, 416), "Meal ideas that fit live stock + budget", 11, MUTED)
    line_text(draw, (323, 401), "PLAN >", 11, FOREST, True)
    line_text(draw, (40, 481), "Browse by aisle", 19, INK, True)
    for x, w, label, active in [(40, 77, "Produce", True), (123, 72, "Dairy", False), (201, 78, "Bakery", False), (287, 90, "Berries", False)]:
        rounded(draw, (x, 513, x + w, 546), FOREST if active else WHITE, 16)
        tw = draw.textbbox((0, 0), label, font=font(11, True))[2]
        line_text(draw, (x + (w - tw) // 2, 522), label, 11, WHITE if active else INK, True)
    line_text(draw, (40, 568), "Picked for your day", 19, INK, True)
    line_text(draw, (40, 594), "Fresh finds, checked before checkout", 11, MUTED)
    for x, product, unit, price, tint in [
        (40, "Avocado", "1 piece", "₹79", "#e3f0dd"),
        (216, "Farm eggs", "6 pieces", "₹99", "#f4e9d9"),
    ]:
        rounded(draw, (x, 625, x + 166, 798), WHITE, 19)
        rounded(draw, (x + 10, 635, x + 156, 710), tint, 14)
        line_text(draw, (x + 52, 654), "FRESH", 11, FOREST, True)
        line_text(draw, (x + 13, 724), product, 14, INK, True)
        line_text(draw, (x + 13, 747), unit, 11, MUTED)
        line_text(draw, (x + 13, 774), price, 15, INK, True)
        rounded(draw, (x + 103, 755, x + 155, 785), MINT, 10)
        line_text(draw, (x + 112, 762), "ADD +", 10, FOREST, True)
    draw.rectangle((23, 822, 397, 877), fill=WHITE)
    for x, label in [(65, "Home"), (155, "Basket"), (250, "Orders"), (342, "Profile")]:
        line_text(draw, (x - 7, 842), label, 10, FOREST if label == "Home" else MUTED, True)
    return image


def ai_planner() -> Image.Image:
    image, draw = phone_base()
    line_text(draw, (40, 88), "BACK  /  HOME", 11, FOREST, True)
    line_text(draw, (40, 128), "AI Basket Planner", 24, INK, True)
    line_text(draw, (40, 164), "Make a meal plan from what is in stock.", 12, MUTED)
    rounded(draw, (38, 203, 382, 302), WHITE, 18)
    line_text(draw, (54, 218), "WHAT DO YOU NEED?", 10, MUTED, True)
    line_text(draw, (54, 247), "Quick vegetarian dinner for two", 15, INK, True)
    line_text(draw, (54, 274), "Use simple ingredients, please", 12, MUTED)
    rounded(draw, (38, 316, 382, 375), WHITE, 17)
    line_text(draw, (54, 329), "BUDGET", 10, MUTED, True)
    line_text(draw, (54, 348), "₹350", 17, INK, True)
    rounded(draw, (38, 391, 382, 443), FOREST, 16)
    line_text(draw, (136, 406), "Build my basket", 15, WHITE, True)
    line_text(draw, (40, 472), "A quick, fresh dinner", 19, INK, True)
    line_text(draw, (40, 501), "Three store items · 2 servings", 11, MUTED)
    for y, emoji, name, quantity, price, reason in [
        (537, "BREAD", "Multigrain loaf", "1 × 400 g", "₹119", "Toasts for the base"),
        (624, "GREENS", "Baby spinach", "1 × 100 g", "₹59", "Fresh green filling"),
        (711, "YOGURT", "Greek yogurt", "1 × 400 g", "₹89", "Cool side for the meal"),
    ]:
        rounded(draw, (38, y, 382, y + 76), WHITE, 16)
        rounded(draw, (48, y + 11, 97, y + 62), MINT, 13)
        label_width = draw.textbbox((0, 0), emoji, font=font(8, True))[2]
        line_text(draw, (73 - label_width // 2, y + 33), emoji, 8, FOREST, True)
        line_text(draw, (111, y + 14), name, 14, INK, True)
        line_text(draw, (111, y + 40), f"{quantity} · {reason}", 10, MUTED)
        line_text(draw, (326, y + 28), price, 13, INK, True)
    rounded(draw, (38, 808, 382, 854), MINT, 14)
    line_text(draw, (54, 822), "Total  ₹267", 14, FOREST, True)
    line_text(draw, (263, 825), "₹83 left", 12, FOREST)
    return image


def rescue() -> Image.Image:
    image, draw = phone_base()
    line_text(draw, (40, 88), "BACK  /  CHECKOUT", 11, FOREST, True)
    line_text(draw, (40, 128), "Basket Rescue", 24, INK, True)
    line_text(draw, (40, 163), "A shelf changed. You decide what happens.", 12, MUTED)
    rounded(draw, (38, 196, 382, 320), RESCUE, 20)
    line_text(draw, (54, 211), "WILD BLUEBERRIES", 10, ORANGE, True)
    line_text(draw, (54, 235), "Need 2 · 0 available now", 14, INK, True)
    line_text(draw, (54, 264), "Choose a same-use swap below.", 11, MUTED)
    line_text(draw, (54, 292), "Full quantity checked · no silent swaps", 10, FOREST, True)
    line_text(draw, (40, 348), "YOUR MAX PRICE INCREASE", 10, MUTED, True)
    for x, w, label, active in [(40, 100, "Strict · ₹0", False), (147, 115, "Balanced · +15%", True), (269, 112, "Flexible · +30%", False)]:
        rounded(draw, (x, 373, x + w, 408), FOREST if active else WHITE, 15)
        bbox = draw.textbbox((0, 0), label, font=font(9, True))
        line_text(draw, (x + (w - (bbox[2] - bbox[0])) // 2, 384), label, 9, WHITE if active else INK, True)
    line_text(draw, (40, 435), "IN STOCK FOR YOUR FULL QUANTITY", 10, FOREST, True)
    for y, emoji, name, stock, price, diff in [
        (465, "BERRY", "Sweet strawberries", "8 in stock", "₹139", "₹30 less"),
        (570, "RASP", "Fresh raspberries", "5 in stock", "₹189", "₹20 more"),
    ]:
        rounded(draw, (38, y, 382, y + 92), WHITE, 18)
        rounded(draw, (50, y + 16, 105, y + 72), "#f4e2e2", 14)
        label_width = draw.textbbox((0, 0), emoji, font=font(8, True))[2]
        line_text(draw, (77 - label_width // 2, y + 39), emoji, 8, FOREST, True)
        line_text(draw, (118, y + 14), name, 13, INK, True)
        line_text(draw, (118, y + 39), stock, 10, MUTED)
        line_text(draw, (118, y + 59), diff, 10, FOREST, True)
        line_text(draw, (315, y + 18), price, 13, INK, True)
        rounded(draw, (306, y + 52, 368, y + 77), MINT, 10)
        line_text(draw, (317, y + 59), "Choose", 10, FOREST, True)
    rounded(draw, (38, 696, 201, 744), WHITE, 14, outline="#ccd7ce")
    rounded(draw, (218, 696, 382, 744), WHITE, 14, outline="#ccd7ce")
    line_text(draw, (75, 712), "Keep 2", 12, FOREST, True)
    line_text(draw, (261, 712), "Remove item", 12, FOREST, True)
    rounded(draw, (38, 764, 382, 825), FOREST, 17)
    line_text(draw, (107, 785), "Resolve item to continue", 13, WHITE, True)
    return image


def save_phone_screens() -> list[Image.Image]:
    screens = [storefront(), ai_planner(), rescue()]
    names = ["storefront", "ai-basket-planner", "basket-rescue"]
    for screen, name in zip(screens, names):
        screen.save(OUT / f"{name}.png", optimize=True)
    return screens


def preview_board(screens: list[Image.Image]) -> Image.Image:
    canvas = Image.new("RGB", (1880, 1120), WARM)
    draw = ImageDraw.Draw(canvas)
    line_text(draw, (86, 62), "BASKETGUARD", 15, FOREST, True)
    line_text(draw, (86, 98), "Shopping with a backup plan.", 42, INK, True)
    line_text(draw, (88, 157), "Live stock checks · budget-aware AI baskets · shopper-approved swaps", 19, MUTED)
    labels = ["01  SHOP", "02  PLAN", "03  RESCUE"]
    xs = [110, 730, 1350]
    for screen, label, x in zip(screens, labels, xs):
        line_text(draw, (x + 7, 222), label, 14, FOREST, True)
        thumb = screen.resize((330, 707), Image.Resampling.LANCZOS)
        canvas.paste(thumb, (x, 260))
    line_text(draw, (87, 1000), "Illustrative UI previews · built from the project design", 15, MUTED)
    line_text(draw, (1610, 1000), "ANDROID  ·  KMP  ·  FASTAPI", 13, FOREST, True)
    return canvas


def video_scene(screen: Image.Image, headline: str, subtitle: str, detail: str, t: float) -> Image.Image:
    canvas = Image.new("RGB", (1280, 720), WARM)
    draw = ImageDraw.Draw(canvas)
    draw.rectangle((0, 0, 1280, 10), fill=FOREST)
    phone_h = 650
    phone_w = int(screen.width * phone_h / screen.height)
    zoom = 0.985 + 0.025 * min(t, 1.0)
    phone_w = int(phone_w * zoom)
    phone_h = int(phone_h * zoom)
    phone = screen.resize((phone_w, phone_h), Image.Resampling.LANCZOS)
    canvas.paste(phone, (90, 35 + (650 - phone_h) // 2))
    draw = ImageDraw.Draw(canvas)
    line_text(draw, (505, 116), "BASKETGUARD", 14, FOREST, True)
    line_text(draw, (505, 196), headline, 35, INK, True)
    line_text(draw, (505, 270), subtitle, 21, FOREST, True)
    wrapped = detail.split("\n")
    for index, line in enumerate(wrapped):
        line_text(draw, (505, 335 + index * 36), line, 16, MUTED)
    rounded(draw, (505, 510, 1168, 582), WHITE, 16)
    line_text(draw, (530, 535), "FULL QUANTITY", 12, FOREST, True)
    line_text(draw, (700, 535), "LIVE STOCK", 12, FOREST, True)
    line_text(draw, (850, 535), "BUDGET CHECK", 12, FOREST, True)
    line_text(draw, (505, 650), "VISUAL WALKTHROUGH · PRODUCT PREVIEW", 10, MUTED, True)
    return canvas


def make_video(screens: list[Image.Image]) -> None:
    scenes = [
        ("One basket. Clear choices.", "Build a basket with stock you can trust.", "BasketGuard checks the store catalog\nand keeps the shopper in control."),
        ("A meal idea into a basket.", "AI plans with the live catalog and a hard cap.", "The server checks product IDs,\nquantities, availability and total."),
        ("A stockout with a way forward.", "Compare same-use swaps before ordering.", "Choose a replacement, keep available\nunits, or remove the item."),
    ]
    output = OUT / "basketguard-walkthrough.mp4"
    with imageio.get_writer(output, fps=18, codec="libx264", quality=8, macro_block_size=16) as writer:
        for screen, scene in zip(screens, scenes):
            for frame_index in range(72):
                frame = video_scene(screen, *scene, frame_index / 71)
                writer.append_data(np.asarray(frame))


def make_readme_gif(screens: list[Image.Image]) -> None:
    scenes = [
        ("One basket. Clear choices.", "Build a basket with stock you can trust.", "BasketGuard checks the store catalog\nand keeps the shopper in control."),
        ("A meal idea into a basket.", "AI plans with the live catalog and a hard cap.", "The server checks product IDs,\nquantities, availability and total."),
        ("A stockout with a way forward.", "Compare same-use swaps before ordering.", "Choose a replacement, keep available\nunits, or remove the item."),
    ]
    frames = []
    for screen, scene in zip(screens, scenes):
        for frame_index in range(0, 72, 3):
            frame = video_scene(screen, *scene, frame_index / 71)
            frame.thumbnail((960, 540), Image.Resampling.LANCZOS)
            frames.append(frame.convert("P", palette=Image.Palette.ADAPTIVE, colors=128))
    frames[0].save(
        OUT / "basketguard-walkthrough.gif",
        save_all=True,
        append_images=frames[1:],
        duration=167,
        loop=0,
        optimize=True,
        disposal=2,
    )


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    screens = save_phone_screens()
    board = preview_board(screens)
    board.save(OUT / "basketguard-preview.png", optimize=True)
    make_video(screens)
    make_readme_gif(screens)
    print(f"Rendered preview images and walkthrough to {OUT}")


if __name__ == "__main__":
    main()
