from PIL import Image, ImageDraw

# ---- 科技银 + 荧光蓝 调色板（与 search_robot.png 的按面配色同源） ----
BORDER = (8, 11, 16, 255)
HI     = (94, 114, 140, 255)
LO     = (14, 18, 25, 255)
BG     = (38, 47, 60, 255)
BGL    = (50, 62, 78, 255)
BGD    = (24, 31, 41, 255)
WELL   = (18, 24, 33, 255)
ACC    = (96, 196, 238, 255)
ACCD   = (52, 98, 120, 255)
ACCM   = (30, 54, 68, 255)
CLEAR  = (0, 0, 0, 0)


def canvas(w=256, h=256):
    return Image.new('RGBA', (w, h), CLEAR)


def raised(d, x, y, w, h, fill=None, edge_hi=None, edge_lo=None):
    """凸起的金属板：顶/左边亮，底/右边暗。"""
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=fill if fill else BGL)
    d.line([x, y, x + w - 1, y], fill=edge_hi if edge_hi else HI)
    d.line([x, y, x, y + h - 1], fill=edge_hi if edge_hi else HI)
    d.line([x, y + h - 1, x + w - 1, y + h - 1], fill=edge_lo if edge_lo else LO)
    d.line([x + w - 1, y, x + w - 1, y + h - 1], fill=edge_lo if edge_lo else LO)


def well(d, x, y, w, h, fill=None):
    """凹陷区：顶/左边暗，底/右边亮（内阴影）。"""
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=fill if fill else WELL)
    d.line([x, y, x + w - 1, y], fill=LO)
    d.line([x, y, x, y + h - 1], fill=LO)
    d.line([x, y + h - 1, x + w - 1, y + h - 1], fill=HI)
    d.line([x + w - 1, y, x + w - 1, y + h - 1], fill=HI)


def outline(d, x, y, w, h, color):
    d.rectangle([x, y, x + w - 1, y + h - 1], outline=color)


def brackets(d, x, y, w, h, color, n=4):
    """四角荧光蓝短角标（科技感）。"""
    d.line([x, y, x + n - 1, y], fill=color)
    d.line([x, y, x, y + n - 1], fill=color)
    d.line([x + w - n, y, x + w - 1, y], fill=color)
    d.line([x + w - 1, y, x + w - 1, y + n - 1], fill=color)
    d.line([x, y + h - 1, x + n - 1, y + h - 1], fill=color)
    d.line([x, y + h - n, x, y + h - 1], fill=color)
    d.line([x + w - n, y + h - 1, x + w - 1, y + h - 1], fill=color)
    d.line([x + w - 1, y + h - n, x + w - 1, y + h - 1], fill=color)


def frame(d, w, h):
    """整块面板的外框：1px 深边 + 内侧凸起倒角。"""
    d.rectangle([0, 0, w - 1, h - 1], fill=BORDER)
    d.line([1, 1, w - 2, 1], fill=HI)
    d.line([1, 1, 1, h - 2], fill=HI)
    d.line([1, h - 2, w - 2, h - 2], fill=LO)
    d.line([w - 2, 1, w - 2, h - 2], fill=LO)


def header(d, w, h=16):
    """顶部标题条：深色底 + 底部荧光蓝细线。"""
    d.rectangle([1, 1, w - 2, h - 1], fill=BGD)
    d.line([1, h - 1, w - 2, h - 1], fill=ACCD)
    d.line([1, h, w - 2, h], fill=ACCM)


