"""搜索机器人 —— 全套物品贴图生成器（16x16 画布 / 14x14 有效区）。

遵循原版物品约定：16x16 只画中间 14x14，四周留 1px 透明边（Sprite.CLIP 强制）。

造型依据 design/parts-guide.md：
  机械臂=取物夹爪 / 视觉传感器=镜头 / 发动机=带散热片的电机 /
  履带=坦克式负重轮+环形履带板 / 爬行肢=关节爪腿 / 推进器=钟形喷口 /
  储物箱=金属货箱 / 电池=电芯+电量条 / 护盾=六边形能量盾 /
  身躯=宽肩胸甲+核心插槽 / 核心=能量晶体 / 传送装置=门框+能量场
统一规则：左上光源、1px 深色描边、内高光/暗边倒角、荧光蓝=能量件。
"""
from PIL import Image

W = H = 16
CLIP = (1, 1, 14, 14)  # 原版：14x14 有效区
# 描边要占有效区最外一圈，所以「本体」一律再往里收一格：只画在 2..13。
# 之前本体画到 1/14，outline() 只能把描边补到 0/15，于是吃掉了留白边框。
BODY_LO, BODY_HI = 2, 13


def _body(v):
    return BODY_LO if v < BODY_LO else (BODY_HI if v > BODY_HI else v)

TIERS = {
    'copper':    ((46, 24, 12), (112, 62, 30), (168, 100, 50), (212, 142, 82), (246, 202, 146)),
    'iron':      ((40, 44, 50), (102, 110, 120), (152, 162, 174), (198, 208, 220), (240, 246, 252)),
    'gold':      ((66, 44, 8), (142, 102, 18), (206, 158, 34), (246, 206, 84), (255, 244, 178)),
    'diamond':   ((10, 50, 58), (40, 124, 138), (80, 190, 204), (156, 234, 244), (228, 252, 255)),
    'netherite': ((18, 15, 17), (52, 45, 49), (90, 80, 84), (132, 120, 124), (180, 168, 170)),
    'ultimate':  ((38, 18, 66), (98, 56, 152), (154, 112, 216), (206, 178, 252), (255, 255, 255)),
}
EN = ((16, 52, 76), (40, 108, 140), (92, 192, 236), (170, 232, 252), (240, 253, 255))
RUB = ((12, 14, 18), (36, 41, 48), (64, 71, 81), (96, 106, 118), (134, 145, 158))
STL = ((28, 32, 38), (70, 78, 88), (112, 122, 134), (158, 170, 184), (212, 220, 230))
DK = (10, 12, 16)

LIGHT = (-0.60, -0.80)


