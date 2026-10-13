import os
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ASSETS_DIR = r"D:\DocumentReaderPDFViewer\play_store_assets"
APP_DIR = r"D:\DocumentReaderPDFViewer"
ICON_PATH = os.path.join(APP_DIR, r"app\src\main\res\mipmap-xxxhdpi\ic_launcher.webp")

# Available fonts
FONT_BOLD = r"C:\Windows\Fonts\segoeuib.ttf"
FONT_REGULAR = r"C:\Windows\Fonts\segoeui.ttf"
FONT_SEMIBOLD = r"C:\Windows\Fonts\segoeuisl.ttf"

def get_font(path, size):
    try:
        return ImageFont.truetype(path, size)
    except:
        return ImageFont.load_default()

def draw_rounded_mask(size, radius):
    mask = Image.new('L', size, 0)
    draw = ImageDraw.Draw(mask)
    draw.rounded_rectangle([(0, 0), size], radius=radius, fill=255)
    return mask

def create_gradient_bg(width, height, top_color, mid_color, bot_color):
    img = Image.new('RGB', (width, height))
    draw = ImageDraw.Draw(img)
    for y in range(height):
        t = y / height
        if t < 0.4:
            st = t / 0.4
            r = int(top_color[0] + (mid_color[0] - top_color[0]) * st)
            g = int(top_color[1] + (mid_color[1] - top_color[1]) * st)
            b = int(top_color[2] + (mid_color[2] - top_color[2]) * st)
        else:
            st = (t - 0.4) / 0.6
            r = int(mid_color[0] + (bot_color[0] - mid_color[0]) * st)
            g = int(mid_color[1] + (bot_color[1] - mid_color[1]) * st)
            b = int(mid_color[2] + (bot_color[2] - mid_color[2]) * st)
        draw.line([(0, y), (width, y)], fill=(r, g, b))
    return img

def clean_status_bar(w, h=70):
    # Create clean status bar (9:41, wifi, battery)
    sb = Image.new('RGBA', (w, h), (255, 255, 255, 255))
    draw = ImageDraw.Draw(sb)
    font = get_font(FONT_BOLD, 26)
    draw.text((45, 20), "9:41", fill=(30, 41, 59), font=font)
    
    # Battery & wifi icons on right
    # Battery body
    bx = w - 85
    by = 24
    draw.rounded_rectangle([(bx, by), (bx + 40, by + 22)], radius=5, outline=(71, 85, 105), width=2)
    draw.rectangle([(bx + 3, by + 3), (bx + 35, by + 19)], fill=(34, 197, 94))
    draw.rectangle([(bx + 41, by + 7), (bx + 44, by + 15)], fill=(71, 85, 105))
    
    # Wifi dots / waves indicator
    wx = w - 125
    wy = 24
    draw.arc([(wx - 10, wy - 4), (wx + 20, wy + 26)], start=210, end=330, fill=(71, 85, 105), width=3)
    draw.arc([(wx - 5, wy + 2), (wx + 15, wy + 22)], start=210, end=330, fill=(71, 85, 105), width=3)
    draw.ellipse([(wx + 3, wy + 16), (wx + 7, wy + 20)], fill=(71, 85, 105))
    return sb

# 1. Clean real screenshots
im1 = Image.open(os.path.join(ASSETS_DIR, 'ss (1).jpeg'))
im2 = Image.open(os.path.join(ASSETS_DIR, 'ss (2).jpeg'))
im3 = Image.open(os.path.join(ASSETS_DIR, 'ss (3).jpeg'))

# clean1:
top1 = im1.crop((0, 0, 720, 925))
row_uac = im3.crop((0, 640, 720, 780))
row_master = im3.crop((0, 780, 720, 920))
row_budget = im3.crop((0, 1120, 720, 1260))
bottom_nav = im1.crop((0, 1345, 720, 1515)) # crop without Android nav keys