# ==================== 装配台 256x230 ====================
# 槽位坐标（RobotAssemblerMenu，勿改）：
#   输入 x8y20 | 躯干 x109y20 | 核心 x129y20 | 输出 x230y20
#   功能组件 x79..159(步18) y40,58   附件 x69..169(步18) y76
#   玩家背包 x47.. y102,120,138      快捷栏 y160
def gen_assembler():
    img = canvas()
    d = ImageDraw.Draw(img)
    W, H = 256, 230
    d.rectangle([1, 1, W - 2, H - 2], fill=BG)
    frame(d, W, H)
    header(d, W)

    # 输入 / 输出端口
    for x in (6, 228):
        well(d, x, 18, 22, 22)
        brackets(d, x, 18, 22, 22, ACC)

    # 拆解 / 组装 按钮底座（按钮本体由代码画：28,20 与 190,20，均 36x18）
    for x in (26, 188):
        outline(d, x, 18, 40, 22, ACCM)
        d.line([x + 1, 39, x + 38, 39], fill=LO)

    # 核心组件板（躯干 + 核心 两格：x109..146, y20..37）
    raised(d, 105, 17, 46, 24)
    brackets(d, 105, 17, 46, 24, ACCD, 3)

    # 组件舱：功能组件(5x2, y40..75) + 附件(y76..93)，共用一块凹槽
    well(d, 66, 34, 122, 63)
    d.line([66, 75, 187, 75], fill=ACCD)
    d.line([66, 34, 187, 34], fill=ACCM)
    brackets(d, 66, 34, 122, 63, ACCD, 4)

    # 装配区 / 玩家背包 分隔线
    d.line([8, 99, W - 9, 99], fill=ACCD)
    d.line([8, 100, 34, 100], fill=ACC)
    d.line([W - 34, 100, W - 9, 100], fill=ACC)

    # 玩家背包（y102..155）与快捷栏（y160..177）
    well(d, 44, 100, 168, 58)
    well(d, 44, 159, 168, 21)
    d.line([44, 158, 211, 158], fill=ACCD)
    brackets(d, 44, 100, 168, 58, ACCM, 3)
    brackets(d, 44, 159, 168, 21, ACCM, 3)

    # 底部铭牌（标题文字由代码画在这里）
    d.line([8, 204, W - 9, 204], fill=ACCM)
    well(d, 6, 207, 244, 18, fill=(28, 36, 47, 255))
    brackets(d, 6, 207, 244, 18, ACCD, 4)
    for x in range(200, 240, 4):
        d.line([x, 210, x, 213], fill=ACCM)

    img.save('robot_assembler.png')
    return img


# ==================== 机器人背包：只烘固定部件 ====================
# 左侧高度随背包行数变化，由代码填充；贴图负责标题条 + 右侧过滤面板。
# 过滤面板（勿改）：标题 x178y4 / 模式按钮 x178y18 76x18 / 忽略按钮 y40 / 槽位 x178,196 y66..156
def gen_inventory():
    img = canvas()
    d = ImageDraw.Draw(img)
    H = 256

    # 顶部标题条（跨整宽）
    header(d, 256)

    # 右侧「标记物品」面板 x=174..255
    d.rectangle([174, 16, 254, H - 1], fill=BGD)
    d.line([174, 16, 174, H - 1], fill=ACCD)
    d.line([248, 3, 251, 3], fill=ACC)
    d.line([248, 5, 251, 5], fill=ACCM)

    # 两个按钮的底座（按钮本体由代码画）
    for y in (18, 40):
        outline(d, 176, y - 2, 80, 22, ACCM)
        d.line([176, y + 19, 255, y + 19], fill=LO)

    # 过滤槽位底板（12 格：x178,196 / y66..173）
    well(d, 175, 62, 44, 114)
    d.line([175, 62, 218, 62], fill=ACCM)
    brackets(d, 175, 62, 44, 114, ACCD, 4)
    # 面板底部装饰刻度
    for x in range(178, 252, 6):
        d.point((x, 177), fill=ACCM)
        d.point((x, 179), fill=ACCM)

    img.save('robot_inventory.png')
    return img


