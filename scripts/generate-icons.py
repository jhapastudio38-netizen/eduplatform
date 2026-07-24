#!/usr/bin/env python3
"""Generate Android launcher icons from the DreamKorea logo.

Creates ic_launcher.png and ic_launcher_round.png at all standard densities,
plus an adaptive icon foreground. The logo is placed on a white background
with padding so it looks clean as an app icon.
"""
from PIL import Image, ImageDraw
import os

BASE = "/home/z/my-project/student-app-rust/android-wrapper/app/src/main/res"
LOGO_PATH = f"{BASE}/drawable/dreamkorea_logo.jpg"

# Android launcher icon densities (px)
DENSITIES = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192,
}

# Play Store icon (512x512)
PLAY_STORE_SIZE = 512

def load_logo():
    """Load the logo and convert to RGBA."""
    img = Image.open(LOGO_PATH).convert("RGBA")
    return img

def make_icon(size, rounded=False, padding_ratio=0.12):
    """Create a launcher icon with the logo centered on a white background.

    padding_ratio: how much padding around the logo (0.0-0.3).
    """
    # White background
    icon = Image.new("RGBA", (size, size), (255, 255, 255, 255))

    # Resize logo to fit with padding
    logo_size = int(size * (1 - 2 * padding_ratio))
    logo = load_logo().resize((logo_size, logo_size), Image.LANCZOS)

    # Center the logo
    offset = ((size - logo_size) // 2, (size - logo_size) // 2)
    icon.paste(logo, offset, logo)

    if rounded:
        # Create a rounded mask
        mask = Image.new("L", (size, size), 0)
        draw = ImageDraw.Draw(mask)
        draw.rounded_rectangle([0, 0, size, size], radius=size // 5, fill=255)
        # Apply mask
        result = Image.new("RGBA", (size, size), (0, 0, 0, 0))
        result.paste(icon, (0, 0), mask)
        icon = result

    return icon

def make_foreground(size):
    """Create adaptive icon foreground (logo with padding, transparent bg)."""
    fg = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    # Adaptive icons use ~66% of the area for the foreground (safe zone)
    logo_size = int(size * 0.55)
    logo = load_logo().resize((logo_size, logo_size), Image.LANCZOS)
    offset = ((size - logo_size) // 2, (size - logo_size) // 2)
    fg.paste(logo, offset, logo)
    return fg

def main():
    print("Generating DreamKorea launcher icons...")

    # Generate density-specific icons
    for density, size in DENSITIES.items():
        dirpath = f"{BASE}/{density}"
        os.makedirs(dirpath, exist_ok=True)

        # Square icon
        icon = make_icon(size, rounded=False)
        icon.save(f"{dirpath}/ic_launcher.png")
        print(f"  {density}/ic_launcher.png ({size}x{size})")

        # Round icon
        icon_round = make_icon(size, rounded=True)
        icon_round.save(f"{dirpath}/ic_launcher_round.png")
        print(f"  {density}/ic_launcher_round.png ({size}x{size})")

    # Play Store icon
    play_icon = make_icon(PLAY_STORE_SIZE, rounded=False, padding_ratio=0.08)
    play_path = f"{BASE}/ic_launcher-playstore.png"
    play_icon.save(play_path)
    print(f"  ic_launcher-playstore.png ({PLAY_STORE_SIZE}x{PLAY_STORE_SIZE})")

    # Adaptive icon foreground (108x108 dp at xxxhdpi = 432px)
    # Standard adaptive icon is 108dp, with 72dp safe zone
    fg_sizes = {
        "mipmap-mdpi": 108,
        "mipmap-hdpi": 162,
        "mipmap-xhdpi": 216,
        "mipmap-xxhdpi": 324,
        "mipmap-xxxhdpi": 432,
    }
    for density, size in fg_sizes.items():
        dirpath = f"{BASE}/{density}"
        os.makedirs(dirpath, exist_ok=True)
        fg = make_foreground(size)
        fg.save(f"{dirpath}/ic_launcher_foreground.png")
        print(f"  {density}/ic_launcher_foreground.png ({size}x{size})")

    print("\nAll icons generated successfully!")

if __name__ == "__main__":
    main()
