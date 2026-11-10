# 物品材质（16×16 / 有效区 14×14）

这里是所有物品贴图的**唯一权威来源**。改美术只改这里，再用脚本装进 `src`。

## 硬性规范：14×14 有效区

原版物品贴图是 16×16，但**最外一圈 1px 是留白**（透明），不参与绘制：

| 区域 | 像素范围 | 用途 |
|------|---------|------|
| 留白边框 | `0` 和 `15` 那一圈 | 必须全透明 |
| 描边 | `1..14` 最外一圈 | 深色轮廓 |
| 本体 | `2..13` | 实际形状 |

**Why:** 边框留白让物品在格子里有呼吸感，和原版物品并排时大小观感一致；描边若画到 `0/15` 就吃掉留白，物品会显得顶满格子、和原版不协调。

**How to apply:** `gen_items_full.py` 里 `Sprite.set()` / `Sprite.add()` 会把坐标夹到 `2..13`（`BODY_LO/BODY_HI`），`outline()` 拒绝写 `CLIP=(1,1,14,14)` 之外的像素。新增绘制函数直接依赖这两道闸，不要绕过 `set()` 裸写 `px[][]`。

## 工作流

```bash
cd 美术材质/物品
python gen_items_full.py        # 生成 68 张 PNG + 两张预览表
python apply_item_textures.py --check   # 看差异 + 查留白违规
python apply_item_textures.py    # 应用（旧图自动备份到 _应用备份/）
```

生成后先看 `all_items_preview.png`（全部）和 `tiers_preview.png`（同部件跨材质色阶），确认形状没被内收一格破坏，再应用。

## 文件

| 文件 | 作用 |
|------|------|
| `gen_items_full.py` | 现行生成器：6 档材质 × 9 部件 + 独立物品，含 `interface()`（外部存储接口） |
| `gen_item_textures.py` | 早期版本，已被上面取代，留着对照 |
| `apply_item_textures.py` | 一键应用 + 留白审计 + 自动备份 |
| `all_items_preview.png` / `tiers_preview.png` | 核对用预览表，不进 `src` |

## 已覆盖范围

`src/.../models/item/*.json` 里所有引用 `search_bot:item/*` 的物品都有贴图；
`charging_station` / `creative_charging_station` / `robot_assembler` 走 `parent: search_bot:block/*`，属于方块模型，不在这里。
