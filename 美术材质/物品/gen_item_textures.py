"""搜索机器人 —— 缺失物品贴图生成器（16x16 像素风）。

统一走「同一套打光 + 同一套描边」，保证和已有的机械臂/电池/储物箱是一个家族：
  · 光源在左上，每个形状按投影分 亮/中/暗 三档
  · 1px 深色描边（取该材质最暗色）
  · 荧光蓝 = 机器人面罩同款能量色（96,196,238）

产出：
  robot_core.png   机器人核心（八角外壳 + 能量晶体）
  wheels.png       履带（原车轮样式改成履带）
  propeller.png    推进器（原螺旋桨样式改成喷口）
  teleporter.png   传送装置（环形传送门 + 三颗轨道节点）
  <tier>_torso.png 机器人身躯 x6（铜/铁/金/钻石/下界合金/下界之星）
"""
from PIL import Image

W = H = 16

# ---- 材质色阶：O 描边 / D 暗 / M 中 / L 亮 / W 高光 ----
TIERS = {
    'copper':    ((58, 32, 16), (122, 68, 34), (176, 104, 52), (214, 142, 80), (242, 196, 136)),
    'iron':      ((48, 52, 58), (108, 116, 126), (158, 168, 180), (200, 210, 222), (238, 244, 250)),
    'gold':      ((74, 52, 10), (150, 110, 20), (212, 164, 36), (246, 208, 86), (255, 242, 176)),
    'diamond':   ((14, 58, 66), (46, 132, 146), (86, 196, 210), (160, 236, 246), (228, 252, 255)),
    'netherite': ((24, 20, 22), (58, 50, 54), (96, 86, 90), (136, 124, 128), (182, 170, 172)),
    'ultimate':  ((44, 22, 74), (104, 62, 158), (158, 116, 220), (210, 182, 255), (255, 255, 255)),
}
# 能量蓝（面罩同款）与中性橡胶/钢
EN = ((20, 60, 84), (44, 116, 148), (96, 196, 238), (176, 234, 252), (240, 253, 255))
RUB = ((18, 21, 26), (38, 43, 50), (58, 65, 74), (86, 95, 106), (120, 130, 142))
STL = ((34, 38, 44), (74, 82, 92), (116, 126, 138), (160, 172, 186), (206, 214, 224))
# 履带橡胶：比 RUB 提亮一档，否则在灰色物品栏底板上会糊成一团黑
TRK = ((14, 16, 20), (44, 50, 58), (78, 86, 96), (116, 126, 138), (156, 166, 178))

LIGHT = (-0.62, -0.78)


class Sprite:
    def __init__(self):
        self.px = [[(0, 0, 0, 0) for _ in range(W)] for _ in range(H)]
        self.mask = set()

    def set(self, x, y, c):
        if 0 <= x < W and 0 <= y < H:
            self.px[int(y)][int(x)] = (c[0], c[1], c[2], 255)

    def get(self, x, y):
        return self.px[int(y)][int(x)]

    def add(self, x, y):
        if 0 <= x < W and 0 <= y < H:
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
                if d <= r_out * r_out + 0.35 and d >= r_in * r_in - 0.35:
                    self.add(x, y)

    def poly(self, pts):
        ys = [p[1] for p in pts]
        for y in range(int(min(ys)), int(max(ys)) + 1):
            xs = []
            for i in range(len(pts)):
                ax, ay = pts[i]
                bx, by = pts[(i + 1) % len(pts)]
                if (ay <= y <= by) or (by <= y <= ay):
                    if by != ay:
                        xs.append(ax + (y - ay) * (bx - ax) / (by - ay))
                    else:
                        xs += [ax, bx]
            if xs:
                for x in range(int(round(min(xs))), int(round(max(xs))) + 1):
                    self.add(x, y)

    def shade(self, pal):
        """按左上光源把当前 mask 分成 亮/中/暗 三档。"""
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

    def outline(self, color, where=None):
        """给已上色像素补 1px 描边；where 限定只描哪些颜色的边。"""
        src = [[self.px[y][x] for x in range(W)] for y in range(H)]
        for y in range(H):
            for x in range(W):
                if src[y][x][3] == 0:
                    continue
                if where is not None and src[y][x][:3] not in where:
                    continue
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if not (0 <= nx < W and 0 <= ny < H):
                        continue
                    if src[ny][nx][3] == 0:
                        self.set(nx, ny, color)
                        break

    def paint(self, x0, y0, x1, y1, c):
        for y in range(int(y0), int(y1) + 1):
            for x in range(int(x0), int(x1) + 1):
                self.set(x, y, c)

    def image(self):
        im = Image.new('RGBA', (W, H))
        for y in range(H):
            for x in range(W):
                im.putpixel((x, y), self.px[y][x])
        return im


