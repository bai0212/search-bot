# -*- coding: utf-8 -*-
"""把 gen_items_full.py 生成的 16x16 物品贴图装进模组资源目录。

用法：
    python apply_item_textures.py            # 应用（先备份被改动的旧图）
    python apply_item_textures.py --check    # 只报告差异，不写文件
    python apply_item_textures.py --dry-run  # 打印将要做什么
"""
import hashlib
import os
import shutil
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
PROJECT = os.path.abspath(os.path.join(HERE, '..', '..'))
DEST = os.path.join(PROJECT, 'src', 'main', 'resources',
                    'assets', 'search_bot', 'textures', 'item')
BACKUP = os.path.join(HERE, '_应用备份')


def textures():
    """只认生成器自己会产出的文件名。

    目录里还有 all_items_preview.png、symmetry_check.png 这类大图，
    靠前缀黑名单挡不住（换个名字就漏进来），所以直接问 gen_items_full。
    """
    import gen_items_full as G
    known = set(G.build())
    return sorted(f for f in os.listdir(HERE)
                  if f.endswith('.png') and f[:-4] in known)


def md5(path):
    with open(path, 'rb') as fh:
        return hashlib.md5(fh.read()).hexdigest()


def border_ok(path):
    from PIL import Image
    im = Image.open(path).convert('RGBA')
    if im.size != (16, 16):
        return 'size %sx%s' % im.size
    px = im.load()
    hit = [(x, y) for x in range(16) for y in range(16)
           if (x in (0, 15) or y in (0, 15)) and px[x, y][3] != 0]
    return 'border px %d' % len(hit) if hit else None


def main():
    args = set(sys.argv[1:])
    check_only = '--check' in args
    if not os.path.isdir(DEST):
        sys.exit('找不到目标目录：%s' % DEST)

    changed, added, same, bad = [], [], [], []
    for name in textures():
        src = os.path.join(HERE, name)
        problem = border_ok(src)
        if problem:
            bad.append('%s: %s' % (name, problem))
        dst = os.path.join(DEST, name)
        if not os.path.exists(dst):
            added.append(name)
        elif md5(src) != md5(dst):
            changed.append(name)
        else:
            same.append(name)

    print('贴图总数 %d ｜ 新增 %d ｜ 覆盖 %d ｜ 已一致 %d'
          % (len(textures()), len(added), len(changed), len(same)))
    for b in bad:
        print('  留白违规 ->', b)
    if check_only or not (added or changed):
        print('无需应用。' if not (added or changed) else '（--check 模式）')
        return 1 if bad else 0
    if '--dry-run' in args:
        for n in added + changed:
            print('  会写入', os.path.join(DEST, n))
        return 0

    os.makedirs(BACKUP, exist_ok=True)
    for n in changed:
        shutil.copy2(os.path.join(DEST, n), os.path.join(BACKUP, n))
    for n in added + changed:
        shutil.copy2(os.path.join(HERE, n), os.path.join(DEST, n))
    print('已应用 %d 张；旧图备份在 %s' % (len(added) + len(changed), BACKUP))
    return 1 if bad else 0


if __name__ == '__main__':
    sys.exit(main())