class Sprite:
    def __init__(self):
        self.px = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]
        self.mask = set()

    # ---- 基础写入（本体裁到 2..13，描边再占 1..14，最外一圈留白）----
    def set(self, x, y, c):
        x, y = _body(int(x)), _body(int(y))
        if CLIP[0] <= x <= CLIP[2] and CLIP[1] <= y <= CLIP[3]:
            self.px[y][x] = (c[0], c[1], c[2], 255)

    def add(self, x, y):
        x, y = _body(int(x)), _body(int(y))
        if CLIP[0] <= x <= CLIP[2] and CLIP[1] <= y <= CLIP[3]:
            self.mask.add((x, y))

    def rect(self, x0, y0, x1, y1):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.add(x, y)

    def rrect(self, x0, y0, x1, y1, r):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                dx = max(x0 + r - x, x - (x1 - r), 0)
                dy = max(y0 + r - y, y - (y1 - r), 0)
                if dx * dx + dy * dy <= r * r + 0.4:
                    self.add(x, y)

    def disc(self, cx, cy, rad):
        for y in range(int(cy - rad), int(cy + rad) + 1):
            for x in range(int(cx - rad), int(cx + rad) + 1):
                if (x - cx) ** 2 + (y - cy) ** 2 <= rad * rad + 0.35:
                    self.add(x, y)

    def ring(self, cx, cy, r_out, r_in):
        for y in range(int(cy - r_out), int(cy + r_out) + 1):
            for x in range(int(cx - r_out), int(cx + r_out) + 1):
                d = (x - cx) ** 2 + (y - cy) ** 2
                if r_in * r_in - 0.35 <= d <= r_out * r_out + 0.35:
                    self.add(x, y)

    def poly(self, pts):
        ys = [p[1] for p in pts]
        for y in range(int(min(ys)), int(max(ys)) + 1):
            xs = []
            for i in range(len(pts)):
                ax, ay = pts[i]
                bx, by = pts[(i + 1) % len(pts)]
                if ay <= y <= by or by <= y <= ay:
                    xs.append(ax if by == ay else ax + (y - ay) * (bx - ax) / (by - ay))
            if xs:
                for x in range(int(round(min(xs))), int(round(max(xs))) + 1):
                    self.add(x, y)

    def paint(self, x0, y0, x1, y1, c):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.set(x, y, c)

    def fill(self, pal):
        pts = sorted(self.mask)
        if not pts:
            return
        cx = sum(p[0] for p in pts) / len(pts)
        cy = sum(p[1] for p in pts) / len(pts)
        span = max(1.0, max(max(abs(x - cx), abs(y - cy)) for x, y in pts))
        for x, y in pts:
            v = ((x - cx) * LIGHT[0] + (y - cy) * LIGHT[1]) / span
            self.set(x, y, pal[4] if v > 0.72 else pal[3] if v > 0.40 else pal[2] if v > -0.30 else pal[1])
        self.mask = set()

    def bevel(self, pal):
        """贴边倒角：上沿提亮、下沿压暗，14px 内立刻立体。"""
        src = [[self.px[y][x][3] > 0 for x in range(W)] for y in range(H)]
        for y in range(CLIP[1], CLIP[3] + 1):
            for x in range(CLIP[0], CLIP[2] + 1):
                if not src[y][x]:
                    continue
                cur = self.px[y][x][:3]
                if y > 0 and not src[y - 1][x]:
                    self.set(x, y, pal[4])
                elif x > CLIP[0] - 1 and not src[y][x - 1]:
                    self.set(x, y, pal[3])
                elif y < H - 1 and not src[y + 1][x]:
                    self.set(x, y, pal[1])
                elif x < W - 1 and not src[y][x + 1]:
                    self.set(x, y, pal[1] if cur != pal[4] else pal[2])

    def outline(self, color):
        src = [[self.px[y][x][3] for x in range(W)] for y in range(H)]
        for y in range(CLIP[1] - 1, CLIP[3] + 2):
            for x in range(CLIP[0] - 1, CLIP[2] + 2):
                if not (0 <= x < W and 0 <= y < H) or src[y][x]:
                    continue
                if not (CLIP[0] <= x <= CLIP[2] and CLIP[1] <= y <= CLIP[3]):
                    continue  # 描边也不许进最外一圈留白
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < W and 0 <= ny < H and src[ny][nx]:
                        self.px[y][x] = (color[0], color[1], color[2], 255)
                        break

    def image(self):
        im = Image.new('RGBA', (W, H))
        for y in range(H):
            for x in range(W):
                im.putpixel((x, y), self.px[y][x])
        return im


# ============ 移动部件 ============
def track(frame=STL):
    """坦克履带：闭环履带板 + 前后大轮 + 中间托带小轮。"""
    s = Sprite()
    s.rrect(1, 3, 14, 12, 2)
    outer = set(s.mask)
    s.mask = set()
    s.rrect(3, 5, 12, 10, 1)
    hole = set(s.mask)
    s.mask = outer - hole
    s.fill(RUB)
    # 履带板分带：上带受光、下带背光
    for x in range(1, 15):
        if (x, 3) in outer:
            s.set(x, 3, RUB[4])
        if (x, 4) in outer:
            s.set(x, 4, RUB[3])
        if (x, 11) in outer:
            s.set(x, 11, RUB[2])
        if (x, 12) in outer:
            s.set(x, 12, RUB[1])
    for y in range(5, 11):
        if (1, y) in outer:
            s.set(1, y, RUB[3])
        if (14, y) in outer:
            s.set(14, y, RUB[1])
    # 上下各一排凸齿（履带板端耳），轮廓一有齿就读得出是履带
    for x in (2, 5, 8, 11, 14):
        s.set(x, 2, RUB[3])
        s.set(x, 13, RUB[1])
    # 每 3~4px 一节，节间只留一条不太扎眼的暗缝
    for x in (4, 7, 10, 13):
        s.set(x, 4, RUB[1])
        s.set(x, 11, RUB[1])
    # 轮架底板
    for x, y in sorted(hole):
        s.set(x, y, (20, 23, 28, 255))
    # 前后两个大轮 + 中间一个小托带轮，三者互不重叠才读得出是轮子
    for cx, r in ((4, 2), (11, 2)):
        s.mask = set()
        s.disc(cx, 8, r)
        s.mask &= hole
        s.fill(frame)
        s.paint(cx, 8, cx, 8, DK)
        s.set(cx - 1, 7, frame[4])
        s.set(cx + 1, 9, frame[1])
    s.mask = set()
    s.disc(7, 8, 1)
    s.mask &= hole
    s.fill(frame)
    s.set(7, 8, DK)
    # 轮缘暗圈，把轮子和底板分开
    for cx, r in ((4, 2), (11, 2), (7, 1)):
        for x, y in list(hole):
            if (x - cx) ** 2 + (y - 8) ** 2 == r * r:
                s.set(x, y, frame[0])
    s.outline(RUB[0])
    return s.image()