clean1 = Image.new('RGB', (720, 1515), (255, 255, 255))
clean1.paste(top1, (0, 0))
clean1.paste(row_uac, (0, 925))
clean1.paste(row_master, (0, 1065))
clean1.paste(row_budget, (0, 1205))
# Wipe any sliver before bottom nav
ImageDraw.Draw(clean1).rectangle([(0, 1330), (720, 1350)], fill=(255, 255, 255))
clean1.paste(bottom_nav, (0, 1345))
sb1 = clean_status_bar(720, 75)
clean1.paste(sb1, (0, 0), sb1)

# clean2 (tools screen):
# Already clean, just replace status bar and crop out nav keys
clean2 = im2.crop((0, 0, 720, 1515)).copy()
sb2 = clean_status_bar(720, 75)
clean2.paste(sb2, (0, 0), sb2)

# clean3 (filtered list):
top3 = im3.crop((0, 0, 720, 925))
row_budget_3 = im3.crop((0, 1120, 720, 1260))
row_aqeel = im1.crop((0, 360, 720, 500))
row_mynew = im1.crop((0, 500, 720, 640))
bottom_nav_3 = im3.crop((0, 1345, 720, 1515))

clean3 = Image.new('RGB', (720, 1515), (255, 255, 255))
clean3.paste(top3, (0, 0))
clean3.paste(row_budget_3, (0, 925))
clean3.paste(row_aqeel, (0, 1065))
clean3.paste(row_mynew, (0, 1205))
clean3.paste(bottom_nav_3, (0, 1345))
sb3 = clean_status_bar(720, 75)
clean3.paste(sb3, (0, 0), sb3)

# Save clean standalone screens
clean1.save(os.path.join(ASSETS_DIR, 'clean_screen_1.jpg'), quality=95)
clean2.save(os.path.join(ASSETS_DIR, 'clean_screen_2.jpg'), quality=95)
clean3.save(os.path.join(ASSETS_DIR, 'clean_screen_3.jpg'), quality=95)
print("Clean standalone screens created!")