# ==================== 机器人核心 ====================
def robot_core():
    s = Sprite()
    pal, en = STL, EN
    # 八角外壳
    s.rect(2, 3, 13, 12)
    for i in range(2):
        s.mask.discard((2 + i, 3))
        s.mask.discard((13 - i, 3))
        s.mask.discard((2 + i, 12))
        s.mask.discard((13 - i, 12))
    s.shade(pal)
    # 上下接线柱
    s.rect(6, 1, 9, 2)
    s.shade(pal)
    s.rect(6, 13, 9, 14)
    s.shade(pal)
    # 内凹的能量舱
    s.paint(4, 5, 11, 10, en[0])
    # 六边形晶体（菱形近似）
    s.poly([(8, 4), (11, 7.5), (8, 11), (5, 7.5)])
    s.shade(en)
    s.paint(7, 7, 8, 8, en[4])
    # 四角铆钉
    for x, y in ((3, 4), (12, 4), (3, 11), (12, 11)):
        s.set(x, y, pal[4])
    s.outline(pal[0])
    return s.image()


# ==================== 履带 ====================
def track(frame=STL):
    s = Sprite()
    # 履带环：压扁的横向闭环（转角压小，否则读成护目镜）
    s.rrect(0, 4, 15, 11, 2)
    outer = set(s.mask)
    s.mask = set()
    s.rrect(3, 6, 12, 9, 1)
    hole = set(s.mask)
    s.mask = outer - hole
    s.shade(TRK)
    # 外侧导向齿：上下各 5 组 2px 高的凸齿，履带最强的识别特征
    for x in (3, 5, 7, 9, 11):
        for yy in (2, 3):
            s.set(x, yy, TRK[3]); s.set(x + 1, yy, TRK[2])
        for yy in (12, 13):
            s.set(x, yy, TRK[1]); s.set(x + 1, yy, TRK[1])
    # 环带接缝
    for x in (1, 4, 7, 10, 13):
        s.set(x, 5, TRK[0]); s.set(x, 10, TRK[0])
    # 轮架底板（先把内部垫暗，避免透出背景）
    for x, y in sorted(hole):
        s.set(x, y, (26, 30, 36, 255))
    # 前后负重轮：材质色轮 + 暗轮毂，中间留 2px 轴隙
    for cx in (4, 11):
        s.mask = set()
        s.disc(cx, 7, 2)
        s.mask &= hole
        s.shade(frame)
        s.set(cx, 7, frame[0])
        s.set(cx - 1, 6, frame[4])
        s.set(cx + 1, 8, frame[1])
    s.paint(7, 6, 8, 9, (14, 16, 20, 255))
    s.paint(7, 7, 8, 7, frame[2])
    s.outline(TRK[0])
    return s.image()


# ==================== 推进器 ====================
def thruster(pal=STL):
    s = Sprite()
    en = EN
    # 横置侧视：竖向的「上窄下宽」在 16px 下必然读成高脚杯
    # 左侧安装板
    s.rect(0, 3, 1, 12)
    s.shade(pal)
    s.set(0, 5, pal[0]); s.set(0, 10, pal[0])
    s.paint(1, 3, 1, 12, pal[3])
    # 燃烧室 + 纵向散热环
    s.rect(2, 4, 7, 11)
    s.shade(pal)
    for x in (3, 5):
        s.paint(x, 4, x, 11, pal[1])
    s.paint(6, 4, 7, 4, pal[4])
    # 喉部
    s.rect(7, 6, 8, 9)
    s.shade(pal)
    # 钟形喷管：向右张开
    s.poly([(8, 6), (8, 9), (14, 13), (14, 2)])
    s.shade(pal)
    # 喷管加强环
    for x in (10, 12):
        y0 = int(round(6 - (x - 8) * 2 / 3.0))
        y1 = int(round(9 + (x - 8) * 4 / 6.0))
        s.paint(x, y0, x, y1, pal[1])
    # 出口：暗内壁 -> 中心炽蓝
    for y in range(2, 14):
        d = abs(y - 7.5)
        s.set(14, y, en[4] if d < 1.5 else en[2] if d < 3 else en[1] if d < 4.5 else en[0])
    # 喷焰
    s.rect(15, 5, 15, 10)
    s.shade(en)
    s.set(15, 7, en[4]); s.set(15, 8, en[4])
    s.set(15, 5, en[0]); s.set(15, 10, en[0])
    s.outline(pal[0])
    return s.image()


