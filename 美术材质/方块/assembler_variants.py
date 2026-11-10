# -*- coding: utf-8 -*-
"""装配台方块贴图：三个「像真机器」的方案对比图。

cube_all 六面同图 => 图案必须四向对称，所以结构元素全部走 S.quad()
（只写左上象限，其余三面镜像）。

出图 assembler_variants.png：每个方案给「顶面 + 侧面」两种光照。
"""
from PIL import Image, ImageDraw, ImageFont

from gen_block_textures import (S, OUT, CASING_DK, CASING, PLATE, PLATE_HI,
                                PLATE_SH, BAY, BAY_DK, BAY_LT, METAL,
                                METAL_SH, GLASS, GLASS_DK)

F = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 15)


def frame(s):
    """公共底座：描边 + 机壳 + 机壳加工槽 + 银色面板（左上光源倒角）。"""
    s.ring(0, OUT)
    s.ring(1, CASING)
    s.ring(2, CASING_DK)
    s.rect(3, 3, 12, 12, PLATE)
    s.rect(3, 3, 12, 3, PLATE_HI)
    s.rect(3, 3, 3, 12, PLATE_HI)
    s.rect(3, 12, 12, 12, PLATE_SH)
    s.rect(12, 3, 12, 12, PLATE_SH)


def octagon(s, r, c):
    """以 7.5 为中心、切掉四角的正方形（比圆在 16px 上更干净）。"""
    lo, hi = 7 - r, 8 + r
    s.rect(lo + 1, lo, hi - 1, hi, c)
    s.rect(lo, lo + 1, hi, hi - 1, c)


def variant_a():
    """A 十字导轨：四条线性导轨从机壳伸进中央八角工位舱，舱底一块蓝色观测量窗。"""
    s = S()
    frame(s)
    # 四角支座：L 形加强角，读起来是框架而不是圆点
    s.quad(3, 4, 3, 6, CASING_DK)
    s.quad(4, 3, 6, 3, CASING_DK)
    # 导轨两侧的机加工槽
    s.quad(6, 3, 6, 5, BAY_DK)
    s.quad(9, 3, 9, 5, BAY_DK)
    s.quad(3, 6, 5, 6, BAY_DK)
    s.quad(3, 9, 5, 9, BAY_DK)
    # 四条导轨本体
    s.quad(7, 3, 8, 5, METAL)
    s.quad(3, 7, 5, 8, METAL)
    # 中央八角工位舱
    octagon(s, 3, BAY)
    octagon(s, 2, BAY_DK)
    # 舱内的银色压板 + 蓝色量窗
    s.rect(6, 6, 9, 9, METAL)
    s.rect(6, 9, 9, 9, METAL_SH)
    s.rect(9, 6, 9, 9, METAL_SH)
    s.rect(7, 7, 8, 8, GLASS)
    return s.image()


def variant_b():
    """B 四爪夹具：中央圆舱，四只夹爪从舱口咬进来，四角是通风栅。"""
    s = S()
    frame(s)
    # 四角通风栅（3 条 1x3 暗槽，镜像成四角）
    for i in (0, 2, 4):
        s.quad(4 + i, 4, 4 + i, 5, BAY_DK)
        s.quad(4, 4 + i, 5, 4 + i, BAY_DK)
    # 中央圆舱
    octagon(s, 3, BAY)
    s.rect(6, 6, 9, 9, BAY_DK)
    # 四只夹爪：从面板伸进舱内 2 格
    s.quad(6, 3, 9, 5, METAL)
    s.quad(6, 6, 9, 6, METAL_SH)
    s.rect(7, 5, 8, 5, METAL_SH)
    s.rect(7, 10, 8, 10, METAL_SH)
    s.rect(5, 7, 5, 8, METAL_SH)
    s.rect(10, 7, 10, 8, METAL_SH)
    # 爪心夹住的蓝色工件
    s.rect(7, 7, 8, 8, GLASS)
    s.rect(7, 8, 8, 8, GLASS_DK)
    return s.image()


def variant_c():
    """C 分层工位：外圈机壳 + 方形深舱 + 舱内十字分隔成四个零件格，中心量窗。"""
    s = S()
    frame(s)
    # 面板上的四段环形走线槽
    s.quad(4, 4, 11, 4, BAY_DK)
    s.quad(4, 4, 4, 11, BAY_DK)
    s.quad(4, 11, 11, 11, BAY_LT)
    s.quad(11, 4, 11, 11, BAY_LT)
    # 深舱
    s.rect(5, 5, 10, 10, BAY)
    s.rect(5, 5, 10, 5, BAY_DK)
    s.rect(5, 5, 5, 10, BAY_DK)
    s.rect(5, 10, 10, 10, BAY_LT)
    s.rect(10, 5, 10, 10, BAY_LT)
    # 舱内十字分隔（四个零件格）
    s.rect(7, 5, 8, 10, METAL)
    s.rect(5, 7, 10, 8, METAL)
    s.rect(7, 9, 8, 10, METAL_SH)
    s.rect(9, 7, 10, 8, METAL_SH)
    # 中心观测窗
    s.rect(6, 6, 9, 9, GLASS)
    s.rect(6, 9, 9, 9, GLASS_DK)
    s.rect(9, 6, 9, 9, GLASS_DK)
    return s.image()