# Function to build framed marketing screenshot (1080 x 2400)
def build_screenshot_banner(clean_screen, tag_text, title_text, subtitle_text, out_name):
    W, H = 1080, 2400
    # Gradient matching app's modern purple-indigo theme
    canvas = create_gradient_bg(W, H, (26, 20, 64), (49, 46, 129), (15, 23, 42))
    draw = ImageDraw.Draw(canvas)
    
    # 1. Category Tag Pill
    tag_font = get_font(FONT_BOLD, 28)
    tag_bbox = draw.textbbox((0, 0), tag_text, font=tag_font)
    tag_w = tag_bbox[2] - tag_bbox[0] + 50
    tag_h = tag_bbox[3] - tag_bbox[1] + 24
    tag_x = (W - tag_w) // 2
    tag_y = 120
    draw.rounded_rectangle([(tag_x, tag_y), (tag_x + tag_w, tag_y + tag_h)], radius=tag_h//2, fill=(99, 102, 241, 60), outline=(129, 140, 248), width=2)
    draw.text((tag_x + 25, tag_y + 10), tag_text, fill=(224, 231, 255), font=tag_font)
    
    # 2. Main Title
    title_font = get_font(FONT_BOLD, 68)
    t_bbox = draw.textbbox((0, 0), title_text, font=title_font)
    t_w = t_bbox[2] - t_bbox[0]
    draw.text(((W - t_w) // 2, 200), title_text, fill=(255, 255, 255), font=title_font)
    
    # 3. Subtitle
    sub_font = get_font(FONT_REGULAR, 36)
    s_bbox = draw.textbbox((0, 0), subtitle_text, font=sub_font)
    s_w = s_bbox[2] - s_bbox[0]
    draw.text(((W - s_w) // 2, 290), subtitle_text, fill=(199, 210, 254), font=sub_font)
    
    # 4. Device Mockup Frame
    target_w = 880
    aspect = clean_screen.height / clean_screen.width
    target_h = int(target_w * aspect) # ~ 1851 px
    resized_screen = clean_screen.resize((target_w, target_h), Image.Resampling.LANCZOS)
    
    # Device frame padding
    bezel = 14
    frame_w = target_w + bezel * 2
    frame_h = min(target_h + bezel * 2, H - 380) # fit within canvas
    frame_x = (W - frame_w) // 2
    frame_y = 380
    
    # Outer drop shadow
    shadow = Image.new('RGBA', (frame_w + 60, frame_h + 60), (0, 0, 0, 0))
    sdraw = ImageDraw.Draw(shadow)
    sdraw.rounded_rectangle([(20, 20), (frame_w + 40, frame_h + 40)], radius=56, fill=(0, 0, 0, 140))
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    canvas.paste(shadow, (frame_x - 30, frame_y - 20), shadow)
    
    # Device Outer Shell
    draw.rounded_rectangle([(frame_x, frame_y), (frame_x + frame_w, frame_y + frame_h + 100)], radius=52, fill=(30, 41, 59), outline=(99, 102, 241), width=4)
    
    # Screen Mask with Rounded Corners
    screen_mask = draw_rounded_mask((target_w, target_h), radius=42)
    screen_crop_h = frame_h - bezel * 2
    screen_mask_crop = screen_mask.crop((0, 0, target_w, screen_crop_h))
    screen_crop = resized_screen.crop((0, 0, target_w, screen_crop_h))
    
    canvas.paste(screen_crop, (frame_x + bezel, frame_y + bezel), screen_mask_crop)
    
    canvas.save(os.path.join(ASSETS_DIR, out_name), quality=96)
    print(f"Generated {out_name} successfully!")

# Build all 3 screenshots with Google Play Policy compliant text
build_screenshot_banner(
    clean1,
    "OFFICE SUITE",
    "All Documents in One Place",
    "Read PDF, Word, Excel & Text Files",
    "screenshot_1_all_docs.jpg"
)

build_screenshot_banner(
    clean2,
    "POWERFUL UTILITIES",
    "Smart PDF & Document Tools",
    "Scan, Merge, Split, Sign & Protect PDFs",
    "screenshot_2_pdf_viewer.jpg"
)

build_screenshot_banner(
    clean3,
    "SMART ORGANIZER",
    "Fast Search & File Manager",
    "Filter by Category, Extension & Storage",
    "screenshot_3_file_manager.jpg"
)

# 2. Build official Google Play Feature Graphic (1024 x 500 px)
def build_feature_graphic():
    W, H = 1024, 500
    # Background: vibrant indigo & royal blue gradient
    canvas = create_gradient_bg(W, H, (26, 20, 64), (30, 58, 138), (15, 23, 42))
    draw = ImageDraw.Draw(canvas)
    
    # Decorative ambient light glowing circles
    glow = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    gdraw = ImageDraw.Draw(glow)
    gdraw.ellipse([(-100, -100), (450, 450)], fill=(99, 102, 241, 40))
    gdraw.ellipse([(600, 50), (1100, 550)], fill=(59, 130, 246, 35))
    glow = glow.filter(ImageFilter.GaussianBlur(50))
    canvas.paste(glow, (0, 0), glow)
    
    # Official App Icon on Left
    if os.path.exists(ICON_PATH):
        icon = Image.open(ICON_PATH).convert('RGBA')
        icon_size = 140
        icon = icon.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        
        # Icon shadow
        icon_shadow = Image.new('RGBA', (icon_size + 40, icon_size + 40), (0, 0, 0, 0))
        is_draw = ImageDraw.Draw(icon_shadow)
        is_draw.rounded_rectangle([(15, 15), (icon_size + 25, icon_size + 25)], radius=36, fill=(0, 0, 0, 160))
        icon_shadow = icon_shadow.filter(ImageFilter.GaussianBlur(12))
        canvas.paste(icon_shadow, (65, 55), icon_shadow)
        
        # Paste app icon
        canvas.paste(icon, (80, 70), icon)
    
    # Official App Name (EXACT MATCH with strings.xml)
    title_font = get_font(FONT_BOLD, 46)
    title_line1 = "Document Reader"
    title_line2 = "PDF Viewer"
    
    tx = 80
    ty = 230
    draw.text((tx, ty), title_line1, fill=(255, 255, 255), font=title_font)
    draw.text((tx, ty + 56), title_line2, fill=(165, 180, 252), font=title_font)
    
    # Subtitle / Feature badge
    sub_font = get_font(FONT_REGULAR, 22)
    badge_text = "All Document Manager & PDF Tools"
    b_bbox = draw.textbbox((0, 0), badge_text, font=sub_font)
    bw = b_bbox[2] - b_bbox[0] + 36
    bh = b_bbox[3] - b_bbox[1] + 18
    by = ty + 130
    draw.rounded_rectangle([(tx, by), (tx + bw, by + bh)], radius=bh//2, fill=(99, 102, 241, 60), outline=(129, 140, 248), width=2)
    draw.text((tx + 18, by + 8), badge_text, fill=(224, 231, 255), font=sub_font)
    
    # Format tags below (PDF, WORD, EXCEL, PPT)
    tags = [("PDF", (239, 68, 68)), ("WORD", (59, 130, 246)), ("EXCEL", (34, 197, 94)), ("PPT", (249, 115, 22))]
    tag_font = get_font(FONT_BOLD, 18)
    cur_x = tx
    for tname, col in tags:
        tw = 68
        th = 32
        draw.rounded_rectangle([(cur_x, by + 50), (cur_x + tw, by + 50 + th)], radius=8, fill=col)
        tb = draw.textbbox((0, 0), tname, font=tag_font)
        draw.text((cur_x + (tw - (tb[2]-tb[0]))//2, by + 50 + 6), tname, fill=(255, 255, 255), font=tag_font)
        cur_x += tw + 14

    # Phone mockup on right showing REAL clean app screen (clean2 - Tools screen)
    phone_w = 270
    phone_aspect = clean2.height / clean2.width
    phone_h = int(phone_w * phone_aspect) # ~ 568 px
    phone_screen = clean2.resize((phone_w, phone_h), Image.Resampling.LANCZOS)
    
    # Second phone screen peeking behind (clean1 - All docs)
    back_w = 240
    back_h = int(back_w * phone_aspect)
    back_screen = clean1.resize((back_w, back_h), Image.Resampling.LANCZOS)
    
    # Draw back phone
    bx = 630
    by_pos = 90
    back_mask = draw_rounded_mask((back_w, 420), radius=28)
    back_crop = back_screen.crop((0, 0, back_w, 420))
    # shadow
    bshadow = Image.new('RGBA', (back_w + 30, 450), (0, 0, 0, 0))
    ImageDraw.Draw(bshadow).rounded_rectangle([(10, 10), (back_w + 20, 430)], radius=32, fill=(0, 0, 0, 150))
    bshadow = bshadow.filter(ImageFilter.GaussianBlur(10))
    canvas.paste(bshadow, (bx - 10, by_pos - 5), bshadow)
    # shell
    draw.rounded_rectangle([(bx - 4, by_pos - 4), (bx + back_w + 4, by_pos + 424)], radius=32, fill=(30, 41, 59), outline=(71, 85, 105), width=2)
    canvas.paste(back_crop, (bx, by_pos), back_mask)
    
    # Draw front phone (Tools screen)
    fx = 725
    fy_pos = 45
    front_mask = draw_rounded_mask((phone_w, 460), radius=32)
    front_crop = phone_screen.crop((0, 0, phone_w, 460))
    # front shadow
    fshadow = Image.new('RGBA', (phone_w + 40, 490), (0, 0, 0, 0))
    ImageDraw.Draw(fshadow).rounded_rectangle([(15, 15), (phone_w + 25, 475)], radius=36, fill=(0, 0, 0, 180))
    fshadow = fshadow.filter(ImageFilter.GaussianBlur(14))
    canvas.paste(fshadow, (fx - 15, fy_pos - 10), fshadow)
    # shell
    draw.rounded_rectangle([(fx - 5, fy_pos - 5), (fx + phone_w + 5, fy_pos + 465)], radius=36, fill=(15, 23, 42), outline=(129, 140, 248), width=3)
    canvas.paste(front_crop, (fx, fy_pos), front_mask)

    out_path = os.path.join(ASSETS_DIR, "feature_graphic.jpg")
    canvas.save(out_path, quality=96)
    print("Feature Graphic generated successfully!")

build_feature_graphic()