def thruster(pal=STL):
    """推进器：横置钟形喷口，左侧安装板 + 散热环燃烧室 + 蓝色喷焰。"""
    s = Sprite()
    s.rect(1, 4, 2, 11)
    s.fill(pal)
    s.set(1, 5, DK); s.set(1, 10, DK)
    s.rect(3, 5, 7, 10)
    s.fill(pal)
    for x in (4, 6):
        s.paint(x, 5, x, 10, pal[1])
    s.paint(3, 5, 7, 5, pal[3])
    s.rect(7, 7, 8, 8)
    s.fill(pal)
    s.poly([(8, 7), (8, 8), (13, 12), (13, 3)])
    s.fill(pal)
    for x in (10, 12):
        y0 = int(round(7 - (x - 8) * 4 / 5.0))
        y1 = int(round(8 + (x - 8) * 4 / 5.0))
        s.paint(x, y0, x, y1, pal[1])
    for y in range(3, 13):
        d = abs(y - 7.5)
        s.set(13, y, EN[4] if d < 1.5 else EN[2] if d < 3 else EN[1] if d < 4 else EN[0])
    s.outline(pal[0])
    return s.image()


def limbs(pal=STL):
    """爬行肢：左右各一条独立关节腿（髋块 - 大腿 - 膝 - 小腿 - 双趾爪）。"""
    s = Sprite()
    for mir in (False, True):
        tf = (lambda pts: [(15 - x, y) for x, y in pts]) if mir else (lambda pts: pts)
        c = (lambda v: 15 - v) if mir else (lambda v: v)
        lo, hi = sorted((c(1), c(5)))
        s.rrect(lo, 1, hi, 3, 1)
        s.fill(pal)
        s.paint(lo, 3, hi, 3, pal[1])
        s.set(c(3), 2, EN[2])
        s.poly(tf([(2, 3), (5, 3), (6, 8), (3, 8)]))
        s.fill(pal)
        s.mask = set()
        s.disc(4, 8, 2)
        s.fill(pal)
        s.set(4, 8, DK)
        s.set(3, 7, pal[4])
        s.poly(tf([(3, 9), (6, 9), (6, 13), (4, 13)]))
        s.fill(pal)
        s.paint(min(c(4), c(6)), 9, max(c(4), c(6)), 9, pal[0])
        s.poly(tf([(2, 13), (4, 13), (3, 14), (1, 14)]))
        s.fill(pal)
        s.poly(tf([(5, 13), (7, 13), (8, 14), (6, 14)]))
        s.fill(pal)
    s.outline(pal[0])
    return s.image()

# ============ 功能部件 ============
def arm(pal=STL):
    """机械臂：斜置大臂 + 肘关节 + 顶端张开的双指夹爪。"""
    s = Sprite()
    # 底座与肩关节
    s.rrect(1, 11, 6, 14, 1)
    s.fill(pal)
    s.paint(1, 13, 6, 13, pal[1])
    s.mask = set()
    s.disc(4, 11, 3)
    s.fill(pal)
    s.set(4, 11, DK)
    s.set(3, 10, pal[4])
    s.set(5, 12, EN[2])
    # 斜向大臂
    s.poly([(2, 10), (5, 10), (11, 3), (8, 2)])
    s.fill(pal)
    s.paint(4, 8, 6, 8, pal[1])
    s.paint(6, 6, 8, 6, pal[1])
    # 肘关节
    s.mask = set()
    s.disc(10, 3, 2)
    s.fill(pal)
    s.set(10, 3, DK)
    s.set(9, 2, pal[4])
    # 双指夹爪
    s.poly([(10, 2), (11, 1), (14, 1), (13, 3)])
    s.fill(pal)
    s.poly([(11, 4), (12, 5), (14, 4), (13, 3)])
    s.fill(pal)
    s.set(13, 1, pal[4]); s.set(14, 1, pal[4]); s.set(14, 4, pal[4])
    s.outline(pal[0])
    return s.image()


