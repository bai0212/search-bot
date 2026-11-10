# -*- coding: utf-8 -*-
"""
机器人皮肤切换器。皮肤 = 一张实体贴图 + 配套模型类 + 配套渲染器变换，三样是一套，
所以这里一起换，换之前把当前状态快照到 _切换备份/。

    python apply_skin.py                 # 列出所有皮肤 + 当前资源包里装的是哪套
    python apply_skin.py 01_boxuv        # 应用
    python apply_skin.py 01_boxuv --fix-renderer   # 连渲染器变换一起改
"""
import datetime
import hashlib
import json
import os
import shutil
import sys

LIB = os.path.dirname(os.path.abspath(__file__))
PROJ = os.path.abspath(os.path.join(LIB, '..', '..', '..'))
IDX = json.load(open(os.path.join(LIB, 'skins.json'), encoding='utf-8'))

TEX = os.path.join(PROJ, IDX['resource_texture'])
MODEL = os.path.join(PROJ, IDX['model_java'])
RENDERER = os.path.join(PROJ, IDX['renderer_java'])

# 渲染器里那段「模型空间 -> 世界空间」的变换，两套皮肤要求各不一样
MARK_START = 'poseStack.pushPose();'
MARK_END = 'float netHeadYaw ='
RENDERERS = {
    'vanilla_flip': '''        poseStack.pushPose();

        // 原版约定：模型空间 y 轴朝下、脚底在 y=24，所以先翻转再抬 1.501
        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));
        poseStack.scale(-1.0f, -1.0f, 1.0f);
        poseStack.translate(0.0f, -1.501f, 0.0f);

        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float netHeadYaw = headYaw - bodyYaw;''',
    'no_flip_half_scale': '''        poseStack.pushPose();

        // HD 模型按 32 单位 = 1 格建（脚底 y=0、+y 向上），缩到 0.5 才是真实体型
        poseStack.scale(0.5f, 0.5f, 0.5f);

        float bodyYaw = Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot);
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0f - bodyYaw));

        float headYaw = Mth.rotLerp(partialTick, entity.yHeadRotO, entity.yHeadRot);
        float netHeadYaw = bodyYaw - headYaw;''',
}
DETECT = {'scale(-1.0f, -1.0f, 1.0f)': 'vanilla_flip',
          'scale(0.5f, 0.5f, 0.5f)': 'no_flip_half_scale'}


def md5(path):
    return hashlib.md5(open(path, 'rb').read()).hexdigest()[:8] if os.path.exists(path) else '--------'


def live_renderer():
    if not os.path.exists(RENDERER):
        return None
    src = open(RENDERER, encoding='utf-8').read()
    for needle, name in DETECT.items():
        if needle in src:
            return name
    return 'unknown'


def identify_installed():
    """按 md5 认出资源包里现在装的是哪张图，认不出就说 unknown（并给出尺寸）。"""
    if not os.path.exists(TEX):
        return '（资源包里没有实体贴图）'
    for name, sk in IDX['skins'].items():
        p = os.path.join(LIB, sk['png'].replace('/', os.sep))
        if os.path.exists(p) and md5(p) == md5(TEX):
            return name
    from PIL import Image
    w, h = Image.open(TEX).size
    return 'unknown（%dx%d，不在皮肤库里）' % (w, h)


def snapshot():
    stamp = datetime.datetime.now().strftime('%Y%m%d_%H%M%S')
    dst = os.path.join(LIB, '_切换备份', stamp)
    os.makedirs(dst, exist_ok=True)
    for p in (TEX, MODEL, RENDERER):
        if os.path.exists(p):
            shutil.copy(p, os.path.join(dst, os.path.basename(p)))
    return dst


def apply(key, fix_renderer=False):
    sk = IDX['skins'][key]
    if not sk.get('in_game'):
        print('!! %s 标了 in_game=false，套进游戏会串色/糊成一片，只是留档用的' % key)
    src = os.path.join(LIB, sk['png'].replace('/', os.sep))
    if md5(src) == md5(TEX):
        print('贴图已经是 %s' % key)
    else:
        bak = snapshot()
        os.makedirs(os.path.dirname(TEX), exist_ok=True)
        shutil.copy(src, TEX)
        print('贴图 -> %s（旧状态快照在 %s）' % (os.path.relpath(TEX, PROJ),
                                                os.path.relpath(bak, PROJ)))
        if sk['model']:
            shutil.copy(os.path.join(LIB, sk['model'].replace('/', os.sep)), MODEL)
            print('模型类 -> %s' % os.path.relpath(MODEL, PROJ))
    want = sk['renderer']
    if want:
        now = live_renderer()
        if now == want:
            print('渲染器变换已经是 %s ✓' % want)
        elif fix_renderer:
            src_txt = open(RENDERER, encoding='utf-8').read()
            i = src_txt.index(MARK_START)
            j = src_txt.index(MARK_END) + len(MARK_END)
            line_end = src_txt.index('\n', j)
            open(RENDERER, 'w', encoding='utf-8', newline='\n').write(
                src_txt[:i] + RENDERERS[want] + src_txt[line_end:])
            print('渲染器变换改成 %s' % want)
        else:
            print('!! 渲染器变换现在是 %s，这套皮肤要 %s —— 加 --fix-renderer 一起改，'
                  '否则机器人会倒着长/朝向反' % (now, want))
    IDX['current'] = key
    json.dump(IDX, open(os.path.join(LIB, 'skins.json'), 'w', encoding='utf-8'),
              ensure_ascii=False, indent=2)


def main():
    args = [a for a in sys.argv[1:]]
    fix = '--fix-renderer' in args
    args = [a for a in args if not a.startswith('--')]
    if not args:
        print('资源包当前：贴图 %s / 模型 %s / 渲染器 %s\n'
              % (identify_installed(), md5(MODEL), live_renderer()))
        for name, sk in IDX['skins'].items():
            print('  %-14s %-10s %s' % (name, md5(os.path.join(LIB, sk['png'].replace('/', os.sep))),
                                         sk['label']))
        print('\n用法：python apply_skin.py <名字> [--fix-renderer]')
        return
    key = args[0]
    if key not in IDX['skins']:
        raise SystemExit('皮肤库里没有 %s' % key)
    apply(key, fix)
    print('装好后：贴图 %s / 模型 %s / 渲染器 %s'
          % (identify_installed(), md5(MODEL), live_renderer()))


if __name__ == '__main__':
    main()
