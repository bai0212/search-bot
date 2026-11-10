# 方块贴图（美术侧）

| 文件 | 装到 | 说明 |
|------|------|------|
| `gen_block_textures.py` | — | 生成器。跑一次出图 + `assembler_in_world.png` 立方预览；`--apply` 装进 src（旧图自动进 `_应用备份/`） |
| `robot_assembler.png` | `textures/block/` | 装配台**侧面 + 底面**：机柜（上盖 / 观察窗 / 两侧散热栅 / 底座） |
| `robot_assembler_top.png` | `textures/block/` | 装配台**顶面**：铸件外框 + 切角工作台 + 深色八角舱 + 四只夹爪 + 蓝色躯干工位 |
| `assembler_in_world.png` | 只看 | 2:1 等轴立方预览，按 MC 面亮度顶 1.0 / 南 0.8 / 东 0.6 压暗 |
| `assembler_variants.py` / `assembler_variants.png` | 只看 | 当初的 5 个方案对比（A 十字导轨 / B 四爪夹具 / C 分层工位 / D 四爪工装 / E 法兰舱口），留档说明为什么走 D 的思路 |
| `charging_station.png`、`creative_charging_station.png`、`locator.png` | `textures/block/` | 历史文件，那三个方块有自己的模型 |

## 装配台为什么是两张图

原来是 `parent: minecraft:block/cube_all`，六面共用一张图 —— 不管从哪个角度看都是
同心方框，这就是「太简单、不像真机器」的根因。现在拆成：

```
models/block/robot_assembler.json
  parent: minecraft:block/cube
  up    -> robot_assembler_top   （工位台面）
  其余  -> robot_assembler       （机柜侧面，底面也用这张）
```

这只是**贴图绑定**，没有建模、没有 Java 改动。改之前的 cube_all 版本存在
`_模型备份/robot_assembler.cube_all.json`，拷回去就能退。

## 对称规矩

- **顶面**：方块没有朝向，转 90° 也得一样 → 结构元素一律走 `S.quad()`
  （只写左上象限，另外三面自动镜像）。
- **侧面**：绕方块水平转一圈要一致 → 走 `S.mirror()`（只左右镜像）；
  但机器有天有地，所以侧面按行分上盖 / 柜体 / 底座，不做上下镜像。
- 1px 倒角仍按原版金属方块「光从左上」：上/左亮、下/右暗。这会让左右差 8 个像素，
  是有意的，不是 bug。

## 16px 的层级上限

一面上最多同时读出 **三层**（机壳 / 工作台 / 舱）。试过加第四层（进料槽 + 槽壁反光 +
舱口四边倒角），出来是一片划痕，就是被毙掉的方案 A。任何不连着机壳或舱壁的小方块
都会被读成「脏点」，所以要么连上去，要么删掉。

## 改完怎么验

```bash
python gen_block_textures.py            # 看 assembler_in_world.png
python assembler_variants.py            # 想试新方案时，加一个 variant_*() 再跑
python gen_block_textures.py --apply    # 满意了才装
```
不要靠进 Minecraft 试错。