def sensor(pal=STL):
    """视觉传感器：方壳 + 偏左大镜头（镜筒环 / 光圈 / 高光）+ 右侧接线柱。"""
    s = Sprite()
    s.rrect(1, 2, 12, 13, 1)
    s.fill(pal)
    s.paint(1, 2, 12, 2, pal[4])
    s.paint(1, 13, 12, 13, pal[1])
    # 右侧接线柱与固定耳
    s.rect(13, 4, 14, 6)
    s.fill(pal)
    s.rect(13, 9, 14, 11)
    s.fill(pal)
    s.set(14, 5, DK); s.set(14, 10, DK)
    # 镜筒
    s.mask = set()
    s.disc(6, 8, 4)
    s.fill(pal)
    s.mask = set()
    s.disc(6, 8, 3)
    s.fill(((14, 44, 64),) * 5)
    s.mask = set()
    s.disc(6, 8, 2)
    s.fill(EN)
    s.paint(6, 8, 6, 8, EN[4])
    s.set(5, 7, EN[4]); s.set(5, 6, EN[3])
    # 状态灯 + 底部安装座
    s.set(11, 4, EN[2]); s.set(11, 6, EN[0])
    s.rect(4, 14, 8, 14)
    s.fill(pal)
    s.outline(pal[0])
    return s.image()


def engine(pal=STL):
    """发动机：带散热片的电机 + 顶部接线盒 + 右侧输出轴法兰。"""
    s = Sprite()
    s.rrect(1, 4, 10, 12, 2)
    s.fill(pal)
    for x in (2, 4, 6, 8):
        s.paint(x, 5, x, 11, pal[1])
        s.paint(x + 1, 5, x + 1, 11, pal[3])
    s.rect(3, 1, 8, 3)
    s.fill(pal)
    s.paint(4, 2, 7, 2, DK)
    s.rect(11, 6, 12, 10)
    s.fill(pal)
    s.rrect(12, 5, 14, 11, 1)
    s.fill(pal)
    s.paint(13, 7, 13, 9, DK)
    s.set(4, 4, EN[2]); s.set(7, 4, EN[2])
    s.outline(pal[0])
    return s.image()


def storage(pal=STL):
    """储物箱：金属货箱，顶盖提手 + 中央锁扣 + 侧筋 + 四角铆钉。"""
    s = Sprite()
    s.rect(5, 1, 10, 1)
    s.fill(pal)
    s.rect(5, 2, 6, 2)
    s.fill(pal)
    s.rect(9, 2, 10, 2)
    s.fill(pal)
    s.paint(7, 2, 8, 2, DK)
    s.rrect(1, 3, 14, 14, 1)
    s.fill(pal)
    s.paint(1, 3, 14, 3, pal[4])
    s.paint(1, 6, 14, 6, pal[0])
    s.paint(1, 7, 14, 7, pal[1])
    s.rect(6, 5, 9, 10)
    s.fill(pal)
    s.paint(6, 5, 9, 5, pal[4])
    s.paint(7, 7, 8, 9, DK)
    s.set(7, 8, EN[3]); s.set(8, 8, EN[2])
    for x in (3, 12):
        s.paint(x, 9, x, 12, pal[1])
    for x, y in ((2, 4), (13, 4), (2, 13), (13, 13)):
        s.set(x, y, pal[4])
    s.outline(pal[0])
    return s.image()


def battery(pal=STL):
    """电池：电芯外壳 + 正极凸台 + 三格电量条。"""
    s = Sprite()
    s.rect(6, 1, 9, 2)
    s.fill(pal)
    s.rrect(3, 3, 12, 14, 2)
    s.fill(pal)
    s.paint(3, 4, 12, 4, pal[3])
    s.rect(5, 6, 10, 12)
    s.fill(((12, 16, 22),) * 5)
    for i, y in enumerate((7, 9, 11)):
        s.paint(6, y, 9, y, EN[4 - min(i, 2)])
    s.paint(5, 13, 10, 13, pal[1])
    s.outline(pal[0])
    return s.image()


