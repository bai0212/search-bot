# -*- coding: utf-8 -*-
"""原版箱子风 GUI 贴图（对应 0.5.8+ 的实际代码布局）。

三张贴图各自对应的坐标全部抄自 src，改代码布局时这里要同步改：
  robot_assembler.png  <- RobotAssemblerMenu（径向）+ RobotAssemblerScreen
  robot_inventory.png  <- RobotInventoryMenu（分页栏 + 右侧过滤面板）
  robot_controller.png <- RobotControllerScreen（8 行频道）

槽位凹格、按钮本体、面板外框都由 TechGuiTheme 代码画，贴图只烘固定部件。

    python gen_gui_textures_vanilla.py
"""
import math

from PIL import Image, ImageDraw

# ==================== 原版箱子风调色板 ====================
# 数值取自原版 container/*.png 的通用配色（不是自创）：
#   面板 #C6C6C6、槽底 #8B8B8B、凹槽上/左阴影 #373737、凹槽下/右高光 #FFFFFF、
#   分区暗线 #A0A0A0、描边 #000000、原版文字 #404040（由代码画，见 LABEL）。
PANEL    = (198, 198, 198, 255)
PANEL_LT = (224, 224, 224, 255)
PANEL_DK = (182, 182, 182, 255)
LINE     = (160, 160, 160, 255)
LINE_DK  = (122, 122, 122, 255)
SLOT     = (139, 139, 139, 255)
SLOT_DK  = (55, 55, 55, 255)
SLOT_LT  = (255, 255, 255, 255)
OUTLINE  = (0, 0, 0, 255)
LABEL    = (64, 64, 64, 255)
CLEAR    = (0, 0, 0, 0)


def canvas(w=256, h=256):
    return Image.new('RGBA', (w, h), CLEAR)


def panel(d, x, y, w, h, color=PANEL):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def edge(d, x, y, w, h):
    """原版那种 1px 黑描边，没有倒角、没有荧光条。"""
    d.rectangle([x, y, x + w - 1, y + h - 1], outline=OUTLINE)


def divider(d, x0, x1, y):
    d.line([x0, y, x1, y], fill=LINE)
    d.line([x0, y + 1, x1, y + 1], fill=PANEL_LT)


def plate(d, x, y, w, h):
    """按钮/标签的浅凹槽底板：上左暗、下右亮，中间和面板同色。"""
    d.line([x, y, x + w - 1, y], fill=LINE_DK)
    d.line([x, y, x, y + h - 1], fill=LINE_DK)
    d.line([x, y + h - 1, x + w - 1, y + h - 1], fill=SLOT_LT)
    d.line([x + w - 1, y, x + w - 1, y + h - 1], fill=SLOT_LT)


def dotted_circle(d, cx, cy, r, color, period=8, on=4):
    """轨道线：沿圆周按弧长每 period 像素点亮 on 个，形成虚线圈。"""
    circumference = 2 * math.pi * r
    steps = max(360, int(circumference * 2))
    for i in range(steps):
        pos = i * circumference / steps
        if pos % period >= on:
            continue
        a = i * 2 * math.pi / steps
        d.point((int(round(cx + r * math.cos(a))), int(round(cy + r * math.sin(a)))),
                fill=color)


# ==================== 装配台 256x256（径向「原子核 + 轨道」）====================
# RobotAssemblerMenu：躯干(119,84) 为核，核心(119,20)，输入(8,84) / 输出(230,84)，
# 内圈功能组件 r=40（10 格）、外圈附件 r=64（6 格），玩家背包 x47 / y176,194,212 / 快捷栏 y234。
# RobotAssemblerScreen：拆解(8,106) 组装(212,106)，均 36x18。
CX, CY = 128, 93
TORSO = (119, 84)
RING1_R, RING2_R = 40, 64
ROBOT_IN, ROBOT_OUT = (8, 84), (230, 84)
DISMANTLE_BTN, ASSEMBLE_BTN = (8, 106), (212, 106)
BTN_W, BTN_H = 36, 18
PLAYER_X = (256 - 9 * 18) // 2   # 47
PLAYER_ROWS, HOTBAR_ROW = (176, 194, 212), 234


