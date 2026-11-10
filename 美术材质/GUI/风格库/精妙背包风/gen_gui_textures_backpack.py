from PIL import Image, ImageDraw

# ==================== 精妙背包风调色板 ====================
# 借鉴精妙背包（Sophisticated Backpacks）：深炭灰面板 + 平坦凹格 + 暖金描边。
# 数值与同目录 TechGuiTheme.java 的常量一一对应。
PANEL    = (43, 43, 43, 255)      # BG      #2B2B2B
PANEL_DK = (31, 31, 31, 255)      # BG_DARK #1F1F1F（右侧面板等略深区域）
PANEL_LT = (58, 58, 58, 255)      # BG_LIGHT #3A3A3A
LINE     = (74, 74, 74, 255)      # 分区线高光 #4A4A4A
LINE_DK  = (22, 22, 22, 255)      # 分区线暗线 #161616
SLOT     = (26, 26, 26, 255)      # SLOT_FILL #1A1A1A
OUTLINE  = (10, 10, 10, 255)      # BORDER #0A0A0A
GOLD     = (224, 179, 76, 255)    # ACCENT #E0B34C
GOLD_DIM = (107, 90, 46, 255)     # ACCENT_DIM #6B5A2E
CLEAR    = (0, 0, 0, 0)

# 槽位凹格（18x18）不进贴图：由 TechGuiTheme.slot() 代码画。
# 贴图只负责固定部件：外框、面板、分区暗线、按钮底板、右侧过滤面板。


def canvas(w=256, h=256):
    return Image.new('RGBA', (w, h), CLEAR)


def panel(d, x, y, w, h, color=PANEL):
    d.rectangle([x, y, x + w - 1, y + h - 1], fill=color)


def edge(d, x, y, w, h):
    """1px 深描边。"""
    d.rectangle([x, y, x + w - 1, y + h - 1], outline=OUTLINE)


def divider(d, x0, x1, y, accent=False):
    d.line([x0, y, x1, y], fill=LINE)
    d.line([x0, y + 1, x1, y + 1], fill=PANEL_LT if not accent else GOLD_DIM)


def plate(d, x, y, w, h):
    """按钮/标签的浅凹槽底板：上左暗、下右亮，中间和面板同色。"""
    d.line([x, y, x + w - 1, y], fill=LINE_DK)
    d.line([x, y, x, y + h - 1], fill=LINE_DK)
    d.line([x, y + h - 1, x + w - 1, y + h - 1], fill=PANEL_LT)
    d.line([x + w - 1, y, x + w - 1, y + h - 1], fill=PANEL_LT)


# ==================== 装配台 256x230 ====================
# 槽位坐标（RobotAssemblerMenu，勿改）
def gen_assembler():
    img = canvas()
    d = ImageDraw.Draw(img)
    W, H = 256, 230
    panel(d, 0, 0, W, H)
    edge(d, 0, 0, W, H)

    # 顶部组件区与玩家背包区的分区线（暖金，呼应精妙背包的金色点缀）
    divider(d, 1, W - 2, 97, accent=True)
    # 组件舱背景：比面板略浅的一块
    d.rectangle([66, 34, 187, 94], fill=PANEL)
    edge(d, 66, 34, 122, 61)
    divider(d, 67, 186, 74)

    # 核心组件（躯干 + 核心）底板
    plate(d, 107, 18, 42, 22)
    # 输入 / 输出 端口底板
    for x in (6, 228):
        plate(d, x, 18, 22, 22)
    # 拆解 / 组装 按钮底板
    for x in (26, 188):
        plate(d, x, 18, 40, 22)

    # 玩家背包（y102..155）与快捷栏（y160..177）外框
    edge(d, 44, 100, 168, 58)
    edge(d, 44, 158, 168, 22)

    # 底部铭牌
    divider(d, 1, W - 2, 205)
    plate(d, 6, 207, 244, 18)

    img.save('robot_assembler.png')
    return img


# ==================== 机器人背包：只烘固定部件 ====================
def gen_inventory():
    img = canvas()
    d = ImageDraw.Draw(img)
    H = 256

    # 右侧「标记物品」面板 x=174..255：一整块略深的面板 + 描边
    d.rectangle([174, 0, 255, H - 1], fill=PANEL_DK)
    d.line([174, 0, 174, H - 1], fill=OUTLINE)
    d.line([255, 0, 255, H - 1], fill=OUTLINE)
    d.line([174, 0, 255, 0], fill=OUTLINE)
    # 面板顶部标题条：暖金分区线
    divider(d, 175, 254, 15, accent=True)

    # 两个按钮的底板
    for y in (16, 38):
        plate(d, 176, y, 80, 22)

    # 过滤槽位底板（12 格，凹格由代码画）
    edge(d, 175, 62, 44, 114)
    d.line([219, 62, 219, 175], fill=OUTLINE)

    img.save('robot_inventory.png')
    return img


# ==================== 操控台 240x244 ====================
def gen_controller():
    img = canvas()
    d = ImageDraw.Draw(img)
    W, H = 240, 244
    ROW_TOP, ROW_H, ROWS = 24, 24, 8

    panel(d, 0, 0, W, H)
    edge(d, 0, 0, W, H)
    # 标题条分区线（暖金）
    divider(d, 1, W - 2, 21, accent=True)

    for i in range(ROWS):
        y = ROW_TOP + i * ROW_H
        if i > 0:
            divider(d, 1, W - 2, y)
        # 模式按钮底板
        plate(d, 61, y + (ROW_H - 16) // 2 - 1, 42, 18)
        # 机器人方块底板
        for ri in range(2):
            x = 108 + ri * (48 + 4)
            plate(d, x - 1, y + (ROW_H - 18) // 2 - 1, 50, 20)
        # 删除按钮底板
        plate(d, 223, y + 5, 14, ROW_H - 10)

    # 底部工具条
    yb = ROW_TOP + ROWS * ROW_H
    divider(d, 1, W - 2, yb)

    img.save('robot_controller.png')
    return img


if __name__ == '__main__':
    a = gen_assembler()
    i = gen_inventory()
    c = gen_controller()
    for name, img in (('assembler', a), ('inventory', i), ('controller', c)):
        img.resize((img.width * 2, img.height * 2), Image.NEAREST).save('preview_' + name + '.png')
    print('ok')
