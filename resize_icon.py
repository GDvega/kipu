import os
from PIL import Image

src_path = "/home/gerson/Descargas/ChatGPT Image 29 jun 2026, 23_21_13.png"
if not os.path.exists(src_path):
    print(f"Error: No se encontro el archivo en {src_path}")
    exit(1)

img = Image.open(src_path).convert("RGBA")

# Mipmap sizes for legacy icons
sizes = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192
}

base_res = "app/src/main/res"

for density, size in sizes.items():
    folder = os.path.join(base_res, f"mipmap-{density}")
    os.makedirs(folder, exist_ok=True)
    
    # Resize legacy icon
    resized = img.resize((size, size), Image.Resampling.LANCZOS)
    
    # Save as webp for normal and round
    resized.save(os.path.join(folder, "ic_launcher.webp"), "WEBP", quality=100)
    resized.save(os.path.join(folder, "ic_launcher_round.webp"), "WEBP", quality=100)
    print(f"Generado {density} - {size}x{size}")

# For adaptive icons (anydpi-v26), foreground size is 108dp. 108dp * 4 (xxxhdpi) = 432px
# We will create a foreground image 432x432.
adaptive_foreground = img.resize((432, 432), Image.Resampling.LANCZOS)
# Add some padding so the circle crop doesn't cut the Kipu text
padded = Image.new("RGBA", (512, 512), (255, 255, 255, 255))
padded.paste(adaptive_foreground, (40, 40), adaptive_foreground)
padded_resized = padded.resize((432, 432), Image.Resampling.LANCZOS)

drawable_folder = os.path.join(base_res, "drawable")
os.makedirs(drawable_folder, exist_ok=True)
padded_resized.save(os.path.join(drawable_folder, "ic_launcher_foreground.webp"), "WEBP", quality=100)

# Replace the XMLs that point to vector with image reference
xml_foreground_path = os.path.join(drawable_folder, "ic_launcher_foreground.xml")
if os.path.exists(xml_foreground_path):
    os.remove(xml_foreground_path)

print("Iconos generados exitosamente.")