# 盾形轮廓：每行 (左端, 右端)，全部满足 左+右=15 —— 关于中央轴 x=7.5 严格对称。
# 平顶 + 直边 + 长收尖，才是「盾」；上一版顶部也圆弧，看着像蛋。
SHIELD_ROWS = {
    2: (3, 12), 3: (2, 13), 4: (2, 13), 5: (2, 13), 6: (2, 13), 7: (2, 13),
    8: (3, 12), 9: (4, 11), 10: (4, 11), 11: (5, 10), 12: (6, 9), 13: (7, 8),
}
# 中央能量导管（竖脊）+ 导管上的能量舱（菱形），同样左右对称。
RIDGE_Y = (3, 12)
CELL_ROWS = {5: (7, 8), 6: (6, 9), 7: (5, 10), 8: (5, 10), 9: (6, 9), 10: (7, 8)}
# 能量舱分行明暗：0 最亮，从上往下压暗（只随 y 变，所以左右一致）。
CELL_SHADE = {5: 4, 6: 3, 7: 2, 8: 2, 9: 1, 10: 0}


def shield(pal=STL):
    """护盾：纹章盾形甲面 + 竖向能量导管 + 中央能量舱。

    正对着玩家的板件不能用 fill() 的左上斜向光源（那样左亮右暗，
    沿中央轴一比就不对称），所以整件按「一行一跨」画：
    每跨两端满足 x0+x1=15，明暗只随 y 变，左右天然一致。
    能量舱不给整圈深色底槽 —— 上下左右一圈深色会在 16px 上长成一张脸。
    """
    s = Sprite()
    for y, (x0, x1) in SHIELD_ROWS.items():
        s.paint(x0, y, x1, y, pal[1])                 # 甲面外沿（暗）
    for y, (x0, x1) in SHIELD_ROWS.items():
        s.paint(x0 + 1, y, x1 - 1, y, pal[2])         # 内缩一格 = 甲面
    s.paint(4, 2, 11, 2, pal[4])                      # 平顶受光
    s.paint(3, 3, 12, 3, pal[3])
    s.paint(3, 4, 12, 4, pal[3])
    s.paint(6, 11, 9, 11, pal[1])                     # 收尖处背光
    s.paint(7, 12, 8, 12, pal[1])
    s.paint(7, 13, 8, 13, pal[1])
    s.paint(7, RIDGE_Y[0], 8, RIDGE_Y[1], pal[3])     # 能量导管
    s.paint(7, RIDGE_Y[0], 8, RIDGE_Y[0], pal[4])
    s.paint(7, RIDGE_Y[1], 8, RIDGE_Y[1], pal[1])
    for y, (x0, x1) in CELL_ROWS.items():             # 能量舱
        s.paint(x0, y, x1, y, EN[CELL_SHADE[y]])
    for y in (7, 8):                                  # 舱体两侧座圈（只在最宽处）
        s.paint(4, y, 4, y, pal[0])
        s.paint(11, y, 11, y, pal[0])
    s.outline(pal[0])
    return s.image()


# ============ 独立部件 ============
def core():
    """机器人核心：八角外壳 + 内凹能量舱 + 晶体。"""
    s = Sprite()
    pal = STL
    s.rect(2, 3, 13, 12)
    for i in range(2):
        for x in (2 + i, 13 - i):
            s.mask.discard((x, 3))
            s.mask.discard((x, 12))
    s.fill(pal)
    s.rect(6, 1, 9, 2)
    s.fill(pal)
    s.rect(6, 13, 9, 14)
    s.fill(pal)
    s.paint(4, 5, 11, 10, EN[0])
    s.poly([(8, 4), (11, 7), (8, 11), (5, 7)])
    s.fill(EN)
    s.paint(7, 7, 8, 8, EN[4])
    for x, y in ((3, 4), (12, 4), (3, 11), (12, 11)):
        s.set(x, y, pal[4])
    s.outline(pal[0])
    return s.image()


def teleporter():
    """传送装置：八角门框 + 四角节点 + 门洞内的能量场。"""
    s = Sprite()
    pal = STL
    s.rrect(1, 1, 14, 14, 3)
    outer = set(s.mask)
    s.mask = set()
    s.rect(5, 5, 10, 10)
    hole = set(s.mask)
    s.mask = outer - hole
    s.fill(pal)
    for x, y in ((3, 3), (12, 3), (3, 12), (12, 12)):
        s.mask = set()
        s.rect(x - 1, y - 1, x + 1, y + 1)
        s.fill(EN)
        s.set(x, y, EN[4])
    for x, y in sorted(hole):
        s.set(x, y, (12, 20, 30, 255))
    s.poly([(8, 5), (10, 7), (8, 10), (5, 7)])
    s.fill(EN)
    s.set(8, 7, EN[4]); s.set(8, 8, EN[3])
    s.outline(pal[0])
    return s.image()