def base(s):
    """公共底座：描边 + 机壳两圈 + 银色面板（左上光源倒角）+ 面板切角。"""
    s.ring(0, OUT)
    s.ring(1, CASING_DK)
    s.ring(2, CASING)
    s.rect(3, 3, 12, 12, PLATE)
    s.rect(3, 3, 12, 3, PLATE_HI)
    s.rect(3, 3, 3, 12, PLATE_HI)
    s.rect(3, 12, 12, 12, PLATE_SH)
    s.rect(12, 3, 12, 12, PLATE_SH)
    s.quad(3, 3, 4, 4, CASING)          # 面板四角切掉，和机壳铸成一体


def variant_d():
    """D 四爪装配工装：切角面板 + 八角深舱 + 四只从舱口长出来的夹爪咬住中心工件。

    规矩：任何元素都必须连着机壳或舱壁，悬空的小方块在 16px 上就是「脏点」；
    夹爪只占 2x2，舱底四角留暗，才有「凹舱」的层次。
    """
    s = S()
    base(s)
    octagon(s, 2, BAY)                  # 深舱 5..10，四角切掉
    s.quad(6, 5, 9, 5, BAY_DK)          # 舱口上/左沿压暗 = 凹进去
    s.quad(5, 6, 5, 9, BAY_DK)
    s.quad(6, 10, 9, 10, BAY_LT)        # 下/右沿提亮 = 舱壁反光
    s.quad(10, 6, 10, 9, BAY_LT)
    s.quad(7, 5, 8, 6, METAL)           # 上/下夹爪
    s.quad(5, 7, 6, 8, METAL)           # 左/右夹爪
    s.quad(7, 6, 8, 6, METAL_SH)        # 爪尖暗边 = 夹口
    s.quad(6, 7, 6, 8, METAL_SH)
    s.rect(7, 7, 8, 8, GLASS)           # 四爪咬住的中心工件（观测窗蓝）
    s.rect(7, 8, 8, 8, GLASS_DK)
    return s.image()


def variant_e():
    """E 法兰舱口：切角面板 + 一圈银色法兰 + 四个连着法兰的卡耳 + 4x4 蓝色工位窗。"""
    s = S()
    base(s)
    octagon(s, 3, BAY)                  # 大舱 4..11
    s.quad(5, 4, 10, 4, BAY_DK)
    s.quad(4, 5, 4, 10, BAY_DK)
    octagon(s, 2, METAL)                # 银色法兰圈
    octagon(s, 1, BAY)                  # 法兰内的工作凹槽
    s.quad(7, 3, 8, 4, METAL_SH)        # 四个卡耳：连着法兰，跨到面板上
    s.quad(3, 7, 4, 8, METAL_SH)
    s.rect(6, 6, 9, 9, GLASS)           # 4x4 工位窗
    s.rect(6, 9, 9, 9, GLASS_DK)
    s.rect(9, 6, 9, 9, GLASS_DK)
    s.rect(6, 6, 9, 6, METAL)           # 工件压圈上/左亮边
    s.rect(6, 6, 6, 9, METAL)
    return s.image()


def shade(im, k):
    """模拟 MC 面亮度：顶面 1.0、侧面 0.8。"""
    return Image.eval(im, lambda v: min(255, int(v * k))) if k != 1.0 else im.copy()


def main():
    variants = [('A 十字导轨', variant_a()), ('B 四爪夹具', variant_b()),
                ('C 分层工位', variant_c()), ('D 四爪工装', variant_d()),
                ('E 法兰舱口', variant_e())]
    z = 12
    pad, cell = 18, 16 * z
    im = Image.new('RGBA', (pad + len(variants) * (cell + pad),
                            pad + 2 * (cell + 26) + 30), (24, 31, 41, 255))
    d = ImageDraw.Draw(im)
    for c, (name, tex) in enumerate(variants):
        for r, (label, k) in enumerate((('顶面', 1.0), ('侧面 0.8', 0.8))):
            x = pad + c * (cell + pad)
            y = 30 + r * (cell + 26)
            d.text((x, y - 18), '%s · %s' % (name, label), font=F,
                   fill=(198, 211, 223, 255))
            big = shade(tex, k).resize((cell, cell), Image.NEAREST)
            d.rectangle([x - 3, y - 3, x + cell + 3, y + cell + 3],
                        outline=(94, 114, 140, 255))
            im.alpha_composite(big, (x, y))
    im.save('assembler_variants.png')
    print('written assembler_variants.png（方案图，重跑本脚本即可再出）')


if __name__ == '__main__':
    main()