# ==================== 传送装置 ====================
def teleporter():
    s = Sprite()
    pal, en = STL, EN
    # 门框环：八角外框挖出方形门洞
    s.rrect(0, 1, 15, 14, 3)
    outer = set(s.mask)
    s.mask = set()
    s.rect(4, 4, 11, 11)
    hole = set(s.mask)
    s.mask = outer - hole
    s.shade(pal)
    # 上下横梁加厚 + 高光
    s.rect(5, 0, 10, 0)
    s.shade(pal)
    s.rect(5, 15, 10, 15)
    s.shade(pal)
    s.paint(6, 0, 9, 0, pal[4])
    s.paint(6, 15, 9, 15, pal[1])
    # 四角能量节点
    for x, y in ((2, 3), (13, 3), (2, 12), (13, 12)):
        s.mask = set()
        s.rect(x - 1, y - 1, x + 1, y + 1)
        s.shade(en)
        s.set(x, y, en[4])
    # 门洞：暗底，中间立一枚发光蓝菱形，读成能量场而不是一块冰
    s.paint(4, 4, 11, 11, (12, 20, 30, 255))
    s.poly([(8, 4), (11, 7), (8, 11), (5, 7)])
    s.shade(en)
    s.poly([(8, 6), (9, 7), (8, 9), (7, 7)])
    s.shade(en)
    s.paint(8, 7, 8, 8, en[4])
    # 门洞内侧压暗一圈，做出深度
    for x in range(4, 12):
        s.set(x, 4, en[0]); s.set(x, 11, en[0])
    for y in range(4, 12):
        s.set(4, y, en[0]); s.set(11, y, en[0])
    s.outline(pal[0])
    return s.image()


# ==================== 机器人身躯 ====================
def torso(tier):
    s = Sprite()
    pal = TIERS[tier]
    # 颈部安装座
    s.rect(6, 0, 9, 1)
    s.shade(pal)
    # 躯干主体：宽肩梯形向下收到腰，整体是一块胸甲
    s.poly([(1, 2), (14, 2), (12, 12), (3, 12)])
    s.shade(pal)
    # 肩/臂接口缝：只在肩部 5 行，读成可拆接口而不是奖杯把手
    for y in range(2, 7):
        s.set(4, y, pal[0])
        s.set(11, y, pal[0])
    s.paint(1, 6, 4, 6, pal[0])
    s.paint(11, 6, 14, 6, pal[0])
    s.paint(1, 2, 3, 2, pal[4])
    s.paint(12, 2, 14, 2, pal[2])
    # 胸甲分缝与散热格栅
    s.paint(5, 3, 10, 3, pal[4])
    for y in (8, 9, 10):
        s.paint(10, y, 11, y, pal[1])
    # 核心插槽：暗孔 + 一圈能量环
    s.paint(6, 7, 9, 10, (10, 13, 17, 255))
    for x in (5, 10):
        for y in (7, 8, 9):
            s.set(x, y, EN[0])
    for y in (6, 10):
        for x in (6, 7, 8, 9):
            s.set(x, y, EN[0])
    s.paint(7, 8, 8, 9, EN[1])
    s.set(7, 8, EN[2])
    # 腰部装甲 + 腿部安装座
    s.paint(3, 12, 12, 12, pal[0])
    s.rect(5, 13, 10, 14)
    s.shade(pal)
    s.paint(7, 13, 8, 14, pal[0])
    s.rect(4, 15, 6, 15)
    s.shade(pal)
    s.rect(9, 15, 11, 15)
    s.shade(pal)
    s.outline(pal[0])
    return s.image()


if __name__ == '__main__':
    out = {
        'robot_core': robot_core(),
        'wheels': track(),
        'propeller': thruster(),
        'teleporter': teleporter(),
    }
    for t, pal in TIERS.items():
        out[t + '_torso'] = torso(t)
        out[t + '_wheels'] = track(pal)
        out[t + '_propeller'] = thruster(pal)
    for name, im in out.items():
        im.save(name + '.png')
        print('ok', name)

    # 预览表
    S = 8
    names = list(out)
    cols = 6
    rows = (len(names) + cols - 1) // cols
    from PIL import ImageDraw, ImageFont
    sheet = Image.new('RGBA', (cols * (16 * S + 10), rows * (16 * S + 26)), (24, 31, 41, 255))
    d = ImageDraw.Draw(sheet)
    f = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 12)
    for i, n in enumerate(names):
        c, r = i % cols, i // cols
        x, y = 10 + c * (16 * S + 10), 10 + r * (16 * S + 26)
        # 物品栏槽位底色，按游戏里的真实对比度来判断
        d.rectangle([x, y, x + 16 * S + 1, y + 16 * S + 1], fill=(110, 114, 120, 255))
        d.rectangle([x - 2, y - 2, x + 16 * S + 3, y + 16 * S + 3], outline=(94, 114, 140, 255))
        sheet.alpha_composite(out[n].resize((16 * S, 16 * S), Image.NEAREST), (x + 1, y + 1))
        d.text((x, y + 16 * S + 6), n, font=f, fill=(198, 211, 223, 255))
    sheet.save('items_new_preview.png')
    print('items_new_preview.png', sheet.size)
