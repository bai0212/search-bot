"""按 Java 里三个 Screen 的画法，把贴图 + 代码绘制合成成一张效果图，用来看对齐和观感。"""
from PIL import Image, ImageDraw, ImageFont

from gen_gui_textures import (BG, BGL, BGD, WELL, ACC, ACCD, ACCM, HI, LO, BORDER,
                              canvas, well, brackets)

F = lambda s: ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', s)
FONT = F(9)
LABEL = (198, 211, 223, 255)
LABEL_DIM = (138, 153, 171, 255)
DANGER = (255, 107, 107, 255)
OK = (107, 224, 138, 255)
YELLOW = (255, 210, 74, 255)


def slot(d, x, y, border=ACCM, fill=WELL):
    d.rectangle([x, y, x + 17, y + 17], fill=border)
    d.rectangle([x + 1, y + 1, x + 16, y + 16], fill=fill)
    d.line([x + 1, y + 1, x + 16, y + 1], fill=LO)
    d.line([x + 1, y + 1, x + 1, y + 16], fill=LO)


def item(d, x, y, color):
    d.ellipse([x + 3, y + 3, x + 14, y + 14], fill=color)


def button(d, x, y, w, h, label, color, hover=False, font=FONT):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=BGL if hover else BGD)
    d.line([x, y, x + w - 1, y], fill=HI if hover else ACCD)
    d.line([x, y + h - 1, x + w - 1, y + h - 1], fill=LO)
    d.line([x, y, x, y + h - 1], fill=HI if hover else ACCD)
    d.line([x + w - 1, y, x + w - 1, y + h - 1], fill=LO)
    tw = d.textlength(label, font=font)
    d.text((x + (w - tw) / 2, y + (h - 9) / 2), label, font=font, fill=color)


def text(d, x, y, s, color):
    d.text((x, y), s, font=FONT, fill=color)


# ---------------- 装配台 ----------------
def assembler():
    img = Image.open('robot_assembler.png').convert('RGBA')
    d = ImageDraw.Draw(img)
    for i, (x, y) in enumerate([(8, 20), (109, 20), (129, 20), (230, 20)]):
        if i in (0, 3):
            slot(d, x, y, ACCD)
        elif i in (1, 2):
            slot(d, x, y, ACCM, BGD)
        else:
            slot(d, x, y)
    item(d, 8, 20, (150, 160, 175, 255))
    item(d, 129, 20, ACC)
    for r in range(2):
        for c in range(5):
            slot(d, 79 + c * 18, 40 + r * 18)
    for c in range(6):
        slot(d, 69 + c * 18, 76)
    item(d, 79, 40, (120, 200, 140, 255))
    item(d, 119, 58, (200, 170, 90, 255))
    for r in range(3):
        for c in range(9):
            slot(d, 47 + c * 18, 102 + r * 18)
    for c in range(9):
        slot(d, 47 + c * 18, 160)
    button(d, 28, 20, 36, 18, '拆解', LABEL)
    button(d, 190, 20, 36, 18, '组装', ACC, hover=True)
    text(d, 8, 6, '输入', LABEL_DIM)
    text(d, 102, 6, '核心组件', LABEL_DIM)
    text(d, 222, 6, '输出', LABEL_DIM)
    text(d, 66, 25, '功能组件', LABEL_DIM)
    text(d, 8, 78, '附件', LABEL_DIM)
    text(d, 8, 90, '物品栏', LABEL_DIM)
    text(d, 10, 211, '机器人装配台', ACC)
    return img, (0, 0, 256, 230)


