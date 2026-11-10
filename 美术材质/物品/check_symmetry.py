"""左右对称检查：沿中央轴镜像后比对，列出不对称的像素团。

用法：
    python check_symmetry.py               # 检查全部生成结果
    python check_symmetry.py shield_part   # 只检查名字里含 shield_part 的

判定口径：
  - 只看已经着色的像素（alpha>0），透明留白不参与。
  - 默认允许「光照不对称」：护盾/盾面这类正面朝向的部件应当完全对称，
    所以给它们设了 SYMMETRIC 名单，名单内 0 容差。
  - 输出 mirror-diff 计数和大图，方便肉眼看是哪一处。
"""
import sys
from PIL import Image, ImageDraw

import gen_items_full as G

# 这些部件在物品栏里是「正对着玩家」的板状物，左右必须一模一样。
# 其余部件用 fill() 的左上光源做体积感，左右本来就该有明暗差，只做参考。
SYMMETRIC = ('shield',)
# 中央轴：14x14 有效区（1..14）的对称轴是 x = (1+14)/2 = 7.5
AXIS_MIRROR = lambda x: 15 - x


def mirror_diff(im):
    """返回 (不对称像素数, [(x, y, 左色, 右色), ...])，只看有效区。"""
    px = im.load()
    hot = []
    for y in range(G.CLIP[1], G.CLIP[3] + 1):
        for x in range(G.CLIP[0], 8):          # 只扫左半边，右半边自动配对
            mx = AXIS_MIRROR(x)
            a, b = px[x, y], px[mx, y]
            if a[3] == 0 and b[3] == 0:
                continue
            if a[:3] != b[:3] or (a[3] > 0) != (b[3] > 0):
                hot.append((x, y, a, b))
    return len(hot), hot


def zoom(im, scale=16):
    big = im.resize((16 * scale, 16 * scale), Image.NEAREST)
    return big


def annotate(im, hot, scale=16):
    """左：原图；右：把不对称点标红的大图，两图之间镜像对齐。"""
    big = zoom(im, scale).convert('RGBA')
    d = ImageDraw.Draw(big)
    for x, y, a, b in hot:
        for cx in (x, AXIS_MIRROR(x)):
            d.rectangle([cx * scale + 2, y * scale + 2,
                         (cx + 1) * scale - 2, (y + 1) * scale - 2],
                        outline=(255, 60, 60, 255), width=2)
    # 中央轴
    d.line([(7.5 * scale, 0), (7.5 * scale, 16 * scale)],
           fill=(255, 255, 0, 255), width=1)
    return big


def main():
    out = G.build()
    filt = sys.argv[1] if len(sys.argv) > 1 else None
    targets = {n: im for n, im in out.items() if filt is None or filt in n}
    bad = []
    for n in sorted(targets):
        cnt, hot = mirror_diff(targets[n])
        strict = any(k in n for k in SYMMETRIC)
        flag = '必对称' if strict else '可不对称'
        print('%-34s diff=%-4d %s%s' % (n, cnt, flag, '  <-- 违规' if strict and cnt else ''))
        if strict and cnt:
            bad.append((n, cnt, hot))
    if not bad:
        print('\n对称名单内没有违规。')
        return
    tiles = [annotate(im, hot) for im, _, hot in
             [(targets[n], c, h) for n, c, h in bad]]
    w = tiles[0].width
    sheet = Image.new('RGBA', (w * len(tiles), tiles[0].height), (24, 31, 41, 255))
    for i, t in enumerate(tiles):
        sheet.alpha_composite(t, (i * w, 0))
    sheet.save('symmetry_check.png')
    print('\n违规：%s ｜ 标红图 -> symmetry_check.png'
          % ', '.join('%s(%d)' % (n, c) for n, c, _ in bad))


if __name__ == '__main__':
    main()
