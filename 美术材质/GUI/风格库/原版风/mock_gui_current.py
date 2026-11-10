# -*- coding: utf-8 -*-
"""按当前 Java 代码逐笔复刻三个 GUI，用来离线核对布局（尤其是居中）。

坐标全部抄自：
  RobotAssemblerMenu / RobotAssemblerScreen
  RobotInventoryMenu / RobotInventoryScreen
  RobotControllerScreen
配色抄自 src 的 TechGuiTheme.java（原版风）。

    python mock_gui_current.py
"""
import os

from PIL import Image, ImageDraw, ImageFont

HERE = os.path.dirname(os.path.abspath(__file__))
SRC_GUI = os.path.normpath(os.path.join(
    HERE, '..', '..', '..',
    'src/main/resources/assets/search_bot/textures/gui'))

# ==== TechGuiTheme（原版风）====
BORDER = (0, 0, 0)
BEVEL_HI = (255, 255, 255)
BEVEL_LO = (95, 95, 95)
BG = (198, 198, 198)
BG_LIGHT = (224, 224, 224)
SLOT_FILL = (139, 139, 139)
SLOT_DK = (55, 55, 55)
SLOT_LT = (255, 255, 255)
ACCENT = (64, 64, 64)
ACCENT_DIM = (0, 0, 0)
LABEL = (64, 64, 64)
LABEL_DIM = (95, 95, 95)
DANGER = (160, 32, 32)
OK = (32, 112, 48)
BG_CANVAS = (24, 31, 41)

_font = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 8)


def text_w(s):
    return _font.getbbox(s)[2]


def slot(d, x, y):
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=SLOT_FILL)
    d.line([x + 1, y + 1, x + 16, y + 1], fill=SLOT_DK)
    d.line([x + 1, y + 1, x + 1, y + 16], fill=SLOT_DK)
    d.line([x + 1, y + 16, x + 16, y + 16], fill=SLOT_LT)
    d.line([x + 16, y + 1, x + 16, y + 16], fill=SLOT_LT)