# ---------------- 机器人背包（3 行储物） ----------------
def inventory(rows=3):
    W, H = 256, max(116 + rows * 18, 181)
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], fill=BG)
    tex = Image.open('robot_inventory.png').convert('RGBA')
    img.alpha_composite(tex, (0, 0))
    d.line([0, 0, W - 1, 0], fill=BORDER)
    d.line([0, H - 1, W - 1, H - 1], fill=BORDER)
    d.line([0, 0, 0, H - 1], fill=BORDER)
    d.line([W - 1, 0, W - 1, H - 1], fill=BORDER)
    d.line([1, 1, W - 2, 1], fill=HI)
    d.line([1, 1, 1, H - 2], fill=HI)
    sep = 18 + rows * 18 + 1
    d.line([6, sep, 172, sep], fill=ACCD)
    d.line([6, sep, 26, sep], fill=ACC)
    for r in range(rows):
        for c in range(9):
            slot(d, 8 + c * 18, 18 + r * 18)
    for r in range(3):
        for c in range(9):
            slot(d, 8 + c * 18, 18 + rows * 18 + 14 + r * 18)
    for c in range(9):
        slot(d, 8 + c * 18, 18 + rows * 18 + 14 + 58)
    for r in range(6):
        for c in range(2):
            slot(d, 178 + c * 18, 66 + r * 18, ACCD, BGD)
    item(d, 8, 18, (170, 120, 90, 255))
    item(d, 26, 18, (90, 170, 120, 255))
    item(d, 178, 66, (200, 80, 80, 255))
    text(d, 8, 6, '搜索机器人', ACC)
    text(d, 8, 18 + rows * 18 + 4, '物品栏', LABEL_DIM)
    text(d, 178, 4, '标记物品', ACC)
    button(d, 178, 18, 76, 18, '黑名单', DANGER)
    button(d, 178, 40, 76, 18, '忽略后缀:开', OK, hover=True)
    # 电量条
    bx, by, bw, bh = 120, 18 + rows * 18 + 4, 48, 8
    s = '1200/3000'
    d.text((bx - 4 - d.textlength(s, font=FONT), by), s, font=FONT, fill=LABEL)
    d.rectangle([bx, by, bx + bw - 1, by + bh - 1], fill=WELL)
    d.line([bx, by, bx + bw - 1, by], fill=LO)
    d.line([bx, by + bh - 1, bx + bw - 1, by + bh - 1], fill=HI)
    ratio = 0.4
    inner = bw - 2
    d.rectangle([bx + 1, by + 2, bx + int(inner * ratio), by + bh - 3], fill=YELLOW)
    for tick in range(1, 8):
        tx = bx + 1 + inner * tick // 8
        d.line([tx, by + 2, tx, by + bh - 3], fill=WELL)
    return img, (0, 0, W, H)


# ---------------- 操控台 ----------------
def controller():
    img = Image.open('robot_controller.png').convert('RGBA')
    d = ImageDraw.Draw(img)
    ROW_TOP, ROW_H = 24, 24
    names = ['一号矿区', '主基地', '废弃矿井']
    modes = ['搜索', '巡逻', '收集']
    robots = [[('采矿机 A', True), ('采矿机 B', False)], [], [('运输机 C', False)]]
    current = 0
    pending = 2
    for i in range(len(names)):
        y = ROW_TOP + i * ROW_H
        if i == current:
            d.rectangle([3, y + 1, 236, y + ROW_H - 2], fill=(96, 196, 238, 48))
            d.line([3, y + 1, 3, y + ROW_H - 2], fill=ACC)
        if i == pending:
            for px in range(2, 236, 4):
                d.line([px + 2, y, px + 3, y], fill=DANGER)
                d.line([px + 2, y + ROW_H - 2, px + 3, y + ROW_H - 2], fill=DANGER)
        text(d, 8, y + 8, names[i], ACC if i == current else LABEL)
        my = y + (ROW_H - 16) // 2
        hov = i == 1
        d.rectangle([62, my, 101, my + 15], fill=BGL if hov else BGD)
        d.line([62, my, 101, my], fill=ACC if hov else ACCD)
        text(d, 62 + (40 - d.textlength(modes[i], font=FONT)) / 2, my + 3, modes[i],
             ACC if hov else LABEL)
        for ri, (rn, drag) in enumerate(robots[i]):
            bx = 108 + ri * 52
            by = y + (ROW_H - 18) // 2
            d.rectangle([bx, by, bx + 47, by + 17], fill=ACCD if drag else BGD)
            d.line([bx, by, bx + 47, by], fill=ACC if drag else HI)
            text(d, bx + 2, by + 4, rn, LABEL)
        dy = y + (ROW_H - 12) // 2
        text(d, 224, dy + 1, '↺' if i == pending else '✕', OK if i == pending else DANGER)
    ay = ROW_TOP + len(names) * ROW_H + 4
    ax = 120 - 8
    d.rectangle([ax, ay, ax + 15, ay + 15], fill=BGD)
    d.line([ax, ay, ax + 15, ay], fill=ACCD)
    d.line([ax, ay, ax, ay + 15], fill=ACCD)
    text(d, ax + 5, ay + 2, '+', LABEL)
    text(d, 8, 6, '机器人操控台', LABEL)
    return img, (0, 0, 240, 244)


if __name__ == '__main__':
    pad, gap = 12, 12
    parts = [assembler(), inventory(), controller()]
    W = sum(p[1][2] for p in parts) + pad * 2 + gap * (len(parts) - 1)
    H = max(p[1][3] for p in parts) + pad * 2
    sheet = Image.new('RGBA', (W, H), (14, 18, 24, 255))
    x = pad
    for img, (_, _, w, h) in parts:
        sheet.alpha_composite(img.crop((0, 0, w, h)), (x, pad))
        x += w + gap
    sheet.resize((W * 2, H * 2), Image.NEAREST).save('gui_mock.png')
    print('gui_mock.png', sheet.size)