def wireless():
    """无线充电：扁充电台 + 台面感应线圈 + 三道扇形辐射弧。"""
    s = Sprite()
    pal = STL
    for r in (3, 5, 7):
        s.mask = set()
        s.ring(8, 10, r, r - 1)
        s.mask = {(x, y) for x, y in s.mask if y <= 9 and abs(x - 8) <= (10 - y) * 0.85}
        s.fill(EN)
    s.rrect(2, 10, 13, 12, 1)
    s.fill(pal)
    s.paint(2, 10, 13, 10, pal[4])
    s.paint(2, 12, 13, 12, pal[1])
    s.paint(6, 11, 10, 11, DK)
    s.paint(7, 11, 9, 11, EN[2])
    s.rect(5, 13, 10, 14)
    s.fill(pal)
    s.paint(6, 14, 9, 14, pal[1])
    s.outline(pal[0])
    return s.image()

def interface():
    """外部存储接口：左侧深色接口座（三根针脚）+ 右侧金属外壳 + 状态灯 + 出线管。"""
    s = Sprite()
    pal = STL
    s.rrect(6, 2, 11, 11, 1)
    s.fill(pal)
    s.bevel(pal)
    s.rect(2, 3, 5, 10)
    s.fill(RUB)
    s.paint(2, 3, 5, 3, RUB[0])
    s.paint(2, 3, 2, 10, RUB[0])
    s.paint(2, 10, 5, 10, RUB[3])
    for y in (5, 7, 9):
        s.paint(3, y, 4, y, pal[4])
    s.paint(8, 4, 9, 5, EN[2])
    s.set(8, 4, EN[4])
    s.rect(12, 6, 13, 8)
    s.fill(RUB)
    s.paint(12, 6, 13, 6, RUB[3])
    s.outline(pal[0])
    return s.image()


def robot_icon():
    """成品机器人：头 + 胸甲 + 履带底座。"""
    s = Sprite()
    pal = STL
    s.set(8, 1, EN[4])
    s.rect(7, 2, 8, 2)
    s.fill(pal)
    s.rrect(4, 3, 11, 7, 1)
    s.fill(pal)
    s.paint(5, 5, 10, 6, EN[2])
    s.paint(5, 5, 10, 5, EN[4])
    s.rrect(3, 8, 12, 11, 1)
    s.fill(pal)
    s.paint(6, 9, 9, 10, DK)
    s.set(7, 9, EN[2])
    s.rrect(2, 12, 13, 14, 1)
    s.fill(RUB)
    s.set(4, 13, RUB[0]); s.set(8, 13, RUB[0]); s.set(11, 13, RUB[0])
    s.outline(pal[0])
    return s.image()


def controller():
    """操控台：手持终端，左侧屏幕 + 右侧摇杆按键 + 顶部天线。"""
    s = Sprite()
    pal = STL
    s.rect(11, 1, 11, 3)
    s.fill(pal)
    s.set(11, 1, EN[4])
    s.rrect(2, 4, 13, 14, 1)
    s.fill(pal)
    s.paint(2, 4, 13, 4, pal[4])
    s.paint(2, 14, 13, 14, pal[1])
    s.rect(3, 7, 8, 12)
    s.fill(((10, 16, 24),) * 5)
    for x in (5, 7):
        s.paint(x, 8, x, 11, EN[0])
    for y in (9, 11):
        s.paint(4, y, 8, y, EN[0])
    s.set(5, 9, EN[4]); s.set(6, 9, EN[2])
    s.paint(3, 5, 8, 6, pal[2])
    s.set(4, 5, EN[3]); s.set(6, 5, EN[3]); s.set(8, 5, EN[1])
    s.mask = set()
    s.disc(11, 11, 2)
    s.fill(pal)
    s.set(11, 11, DK)
    s.set(10, 10, pal[4])
    s.set(10, 7, EN[2]); s.set(12, 7, EN[2])
    s.paint(10, 8, 12, 8, pal[1])
    s.outline(pal[0])
    return s.image()