def button(d, x, y, w, h, label, hover=False, color=LABEL):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=BG_LIGHT if hover else BG)
    d.rectangle([x, y, x + w - 1, y + h - 1], outline=BORDER)
    d.line([x + 1, y + 1, x + w - 2, y + 1], fill=BEVEL_HI)
    d.line([x + 1, y + 1, x + 1, y + h - 2], fill=BEVEL_HI)
    d.line([x + 1, y + h - 2, x + w - 2, y + h - 2], fill=BEVEL_LO)
    d.line([x + w - 2, y + 1, x + w - 2, y + h - 2], fill=BEVEL_LO)
    d.text((x + (w - text_w(label)) // 2, y + (h - 8) // 2), label,
           font=_font, fill=color)


def label(d, x, y, s, color=LABEL):
    d.text((x, y), s, font=_font, fill=color)


def load(name):
    p = os.path.join(SRC_GUI, name)
    return Image.open(p).convert('RGBA') if os.path.exists(p) else None


def new_panel(w, h, tex):
    """复刻 renderBg：铺底色 -> 贴 texture -> 画外框。"""
    im = Image.new('RGBA', (w, h), BG + (255,))
    if tex is not None:
        im.alpha_composite(tex.crop((0, 0, min(w, tex.width), min(h, tex.height))), (0, 0))
    d = ImageDraw.Draw(im)
    d.line([0, 0, w - 1, 0], fill=BORDER)          # panelFrame
    d.line([0, h - 1, w - 1, h - 1], fill=BORDER)
    d.line([0, 0, 0, h - 1], fill=BORDER)
    d.line([w - 1, 0, w - 1, h - 1], fill=BORDER)
    return im, d


# ==================== 装配台 ====================
FUNC_X = [119, 143, 157, 157, 143, 119, 95, 81, 81, 95]
FUNC_Y = [44, 52, 72, 96, 116, 124, 116, 96, 72, 52]
ATT_X = [169, 181, 147, 91, 57, 69]
ATT_Y = [44, 98, 142, 142, 98, 44]


def assembler():
    im, d = new_panel(256, 256, load('robot_assembler.png'))
    for x, y in ((8, 84), (119, 84), (119, 20), (230, 84)):
        slot(d, x, y)
    for x, y in zip(FUNC_X, FUNC_Y):
        slot(d, x, y)
    for x, y in zip(ATT_X, ATT_Y):
        slot(d, x, y)
    inv_x = (256 - 9 * 18) // 2
    for r in range(3):
        for c in range(9):
            slot(d, inv_x + c * 18, 176 + r * 18)
    for c in range(9):
        slot(d, inv_x + c * 18, 234)
    button(d, 8, 106, 36, 18, '拆解')
    button(d, 212, 106, 36, 18, '组装')
    label(d, 8, 6, '机器人装配台', ACCENT)
    label(d, 8, 60, '输入', LABEL_DIM)
    label(d, 222, 60, '输出', LABEL_DIM)
    label(d, 8, 163, '玩家背包', LABEL_DIM)
    return im


# ==================== 机器人背包 ====================
def inventory(rows, boxes=2):
    w = 256
    h = max(133 + rows * 18, 181)
    im, d = new_panel(w, h, load('robot_inventory.png'))
    # 分页栏
    x = 8
    for t in range(boxes):
        s = '箱子%d' % (t + 1)
        tw = max(text_w(s) + 16, 30)
        active = t == 0
        d.rectangle([x, 16, x + tw - 1, 16 + 17], fill=ACCENT if active else (95, 95, 95))
        d.rectangle([x, 16, x + tw - 1, 16 + 17], outline=ACCENT_DIM)
        d.text((x + (tw - text_w(s)) // 2, 16 + 5), s, font=_font,
               fill=ACCENT if active else LABEL)
        x += tw + 2
    # 储物区分隔带
    sep_y = 36 + rows * 18 + 1
    d.line([6, sep_y, 172, sep_y], fill=ACCENT_DIM)
    d.line([6, sep_y, 26, sep_y], fill=ACCENT)
    # 机器人储物格
    for r in range(rows):
        for c in range(9):
            slot(d, 8 + c * 18, 36 + r * 18)
    # 玩家背包 + 快捷栏
    py = 36 + rows * 18 + 14
    for r in range(3):
        for c in range(9):
            slot(d, 8 + c * 18, py + r * 18)
    for c in range(9):
        slot(d, 8 + c * 18, py + 58)
    # 右侧过滤槽（2 列 6 行）
    for i in range(12):
        slot(d, 178 + (i % 2) * 18, 66 + (i // 2) * 18)
    # 右侧面板
    label(d, 178, 4, '标记物品', ACCENT)
    button(d, 178, 18, 76, 18, '黑名单')
    button(d, 178, 40, 76, 18, '忽略后缀:开')
    # 标签 + 电量条
    label_y = 36 + rows * 18 + 4
    label(d, 8, label_y, '背包', LABEL_DIM)
    e = '1200/3000'
    label(d, 112 - 4 - text_w(e), label_y, e, LABEL)
    d.rectangle([112, label_y, 112 + 51, label_y + 5], fill=SLOT_FILL)
    d.line([112, label_y, 163, label_y], fill=BEVEL_LO)
    d.rectangle([113, label_y + 2, 113 + int(50 * 0.4), label_y + 3], fill=(224, 192, 0))
    label(d, 8, 6, '搜索机器人', LABEL)
    return im


# ==================== 操控台 ====================
def controller(channels=3):
    w, row_top, row_h, bottom = 240, 24, 24, 28
    h = row_top + 8 * row_h + bottom
    im, d = new_panel(w, h, load('robot_controller.png'))
    names = ['一号矿区', '主基地', '废弃矿井']
    modes = ['搜索', '巡逻', '收集']
    robots = [['采矿机A', '采矿机B'], [], ['运输机C']]
    for i in range(8):
        y = row_top + i * row_h
        if i < channels:
            label(d, 8, y + 8, names[i % 3], LABEL)
            button(d, 62, y + 4, 40, 16, modes[i % 3])
            for ri in range(2):
                bx = 108 + ri * 52
                d.rectangle([bx - 1, y + 2, bx + 48, y + 21], outline=BORDER)
                d.rectangle([bx, y + 3, bx + 47, y + 20], fill=BG_LIGHT)
                lst = robots[i % 3]
                if ri < len(lst):
                    label(d, bx + 2, y + 8, lst[ri], LABEL)
            d.rectangle([224, y + 6, 235, y + 17], outline=BORDER)
            label(d, 225, y + 7, '✕', DANGER)
        else:
            d.rectangle([108, y + 2, 156, y + 21], outline=BEVEL_LO)
    d.line([1, row_top + channels * row_h, w - 2, row_top + channels * row_h], fill=BEVEL_LO)
    ax, ay = w // 2 - 8, row_top + channels * row_h + 4
    d.rectangle([ax, ay, ax + 17, ay + 17], outline=BORDER)
    d.rectangle([ax + 1, ay + 1, ax + 16, ay + 16], fill=BG_LIGHT)
    label(d, ax + 5, ay + 4, '+', LABEL)
    label(d, 8, 6, '机器人操控台', LABEL)
    return im


def montage(items, cols, cell_pad=12, scale=2):
    tiles = [im.resize((im.width * scale, im.height * scale), Image.NEAREST) for _, im in items]
    rows = (len(tiles) + cols - 1) // cols
    cw = max(t.width for t in tiles) + cell_pad
    chh = max(t.height for t in tiles) + 30
    out = Image.new('RGBA', (cols * cw + cell_pad, rows * chh + cell_pad), BG_CANVAS + (255,))
    d = ImageDraw.Draw(out)
    for i, ((name, _), t) in enumerate(zip(items, tiles)):
        cx, cy = cell_pad + (i % cols) * cw, cell_pad + (i // cols) * chh
        out.alpha_composite(t, (cx, cy))
        d.text((cx, cy + t.height + 6), name, font=_font, fill=(198, 211, 223))
    return out


if __name__ == '__main__':
    shots = [('装配台 256x256', assembler()),
             ('背包 3行', inventory(3)),
             ('背包 6行', inventory(6)),
             ('操控台', controller())]
    montage(shots, 2).save(os.path.join(HERE, 'gui_mock_current.png'))
    for name, im in shots:
        im.save(os.path.join(HERE, 'mock_%s.png' % name.split(' ')[0]))
    print('ok ->', os.path.join(HERE, 'gui_mock_current.png'))
