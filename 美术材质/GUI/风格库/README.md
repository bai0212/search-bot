# GUI 风格库（留档 + 一键切换）

和 `美术材质/实体/皮肤库/` 一个套路：**每套风格自带贴图 + 画法文件 + 生成脚本**，
换风格跑一条命令，旧文件自动快照，随时退回去。

```
风格库/
├── gui_styles.json          索引：current 记的是哪套、每套的说明和文件清单
├── apply_gui_style.py       切换器（list / apply <风格名> [--with-theme] [--dry-run]）
├── 科技风/                  三张 PNG + TechGuiTheme.java + 三个 Screen.java + 生成脚本
├── 原版风/                  ★ 当前生效。三张 PNG + TechGuiTheme.java + 生成脚本 + 效果图
│   ├── gen_gui_textures_vanilla.py   按当前代码布局出三张固定部件贴图
│   ├── mock_gui_current.py           逐笔复刻现在的 Java 画法，出「现状」效果图
│   ├── centered_target.py            出「主体居中」目标效果图（等代码同步）
│   ├── mock_gui_vanilla.py           早期版本的合成器，assembler_radial 还依赖它的画图原语
│   └── assembler_radial.py           装配台原子式布局效果图（布局已接入代码）
└── 精妙背包风/              深炭灰 + 平坦凹格 + 暖金描边，留档
```

## 三套风格

| 风格 | 观感 | 状态 |
|------|------|------|
| 原版风 | 原版箱子那套：`#C6C6C6` 面板、`#8B8B8B` 凹格、上左 `#373737` 下右 `#FFFFFF`、1px 黑描边、文字 `#404040`，没有发光装饰 | **当前生效**（2026-11-08 按 0.5.8+ 布局重出并装进 src） |
| 精妙背包风 | 深炭灰面板 + 无斜面平坦凹格 + 暖金描边，借鉴 Sophisticated Backpacks | 留档，可回退 |
| 科技风 | 科技银 + 荧光蓝、四角角标、分格电量条 | 留档，可回退 |

> 2026-11-08 修掉的一个坑：src 里只有装配台换成了原版贴图，操控台和背包仍是科技风那两张
> （灰面板叠深色科技槽）。现在三张都按当前代码布局重出，风格统一。

## 怎么切

```bash
cd 美术材质/GUI/风格库
python apply_gui_style.py list                        # 看有哪些风格、当前是哪套、md5
python apply_gui_style.py apply 精妙背包风 --with-theme  # 换风格（贴图 + 画法文件一起换）
```

- 默认只换 **贴图**。三套 Screen 的槽位/按钮都是 `TechGuiTheme` 画的，
  只换贴图会出现「浅灰面板上叠深色科技槽」的混搭 —— 换风格必须加 `--with-theme`。
- `--with-theme` 会覆盖 `src/.../client/screen/TechGuiTheme.java`。
  这个文件**只有配色和画法，没有任何坐标/交互逻辑**，几套的类名、方法签名完全一致，
  所以调用点一行都不用改。
- 每次覆盖前，被动的文件都会存进 `_切换备份/<时间戳>/`；`gui_styles.json` 里的 `current` 会更新。

## 改风格/改配色时的规矩

1. **坐标不动**：槽位、按钮、分区线的位置由 `menu/custom/*Menu.java` 和 `client/screen/*Screen.java`
   的常量决定，几套风格共用同一套坐标。要动坐标先写进 `design/gui-code-requirements.md`。
2. 改配色 → 同时改两处：`风格库/<风格>/gen_gui_textures_*.py` 的调色板 和
   `风格库/<风格>/TechGuiTheme.java` 的常量（贴图烘静态面板，代码画动态部分）。
3. 改完跑生成脚本重出 PNG，再跑 `mock_gui_current.py` 看现状效果图，
   最后 `apply_gui_style.py apply <风格> --with-theme` 装进 src。不要直接在 Minecraft 里试错。
4. 凹格（18×18 槽位底）**不烘进贴图**，由 `TechGuiTheme.slot()` 画：
   机器人背包的行数会随躯干等级变（1..10 行），烘死就要为每种行数出一张贴图。

## 贴图对应哪套代码坐标

`gen_gui_textures_vanilla.py` 顶部把坐标都抄成了常量，改 Java 时对着改：

| 贴图 | 来源 |
|------|------|
| `robot_assembler.png` | `RobotAssemblerMenu`（径向：躯干为核，内圈 r=40 功能组件、外圈 r=64 附件）+ `RobotAssemblerScreen`（拆解/组装按钮） |
| `robot_inventory.png` | `RobotInventoryMenu`（`bagTop=36`、过滤槽 `178,196 / y66..156`）+ `RobotInventoryScreen`（分页栏 `y16`、`PANEL_X=174`、按钮 `x178 w76`） |
| `robot_controller.png` | `RobotControllerScreen`（`ROW_TOP=24`、`ROW_H=24`、8 行、`BOTTOM_H=28`） |

## 还没生效的部分

主体居中的差量在 `design/gui-code-requirements.md` 的 **R5**：过滤槽列要在右侧面板里居中
（现在贴左边缘，右边空 42px）、分页栏和面板标题要居中压在它们管的格子上方、
当前页签应提亮而不是压暗、操控台高度应跟随频道数。
目标效果图：`原版风/gui_mock_centered_target.png`。
代码改完之前，贴图里的托盘框仍按老 x 烘，两边必须同一次改。
