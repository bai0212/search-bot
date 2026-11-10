# -*- coding: utf-8 -*-
"""装配台方块贴图（16x16，顶面和侧面分开）。

配色取自机器人实体贴图 美术材质/实体/search_robot.png：冷调中银 + 观测窗蓝。

一张图贴六面（cube_all）不管从哪个角度看都是同心方框，不像机器，
所以拆成两张：
  robot_assembler.png       侧面 + 底面 = 机柜（上盖 / 观察窗 / 散热栅 / 底座）
  robot_assembler_top.png   顶面 = 装配工位（外框 / 进料槽 / 四爪夹舱）
侧面绕方块转一圈要一致 => 左右镜像（S.mirror）；顶面四个朝向都要一致 =>
四向镜像（S.quad）。倒角仍按原版金属方块「光从左上」。

    python gen_block_textures.py            # 出图 + assembler_in_world.png 立方预览
    python gen_block_textures.py --apply    # 装进 src 资源目录（旧图自动备份）
"""
from PIL import Image, ImageDraw
import os

# ==== 从机器人贴图取色，按「机壳 < 面板 < 夹爪」拉开层级 ====
OUT = (30, 35, 40, 255)          # 最外描边
CASING_DK = (58, 66, 74, 255)    # 机壳暗
CASING = (94, 104, 115, 255)     # 机壳中
PLATE = (140, 151, 163, 255)     # 面板主色
PLATE_HI = (176, 186, 197, 255)  # 机器人主银 #B0BAC5，面板上/左亮边
PLATE_SH = (110, 120, 131, 255)  # 面板下/右暗边
BAY = (46, 53, 60, 255)          # 装配舱底
BAY_DK = (26, 30, 35, 255)       # 凹陷上/左（凹进去所以是暗的）
BAY_LT = (86, 95, 105, 255)      # 凹陷下/右
METAL = (206, 215, 226, 255)     # 夹爪/插座，接近机器人高光 #CED9E7
METAL_SH = (150, 161, 173, 255)  # 夹爪/插座下/右
GLASS = (96, 196, 238, 255)      # 观测窗亮蓝 #60C4EE
GLASS_DK = (84, 172, 209, 255)   # 观测窗暗蓝 #54ACD1


class S(object):
    def __init__(self):
        self.px = [[(0, 0, 0, 0)] * 16 for _ in range(16)]

    def set(self, x, y, c):
        if 0 <= x < 16 and 0 <= y < 16:
            self.px[y][x] = c

    def rect(self, x0, y0, x1, y1, c):
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)

    def quad(self, x0, y0, x1, y1, c):
        """只画左上象限，另外三面自动镜像。

        cube_all 六面同图，图案必须四向对称；手写坐标很容易漏一面，
        所以机器结构类的元素一律走这里，画一块就是四块。
        """
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                for a, b in ((x, y), (15 - x, y), (x, 15 - y), (15 - x, 15 - y)):
                    self.set(a, b, c)

    def ring(self, inset, c):
        """从外数第 inset 圈（0=最外圈）画成 c。"""
        a, b = inset, 15 - inset
        for i in range(a, b + 1):
            self.set(i, a, c)
            self.set(i, b, c)
            self.set(a, i, c)
            self.set(b, i, c)

    def mirror(self, x0, y0, x1, y1, c):
        """只左右镜像（不上下翻）。侧面贴图绕方块转一圈要看着一致，
        但机器有「天/地」方向，所以侧面用这个，顶面用 quad()。"""
        for y in range(y0, y1 + 1):
            for x in range(x0, x1 + 1):
                self.set(x, y, c)
                self.set(15 - x, y, c)

    def octagon(self, r, c):
        """以 7.5 为中心、切掉四个 1px 角的正方形：比圆在 16px 上干净，且四向对称。"""
        lo, hi = 7 - r, 8 + r
        self.rect(lo + 1, lo, hi - 1, hi, c)
        self.rect(lo, lo + 1, hi, hi - 1, c)

    def image(self):
        im = Image.new('RGBA', (16, 16))
        im.putdata([p for row in self.px for p in row])
        return im


def assembler_top():
    """顶面 = 装配工位：铸件外框 + 切角工作台 + 深色八角舱 + 四只夹爪咬住中心躯干位。

    顶面绕方块转圈都要一致，结构元素全走 quad()（写左上象限，四向镜像）。
    16px 上能同时读出来的层级只有三层：机壳 / 工作台 / 舱，
    再多一档（进料槽、槽壁反光、舱口四边倒角）就会糊成一片划痕。
    """
    s = S()
    s.ring(0, OUT)
    s.ring(1, CASING_DK)                 # 铸件外沿
    s.ring(2, CASING)                    # 内一档台阶
    s.rect(3, 3, 12, 12, PLATE)          # 工作台面板
    s.rect(3, 3, 12, 3, PLATE_HI)
    s.rect(3, 3, 3, 12, PLATE_HI)
    s.rect(3, 12, 12, 12, PLATE_SH)
    s.rect(12, 3, 12, 12, PLATE_SH)
    s.quad(3, 3, 4, 4, CASING)           # 四角切掉，和机壳铸成一体
    s.octagon(2, BAY_DK)                 # 中央深舱 5..10（切角）
    s.quad(7, 5, 8, 6, METAL)            # 四只夹爪，从舱口咬进来
    s.quad(5, 7, 6, 8, METAL)
    s.quad(7, 6, 8, 6, METAL_SH)         # 爪尖暗边 = 夹口
    s.quad(6, 7, 6, 8, METAL_SH)
    s.rect(7, 7, 8, 8, GLASS)            # 四爪中间的躯干工位（观测窗蓝）
    s.rect(7, 8, 8, 8, GLASS_DK)
    return s.image()


