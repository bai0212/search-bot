# -*- coding: utf-8 -*-
"""居中/SB 化后的目标布局模拟图（只出 mock，不装贴图）。

对照 design/gui-code-requirements.md 的 R5：代码按这里的坐标改完后，
再回来把 gen_gui_textures_vanilla.py 的对应底板坐标同步改掉并安装。

    python centered_target.py
"""
from PIL import Image, ImageDraw

from mock_gui_current import (ACCENT, ACCENT_DIM, BEVEL_LO, BG, BG_LIGHT, BORDER, LABEL,
                              LABEL_DIM, SLOT_FILL, new_panel, slot, button, label,
                              text_w, load, montage, FUNC_X, FUNC_Y, ATT_X, ATT_Y)
from mock_gui_current import _font

# ==== 目标常量 ====
FILTER_PANEL_X = 174          # 右侧面板左边界（不变）
FILTER_COLS = (196, 214)      # 2 列在 82px 面板里居中：174 + (82-36)/2 = 197 -> 取偶数 196
FILTER_ROWS_Y = tuple(range(66, 66 + 6 * 18, 18))
FILTER_TITLE_X = 215          # 面板中心
IN_LABEL = (8, 72)            # 「输入」贴着端口槽上方（槽在 8,84）
OUT_LABEL = (231, 72)         # 「输出」贴着端口槽上方（槽在 230,84）
ACTION_BAR = ((8, 106), (212, 106))   # 拆解 / 组装：关于 x=128 对称，保持原位
TAB_ACTIVE_BG = BG_LIGHT


def assembler():
    im, d = new_panel(256, 256, None)
    for x, y in ((8, 84), (119, 84), (119, 20), (230, 84)):
        slot(d, x, y)
    for x, y in zip(FUNC_X, FUNC_Y):
        slot(d, x, y)
    for x, y in zip(ATT_X, ATT_Y):
        slot(d, x, y)
    inv_x = (256 - 162) // 2
    for r in range(3):
        for c in range(9):
            slot(d, inv_x + c * 18, 176 + r * 18)
    for c in range(9):
        slot(d, inv_x + c * 18, 234)
    # 居中的动作条：两个按钮并排，整体在 256 宽里居中
    button(d, ACTION_BAR[0][0], ACTION_BAR[0][1], 36, 18, '拆解')
    button(d, ACTION_BAR[1][0], ACTION_BAR[1][1], 36, 18, '组装')
    label(d, 8, 6, '机器人装配台', ACCENT)
    label(d, *IN_LABEL, '输入', LABEL_DIM)
    label(d, *OUT_LABEL, '输出', LABEL_DIM)
    label(d, 8, 163, '玩家背包', LABEL_DIM)
    return im


def inventory(rows, boxes=2):
    w = 256
    h = max(133 + rows * 18, 181)
    im, d = new_panel(w, h, None)
    # 分页栏：整条居中，当前页提亮而不是压暗
    widths = [max(text_w('箱子%d' % (t + 1)) + 16, 30) for t in range(boxes)]
    x = (w - 174 - sum(widths) - 2 * (boxes - 1)) // 2
    for t, tw in enumerate(widths):
        active = t == 0
        bg = TAB_ACTIVE_BG if active else BG
        d.rectangle([x, 16, x + tw - 1, 33], fill=bg)
        d.rectangle([x, 16, x + tw - 1, 33], outline=BORDER)
        s = '箱子%d' % (t + 1)
        d.text((x + (tw - text_w(s)) // 2, 21), s, font=_font, fill=LABEL)
        x += tw + 2
    sep_y = 36 + rows * 18 + 1
    d.line([6, sep_y, 172, sep_y], fill=ACCENT_DIM)
    for r in range(rows):
        for c in range(9):
            slot(d, 8 + c * 18, 36 + r * 18)
    py = 36 + rows * 18 + 14
    for r in range(3):
        for c in range(9):
            slot(d, 8 + c * 18, py + r * 18)
    for c in range(9):
        slot(d, 8 + c * 18, py + 58)
    # 过滤槽：在右侧面板里水平居中
    for cx in FILTER_COLS:
        for cy in FILTER_ROWS_Y:
            slot(d, cx, cy)
    d.rectangle([192, 62, 236, 178], outline=BORDER)      # 托盘框住居中的 2 列
    # 面板标题与两个按钮都在 174..256 里居中
    s = '标记物品'
    label(d, FILTER_TITLE_X - text_w(s) // 2, 4, s, ACCENT)
    button(d, 177, 18, 76, 18, '黑名单')
    button(d, 177, 40, 76, 18, '忽略后缀:开')
    label(d, 8, 6, '搜索机器人', LABEL)
    label(d, 112 - 8, 36 + rows * 18 + 4, '1200/3000', LABEL)
    return im


def controller(channels=3):
    row_top, row_h, bottom = 24, 24, 28
    w = 240
    h = row_top + channels * row_h + bottom          # 高度跟着频道数走，不留空行
    im, d = new_panel(w, h, None)
    names = ['一号矿区', '主基地', '废弃矿井']
    modes = ['搜索', '巡逻', '收集']
    robots = [['采矿机A', '采矿机B'], [], ['运输机C']]
    for i in range(channels):
        y = row_top + i * row_h
        label(d, 8, y + 8, names[i], LABEL)
        button(d, 62, y + 4, 40, 16, modes[i])
        for ri in range(2):
            bx = 108 + ri * 52
            d.rectangle([bx - 1, y + 2, bx + 48, y + 21], outline=BORDER)
            d.rectangle([bx, y + 3, bx + 47, y + 20], fill=BG_LIGHT)
            lst = robots[i]
            if ri < len(lst):
                label(d, bx + 2, y + 8, lst[ri], LABEL)
        d.rectangle([224, y + 6, 235, y + 17], outline=BORDER)
        label(d, 225, y + 7, '✕', BORDER)
        if i:
            d.line([1, y, w - 2, y], fill=BEVEL_LO)
    label(d, 8, 6, '机器人操控台', LABEL)
    return im


if __name__ == '__main__':
    shots = [('装配台：端口标签贴到槽位上方', assembler()),
             ('背包：过滤列在面板内居中', inventory(3)),
             ('背包 6 行', inventory(6)),
             ('操控台：高度跟随频道数', controller(3))]
    montage(shots, 2).save('gui_mock_centered_target.png')
    print('ok -> gui_mock_centered_target.png')
