"""GUI 风格切换器（美术侧）。

用法：
    python apply_gui_style.py list
    python apply_gui_style.py apply 原版风
    python apply_gui_style.py apply 科技风 --with-theme   # 连 TechGuiTheme.java 一起换

只动两类文件：
  1) 三张 GUI 贴图（纯美术，默认就换）；
  2) TechGuiTheme.java（代码，必须显式加 --with-theme 才动，且会先备份）。
每次切换前把当前生效的文件快照到 _切换备份/<时间戳>/，随时能退回去。
"""
import hashlib
import json
import os
import shutil
import sys
import time

HERE = os.path.dirname(os.path.abspath(__file__))
INDEX = os.path.join(HERE, 'gui_styles.json')


def md5(path):
    if not os.path.exists(path):
        return None
    with open(path, 'rb') as f:
        return hashlib.md5(f.read()).hexdigest()


def load():
    with open(INDEX, encoding='utf-8') as f:
        return json.load(f)


def snapshot(files, ts):
    """把即将被覆盖的文件存进 _切换备份/<ts>/，返回备份目录。"""
    dst = os.path.join(HERE, '_切换备份', ts)
    os.makedirs(dst, exist_ok=True)
    kept = 0
    for src in files:
        if os.path.exists(src):
            shutil.copy2(src, os.path.join(dst, os.path.basename(src)))
            kept += 1
    return dst, kept


def do_list(cfg):
    print('当前生效：%s（%s）' % (cfg['current'], cfg['installed_at']))
    for name, st in cfg['styles'].items():
        mark = '*' if name == cfg['current'] else ' '
        print('%s %-6s %s' % (mark, name, st['label']))
        print('    目录: 风格库/%s   生成脚本: %s' % (st['dir'], st.get('generator', '-')))
        for key, fn in st['textures'].items():
            p = os.path.join(HERE, st['dir'], fn)
            print('    %-11s %s  %s' % (key, fn, '缺文件' if not os.path.exists(p) else md5(p)[:8]))


def do_apply(cfg, name, with_theme, dry):
    if name not in cfg['styles']:
        raise SystemExit('没有这个风格：%s（可选：%s）' % (name, ' / '.join(cfg['styles'])))
    st = cfg['styles'][name]
    sdir = os.path.join(HERE, st['dir'])
    targets = []
    plan = []
    for key, fn in st['textures'].items():
        src = os.path.join(sdir, fn)
        if not os.path.exists(src):
            raise SystemExit('风格库里缺贴图：%s' % src)
        dst = os.path.join(cfg['target_dir'], cfg['textures'][key])
        plan.append((src, dst))
        targets.append(dst)
    if with_theme:
        src = os.path.join(sdir, st['theme'])
        if not os.path.exists(src):
            raise SystemExit('风格库里缺主题代码：%s' % src)
        plan.append((src, cfg['theme_target']))
        targets.append(cfg['theme_target'])
        print('将替换代码文件 TechGuiTheme.java（--with-theme）：这是画法/配色，坐标不变。')
    else:
        theme_src = os.path.join(sdir, st['theme'])
        if os.path.exists(theme_src) and md5(theme_src) == md5(cfg['theme_target']):
            print('src 里的 TechGuiTheme.java 已经就是这套风格的画法，只换贴图即可。')
        else:
            print('提示：只换贴图，但 src 里的 TechGuiTheme.java 不是这套风格的画法，'
                  '会出现「浅灰面板叠深色科技槽」这类混搭。要完整换风格请加 --with-theme。')

    same = [s for s, d in plan if md5(s) == md5(d)]
    if len(same) == len(plan):
        print('目标文件与当前一致，无需切换。')
        return
    ts = time.strftime('%Y%m%d-%H%M%S')
    bdir, kept = snapshot(targets, ts)
    print('备份 %d 个文件 -> %s' % (kept, bdir))
    for src, dst in plan:
        if md5(src) == md5(dst):
            print('  跳过（已一致）%s' % os.path.basename(dst))
            continue
        if dry:
            print('  [dry-run] %s -> %s' % (os.path.basename(src), dst))
            continue
        shutil.copy2(src, dst)
        print('  安装 %-26s -> %s' % (os.path.basename(src), os.path.relpath(dst, HERE)))
    if dry:
        return
    for other in cfg['styles']:
        cfg['styles'][other]['in_game'] = (other == name)
    cfg['current'] = name
    cfg['installed_at'] = time.strftime('%Y-%m-%d %H:%M')
    with open(INDEX, 'w', encoding='utf-8') as f:
        json.dump(cfg, f, ensure_ascii=False, indent=2)
    print('已切到「%s」。回退：%s' % (name, os.path.join(bdir, '')))


if __name__ == '__main__':
    cfg = load()
    args = [a for a in sys.argv[1:]]
    if not args or args[0] == 'list':
        do_list(cfg)
    elif args[0] == 'apply':
        if len(args) < 2:
            raise SystemExit('apply 后面要跟风格名：%s' % ' / '.join(cfg['styles']))
        do_apply(cfg, args[1], '--with-theme' in args, '--dry-run' in args)
    else:
        raise SystemExit('用法：apply_gui_style.py [list | apply <风格名> [--with-theme] [--dry-run]]')
