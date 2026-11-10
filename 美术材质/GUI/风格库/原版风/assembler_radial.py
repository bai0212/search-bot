"""装配台「原子式」布局的模拟图。
坐标和贴图都由 gen_gui_textures_vanilla.gen_assembler 出，这里只负责把
代码要画的槽位/按钮叠上去，用来离线核对对齐（含没放身躯的门控状态）。
"""
from PIL import Image, ImageDraw

from gen_gui_textures_vanilla import (PANEL, PANEL_LT, LINE, LINE_DK, SLOT, SLOT_DK,
                                      SLOT_LT, OUTLINE, canvas, plate,
                                      dotted_circle, gen_assembler,
                                      CX, CY, TORSO, RING1_R, RING2_R,
                                      ROBOT_IN, ROBOT_OUT, DISMANTLE_BTN, ASSEMBLE_BTN,
                                      BTN_W, BTN_H, PLAYER_X, PLAYER_ROWS, HOTBAR_ROW)
from mock_gui_vanilla import slot, button, text, item, FONT, TEXT, TEXT_DIM

RING1 = [(119, 44), (143, 52), (157, 72), (157, 96), (143, 116),
         (119, 124), (95, 116), (81, 96), (81, 72), (95, 52)]
RING2 = [(119, 20), (169, 44), (181, 98), (147, 142), (91, 142),
         (57, 98), (69, 44)]
CORE_SLOT, ATTACH_SLOTS = RING2[0], RING2[1:]
W, H = 256, 256


def gen_texture():
    return gen_assembler()


def render_mock():
    img = gen_texture().convert('RGBA')
    d = ImageDraw.Draw(img)
    slot(d, *TORSO)
    for x, y in RING1:
        slot(d, x, y)
    slot(d, *CORE_SLOT)
    for x, y in ATTACH_SLOTS:
        slot(d, x, y)
    slot(d, *ROBOT_IN)
    slot(d, *ROBOT_OUT)
    item(d, *ROBOT_IN, (150, 160, 175, 255))
    item(d, *TORSO, (120, 130, 145, 255))
    item(d, *CORE_SLOT, (96, 196, 238, 255))
    item(d, *RING1[0], (120, 200, 140, 255))
    item(d, *RING1[1], (200, 170, 90, 255))
    button(d, DISMANTLE_BTN[0], DISMANTLE_BTN[1], BTN_W, BTN_H, '拆解')
    button(d, ASSEMBLE_BTN[0], ASSEMBLE_BTN[1], BTN_W, BTN_H, '组装', hover=True)
    for y in PLAYER_ROWS:
        for c in range(9):
            slot(d, PLAYER_X + c * 18, y)
    for c in range(9):
        slot(d, PLAYER_X + c * 18, HOTBAR_ROW)
    text(d, 8, 6, '机器人装配台')
    text(d, 8, 60, '输入', TEXT_DIM)
    text(d, 222, 60, '输出', TEXT_DIM)
    text(d, 8, 163, '物品栏', TEXT_DIM)
    img.resize((W * 2, H * 2), Image.NEAREST).save('gui_mock_assembler_radial.png')


def render_gated_mock():
    """躯干没放时：只有中心槽 + 输入/输出，两圈槽位不出现（轨道线留虚影）。"""
    img = canvas(W, H)
    d = ImageDraw.Draw(img)
    d.rectangle([0, 0, W - 1, H - 1], fill=PANEL)
    d.rectangle([0, 0, W - 1, H - 1], outline=OUTLINE)
    dotted_circle(d, CX, CY, RING1_R, PANEL_LT, 8)
    dotted_circle(d, CX, CY, RING2_R, PANEL_LT, 8)
    plate(d, TORSO[0] - 4, TORSO[1] - 4, 26, 26)
    d.rectangle([TORSO[0] - 4, TORSO[1] - 4, TORSO[0] + 21, TORSO[1] + 21], outline=OUTLINE)
    slot(d, *TORSO)
    for x, y in (ROBOT_IN, ROBOT_OUT):
        plate(d, x - 2, y - 2, 22, 22)
        slot(d, x, y)
        item(d, x, y, (150, 160, 175, 255))
    for x, y in (DISMANTLE_BTN, ASSEMBLE_BTN):
        plate(d, x - 2, y - 2, BTN_W + 4, BTN_H + 4)
        button(d, x, y, BTN_W, BTN_H, '组装' if x == ASSEMBLE_BTN[0] else '拆解')
    d.line([1, 167, W - 2, 167], fill=LINE)
    d.rectangle([44, 174, 211, 231], outline=OUTLINE)
    d.rectangle([44, 232, 211, 253], outline=OUTLINE)
    for y in PLAYER_ROWS:
        for c in range(9):
            slot(d, PLAYER_X + c * 18, y)
    for c in range(9):
        slot(d, PLAYER_X + c * 18, HOTBAR_ROW)
    text(d, 8, 6, '机器人装配台')
    text(d, 8, 163, '物品栏', TEXT_DIM)
    text(d, 96, 130, '先放入身躯', (120, 120, 120, 255))
    img.resize((W * 2, H * 2), Image.NEAREST).save('gui_mock_assembler_empty.png')


if __name__ == '__main__':
    render_mock()
    render_gated_mock()
    print('ok')
