"""原版箱子风效果图：严格按三个 Screen 的 Java 画法合成（贴图 + 代码绘制的槽位/按钮/文字）。
坐标全部照抄 src 里的常量，改 GUI 时先在这里看效果，别在 Minecraft 里试。
"""
from PIL import Image, ImageDraw, ImageFont

from gen_gui_textures_vanilla import (PANEL, PANEL_LT, LINE, LINE_DK, SLOT, SLOT_DK,
                                      SLOT_LT, OUTLINE, canvas, plate)

F = lambda s: ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', s)
FONT = F(9)
TEXT = (64, 64, 64, 255)          # 原版标题/标签色 #404040
TEXT_DIM = (95, 95, 95, 255)
ENERGY_OK = (32, 112, 48, 255)
ENERGY_MID = (150, 130, 20, 255)
ENERGY_LOW = (160, 32, 32, 255)


def slot(d, x, y):
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=SLOT)
    d.line([x + 1, y + 1, x + 16, y + 1], fill=SLOT_DK)
    d.line([x + 1, y + 1, x + 1, y + 16], fill=SLOT_DK)
    d.line([x + 1, y + 16, x + 16, y + 16], fill=SLOT_LT)
    d.line([x + 16, y + 1, x + 16, y + 16], fill=SLOT_LT)


def button(d, x, y, w, h, label, hover=False):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=PANEL_LT if hover else PANEL)
    d.rectangle([x, y, x + w - 1, y + h - 1], outline=OUTLINE)
    d.line([x + 1, y + 1, x + w - 2, y + 1], fill=SLOT_LT)
    d.line([x + 1, y + 1, x + 1, y + h - 2], fill=SLOT_LT)
    d.line([x + 1, y + h - 2, x + w - 2, y + h - 2], fill=LINE_DK)
    d.line([x + w - 2, y + 1, x + w - 2, y + h - 2], fill=LINE_DK)
    tw = d.textlength(label, font=FONT)
    d.text((x + (w - tw) / 2, y + (h - 9) / 2), label, font=FONT, fill=TEXT)


def text(d, x, y, s, color=TEXT):
    d.text((x, y), s, font=FONT, fill=color)


def item(d, x, y, color):
    d.rectangle([x + 3, y + 4, x + 14, y + 15], fill=color)
    d.rectangle([x + 3, y + 4, x + 14, y + 7], fill=tuple(c + 30 for c in color[:3]) + (255,))


def panel_frame(d, w, h):
    d.rectangle([0, 0, w - 1, h - 1], outline=OUTLINE)


# ---------------- 装配台（当前坐标） ----------------
ASM_SLOTS = [(8, 20), (109, 20), (129, 20), (230, 20)]
ASM_SLOTS += [(x, 40 + r * 18) for r in range(2) for x in (79, 99, 119, 139, 159)]
ASM_SLOTS += [(x, 76) for x in (69, 89, 109, 129, 149, 169)]
ASM_SLOTS += [(47 + c * 18, y) for y in (102, 120, 138) for c in range(9)]
ASM_SLOTS += [(47 + c * 18, 160) for c in range(9)]


def assembler():
    img = Image.open('robot_assembler.png').convert('RGBA')
    d = ImageDraw.Draw(img)
    for x, y in ASM_SLOTS:
        slot(d, x, y)
    item(d, 8, 20, (150, 160, 175, 255))
    item(d, 129, 20, (96, 196, 238, 255))
    item(d, 79, 40, (120, 200, 140, 255))
    button(d, 28, 20, 36, 18, '拆解')
    button(d, 190, 20, 36, 18, '组装', hover=True)
    text(d, 8, 6, '输入', TEXT_DIM)
    text(d, 102, 6, '核心组件', TEXT_DIM)
    text(d, 222, 6, '输出', TEXT_DIM)
    text(d, 66, 25, '功能组件', TEXT_DIM)
    text(d, 8, 78, '附件', TEXT_DIM)
    text(d, 8, 90, '物品栏', TEXT_DIM)
    text(d, 10, 211, '机器人装配台')
    return img


# ---------------- 机器人背包（rows 可变） ----------------
BTN_X, BTN_W, BTN_H = 178, 76, 18
BAG_TOP, ROW_H, PANEL_X = 18, 18, 174