# ============ 出图 ============
def torso(pal):
    """身躯：宽肩胸甲 + 肩/臂接口缝 + 核心插槽 + 腰部与腿座。"""
    s = Sprite()
    s.rect(6, 1, 9, 2)
    s.fill(pal)
    s.poly([(2, 3), (13, 3), (11, 11), (4, 11)])
    s.fill(pal)
    for y in range(3, 7):
        s.set(4, y, pal[0]); s.set(11, y, pal[0])
    s.paint(2, 6, 4, 6, pal[0]); s.paint(11, 6, 13, 6, pal[0])
    s.paint(2, 3, 13, 3, pal[4])
    s.paint(5, 4, 10, 4, pal[3])
    for y in (8, 9):
        s.paint(9, y, 10, y, pal[1])
    s.rect(6, 7, 9, 10)
    s.fill(((10, 13, 17),) * 5)
    s.mask = set()
    s.rect(5, 6, 10, 11)
    ringpx = [(x, y) for x, y in s.mask if not (6 <= x <= 9 and 7 <= y <= 10)]
    s.mask = set(ringpx)
    s.fill(EN)
    s.paint(7, 8, 8, 9, EN[1])
    s.set(7, 8, EN[3])
    s.paint(4, 11, 11, 11, pal[0])
    s.rect(5, 12, 10, 13)
    s.fill(pal)
    s.paint(7, 12, 8, 13, pal[0])
    s.rrect(4, 14, 6, 14, 0)
    s.fill(pal)
    s.rrect(9, 14, 11, 14, 0)
    s.fill(pal)
    s.outline(pal[0])
    return s.image()


TIERED = {
    'torso': torso, 'wheels': track, 'propeller': thruster,
    'mechanical_arm': arm, 'sensor': sensor, 'engine': engine,
    'storage_box': storage, 'battery': battery, 'shield_part': shield,
}
BASE_ONLY = {
    'wheels': track, 'propeller': thruster, 'crawling_limbs': limbs,
    'mechanical_arm': arm, 'engine': engine, 'storage_box': storage,
    'battery': battery, 'shield_part': shield,
}
UNIQUE = {'robot_core': core, 'teleporter': teleporter,
          'wireless_charging': wireless, 'robot': robot_icon, 'robot_controller': controller,
          'external_storage_interface': interface}


def build():
    out = {}
    for t, pal in TIERS.items():
        for part, fn in TIERED.items():
            if fn is None:
                continue
            out['%s_%s' % (t, part)] = fn(pal)
        out['%s_torso' % t] = torso(pal)
    for part, fn in BASE_ONLY.items():
        out[part] = fn(STL)
    for name, fn in UNIQUE.items():
        out[name] = fn()
    return out


def sheet(names, fn, rows_cols, pad=10, scale=8, label=12):
    from PIL import ImageDraw, ImageFont
    cols = rows_cols
    rows = (len(names) + cols - 1) // cols
    cell = 16 * scale
    im = Image.new('RGBA', (cols * (cell + pad) + pad, rows * (cell + 22) + pad), (24, 31, 41, 255))
    d = ImageDraw.Draw(im)
    f = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', label)
    for i, n in enumerate(names):
        c, r = i % cols, i // cols
        x, y = pad + c * (cell + pad), pad + r * (cell + 22)
        d.rectangle([x, y, x + cell + 1, y + cell + 1], fill=(110, 114, 120, 255))
        d.rectangle([x - 2, y - 2, x + cell + 3, y + cell + 3], outline=(94, 114, 140, 255))
        im.alpha_composite(fn(n).resize((cell, cell), Image.NEAREST), (x + 1, y + 1))
        d.text((x, y + cell + 5), n, font=f, fill=(198, 211, 223, 255))
    return im


if __name__ == '__main__':
    out = build()
    for name, im in out.items():
        im.save(name + '.png')
    print('generated', len(out), 'textures')
    sheet(sorted(out), lambda n: out[n], 6).save('all_items_preview.png')
    # 按部件分行、材质分列，方便核对色阶一致性
    order = ['torso', 'wheels', 'propeller', 'mechanical_arm',
             'sensor', 'engine', 'storage_box', 'battery', 'shield_part']
    grid = [('%s_%s' % (t, p)) for p in order for t in TIERS]
    sheet(grid, lambda n: out[n], 6).save('tiers_preview.png')
    print('sheets written')