def gen_assembler():
    img = canvas()
    d = ImageDraw.Draw(img)
    panel(d, 0, 0, 256, 256)
    edge(d, 0, 0, 256, 256)

    # 两条轨道 + 中间的浅弧
    dotted_circle(d, CX, CY, RING1_R, LINE)
    dotted_circle(d, CX, CY, RING2_R, LINE)
    dotted_circle(d, CX, CY, (RING1_R + RING2_R) // 2, PANEL_LT)

    # 原子核（躯干槽）底板
    plate(d, TORSO[0] - 4, TORSO[1] - 4, 26, 26)
    edge(d, TORSO[0] - 4, TORSO[1] - 4, 26, 26)

    # 输入 / 输出端口底板
    for x, y in (ROBOT_IN, ROBOT_OUT):
        plate(d, x - 2, y - 2, 22, 22)
    # 拆解 / 组装按钮底板
    for x, y in (DISMANTLE_BTN, ASSEMBLE_BTN):
        plate(d, x - 2, y - 2, BTN_W + 4, BTN_H + 4)

    # 装配区 / 玩家背包分区线 + 背包外框
    divider(d, 1, 254, 167)
    edge(d, 44, 174, 168, 58)
    edge(d, 44, 232, 168, 22)

    img.save('robot_assembler.png')
    return img


# ==================== 机器人背包 256x256（分页栏 + 右侧过滤面板）====================
# RobotInventoryMenu：bagTop=36，储物格 x8 起 9 列；过滤槽 2 列 x178,196 / y66..156。
# RobotInventoryScreen：分页栏 y16 高 18 从 x8 起；PANEL_X=174；按钮 x178 宽 76，y18 / y40。
PANEL_X = 174
BAG_TOP = 36
TAB_Y, TAB_H = 16, 18
FILTER_X = (178, 196)
FILTER_TOP, FILTER_ROWS = 66, 6
BTN_X, BTN_W, BTN_H = 178, 76, 18


def gen_inventory():
    img = canvas()
    d = ImageDraw.Draw(img)
    H = 256

    # 右侧「标记物品」面板：一整块略深的一栏 + 左边界
    d.rectangle([PANEL_X, 0, 255, H - 1], fill=PANEL_DK)
    d.line([PANEL_X, 0, PANEL_X, H - 1], fill=OUTLINE)
    d.line([255, 0, 255, H - 1], fill=OUTLINE)
    d.line([PANEL_X, 0, 255, 0], fill=OUTLINE)
    # 面板标题条下的分区线
    divider(d, PANEL_X + 1, 254, 15)

    # 两个按钮的底板（按钮本体由代码画在 178,18 与 178,40）
    for y in (16, 38):
        plate(d, 176, y, 80, 22)

    # 过滤槽托盘：正好包住 2 列 6 行（凹格由代码画）
    tray_w = 2 * 18 + 2
    tray_h = FILTER_ROWS * 18 + 2
    edge(d, FILTER_X[0] - 2, FILTER_TOP - 4, tray_w, tray_h)

    # 左侧分页栏底衬线（分页按钮本体由代码画在 y16..33）
    d.line([6, TAB_Y + TAB_H + 1, PANEL_X - 4, TAB_Y + TAB_H + 1], fill=LINE)

    img.save('robot_inventory.png')
    return img


# ==================== 操控台 240x244（8 行频道）====================
# RobotControllerScreen：ROW_TOP=24 ROW_H=24 BOTTOM_H=28；
# 名称 x8 / 模式 x62 w40 h16 / 机器人 x108 起 48x18 间隔4 / 删除 x224 w12。
ROW_TOP, ROW_H, ROWS, BOTTOM_H = 24, 24, 8, 28


def gen_controller():
    img = canvas(240, 244)
    d = ImageDraw.Draw(img)
    W = 240
    H = ROW_TOP + ROWS * ROW_H + BOTTOM_H

    panel(d, 0, 0, W, H)
    edge(d, 0, 0, W, H)
    divider(d, 1, W - 2, ROW_TOP - 3)

    for i in range(ROWS):
        y = ROW_TOP + i * ROW_H
        if i > 0:
            divider(d, 1, W - 2, y)
        plate(d, 61, y + (ROW_H - 16) // 2 - 1, 42, 18)
        for ri in range(2):
            x = 108 + ri * 52
            plate(d, x - 1, y + (ROW_H - 18) // 2 - 1, 50, 20)
        plate(d, 223, y + 5, 14, ROW_H - 10)

    divider(d, 1, W - 2, ROW_TOP + ROWS * ROW_H)

    img.save('robot_controller.png')
    return img


if __name__ == '__main__':
    out = {'assembler': gen_assembler(), 'inventory': gen_inventory(),
           'controller': gen_controller()}
    for name, img in out.items():
        img.resize((img.width * 2, img.height * 2), Image.NEAREST).save(
            'preview_' + name + '.png')
    print('generated', ', '.join(out))