def inventory(rows=3, energy=1200, max_energy=3000):
    """rows 会跟着躯干等级变；这里按 Screen 的算法算高度和标签行，用来验对齐。"""
    W = 256
    H = max(116 + rows * ROW_H, 181)
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    # 对应 Java 里的 graphics.fill(..., TechGuiTheme.BG)：贴图只烘固定部件，整板先铺底色
    d.rectangle([0, 0, W - 1, H - 1], fill=PANEL)
    tex = Image.open('robot_inventory.png').convert('RGBA')
    img.alpha_composite(tex, (0, 0))
    panel_frame(d, W, H)

    sep = BAG_TOP + rows * ROW_H + 1
    d.line([6, sep, PANEL_X - 2, sep], fill=LINE)
    d.line([6, sep + 1, PANEL_X - 2, sep + 1], fill=PANEL_LT)

    player_top = BAG_TOP + rows * ROW_H + 14
    for r in range(rows):
        for c in range(9):
            slot(d, 8 + c * 18, BAG_TOP + r * ROW_H)
    for r in range(3):
        for c in range(9):
            slot(d, 8 + c * 18, player_top + r * 18)
    for c in range(9):
        slot(d, 8 + c * 18, player_top + 58)
    for r in range(6):
        for c in range(2):
            slot(d, 178 + c * 18, 66 + r * 18)
    item(d, 8, 18, (170, 120, 90, 255))
    item(d, 26, 18, (90, 170, 120, 255))
    item(d, 178, 66, (200, 80, 80, 255))

    # 注意：Java 里 drawEnergyBar 是在 renderLabels 内调用的，而 vanilla 已经把画布平移过
    # (leftPos, topPos)，代码却又加了一次 leftPos/topPos —— 这就是电量条压到格子的原因。
    # 这里按「修正后」的坐标画（相对面板左上角），修正要求见 design/gui-code-requirements.md。
    ly = BAG_TOP + rows * ROW_H + 4
    text(d, 8, ly, '物品栏', TEXT_DIM)
    text(d, 8, 6, '搜索机器人')
    text(d, 178, 4, '标记物品')
    button(d, BTN_X, 18, BTN_W, BTN_H, '黑名单')
    button(d, BTN_X, 40, BTN_W, BTN_H, '忽略后缀:开', hover=True)

    bx, by, bw, bh = 120, ly, 48, 8
    s = '%d/%d' % (energy, max_energy)
    d.text((bx - 4 - d.textlength(s, font=FONT), by), s, font=FONT, fill=TEXT)
    d.rectangle([bx, by, bx + bw - 1, by + bh - 1], fill=SLOT)
    d.rectangle([bx, by, bx + bw - 1, by + bh - 1], outline=OUTLINE)
    ratio = min(1.0, energy / max(1, max_energy))
    col = ENERGY_LOW if ratio < .2 else (ENERGY_MID if ratio < .5 else ENERGY_OK)
    d.rectangle([bx + 1, by + 1, bx + 1 + int((bw - 2) * ratio), by + bh - 2], fill=col)
    return img


# ---------------- 操控台 ----------------
ROW_TOP, ROW_H_C = 24, 24


def controller():
    img = Image.open('robot_controller.png').convert('RGBA')
    d = ImageDraw.Draw(img)
    names = ['一号矿区', '主基地', '废弃矿井']
    modes = ['搜索', '巡逻', '收集']
    robots = [[('采矿机A', False), ('采矿机B', False)], [], [('运输机C', False)]]
    current, pending = 0, 2
    for i in range(len(names)):
        y = ROW_TOP + i * ROW_H_C
        if i == current:
            d.rectangle([2, y + 1, 237, y + ROW_H_C - 2], fill=(255, 255, 255, 90))
            d.rectangle([2, y + 1, 237, y + ROW_H_C - 2], outline=(64, 64, 64, 255))
        text(d, 8, y + 8, names[i])
        my = y + (ROW_H_C - 16) // 2
        button(d, 62, my, 40, 16, modes[i], hover=(i == 1))
        for ri, (rn, _) in enumerate(robots[i]):
            bx = 108 + ri * 52
            by = y + (ROW_H_C - 18) // 2
            d.rectangle([bx, by, bx + 47, by + 17], fill=PANEL_LT)
            d.rectangle([bx, by, bx + 47, by + 17], outline=OUTLINE)
            text(d, bx + 2, by + 4, rn)
        dy = y + (ROW_H_C - 12) // 2
        text(d, 224, dy + 1, '↺' if i == pending else '✕',
             ENERGY_OK if i == pending else ENERGY_LOW)
    ay = ROW_TOP + len(names) * ROW_H_C + 4
    button(d, 112, ay, 16, 16, '+')
    text(d, 8, 6, '机器人操控台')
    return img


if __name__ == '__main__':
    pad, gap = 14, 14
    parts = [('装配台', assembler()), ('背包 3 行', inventory(3)),
             ('背包 6 行', inventory(6)), ('操控台', controller())]
    W = sum(p[1].width for p in parts) + pad * 2 + gap * (len(parts) - 1)
    H = max(p[1].height for p in parts) + pad * 2 + 16
    sheet = Image.new('RGBA', (W, H), (60, 60, 70, 255))
    x = pad
    for name, img in parts:
        sheet.paste(img, (x, pad + 14))
        sheet_draw = ImageDraw.Draw(sheet)
        sheet_draw.text((x, pad), name, font=F(11), fill=(240, 240, 240, 255))
        x += img.width + gap
    sheet = sheet.resize((W * 2, H * 2), Image.NEAREST)
    sheet.save('gui_mock_vanilla.png')
    print('ok', sheet.size)