def assembler_side():
    """侧面 = 机柜：上盖、观察窗、两侧散热栅、底座。有天地方向，所以按行画。"""
    s = S()
    s.ring(0, OUT)
    s.rect(1, 1, 14, 2, CASING_DK)       # 上盖
    s.rect(1, 3, 14, 3, PLATE_HI)        # 上盖下沿的亮边
    s.rect(1, 4, 14, 12, CASING)         # 柜体
    s.rect(1, 4, 1, 12, CASING_DK)       # 左立柱
    s.rect(14, 4, 14, 12, CASING_DK)     # 右立柱
    s.mirror(3, 6, 4, 6, BAY_DK)         # 散热栅：三格一组的暗槽
    s.mirror(3, 8, 4, 8, BAY_DK)
    s.mirror(3, 10, 4, 10, BAY_DK)
    s.mirror(3, 7, 4, 7, BAY_LT)         # 栅条之间的亮棱
    s.mirror(3, 9, 4, 9, BAY_LT)
    s.rect(5, 5, 10, 10, BAY_DK)         # 观察窗凹座
    s.rect(5, 5, 10, 5, METAL)           # 窗框上/左亮
    s.rect(5, 5, 5, 10, METAL)
    s.rect(5, 10, 10, 10, METAL_SH)      # 窗台（下沿）
    s.rect(10, 5, 10, 10, METAL_SH)
    s.rect(6, 6, 9, 9, GLASS)            # 窗内，和机器人头部同色
    s.rect(6, 9, 9, 9, GLASS_DK)
    s.rect(9, 6, 9, 9, GLASS_DK)
    s.rect(1, 13, 14, 13, PLATE_SH)      # 底座上沿
    s.rect(1, 14, 14, 14, CASING_DK)     # 底座
    return s.image()


def cube_mock(top_im, side_im, w=288):
    """把顶面/侧面贴图拼成 2:1 等轴立方，模拟方块在世界的样子。

    不用 PIL 的 QUAD 变换（它对源图尺寸的归一化方式和直觉不一致，出来是糊的），
    直接按等轴投影逐像素反解 UV。MC 面亮度：顶面 1.0、南北 0.8、东西 0.6。
    """
    q = w / 4.0
    tpx, spx = top_im.load(), side_im.load()
    out = Image.new('RGBA', (w, w), (24, 31, 41, 255))
    op = out.load()

    def sample(px, sx, sy, k):
        if not (0.0 <= sx < 1.0 and 0.0 <= sy < 1.0):
            return None
        r, g, b, a = px[min(15, int(sx * 16)), min(15, int(sy * 16))]
        return (int(r * k), int(g * k), int(b * k), a)

    for y in range(w):
        for x in range(w):
            c = None
            if y <= 2 * q:                                   # 顶面菱形
                u, v = (x - w / 2.0) / (w / 2.0), y / q
                c = sample(tpx, (v + u) / 2.0, (v - u) / 2.0, 1.0)
            elif x <= w / 2.0:                               # 左面
                sx = x / (w / 2.0)
                c = sample(spx, sx, (y - q - sx * q) / (2 * q), 0.8)
            else:                                            # 右面
                sx = (x - w / 2.0) / (w / 2.0)
                c = sample(spx, sx, (y - 2 * q + sx * q) / (2 * q), 0.6)
            if c:
                op[x, y] = c
    d = ImageDraw.Draw(out)
    A, B, C = (w / 2.0, 0), (w - 1, q), (w / 2.0, 2 * q)
    E, F, G = (0, 3 * q), (w / 2.0, 4 * q), (w - 1, 3 * q)
    D = (0, q)
    for p1, p2 in ((A, B), (B, G), (G, F), (F, E), (E, D), (D, A),
                   (D, C), (C, B), (C, F)):        # 外轮廓 + 三条内棱
        d.line([p1, p2], fill=(12, 14, 18, 255), width=2)
    return out


HERE = os.path.dirname(os.path.abspath(__file__))
DEST = os.path.normpath(os.path.join(HERE, '..', '..',
                                     'src/main/resources/assets/search_bot/textures/block'))
BACKUP = os.path.join(HERE, '_应用备份')

if __name__ == '__main__':
    import shutil
    import sys

    top, side = assembler_top(), assembler_side()
    out = {'robot_assembler': side, 'robot_assembler_top': top}
    for name, im in out.items():
        im.save(os.path.join(HERE, name + '.png'))
        im.resize((256, 256), Image.NEAREST).save(
            os.path.join(HERE, 'preview_' + name + '.png'))
    cube_mock(top, side).resize((560, 560), Image.NEAREST).save(
        os.path.join(HERE, 'assembler_in_world.png'))
    print('generated', ', '.join(out), '+ assembler_in_world.png')

    if '--apply' in sys.argv:
        os.makedirs(DEST, exist_ok=True)
        os.makedirs(BACKUP, exist_ok=True)
        for name in out:
            dst = os.path.join(DEST, name + '.png')
            if os.path.exists(dst):
                shutil.copy2(dst, os.path.join(BACKUP, name + '.png'))
            shutil.copy2(os.path.join(HERE, name + '.png'), dst)
            print('  ->', dst)