# ==================== 操控台 240x244 ====================
# 行布局（勿改）：ROW_TOP=24 ROW_H=24；名称 x8 / 模式 x62 w40 h16 / 机器人 x108 起 48x18 间隔4 / 删除 x224 w12
def gen_controller():
    img = canvas()
    d = ImageDraw.Draw(img)
    W, H = 240, 244
    ROW_TOP, ROW_H, ROWS = 24, 24, 8

    d.rectangle([1, 1, W - 2, H - 2], fill=BG)
    frame(d, W, H)
    header(d, W)

    for i in range(ROWS):
        y = ROW_TOP + i * ROW_H
        if i % 2 == 0:
            d.rectangle([2, y, W - 3, y + ROW_H - 1], fill=(44, 54, 68, 255))
        d.line([2, y, W - 3, y], fill=LO)
        # 频道名左侧荧光蓝定位短条
        d.line([3, y + 6, 3, y + ROW_H - 7], fill=ACCD)
        # 模式按钮凹槽（x62..101, 16 高居中）
        outline(d, 61, y + (ROW_H - 16) // 2 - 1, 42, 18, ACCM)
        # 机器人方块槽位（最多 2 个 48x18，间隔 4）
        for ri in range(2):
            x = 108 + ri * (48 + 4)
            outline(d, x - 1, y + (ROW_H - 18) // 2 - 1, 50, 20, ACCM)
        # 删除按钮底座（x224..235）
        d.rectangle([223, y + 5, 236, y + ROW_H - 7], fill=BGD)
        brackets(d, 223, y + 5, 14, ROW_H - 11, ACCM, 2)

    # 底部工具条（BOTTOM_H=28，最后一行下方）
    yb = ROW_TOP + ROWS * ROW_H
    d.line([2, yb, W - 3, yb], fill=ACCD)
    d.rectangle([2, yb + 1, W - 3, H - 2], fill=BGD)
    d.line([8, yb + 1, 40, yb + 1], fill=ACC)
    d.line([W - 40, yb + 1, W - 8, yb + 1], fill=ACC)

    img.save('robot_controller.png')
    return img


def mock(img, slots, labels, size=3):
    """把代码将要画的槽位/文字位置叠上去，检查对齐。"""
    m = img.copy()
    d = ImageDraw.Draw(m)
    for x, y in slots:
        d.rectangle([x, y, x + 17, y + 17], outline=(255, 60, 60, 255), width=1)
    for x, y, t in labels:
        d.rectangle([x, y, x + 8 * len(t), y + 8], fill=(255, 220, 0, 255))
    return m.resize((256 * size, 256 * size), Image.NEAREST)


if __name__ == '__main__':
    a = gen_assembler()
    i = gen_inventory()
    c = gen_controller()
    for name, img in (('assembler', a), ('inventory', i), ('controller', c)):
        img.resize((512, 512), Image.NEAREST).save('preview_' + name + '.png')
    asm_slots = [(8, 20), (109, 20), (129, 20), (230, 20)]
    asm_slots += [(x, 40 + r * 18) for r in range(2) for x in (79, 99, 119, 139, 159)]
    asm_slots += [(x, 76) for x in (69, 89, 109, 129, 149, 169)]
    asm_slots += [(47 + col * 18, y) for y in (102, 120, 138) for col in range(9)]
    asm_slots += [(47 + col * 18, 160) for col in range(9)]
    mock(a, asm_slots, [(8, 6, 'T'), (8, 8, '输入'), (102, 8, '核心'), (222, 8, '输出'),
                        (79, 28, '功能'), (69, 64, '附件'), (47, 90, '背包')]).save('mock_assembler.png')
    inv_slots = [(8 + col * 18, y) for y in (18, 36, 54, 72, 90, 108) for col in range(9)]
    inv_slots += [(178 + col * 18, 66 + r * 18) for r in range(6) for col in range(2)]
    mock(i, inv_slots, [(8, 6, 'T'), (178, 4, '标记'), (178, 22, 'MODE'), (178, 44, 'IGNORE')]).save('mock_inventory.png')
    print('ok')
