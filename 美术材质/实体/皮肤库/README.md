# 机器人皮肤库

一套「皮肤」= **实体贴图 + 配套模型类 + 配套渲染器变换**，三样必须一起换，
所以别只往 `src/main/resources/.../textures/entity/search_robot.png` 里塞图。

```
python apply_skin.py                    # 看有哪些皮肤、资源包里现在装的是哪套
python apply_skin.py 01_boxuv           # 应用（换图 + 换模型类）
python apply_skin.py 01_boxuv --fix-renderer   # 连渲染器变换一起改
```

每次会改动资源包之前，先把当前 贴图/模型/渲染器 快照到 `_切换备份/<时间戳>/`，
换坏了直接从那里拷回来。`skins.json` 是机器读的索引（尺寸、单位/格、要哪种渲染器变换、
生成脚本在哪），改皮肤就改它。

## 各套皮肤

| 名字 | 贴图 | 模型 | 能进游戏 | 说明 |
| --- | --- | --- | --- | --- |
| `01_boxuv` | 128×64 | 01（用户建模） | ✅ | **当前用的这套**。UV 岛按 MC `ModelPart.Cube` 的浮点 box-UV 公式排，按面纯色，只有头部观察口是蓝的。渲染器走原版翻转 `scale(-1,-1,1)+translate(0,-1.501,0)`。 |
| `hd_256` | 256×256 | HD 重建（我建的） | ✅ | 32 单位/格，渲染时 `scale(0.5)`、不翻转。像素密度高，画得开屏幕细节，但几何不是 01。 |
| `01_旧版任意UV` | 128×64 | — | ❌ | 01 第一次上色的原图，UV 是逐面自由排的。MC 实体模型只能表达 box-UV，套进游戏会串色，**只能在 Blockbench 里当配色参考**。 |
| `占位_64x32` | 64×32 | — | ❌ | 7 月那个纯色占位图，整张一个颜色，套任何模型都是一坨灰。 |

## 源文件在哪

- 01 几何：`D:\Blockbench\mcp\01.bbmodel`（用户自己建的），上色规则见
  `D:\Blockbench\mcp\robot_materials.py`（材质表，按立方体包围盒登记）。
- 重排 + 出图 + 改 Java：`D:\Blockbench\mcp\apply_01.py`，产物
  `D:\Blockbench\mcp\01_applied.bbmodel`（每面 UV 与 MC 一致，Blockbench 里看到的就是游戏效果）
  和 `01_search_robot_atlas.png`。改了 01 的几何或材质表，重跑这个脚本再 `apply_skin.py 01_boxuv`。
- 离线预览（不开 Blockbench）：`python D:\Blockbench\mcp\render_01_preview.py <bbmodel> <贴图> <输出png>`。
  注意它不还原组的旋转，所以手臂是直的，只看配色和有没有漏涂。
- HD 那套：`build_robot_hd.py` / `paint_robot_hd.py` / `gen_java_hd.py`。
