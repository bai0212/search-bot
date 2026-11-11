# 搜索机器人模组 —— 项目总览与进度报告

> **生成日期**：2026-07-12  
> **当前版本**：v0.8.0（gradle 中 mod_version=0.8.0）  
> **目标版本**：v0.8.0 —— 6 项新需求（JEI 拖动标记 / 磁力半径6 / 堆叠卡6档 / 流体运输 / 存储9格每行+滚动条 / 卡槽左+顶部分页+右侧配置）  
> **MC 版本**：1.20.1 | **Forge**：47.4.10  
> **开发状态**：🟡 编码完成，待全面测试

---

## 1. 项目结构总览

```
search_bot/                              # 模组根目录
├── src/main/java/com/cheng/search_bot/
│   ├── SearchBotMod.java                # 主模组类
│   ├── client/
│   │   ├── ClientSetup.java
│   │   ├── screen/
│   │   │   ├── RobotAssemblerScreen.java    # 装配台 GUI
│   │   │   └── RobotInventoryScreen.java    # 机器人背包 GUI
│   │   └── renderer/entity/
│   │       └── SearchRobotRenderer.java     # 机器人实体渲染
│   ├── common/
│   │   └── CommonEventHandler.java          # 右键方块事件（放置机器人+绑定主存）
│   ├── server/
│   │   └── ServerEventHandler.java
│   ├── block/
│   │   ├── ModBlocks.java                   # 方块注册器
│   │   └── custom/
│   │       ├── RobotAssemblerBlock.java     # 装配台方块
│   │       ├── LocatorBlock.java            # 定位方块
│   │       └── ChargingStationBlock.java    # 充电站方块
│   ├── block/entity/
│   │   ├── ModBlockEntities.java            # BE 注册器
│   │   └── custom/
│   │       ├── RobotAssemblerBlockEntity.java   # 装配台逻辑（组装/拆解/电量继承）
│   │       └── ChargingStationBlockEntity.java  # 充电站逻辑（存入/充电/释放）
│   ├── entity/
│   │   ├── ModEntities.java                 # 实体注册器
│   │   └── custom/
│   │       └── SearchRobotEntity.java       # 机器人实体（状态机/AI/能量/背包）
│   ├── entity/ai/
│   │   ├── FollowOwnerGoal.java             # 跟随主人（支持 OUT_OF_POWER 减速）
│   │   ├── CollectItemsGoal.java            # 收集物品（3格交互半径，并发不阻塞）
│   │   ├── ReturnToStorageGoal.java         # 返航存储（探索模式）
│   │   └── ReturnToChargeGoal.java          # 低电量返航充电
│   ├── item/
│   │   ├── ModItems.java                    # 物品注册器（旧零件 + 新材料等级零件）
│   │   ├── ModCreativeTabs.java
│   │   └── custom/
│   │       ├── RobotItem.java               # 机器人物品（放置/继承NBT）
│   │       └── BatteryItem.java             # 电池零件（默认电量0，支持FE）
│   ├── menu/
│   │   ├── ModMenus.java
│   │   └── custom/
│   │       ├── RobotAssemblerMenu.java
│   │       └── RobotInventoryMenu.java
│   ├── network/
│   │   ├── ModNetworking.java
│   │   └── AssemblerActionPacket.java
│   ├── energy/
│   │   └── CustomEnergyStorage.java
│   └── util/
│       ├── LocatorUtil.java                 # 定位区域计算
│       └── RobotStats.java                  # 机器人属性计算
├── src/main/resources/assets/search_bot/    # 模型/材质/语言文件
├── design/
│   ├── v0.2.0-follow-and-charge.md          # v0.2.0 设计文档（完整方案）
│   ├── plan-v0.2.0.md                       # v0.2.0 实施计划（9个步骤）
│   └── parts-guide.md
├── changelogs/0.1.0.md ~ 0.1.5.md
├── 模组需求文档.md                          # 原始需求文档
└── versions/search_bot-0.1.0.jar ~ 0.1.5.jar
```

---

## 2. 已实现功能清单

### v0.1.x 已完成
- [x] **核心方块**：装配台、定位方块、充电站
- [x] **核心实体**：搜索机器人（PathfinderMob）
- [x] **零件系统**：9种零件 × 6种材料等级（铜/铁/金/钻石/下界合金/终极）
- [x] **装配台**：GUI 组装/拆解机器人，零件槽位验证，NBT 传递
- [x] **能量系统**：Forge Energy (FE/RF)，CustomEnergyStorage
- [x] **机器人 GUI**：右键打开背包，显示电量
- [x] **定位系统**：两个定位方块对角放置，框定探索区域
- [x] **返航存储**：探索模式下收集满后返回主存容器
- [x] **网络包**：装配台按钮动作同步

### v0.2.0 已编码实现（代码已写，待测试验证）
- [x] **Step 1**：电池零件默认电量=0；组装/拆解时电量正确传递
- [x] **Step 2**：机器人状态机（WORKING / RETURNING_TO_CHARGE / CHARGING / OUT_OF_POWER）
- [x] **Step 3**：FollowOwnerGoal 跟随距离 4.5f，OUT_OF_POWER 速度减半
- [x] **Step 4**：CollectItemsGoal 交互半径=3，并发取物不阻塞移动，过滤非 WORKING 状态
- [x] **Step 5**：ReturnToChargeGoal 低电量寻路到充电站
- [x] **Step 6**：ChargingStationBlockEntity 无 GUI、无限电量、单机器人舱位、自动极速充电、充满自弹
- [x] **Step 7**：ChargingStationBlock 右键放入机器人/可充电物品，空手取出物品，破坏时吐出机器人
- [x] **Step 8**：充电站渲染器（`ChargingStationRenderer.java`）—— 站内有机器人时在站顶渲染缩小机器人 (2026-10-27)
- [ ] **Step 9**：综合测试与旧存档兼容验证

---

## 3. 当前代码关键状态

### 3.1 SearchRobotEntity 状态机
- `RobotState` 枚举已定义（0=WORKING, 1=RETURNING_TO_CHARGE, 2=CHARGING, 3=OUT_OF_POWER）
- `tick()` 每 tick 调用 `updateRobotState()`：
  - 电量 ≤ 0 → OUT_OF_POWER
  - WORKING 且电量 < 20% max → 寻找最近充电站 → RETURNING_TO_CHARGE
  - OUT_OF_POWER 且电量 > 0 → WORKING
- 移动耗能：`getDeltaMovement().lengthSqr() > 0.001` 时每 tick 抽 1 FE
- 没电时速度强制设为 0.05f（极慢）

### 3.2 AI Goal 注册优先级（跟随模式）
1. `FloatGoal`
2. `ReturnToChargeGoal`
3. `FollowOwnerGoal`（速度 1.2，触发 4.5f，停止 1.5f）
4. `CollectItemsGoal`（不设置 MOVE flag，实现并发）
5. `WaterAvoidingRandomStrollGoal`
6. `LookAtPlayerGoal`
7. `RandomLookAroundGoal`

### 3.3 充电站逻辑
- `tick()`：保持自身能量满格（无限电量）
- 有机器人时：每 tick +5000 FE，最短驻留 40 tick
- `AutoEject` 逻辑：自动返航存入的机器人充满后自动弹出；手动放入的不自动弹出
- `ejectRobot()`：在充电站上方 y+1.0 生成实体，强制状态 WORKING，电量充满
- `onStationDestroyed()`：破坏时吐出机器人和槽内物品

### 3.4 装配台电量传递
- `assembleRobot()`：读取电池槽 `tag.getInt("energy")`，写入机器人 NBT `Energy`
- `disassembleRobot()`：读取机器人 `Energy`，写回电池零件 `energy`
- 储物箱同理继承 `Inventory`

---

## 4. 待办事项（TODO）

| 优先级 | 任务 | 状态 | 备注 |
|--------|------|------|------|
| P0 | 充电站渲染器 | ✅ 已完成 | `client/renderer/block/ChargingStationRenderer.java`，站内有机器人时在站顶渲染缩小模型 (2026-10-27) |
| P0 | 综合测试 v0.2.0 | 🔴 未开始 | 按 design/v0.2.0-follow-and-charge.md 第6节测试策略逐项验证 |
| P0 | 爬行肢爬墙功能 | ✅ 已完成 | v2：视线检测触发爬墙 + 2D距离防振荡 + 爬墙持续更久 (2026-07-16 R2) |
| P0 | 螺旋桨飞行功能 | ✅ 已完成 | FLYING_SPEED属性 + FlyingMoveControl + travel()飞行物理 (2026-07-16 R2) |
| P0 | 寻路系统重构 | ✅ 已完成 | 三模式(车轮2D/爬行2D+视线/飞行3D) + 平滑lerp + 滞后余量 (2026-07-16 R2) |
| P0 | 摔落伤害减免 | ✅ 已完成 | 螺旋桨免疫 + 爬行肢每个-1格 (2026-07-16 R2) |
| P0 | 静止时继续收集 | ✅ 已完成 | tickCollectItems() 仅 CHARGING 时暂停 (2026-07-16) |
| P1 | 旧存档兼容 | 🟡 待验证 | NBT 中无 RobotState 时默认 WORKING，无 Energy 时默认 0 |
| P1 | 配置类提取 | 🔴 未开始 | 将硬编码常量（交互半径、低电量阈值、充电速率）提取到 SearchBotConfig |
| P2 | 探索模式完善 | 🔴 未开始 | 定位区域内自动巡逻、大范围搜索 |
| P1 | 部件属性系统查表 | ✅ 已完成 | RobotStats 已重构为查表模式，装配台组装时计算 stats 并写入机器人 NBT |
| P1 | 新材料零件可用性 | ✅ 已完成 | 创造标签页显示全部54个新材料零件；装配台槽位验证支持新材料 |
| P2 | 充电站等级 | 🔴 未开始 | 当前无限电量，后续可分等级 |
| P2 | 机器人模型/动画 | 🔴 未开始 | 当前用正方体代替 |
| P3 | 白名单/黑名单 GUI | ✅ 已完成 | 机器人仓库右侧新增「物品过滤」面板（黑/白名单 + 忽略后缀 + 12 过滤槽）(2026-10-31) |
| P3 | 多台机器人排队 | 🔴 未开始 | 充电站单舱位，后续扩展 |
| P1 | 外部存储接口方案 | 🟡 已分离归档/待开发 | 代码已整体移出模组，归档到 `待开发/外部存储接口/`（源码备份 + README 恢复清单）+ `design/plan-external-storage.md`；后续按清单恢复（补材质、进游戏实测、原生 GUI 高风险、ME 箱子方向待拍板）(2026-11-07) |

---

## 5. 已知问题 / 风险点

1. ~~**爬行肢无法爬墙**~~：✅ 已修复 (2026-07-16 R2)。FollowOwnerGoal v2：视线检测触发爬墙 + 2D距离防振荡。
2. ~~**螺旋桨崩溃**~~：✅ 已修复 (2026-07-16 R2)。createAttributes() 添加 FLYING_SPEED。
3. ~~**螺旋桨无飞行能力**~~：✅ 已修复 (2026-07-16 R1)。FlyingMoveControl + travel() 飞行物理 + isNoGravity()。
4. ~~**静止时无法收集物品**~~：✅ 已修复 (2026-07-16 R1)。tickCollectItems() 改为仅 CHARGING 状态暂停。
5. **收集物品 tick 耗时**：`collectFromContainer()` 中循环遍历 slot 可能耗时较长，极端情况下可能阻塞 tick。需要测试确认。
6. ~~**充电站渲染器缺失**~~：✅ 已实现 (2026-10-27)。站内有机器人时在站顶渲染缩小机器人提示（方块本体不透明，机器人画在站顶而非内部）。
7. **OUT_OF_POWER 传送**：设计文档中提到飞行玩家甩丢机器人时尝试传送，代码中未实现（标记为可选）。
8. **BatteryItem 容量固定**：`BatteryItem` 内部 `BatteryEnergyProvider` 的容量硬编码为 10000，但不同材料电池应有不同容量。需要让 `BatteryItem` 支持按材料区分容量。

---

## 6. 进度报告

### 2026-11-11 —— 磁力卡性能优化 + 需求文档补全

**磁力卡性能**：原磁力卡每 tick 都做一次实体范围查询（`getEntitiesOfClass(ItemEntity.class, AABB)`），机器人多了会卡。新增 `magnetCooldownTick` 冷却，改为每 5 tick（0.25 秒）扫一次，与收集模块节奏对齐，肉眼无感、性能下降。

**需求文档**：`模组需求文档.md` 头部版本/日期更新、修正 4 处过时 checkbox、新增「## 13. 当前功能全貌」章节汇总当前全部功能。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 新增 `magnetCooldownTick` 字段，`tickMagnetPickup` 每 5 tick 才做一次范围查询（吸物半径仍固定 6 格） |
| `模组需求文档.md` | 头部更新（v0.8.0 / 2026-11-11）、修正 checkbox、新增第 13 节功能全貌 |

**验证状态**：
- [x] BUILD SUCCESSFUL（仅既有 ResourceLocation 弃用警告）
- [x] jar 归档 + 同步测试游戏 mods（403171 字节）
- [ ] 进游戏验证：磁力卡吸物仍正常、多机器人不再卡

---

### 2026-11-11 —— 修复机器人仓库 GUI 三个 bug（滚动条只能往下 / GUI 重叠 / 过滤卡标记复制）

**三个 bug 及根因**：
1. **滚动条只能往下不能往上**：`repositionStorageSlots` 重建槽位时用了「已经被滚过的 baseY」继续减偏移，滚动偏移逐次累加、且 `-scrollRow*SLOT` 恒为负 → 槽位只会向上漂、永远回不到顶部。
2. **GUI 重叠**：电量条文字和左侧「物品栏」标签挤在同一行重叠；GUI 贴图仍是旧 256×256，界面 320 宽导致 blit UV 越界。
3. **过滤卡标记不能标记还复制物品**：过滤槽是幽灵槽但没禁止拾取（`mayPickup` 仍为 true），标记可能被当作真实物品拿走。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `menu/custom/RobotInventoryMenu.java` | `TabSlot` 增加不可变 `originY`，`repositionStorageSlots` 始终按 originY 计算显示位，修复滚动偏移累加；`FilterSlot` 增加 `mayPickup=false` 禁拾取幽灵标记 |
| `client/screen/RobotInventoryScreen.java` | 电量条右对齐到储物区右缘（避免与「物品栏」文字重叠）；贴图 blit 宽度 clamp 到 256 防止 UV 越界 |

**验证状态**：
- [x] BUILD SUCCESSFUL（43s，仅既有 ResourceLocation 弃用警告）
- [x] jar 重新归档 + 同步测试游戏 mods（403116 字节，mods 里只保留 0.8.0 一个 jar）
- [ ] 进游戏验证：滚动条双向、GUI 无重叠、过滤卡标记正常且不复制

---

### 2026-11-10 —— 崩溃修复（高等级存储箱槽位越界）+ Git/GitHub 同步

**崩溃修复**：v0.8.0 存储箱扩到终极 117 格后，装配台合并背包仍硬编码 90 格 → `Slot 90 not in valid range` 崩溃。3 处修复 + 重新编译同步。

**Git 同步**：装 git 后从零搭版本管理并推到 GitHub（bai0212/search-bot）。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 组装合并 `ItemStackHandler(90)` → `Math.max(1, inventorySize)`、循环上限 `< inventorySize`；拆解读档后 `setSize` 扩容兜底 |
| `entity/custom/SearchRobotEntity.java` | `loadRobotData` 读档后若处理器 < `inventory_size` 则 `setSize` 扩容（救旧存档） |
| `.gitignore` | 补充排除构建产物/日志/备份 |
| （git 初始化） | `git init` + 首次提交 `4bf3518`（824 文件）+ 分支 main + 推送到 GitHub |

**验证状态**：
- [x] BUILD SUCCESSFUL（67s，仅既有 ResourceLocation 弃用警告）
- [x] jar 重新归档 + 同步测试游戏 mods（403036 字节）
- [x] git 仓库建立 + 推送到 GitHub（https://github.com/bai0212/search-bot）
- [ ] 进游戏验证：打开之前崩溃的机器人仓库界面不再崩

---

### 2026-11-10 —— v0.8.0（6 项新需求：JEI 拖动标记 / 磁力半径6 / 堆叠卡6档+NBT / 流体运输 / 存储9格每行+滚动条 / 卡槽左+顶部分页+右侧配置）

用户 6 项新需求全部落地，版本 0.7.0 → **0.8.0**。

**新增功能**：

| # | 需求 | 处理 |
|---|------|------|
| 1 | 所有标记兼容 JEI 拖动标记 | 新增 `compat/SearchBotJeiPlugin.java`（`@JeiPlugin` + `IGhostIngredientHandler`），JEI 拖物品到「标记物品」过滤槽即写入幽灵标记（走 `RobotFilterPacket`，不消耗物品）；`build.gradle` 加 JEI maven + compileOnly/runtimeOnly，`gradle.properties` 加 `jei_version=15.20.0.133` |
| 2 | 磁力卡范围固定半径 6 格 | `tickMagnetPickup()` 半径硬编码 6，不随传感器变化 |
| 3 | 堆叠卡带 NBT 也可提上限 + 分六档 | 6 张材质堆叠卡（铜128/铁256/金512/钻1024/下界合金2048/终极4096），NBT 一致才合并（`canItemStacksStack` 把关）；每槽一张、≤4 张生效 |
| 4 | 机器人运输流体 | `transferFluidFromContainerToTank` / `transferFluidFromTankToContainer` + `TransportGoal.doTransfer` 起点抽罐/终点注罐（需储罐卡，≤1000mB/次，同种不混罐） |
| 5 | 存储 9 格/行 + 滚动条 | `STORAGE_SLOTS` 铜36(4×9)/铁45(5×9)/金63(7×9)/钻81(9×9)/下界合金99(11×9)/终极117(13×9)；无储物箱=机械臂取物数；GUI 超 5 行滚动 |
| 6 | 卡槽左 + 顶部卡分页 + 右侧配置面板 | GUI 重构：左列卡槽、顶部只显示已装卡的分页、点击分页右侧显示对应功能面板 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `menu/custom/RobotInventoryMenu.java` | 整体重构：布局常量、`TabSlot`（滚动重建）、`CardSlot`/`FilterSlot`、卡分页数据、滚动条 `setScrollRow/scrollBy`、流体储罐数据 |
| `client/screen/RobotInventoryScreen.java` | 整体重构：320×241、卡分页栏、滚动条（拖拽/滚轮）、右侧配置面板（过滤/红石/堆叠/储罐/磁力/销毁）、过滤槽幽灵点击 |
| `entity/custom/SearchRobotEntity.java` | `getStackingLimit`（≤4 张取最高档）+ `getStackLimit`（可堆叠+NBT）、磁力半径 6、流体运输两方法、`openRobotInventory` 写红石模式+储罐流体 |
| `entity/ai/TransportGoal.java` | `doTransfer` 物品+流体都搬 |
| `item/ModItems.java` | 6 张堆叠卡（`STACKING_LIMITS`/`isStackingCard`/`getStackingLimit`）、`CARD_CATEGORIES`/`CARD_CATEGORY_NAMES`、`MAX_STACKING_CARDS=4` |
| `util/RobotStats.java` | `STORAGE_SLOTS`（4×9~13×9）+ `getBaseStorageSlots`（机械臂取物数） |
| `network/RobotRedstonePacket.java`（新） | 红石模式循环切换包 |
| `network/ModNetworking.java` | 注册 `RobotRedstonePacket` |
| `compat/SearchBotJeiPlugin.java`（新） | JEI 幽灵槽 handler |
| `build.gradle` / `gradle.properties` | JEI maven + 依赖 + `jei_version`；`mod_version` 0.7.0 → 0.8.0 |
| `lang/zh_cn.json` | 6 张堆叠卡中文名 |
| `models/item/stacking_card_*.json` ×6 + `textures/item/stacking_card_*.png` ×6 | 堆叠卡模型/材质（占位同底色） |

**验证状态**：
- [x] BUILD SUCCESSFUL（20s，仅既有 ResourceLocation 弃用警告）
- [x] jar 归档 `versions/search_bot-0.8.0.jar`（402941 字节）+ 同步测试游戏 mods（删旧 0.7.0）
- [ ] 进游戏验证：JEI 拖动标记到过滤槽
- [ ] 进游戏验证：磁力卡半径 6 格
- [ ] 进游戏验证：堆叠卡 6 档 + NBT 一致才合并
- [ ] 进游戏验证：储罐卡运输模式自动搬流体
- [ ] 进游戏验证：大箱子滚动条、新 GUI 布局（卡槽左/顶部分页/右侧配置）
- [ ] 6 张堆叠卡正式材质（等美术 AI 出图后 syncArtTextures）

---

### 2026-11-09 —— 11 项需求第二批（左键标记 + 6 张插件卡 + 信号控制器方块 + 文档归档）

用户 11 项需求的**剩余项**全部落地：需求 2（左键标记物品）、需求 6（插件卡系统）、需求 7（信号控制器方块），以及需求 9（文档归档）、需求 10（卡片/方块占位材质）。

**新增功能**：

| 项 | 处理 |
|----|------|
| 需求 2 左键标记 | 手持物品**左键一下**即把该物品标记进过滤名单（每样存 1 个作标记，重复/名单满提示），不再需要往 GUI 里放具体东西。`hurt()` 里加标记逻辑 + `addFilterItem()` |
| 需求 6 插件卡 | 新增 6 张插件卡（过滤卡/磁力卡/红石卡/销毁卡/储罐卡/堆叠卡），装在机器人仓库 GUI 右侧「插件卡」栏；插槽数由传感器等级决定（铜1…终极6，见 `RobotStats.SENSOR_CARD_SLOTS`）。功能：过滤卡启用黑白名单、磁力卡吸掉落物、红石卡配合信号控制器、销毁卡销毁被过滤物品、储罐卡获得 8 桶流体储罐（桶装/抽）、堆叠卡单格堆叠上限 1024 |
| 需求 7 信号控制器 | 新增信号控制器方块 + 物品：手持 shift 右键机器人绑定（存稳定 RobotId，拆装不失效），放下后每 tick 把红石电平推送给绑定机器人；红石卡三种模式（无/高信号暂停/低信号暂停），AI 目标按 `isRedstonePaused()` 暂停工作 |
| 需求 9 文档归档 | 更新 SESSION_SUMMARY + PROJECT_STATUS |
| 需求 10 材质 | 生成 6 张卡片 + 信号控制器方块**占位 PNG**（等美术 AI 出正式图后 syncArtTextures） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `item/custom/RobotCardItem.java`（新） | 插件卡物品（tooltip 中文说明） |
| `item/custom/SignalControllerItem.java`（新） | 信号控制器物品（tooltip 绑定状态） |
| `block/custom/SignalControllerBlock.java`（新） | 信号控制器方块（放置读 NBT 绑定、中键选取/破坏掉落保留绑定） |
| `block/entity/custom/SignalControllerBlockEntity.java`（新） | 每 tick 推送红石电平给绑定机器人（按 RobotId 搜 AABB 兜底） |
| `item/ModItems.java` | 注册 6 卡 + 信号控制器；`isCard`/`getCardType`；`CARD_IDS`/`CARD_DESCRIPTIONS` |
| `block/ModBlocks.java` | 注册信号控制器方块 |
| `block/entity/ModBlockEntities.java` | 注册信号控制器 BE |
| `item/ModCreativeTabs.java` | 创造标签加入 6 卡 + 信号控制器 |
| `util/RobotStats.java` | `SENSOR_CARD_SLOTS` 查表 + `getSensorCardSlots` |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 记录最高等级传感器材料 → `sensor_material` + `card_slots` 写入 stats |
| `entity/custom/SearchRobotEntity.java` | 插件卡背包/红石模式/流体储罐字段 + save/load；堆叠卡改 `getSlotLimit/getStackLimit`；过滤卡门控 `canPickupItem`；`shouldVoid` 销毁；磁力卡 `tickMagnetPickup`；储罐卡 `handleTankBucket`；红石 `isRedstonePaused`；`mobInteract` 绑定/切红石模式/桶交互；`hurt()` 左键标记 + `addFilterItem`；`openRobotInventory` 写 card_slots |
| `menu/custom/RobotInventoryMenu.java` | 插件卡槽位（`CardSlot` 只收卡）+ `cardData`/`getCardSlots` + quickMoveStack 卡区 |
| `client/screen/RobotInventoryScreen.java` | 插件卡面板标题 + imageHeight 计入卡槽 |
| `entity/ai/CollectGoal.java`、`CollectItemsGoal.java`、`TransportGoal.java` | `isRedstonePaused()` 暂停检查 |
| `lang/zh_cn.json` | 6 卡 + 信号控制器中文名 |
| `models/item/*.json`（6 卡 + 信号控制器）、`models/block/signal_controller.json`、`blockstates/signal_controller.json` | 模型/方块状态 |
| `textures/item/*_card.png`、`textures/block/signal_controller.png` | 占位 PNG |

**验证状态**：
- [x] BUILD SUCCESSFUL（26s，仅既有 ResourceLocation 弃用警告）
- [x] jar 归档 `versions/search_bot-0.7.0.jar` + 同步测试游戏 mods（覆盖旧 0.7.0）
- [ ] 进游戏验证：左键标记物品进过滤名单
- [ ] 进游戏验证：6 张插件卡在 GUI 卡槽装入、各自功能生效（吸物/过滤/销毁/储罐/堆叠/红石暂停）
- [ ] 进游戏验证：信号控制器绑定机器人 + 红石信号暂停工作
- [ ] 卡片/信号控制器正式材质（等美术 AI 出图后 syncArtTextures）

---

### 2026-11-09 —— 11 项需求第一批（默认模式/标记交互说明/探索取物/躯干防火/护盾=护甲/名称提示/材质同步修复）+ 升 0.7.0

用户发来 11 项需求清单，本会话完成**第一批 6 项可直接改代码的项**，剩余 3 项（左键标记、插件卡系统、信号控制器）留到下一会话。

| # | 需求 | 处理 |
|---|------|------|
| 1 | 放下默认跟随应与核心统一 | 核心记录上次模式：拆解存 `Mode` 进核心 NBT（`RobotMode`），组装读回 |
| 3 | 探索模式仍去已空容器附近 | 取物判定改为「容器有**可拿取**物品」（过黑白名单），已空/被过滤的容器不再去 |
| 4 | 下界合金躯干不应着火 | 躯干下界合金/终极 → `fireproof=true`；`hurt()` 火焰伤害返回 false、`fireImmune()` 返回 true |
| 5 | 护盾=护甲，终极比下界合金多点恒定减伤 | 删护盾血量/护甲类型，改「护盾减伤值」查表（铜1/铁2/金3/钻4/下界合金5/终极6）+ 躯干减伤相加 |
| 8 | 模式提示也体现在物品名称 | 机器人物品 `getName()` 追加 `" · 模式"`（带专属颜色） |

**顺带修复**：`syncArtTextures` 物品同步把 `美术材质/物品/_应用备份/`、`__pycache__/` 里同名 PNG 展平后撞名导致同步失败 → `build.gradle` 物品 copy 加 `exclude` 排除这两个目录（不删用户备份）。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `util/RobotStats.java` | `SHIELD_HEALTH`/`SHIELD_ARMOR_TYPE` → `SHIELD_DAMAGE_REDUCTION`（铜1…终极6），`getShieldHealth` → `getShieldDamageReduction` |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 拆解存 `RobotMode`、组装读回；`shieldHealth` → `shieldDamageReduction`；`damage_reduction`=躯干+护盾；`fireproof` 布尔 |
| `entity/custom/SearchRobotEntity.java` | `isFireproof()`/`fireImmune()`、`hurt()` 火焰减伤 0 + 恒定减伤；新增 `containerHasCollectibleItems()`（过 `canPickupItem` 过滤） |
| `entity/ai/CollectGoal.java` | `containerHasItems` → `containerHasCollectibleItems` |
| `entity/ai/CollectItemsGoal.java` | 同上 |
| `item/custom/RobotItem.java` | `getName()` 追加带色模式文字 |
| `build.gradle` | 物品 copy 排除 `_应用备份`/`__pycache__` |
| `gradle.properties` | `mod_version` 0.6.4 → 0.7.0 |

**验证状态**：
- [x] BUILD SUCCESSFUL（21s，仅既有 ResourceLocation 弃用警告）
- [x] jar 同步测试游戏 mods（`search_bot-0.7.0.jar` 364068 字节，删旧 0.6.4）
- [ ] 进游戏验证：拆解→组装后恢复上次模式
- [ ] 进游戏验证：下界合金/终极躯干火焰不伤
- [ ] 进游戏验证：护盾减伤数值、探索模式不再去空容器、物品名显示模式

---

### 2026-11-09 —— 代码清理（抽公共渲染校正 + 去掉魔法数字重复）

用户问「代码有没有框架、可读性、冗杂」，顺手做了一次无行为变化的清理：

| 清理点 | 改动 |
|--------|------|
| `-1.501` / `scale(-1,-1,1)` 机器人翻转约定在 3 个渲染器重复 | 抽到 `SearchRobotModel.FEET_OFFSET` + `SearchRobotModel.applyModelCorrection(PoseStack)`，实体/充电站/物品 3 处统一调用 |
| 充电站 `maxEnergy<=0→10000` 兜底重复 2 次 | 抽成 `ChargingStationBlockEntity.getRobotMaxEnergy()` 方法，`tick`/`ejectRobot` 共用 |

**验证状态**：
- [x] BUILD SUCCESSFUL（31s，仅既有 ResourceLocation deprecation 警告）
- [x] jar 同步到测试游戏 mods（332499 字节，覆盖 0.6.4）
- [x] 纯重构、无行为变化（等价变换：充电站渲染器把 `scale(-s,-s,s)` 拆成 `scale(s)` + 校正，结果相同）

---

### 2026-11-08 —— 机器人物品图标真正居中 + 充电站恢复电力需求

**用户再次反馈**：①机器人物品图标仍是「飞到左下角、整体不居中」（上一轮变换只对了朝向、位置仍错）；②充电站应该恢复电力需求（不再无限电量）。

**处理**：

| 问题 | 根因 | 修复 |
|------|------|------|
| ① 图标仍偏左下 | 上一轮把机器人摆在了「方块一角 (0,0,0)」，而物品渲染器随后会 -0.5 把方块居中，于是机器人被推到格子左下角 | `RobotItemRenderer` 改为把机器人当作一个方块摆到**方块中心 (0.5,0.5,0.5)** 并下移半身高，居中计算用 `ROBOT_HEIGHT=1.03` |
| ② 充电站无限电量 | `ChargingStationBlockEntity.tick` 每 tick 自动补满自身能量，且给机器人/物品充电时不扣电 | 普通站：删掉自动补满，充电时从 `energyStorage` 实际扣电（无线充电本就扣电，现在才真正生效）；创造站：保持无限 + 秒充满 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `client/renderer/item/RobotItemRenderer.java` | 变换重写：`translate(0.5, 0.5-SCALE*ROBOT_HEIGHT/2, 0.5)` + `rotateY` + `scale(SCALE)` + `scale(-1,-1,1)` + `translate(0,-1.501,0)` |
| `block/entity/custom/ChargingStationBlockEntity.java` | 普通站改为需要供电（扣 `energyStorage`），创造站保留无限；删除调试 `System.out.println` |

**验证状态**：
- [x] BUILD SUCCESSFUL（34s，仅既有 ResourceLocation deprecation 警告 ×2）
- [x] jar 同步到测试游戏 mods（332331 字节，覆盖 0.6.4）
- [ ] 进游戏验证：机器人物品图标居中显示
- [ ] 进游戏验证：普通充电站没电时停止充电、外部供电后恢复充电；创造站仍秒充满

---

### 2026-11-08 —— 机器人物品图标修复 + 充电站双贴图 + 移除创造栏机器人

**用户测试后反馈 3 个新问题**：①机器人物品图标飞到左下角；②充电站和创造充电站的皮肤应有细微差距（用户其实给了两张不同的贴图）；③不需要单独设立一个机器人的创造栏物品。

**处理**：

| 问题 | 根因 | 修复 |
|------|------|------|
| ① 图标飞到左下角 | `RobotItemRenderer` 变换写错（缩放方向/居中偏移都反了，机器人整体偏到格子下方） | 重写变换：`scale(0.6)` + `rotateY(180)` + `translate(0,-0.5,0)` + `scale(-1,-1,1)` + `translate(0,-1.501,0)`，机器人居中约 0.6 格高 |
| ② 双贴图没区分 | 两个 bbmodel 内嵌贴图其实是两张不同的 PNG（同名「充电站材质.png」但 base64 不同），旧流程误复制同一张 | 从各自 bbmodel 的 `textures[0].source` 提取出两张不同的内嵌 PNG → `charging_station.png`(775B) / `creative_charging_station.png`(778B)，MD5 已确认不同 |
| ③ 创造栏多余机器人 | 机器人被列进创造栏（机器人只能由装配台产出，不该直接给） | `ModCreativeTabs` 删掉 `output.accept(ROBOT_ITEM)`，保留其作标签页图标 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `client/renderer/item/RobotItemRenderer.java` | 重写物品变换（SCALE=0.6、Y_ROT=180 可调） |
| `美术材质/方块/charging_station.png` | 从 `充电站.bbmodel` 重新提取 |
| `美术材质/方块/creative_charging_station.png` | 从 `创造充电站.bbmodel` 重新提取 |
| `item/ModCreativeTabs.java` | 移除创造栏 ROBOT_ITEM 条目 |

**验证状态**：
- [x] BUILD SUCCESSFUL（23s，仅 1 个既有 ResourceLocation deprecation 警告）
- [x] jar 同步到测试游戏 mods（332521 字节，覆盖 0.6.4）
- [x] 两张充电站贴图已确认不同（MD5 不同）
- [ ] 进游戏验证：机器人物品 3D 模型居中显示（大小/朝向可微调 `RobotItemRenderer` 的 `SCALE`/`Y_ROT`）
- [ ] 进游戏验证：充电站 vs 创造充电站皮肤有细微差距

---

### 2026-11-08 —— 充电站 5 项修复（贴图/碰撞箱/渲染同步 + 机器人物品 3D 模型）

**用户测试充电站后反馈 5 个问题**：①皮肤渲染没对上；②碰撞箱不贴合；③贴图影响相邻方块；④机器人在站内时没渲染提示模型；⑤机器人应像精妙背包直接显示 3D 模型。

**处理**：

| 问题 | 根因 | 修复 |
|------|------|------|
| ①③ 贴图错位/污染相邻方块 | bbmodel UV 是 64px 坐标，MC 方块模型 UV 用 16px 单位，原样写 64px 越界 0~16 | `_convert_charging.py` UV 除以 4，重新生成两个方块模型 JSON |
| ② 碰撞箱不贴合 | 方块没自定义碰撞箱（默认整格） | `ChargingStationBlock.getShape()` 拼 6 cube 碰撞箱 |
| ④ 站内机器人不渲染 | hasRobot 只 `setChanged()`，没同步客户端 | BlockEntity 加 `getUpdatePacket/getUpdateTag/markUpdated`，4 处状态变化改用 markUpdated |
| ⑤ 物品 3D 模型 | 物品是 2D 图标 | 新增 `RobotItemRenderer`(BEWLR) + `RobotItem.initializeClient` + `robot.json` 改 `builtin/entity` |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `D:\Blockbench\mcp\_convert_charging.py` | UV 除以 4 |
| `src/.../models/block/charging_station.json` | 重新生成（UV/4） |
| `src/.../models/block/creative_charging_station.json` | 重新生成（UV/4） |
| `block/custom/ChargingStationBlock.java` | 新增 `getShape()` 自定义碰撞箱 |
| `block/entity/custom/ChargingStationBlockEntity.java` | 新增 `getUpdatePacket/getUpdateTag/markUpdated`，替换 4 处 `setChanged` |
| `client/renderer/item/RobotItemRenderer.java` | 新建（BEWLR 渲染 SearchRobotModel） |
| `item/custom/RobotItem.java` | 新增 `initializeClient()` 挂渲染器 |
| `src/.../models/item/robot.json` | 改 `builtin/entity`（3D 渲染路径） |

**验证状态**：
- [x] BUILD SUCCESSFUL（37s，仅 2 个既有 ResourceLocation deprecation 警告）
- [x] jar 同步到测试游戏 mods（332508 字节，覆盖 0.6.4）
- [ ] 进游戏验证：充电站贴图对位 + 碰撞箱贴合 + 不污染相邻方块
- [ ] 进游戏验证：站内机器人提示模型即时显示/消失
- [ ] 进游戏验证：机器人物品 3D 模型（大小/角度/位置可能需微调 `RobotItemRenderer` 的 SCALE/旋转/偏移）

---

### 2026-11-08 —— 充电站模型统一 + GUI 贴图同步

**用户需求**：① Blockbench 做好了充电站和创造充电站的模型，要统一；② GUI 贴图由另一个 AI 重做，要同步。

**处理**：
- 从 `D:\Blockbench\mcp\` 的 `充电站.bbmodel`、`创造充电站.bbmodel`（java_block，各 6 cube）转成 Minecraft 方块模型 JSON，输出到 `models/block/`。UV 用 bbmodel 原生的 64px 坐标，配 64×64 贴图。
- 两个方块贴图统一用 `充电站材质.png`（64×64），外观一致。
- GUI 贴图把 `美术材质/GUI/风格库/原版风/robot_*.png` 同步到 `美术材质/GUI/` 根目录（单一真源），src 里已是原版风贴图。
- 顺带清理 `美术材质/方块/preview_robot_assembler.png` 预览图（避免污染资源）。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `src/.../models/block/charging_station.json` | 由 bbmodel 生成 6 cube 方块模型 |
| `src/.../models/block/creative_charging_station.json` | 同上（几何一致，纹理指向各自贴图） |
| `美术材质/方块/charging_station.png`、`creative_charging_station.png` | 替换为充电站材质.png（64×64） |
| `美术材质/GUI/robot_*.png` | 更新为原版风新贴图 |
| `美术材质/方块/preview_robot_assembler.png` | 移到 `美术材质/预览图/` |

**验证状态**：
- [x] BUILD SUCCESSFUL（退出码 0）
- [x] jar 同步到 `versions/` + 测试游戏 mods（328884 字节，覆盖 0.6.4）
- [ ] 进游戏验证：充电站/创造充电站新模型外观、贴图 UV 对位、GUI 新贴图无错位

---

### 2026-11-08 —— 模式显示在手持操控台物品名称（纠正位置）

**用户纠正**：模式切换反馈应显示在「玩家手持操控台物品时，快捷栏上方显示的那个物品名称」处，不是 GUI 标题、也不是消息弹窗。

**处理**：
- `RobotControllerItem` 新增 `getName(ItemStack)` 重写，在「机器人操控台」名称后追加 `" · 模式"`，模式文字用专属颜色（待命白/跟随橙/探索青/收集绿/运输蓝），未绑定频道时不追加。
- 回退上一轮误解加在 GUI 标题里的模式显示：`RobotControllerScreen.renderLabels`、`RobotInventoryScreen.renderLabels` 恢复只显示标题；`RobotInventoryScreen.mouseClicked` 标题点击切换模式功能保留，但点击宽度只覆盖标题本身。

**用户复测反馈**：名称「显示了」，但右键空气切模式「不能切换」（名称不变）。

**二次修复**：根因是 `cycleCurrentMode()`（右键空气切模式）只改机器人实体模式，没把新模式写回操控台物品 NBT，而手持名称读的是物品 NBT，所以切完名称不动。补上 `setChannelMode(stack, getCurrentChannelIndex(stack), first.getMode().id)` 写回 NBT。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `src/.../item/custom/RobotControllerItem.java` | 新增 `getName()` 重写显示当前频道模式（带颜色），加 `Style`/`TextColor` import；`cycleCurrentMode()` 补 `setChannelMode` 写回 NBT |
| `src/.../client/screen/RobotControllerScreen.java` | `renderLabels` 去掉标题后追加的模式文字 |
| `src/.../client/screen/RobotInventoryScreen.java` | `renderLabels` 去掉模式文字；`mouseClicked` 标题点击宽度改回只覆盖标题 |

**验证状态**：
- [x] BUILD SUCCESSFUL（退出码 0）
- [x] jar 同步到 `versions/` + 测试游戏 mods（327591 字节，覆盖 0.6.4）
- [x] 进游戏验证：手持操控台名称显示「机器人操控台 · 模式」（用户反馈「显示了」）
- [ ] 进游戏复测：右键空气切模式后名称即时刷新、待命白色可见性

---

### 2026-11-08 —— 模式专属颜色 + 去模式切换弹窗

**用户反馈**（2 条）：
1. 模式颜色没区别、不要消息弹窗，直接显示在操控台物品名称（标题）位置；
2. 待命白、跟随橙、运输蓝、收集绿（探索=青，经追问确认）。

**处理**：
- `RobotMode` 枚举新增 `color()` 方法（待命白/跟随橙/探索青/收集绿/运输蓝）。
- 仓库 `RobotInventoryScreen` 标题模式文字、操控台 `RobotControllerScreen` 标题后追加的当前频道模式，都用 `color()` 上色。
- `RobotControllerItem` 的 `cycleCurrentMode`/`cycleModeForChannel` 删掉切换成功后的 `sendSystemMessage` 弹窗（保留"未绑定机器人"错误提示）。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `src/.../entity/custom/SearchRobotEntity.java` | `RobotMode` 加 `color()` 方法 |
| `src/.../client/screen/RobotInventoryScreen.java` | 标题模式文字用 `color()` |
| `src/.../client/screen/RobotControllerScreen.java` | 标题后追加当前频道模式并用 `color()` |
| `src/.../item/custom/RobotControllerItem.java` | 去掉两处切模式成功后的聊天栏提示 |

**验证状态**：
- [x] BUILD SUCCESSFUL（退出码 0）
- [x] jar 同步到 `versions/` + 测试游戏 mods（327468 字节，覆盖 0.6.4）
- [ ] 进游戏验证：标题模式颜色正确、切模式不弹窗、待命白色是否够醒目

---

### 2026-11-08 —— 仓库标题模式显示修复 + 模式切换去提示窗

**用户反馈**（2 条，经澄清 "控制台"="物品栏"）：
1. 仓库（物品栏）标题没显示当前模式；
2. 切换模式还是弹聊天栏提示（提示窗）。

**处理**：
- **标题不显示模式**：根因是 `RobotInventoryScreen.renderLabels` 用 `this.title.getString()` 提取标题文字，对 `Component.translatable` 返回的是翻译键而非"搜索机器人"，导致标题渲染错、模式文字跟着错位。改用 `this.title` 组件直接渲染，宽度用 `this.font.width(this.title)`，模式文字用 `LABEL_DIM`（中灰）区分。
- **切换模式弹提示窗**：根因是 `SearchRobotEntity.cycleMode()` 背包满切"探索"时 `notifyOwner("跟随模式（仓库已满）")` 弹提示再退回跟随。改为满仓时**直接跳过探索、切下一个可用模式**，不再弹提示。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `src/.../client/screen/RobotInventoryScreen.java` | `renderLabels`/`mouseClicked` 标题文字用 `this.title` 组件替代 `getString()`；模式文字 LABEL_DIM |
| `src/.../entity/custom/SearchRobotEntity.java` | `cycleMode()` 满仓跳过探索而非退回跟随+弹提示 |

**验证状态**：
- [x] BUILD SUCCESSFUL（退出码 0）
- [x] jar 同步到 `versions/` + 测试游戏 mods（327350 字节，覆盖 0.6.4）
- [ ] 进游戏验证：标题显示模式、点标题切模式不弹提示、满仓时跳过探索

---

### 2026-11-08 —— 配色纠正回原版风 + 装配台贴图错位修复

**用户纠正**（3 条）：
1. 精妙背包配色就是**原版风格**，之前做的深炭灰+暖金是自作主张、错的；
2. GUI 主体不居中、有大量空白；
3. 黑白名单 GUI 参照精妙背包（= 原版配色）。

**处理**：
- **配色切回原版风**（面板 #C6C6C6、凹格 #8B8B8B、黑描边、文字 #404040），并在原版风主题补齐 5 个交互用常量（`DANGER_HI`/`OK_HI`/`ACCENT_SELECT`/`DRAG_BG`/`ENERGY_MID`），否则操控台/仓库引用会编译失败。
- **发现并修复装配台贴图错位（空白/不居中的根源）**：代码是 0.5.8 径向布局（玩家背包 y=176、快捷栏 y=234、躯干 y=84），但 src 里的 `robot_assembler.png` 还是旧坐标（玩家背包 y=102、快捷栏 y=160），框线错位约 76px。用 `assembler_radial.py` 重新生成径向贴图替换 src + 原版风目录。
- **仓库 GUI 底部空白紧凑化**：`imageHeight` 固定最小 200 → 改为按内容 `max(133 + bagRows*18, 181)`，单/双箱子时底部不再空一大块。
- **黑白名单面板**：随配色回原版自动变回原版灰面板（黑名单红 / 白名单绿），无需额外改布局。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `美术材质/GUI/风格库/原版风/TechGuiTheme.java` | 补齐 5 个交互常量（DANGER_HI 等） |
| `美术材质/GUI/风格库/原版风/robot_assembler.png` | 旧坐标版 → 径向版（重新生成） |
| `src/.../client/screen/TechGuiTheme.java` | 深炭灰+暖金 → 原版风（apply 切回） |
| `src/.../textures/gui/robot_*.png` | 3 张贴图切回原版风 |
| `src/.../textures/gui/robot_assembler.png` | 旧坐标版 → 径向版（修复错位） |
| `src/.../client/screen/RobotInventoryScreen.java` | `imageHeight` 200 → 紧凑值，去底部空白 |
| `美术材质/GUI/风格库/gui_styles.json` | `current` 切回「原版风」 |

**验证状态**：
- [x] BUILD SUCCESSFUL（退出码 0）
- [x] jar 同步到 `versions/` + 测试游戏 mods（327355 字节）
- [ ] 进游戏验证：装配台径向布局对齐（贴图框线和槽位重合）、仓库 GUI 底部无空白、整体原版灰观感

---

### 2026-11-08 —— GUI 全面借鉴精妙背包风 + 仓库标题处模式切换

**用户要求**（4 条）：
1. 运输模式没物品就原地待命；
2. 借鉴精妙背包（Sophisticated Backpacks）的 GUI 风格；
3. 模式切换显示在「物品名称」位置；
4. GUI 风格都借鉴背包 mod。

**处理**：
- **请求 1**：确认无需改代码——`TransportGoal` 已实现「空车跑完一轮后停寻路、原地待命（2 秒重检）」。
- **请求 2/4**：新增「精妙背包风」GUI 风格（深炭灰面板 + 平坦凹格 + 暖金描边），写入风格库 `美术材质/GUI/风格库/精妙背包风/`（`TechGuiTheme.java` + `gen_gui_textures_backpack.py` + 3 张贴图），并 `apply_gui_style.py apply 精妙背包风 --with-theme` 安装进 src，`gui_styles.json` 标记 `current=精妙背包风`。
- **请求 3**：仓库 GUI 标题处显示「机器人名 · 当前模式」（模式用暖金高亮），并做成**可点击**——点击标题里的模式文字即循环切换工作模式（跟随→探索→收集→运输→待命）。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | `openRobotInventory` 网络包追加写 `getMode().id` |
| `menu/custom/RobotInventoryMenu.java` | 新增 `modeData`（ContainerData 实时同步模式）+ `getRobotMode()`/`cycleRobotMode()` |
| `network/RobotModePacket.java` | 新建（无参数据包，服务端调用 `cycleRobotMode()`） |
| `network/ModNetworking.java` | 注册 `RobotModePacket`（id 7） |
| `client/screen/RobotInventoryScreen.java` | 标题处显示「机器人名 · 模式」，模式点击切换 |
| `client/screen/TechGuiTheme.java` | 整体替换为精妙背包风配色（深炭灰+暖金，平坦凹格） |
| `resources/.../textures/gui/robot_*.png` | 3 张贴图替换为精妙背包风（深炭灰+暖金分区线） |
| `美术材质/GUI/风格库/` | 新增「精妙背包风」风格目录 + `gui_styles.json` 更新 |

**验证状态**：
- [x] BUILD SUCCESSFUL（退出码 0，仅 4 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 已同步到 `versions/` 归档 + 测试游戏 mods 文件夹（327387 字节）
- [ ] 进游戏验证：GUI 观感是否像精妙背包、点击标题模式文字能否正常切换、运输空车原地待命

---

### 2026-11-07 —— 外部存储接口整体分离归档，模组回到干净核心状态

**用户要求**：把「外部接口的物品以及相关」从模组里**分离出去**，标记「待开发」，先完善模组主体。

**处理**：把整个外部存储功能（外部存储接口物品 + 外部容器槽 + 仓库「外部」分页 + AE2 便携元件读/写/充电）从当前代码**完整移出**，模组回到不含外部存储的干净核心状态；**多储物箱分页栏（核心功能）保留**，只删了其中的「外部」标签。

**归档内容**：
- `待开发/外部存储接口/源码备份_含外部存储/`：移出前的完整源码快照（`src/` + `build.gradle` + `gradle.properties`）
- `待开发/外部存储接口/README.md`：移出内容对照清单 + 恢复方法
- `design/plan-external-storage.md`：方案文档（目标/已完成实现/待开发清单/接手要点）

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `integration/Ae2PortableCell.java` | 整文件删除（AE2 便携元件反射适配） |
| `item/ModItems.java` | 删 `EXTERNAL_STORAGE_INTERFACE` + `isPart`/`getPartType` 分支 |
| `item/ModCreativeTabs.java` | 删创造标签里的外部存储接口 |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 删外部容器槽、`TOTAL_SLOTS` 21→20、接口去重、`hasExternalInterface()`、`isContainerItem()`、组装/拆解外部容器逻辑 |
| `menu/custom/RobotAssemblerMenu.java` | 删外部容器槽 + 相关 getter |
| `client/screen/RobotAssemblerScreen.java` | 删外部容器槽特化 + 「外部容器」标签 |
| `entity/custom/SearchRobotEntity.java` | 删全部外部存储字段/方法 + 相关 import + `ExternalContainer` NBT 读写 |
| `menu/custom/RobotInventoryMenu.java` | 删外部分页（`externalHandler`/`externalSize`/`externalFlush`、外部分页槽位、`hasExternalTab()`） |
| `client/screen/RobotInventoryScreen.java` | 删「外部」分页标签 |
| `network/RobotTabPacket.java` | 注释更新 |
| `resources/.../lang/zh_cn.json` | 删语言条目 |
| `resources/.../models/item/external_storage_interface.json` | 删物品模型 |
| `resources/.../textures/item/external_storage_interface.png` | 删孤儿材质 |

**验证状态**：
- [x] BUILD SUCCESSFUL（退出码 0，仅 4 个既有 `ResourceLocation` deprecation 警告）
- [x] 全局 grep 确认 `src/` 无任何外部存储残留引用
- [ ] 进游戏验证：多箱子分页栏切页正常、装配台 20 槽组装/拆解正常、收集/运输/过滤不受影响

### 2026-11-03 —— 外部存储接口相关打包为「待开发」方案

**用户决定**：先把模组主体完善，外部存储接口相关整体暂停，打包成一个方案文档存档，后续按文档接手。

**处理**：
- 新建 `design/plan-external-storage.md`：把「外部存储接口物品 + 外部容器挂载 + 仓库外部分页 + AE2 便携元件（读/写/充电/绝不吞物）」整体归档为「待开发」方案，含目标、已完成的实现、待开发清单（补材质 → 实测读写充电掉落 → 原生 GUI 高风险暂缓 → ME 箱子方向待拍板）、关键决策记录、文件路径、接手要点。
- `PROJECT_STATUS.md` 第 4 节 TODO 新增「外部存储接口方案 🟡 已暂停/待开发」条目。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `design/plan-external-storage.md`（新增） | 外部存储接口方案归档（待开发） |
| `PROJECT_STATUS.md` | TODO 增「外部存储接口方案」条目 + 本节进度报告 |
| `SESSION_SUMMARY.md` | 更新「下一会话推荐操作」为模组主体完善方向 |

**验证状态**：
- [x] 方案文档已建、TODO 已归档、SESSION_SUMMARY 已更新
- [ ] （外部存储后续接手，非当前主线）

### 2026-11-01 —— v0.6.4 AE2 便携存储元件接入外部存储 + 充电

**用户新方向**（拍板）：放弃 ME 箱子，改成机器人把 AE2「便携存储元件」(Portable Cell) 链接为外部存储，同时给它充电；元件里的物品显示在机器人「外部」分页里（元件仍挂在机器人身上）。

**处理**：新增反射适配层 `Ae2PortableCell`，本模组依然**不依赖 AE2 编译**，全部运行时反射读写。

| 项 | 处理 |
|----|------|
| 检测 | 向上遍历物品类父类链，是否继承 `appeng.items.tools.powered.AbstractPortableCell`（AE2 不在场时永远 false，零风险） |
| 读 | `StorageCells.getCellInventory(stack, null)` → `getAvailableStacks()` → 遍历 KeyCounter，把每个 (AEItemKey, 数量) 展开成 ≤64 一叠的 ItemStack，最多 63 格（7 行，对应 1K 元件类型量） |
| 写 | 打开时深拷贝一份快照，关闭时按「快照 vs 关闭内容」逐槽位算差值，对差值 insert/extract + `persist()` 写回元件 NBT（幂等，可安全多次 flush） |
| 充电 | `Capabilities.FORGE_ENERGY` 拿元件 FE 能力，机器人每 tick 转 200 FE 进元件，元件满 / 机器人没电自动停 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `integration/Ae2PortableCell.java`（新增） | AE2 便携元件反射适配：`isPortableCell` / `readItems` / `applyDelta` / `getEnergyStorage`，全 try-catch 静默降级不崩服 |
| `entity/custom/SearchRobotEntity.java` | 外部容器增加便携元件分支（`buildExternalContainerHandler` / `getExternalContainerTabSlots`）；新增 `flushPortableCell`（差值写回）、`chargePortableCell`（每 tick 充电）、`portableCellSnapshot`/`portableCellHandler` 会话状态 |
| `gradle.properties` | mod_version 0.6.3 → 0.6.4 |

**验证状态**：
- [x] BUILD SUCCESSFUL（仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 归档 `versions/search_bot-0.6.4.jar` + 同步测试游戏 mods（删旧 0.6.3）
- [ ] 进游戏验证：挂载便携元件 → 外部标签显示元件内物品（读取/展开）
- [ ] 进游戏验证：元件内取出/放入物品后关 GUI 再开，数据不丢（差值写回）
- [ ] 进游戏验证：机器人有电时元件电量持续上涨（充电）

> 说明：①AE2 反射读写路径无法在编译期验证，需进游戏实测（读/充电风险低，写路径最需确认）；②**绝不吞物**——写入改为「先全取出腾空间→再放入」，放不进的余额（`insert` 返回 < 请求量）会退成 ItemStack、由 `flushPortableCell` 掉落在机器人脚下，玩家可捡回，不再消失；③超过 63 格的类型不显示但不会丢（差值只针对可见槽位，隐藏部分不受影响）。

### 2026-11-01 —— v0.6.3 GUI 对齐修复（槽位 1px 偏移 + 电量条遮挡）+ AE2 真实 GUI/供电结论

**用户 6 点反馈**：①电量在 GUI 里挡住别的；②别的 GUI 文字有些歪（离得远/太近）；③装配台槽位歪、物品贴边缘；④AE 箱子应有两个 GUI（上面板终端 + 正面存储原件槽）；⑤机器人身上打开的是自己新做的 GUI，不是 ME 箱子原生 GUI；⑥说好给箱子充电但箱子没得到电。

**处理**（本次落地 ①②③；④⑤⑥ 是 AE2 硬限制，见文末说明）：

| 项 | 处理 |
|----|------|
| ③槽位偏移 | `TechGuiTheme.slot()` 之前把 16x16 灰底画在 `(x+1,y+1)`，物品（原生画在 `(x,y)`）整体左上偏 1px、贴住凹格边缘。改成灰底画在 `(x,y)`、1px 凹槽环画四周（总 18x18），装配台和仓库 GUI 的槽位都对齐 |
| ①电量遮挡 | 电量数字大时（金电池 210 万 / 终极电池 10 亿 FE）会顶到左侧「背包」标签。加 `compactEnergy()` 缩写（K/M/G，最多一位小数），条仍在背包标签行右侧 |
| ②文字歪 | 主要是槽位偏移导致的「参照物偏了」；槽位对齐后文字相对槽位不再显歪 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `client/screen/TechGuiTheme.java` | `slot()` 重写：16x16 灰底对齐物品 `(x,y)`，凹槽环画四周，修掉 1px 偏移 |
| `client/screen/RobotInventoryScreen.java` | `drawEnergyBar()` 电量数字用 `compactEnergy()` 缩写；新增 `compactEnergy`/`withDecimal` 两个私有方法 |

**验证状态**：
- [x] BUILD SUCCESSFUL（仅 4 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 归档 `versions/search_bot-0.6.3.jar` + 同步测试游戏 mods（覆盖旧 0.6.3，清掉误生成的 0.7.0）
- [ ] 进游戏验证：装配台/仓库 GUI 物品居中、不再贴边缘
- [ ] 进游戏验证：高容量电池（金/钻石/终极）电量数字不再遮挡「背包」

> 说明（④⑤⑥）：AE2 的 ME 箱子不是普通容器，是「必须放在世界里、接入 AE 网络、通电才能用」的网络机器。机器人外部容器目前是把箱子作为物品带在身上、开 GUI 时重建一个「幽灵」方块实体，没有位置、没有网络、也没有电，所以：⑥充不上电（幽灵实体不 tick、收不到 FE）；⑤打不开 ME 箱子原生 GUI（AE2 菜单要按坐标找真实方块，`getBlockEntity(pos)==this` 自检过不了）；④我的「外部」分页用反射只能显示 60 格存储原件槽（相当于正面那一半），上面的终端面板做不出来。**方向待用户定**：A) 接受现状，外部容器只支持原版箱子类简单容器，AE 箱子当普通方块放世界里用；B) 重构外部容器为「把箱子真正放到世界里并关联」，工作量较大。

### 2026-11-01 —— v0.6.3 仓库 GUI 分页栏（多箱子 + 外部容器）+ AE2 箱子可读

**用户 6 点需求**：①研究车万女仆；②研究旅行背包；③旅行背包磁力升级的黑白名单可照搬；④机器人 GUI 为何不是原版风格；⑤机器人能装多个箱子 + 外部容器，但 GUI 没有切页能力，要在顶部加类似创造模式的分页栏逐个查看各容器内部；⑥AE 箱子放进外部插件没反应，点开是一行物品栏、和 AE 箱子无关。

**处理**（本次落地 ⑤⑥；①②③④ 见文末说明）：

| 项 | 处理 |
|----|------|
| ⑤分页栏 | 机器人仓库 GUI 顶部加类似创造模式的分页栏：每个储物箱一个分页（箱子1、箱子2…），有外部容器时最后一个是「外部」分页。只渲染/交互当前分页的槽位，其它分页不画 |
| ⑥AE 箱子 | 不再尝试打开 AE2 原生菜单（其菜单自检 `getBlockEntity(pos)==this`，物品态打不开）。改成在「外部」分页里直接显示容器内容：原版容器读 `BlockEntityTag.Items`；模组容器重建方块实体拿 `IItemHandler`（AE2 用反射 `getInternalInventory().toItemHandler()` 兜底），关菜单 `saveWithoutMetadata()` 写回物品 |
| 数据一致性 | 组装时把各储物箱格数记进 `stats.box_slots`；拆解按箱子逐个写回各自 `robot_inventory`，组装再按顺序合并，多箱子数据不串不丢 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 新增 `getBoxSlots/getBoxCount/getBoxStart`；右键统一开仓库 GUI；新增 `buildExternalContainerHandler/getExternalContainerTabSlots/flushExternalContainer/itemHandlerOf`（反射兜底）；删除旧的 `openExternalContainerGui/openModdedContainerGui/createExternalContainerMenu` |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 组装记 `box_slots` ListTag；拆解按箱子拆开写回、组装按顺序合并 `robot_inventory` |
| `menu/custom/RobotInventoryMenu.java` | 重写：分页模型（每箱子 + 外部容器各一分页）、`TabSlot`（`isActive()` 按当前分页）、`currentTab` 数据槽、外部容器 IItemHandler、关菜单 flush |
| `client/screen/RobotInventoryScreen.java` | 顶部分页栏绘制 + 点击切页；只画当前分页槽位 |
| `network/RobotTabPacket.java`（新增）+ `network/ModNetworking.java` | 分页切换网络包 |
| `gradle.properties` | mod_version 0.6.1 → 0.6.3 |

**验证状态**：
- [x] BUILD SUCCESSFUL（仅 4 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 归档 `versions/search_bot-0.6.3.jar` + 同步测试游戏 mods（删旧 0.6.1）
- [ ] 进游戏验证：装多个储物箱 + 外部容器，顶部出现分页栏，能逐个查看各箱子与外部容器内部
- [ ] 进游戏验证：AE2 ME 箱子作外部容器，外部分页显示 60 格存储单元（不再是一行物品栏）
- [ ] 进游戏验证：外部容器（箱子/AE 箱子）存取物品后关 GUI 再开，数据不丢

> 说明：本次把「右键打开外部容器原生 GUI」改成「右键统一打开机器人仓库 GUI + 顶部分页栏」，外部容器作为最后一个分页查看。原版容器（箱子/木桶/潜影盒/漏斗/投掷器）和能通过 IItemHandler 读取的模组容器（含 AE2 ME 箱子的 60 格存储单元）都能在分页里显示；个别拿不到 IItemHandler 的容器会显示为空分页。

### 2026-11-01 —— v0.6.1 外部容器原生 GUI 扩展到模组容器 + 外部接口限装一个

**用户 3 条纠正**（对 v0.6.0 的补充）：①「我要的效果是能打开 GUI，能不能读不是你的事情」——不要因为 AE2 箱子从物品读不出物品就拦着不开 GUI，任何容器都要尽量打开它自己的原生 GUI；②「外部接口也属于只能装一个的插件」；③「外部接口可以给方块供电，所以 ME 箱子也会得到供电」（设计依据，说明 GUI 打开后能正常跑）。

**处理**：

| 项 | 处理 |
|----|------|
| ①模组容器原生 GUI | `SearchRobotEntity.openExternalContainerGui` 增加第二分支：非原版容器（`externalContainerSlotCount` 返回 0 的 `EntityBlock` 方块）走 `openModdedContainerGui`——用 `EntityBlock.newBlockEntity` 重建方块实体 → `setLevel` → `load(物品的 BlockEntityTag)` → 作为 `MenuProvider` 直接开它自己的原生菜单；保存/拾取前 `flushMountedContainer()` 把重建的方块实体 `saveWithoutMetadata()` 写回物品 `BlockEntityTag`，物品不丢 |
| ②外部接口限装一个 | `RobotAssemblerBlockEntity.isItemValid` 功能槽校验里，`external_storage_interface` 和附件一样做「同类型去重」，第二块接口放不进 |
| ③供电 | 确认：外部存储接口本来就给方块供电（既有设计），所以 ME 箱子 GUI 打开后有电、界面能正常跑 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | `openExternalContainerGui` 拆成「原版 SimpleContainer 路径 + 模组方块实体路径」；新增 `openModdedContainerGui` / `flushMountedContainer` / `mountedContainerEntity` 字段；`saveRobotData` 开头调用 `flushMountedContainer` |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | `isItemValid` 功能槽：外部存储接口同类型去重 |
| `gradle.properties` | mod_version 0.6.0 → 0.6.1 |

**验证状态**：
- [x] BUILD SUCCESSFUL（仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 数据一致性补丁：`openModdedContainerGui` 开头 `flushMountedContainer()`，反复开关模组容器 GUI 不再读到旧数据（取出/换机器人/放地上三条已天然满足）
- [x] jar 归档 `versions/search_bot-0.6.1.jar` + 同步测试游戏 mods（覆盖同版本，删旧 0.6.0）
- [ ] 进游戏验证：AE2 箱子/旅行背包/更多箱子作外部容器，右键机器人打开其原生 GUI，存取物品不丢
- [ ] 进游戏验证：反复开关同一模组容器 GUI，物品不丢、数据一致
- [ ] 进游戏验证：功能槽放第二块外部存储接口被拒绝
- [ ] 进游戏验证：原版箱子/木桶/潜影盒/漏斗/投掷器原生 GUI 仍正常（0.6.0 已测部分）

> ⚠️ 边界：个别模组容器的原生菜单会在打开后自检「这个方块实体是否真的摆在世界里」而自动关闭（如 AE2 的 ME 箱子菜单有 `getBlockEntity(pos) == this` 校验）。这是对方模组自身的一致性校验，物品态无法满足；大多数容器的菜单不带该校验，可直接打开。若进游戏发现某款容器的 GUI 打开即关，回来告诉我具体是哪一款，我再针对性处理。

### 2026-11-01 —— v0.6.0 外部容器整件保留 + 右键打开容器原生 GUI

**用户反馈**：上一条「外部容器判定白名单（拒绝 AE2 箱子）」的方向不对。用户要的是——「能放物品的都能放进去是可以的，主要是右击机器人时要打开这些方块的 GUI」。

**处理**（推翻 v0.5.8 的「拍平 + 白名单」方案，改成「整件保留 + 原生 GUI」）：

| 项 | 处理 |
|----|------|
| 判定放宽 | `isContainerItem` 从原版白名单改回 `BlockItem + EntityBlock`：任何带方块实体的方块物品都能放（AE2 箱子/旅行背包/Iron Chests 也放行） |
| 不再拍平 | 组装时外部容器**整件**存入机器人 `ExternalContainer` NBT，物品数据不再并入机器人背包，`inventory_size` 只算储物箱 |
| 右键开原生 GUI | `SearchRobotEntity.mobInteract`：装了外部容器且空手右键时，打开容器自己的原生菜单（箱子/木桶→ChestMenu、潜影盒→ShulkerBoxMenu、漏斗→HopperMenu、投掷器→DispenserMenu） |
| 重建原理 | 同潜影盒手持打开：从物品 `BlockEntityTag.Items` 重建 `SimpleContainer`，开出对应原生菜单；槽位一变就写回物品，取出物品不丢 |
| 兜底 | 无法从物品直接打开的容器（熔炉、AE2 箱子等）回退到机器人仓库 GUI，容器物品仍原样保留 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `block/entity/custom/RobotAssemblerBlockEntity.java` | `isContainerItem` 放宽；删 `readContainerItems`/`writeContainerItems`/`clearContainerItems`/`getContainerSlotCount`/`externalSlotsFor`/`computeStorageSlots`；组装整件存 `ExternalContainer`、拆解整件还原 |
| `entity/custom/SearchRobotEntity.java` | 新增 `openRobotInventory`/`openExternalContainerGui`/`readExternalContainerItems`/`writeExternalContainerItems`/`externalContainerSlotCount`/`createExternalContainerMenu`；`mobInteract` 空手右键优先开外部容器原生 GUI |
| `gradle.properties` | mod_version 0.5.9 → 0.6.0 |

**验证状态**：
- [x] BUILD SUCCESSFUL（23s，仅既有 deprecation 警告）
- [x] jar 归档 `versions/search_bot-0.6.0.jar` + 同步测试游戏 mods（删旧 0.5.9）
- [ ] 进游戏验证：原版箱子/木桶/潜影盒/漏斗/投掷器作外部容器，右键机器人打开对应原生 GUI，放入取出物品不丢
- [ ] 进游戏验证：拆解后容器整件还原到外部容器槽，物品仍在容器里
- [ ] 进游戏验证：AE2 箱子（放得进但打不开原生 GUI）回退到机器人仓库 GUI，不丢物品、不崩溃

> ⚠️ 行为变化：外部容器不再是机器人工作背包的一部分（AI 收集/运输只用储物箱背包），它是独立的「随身隔间」，右键打开它的原生 GUI 手动存取。

### 2026-11-01 —— v0.5.9 收集模式修复（换配件记忆标记 + 等待行为）

**用户 3 条反馈**：①频道设过收集容器、机器人换配件后，操控台绿框还在但切不到收集模式；②收集模式不用提醒附近有没有物品，机器人内部无物品且没有源容器时回收集容器附近等待；③问外部容器识别原理（仅解释）。

**处理**：

| 项 | 处理 |
|----|------|
| ①换配件丢标记 | 根因：收集/运输标记只存机器人 NBT，拆解即丢，操控台绿框只是缓存。修复：拆解时把标记存进核心 `RobotMarkers`，组装时读回 |
| ②收集等待 | 删 `notifyOwner` 提醒；找不到源容器时回收集容器附近（远则走过去、近则原地等，2 秒节流重扫） |
| ③识别原理 | 口头解释：只认原版容器方块类白名单，AE2/ME 箱子用磁盘存物、非原版 `BlockEntityTag.Items`，故拒绝 |
| 升版本 | 0.5.8 → 0.5.9 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 拆解保存标记到核心 `RobotMarkers` + 组装读回 |
| `entity/ai/CollectGoal.java` | 删提醒 + 找不到源容器回收集容器等待 |
| `gradle.properties` | mod_version 0.5.8 → 0.5.9 |

**验证状态**：
- [x] BUILD SUCCESSFUL（3m，无新增错误）
- [x] jar 归档 `versions/search_bot-0.5.9.jar` + 同步测试游戏 mods（删旧 0.5.8）
- [ ] 进游戏验证：换配件（复用核心）后仍能切收集模式、绿框还在
- [ ] 进游戏验证：收集范围无货源时回收集容器附近等待、不再弹提醒

### 2026-10-31 —— v0.5.8 GUI 需求落地 + 外部容器判定修复 + 机器人皮肤

**用户 4 条要求**：①美术 AI 不负责代码，她留下的 GUI 代码需求（R0–R4）由我实现；②外部容器判定 bug——AE2 箱子被误当容器、右键开假箱子 GUI、物品取出即丢（AE2 箱子用磁盘存物、非 `BlockEntityTag.Items`）；③按她提供的美术材质实现 GUI（含径向布局）；④机器人一直纯灰、皮肤没生效。

**处理**：

| 项 | 处理 |
|----|------|
| ①外部容器判定修复 | `isContainerItem` 从 `BlockItem+EntityBlock`（过宽）改为**原版容器方块类白名单**（Chest/Barrel/ShulkerBox/Hopper/Dispenser/AbstractFurnaceBlock），`getContainerSlotCount` 同步按方块类返回、非白名单 0；AE2 箱子/旅行背包/Iron Chests 放不进外部容器槽 |
| ②R0 电量条 | `RobotInventoryScreen#drawEnergyBar` 去掉 `leftPos/topPos` 二次偏移，改相对坐标 barX=112/barY=inventoryLabelY/52×6 |
| ②R1 blit 越界 | 仓库 GUI blit 高度夹到 `Math.min(imageHeight,256)` |
| ②R2 径向布局 | 装配台 256×230→256×256「原子核+电子轨道」：躯体居中、核心正上方、10 功能内圈、6 附件外圈、输入/输出左右、拆解/组装按钮、外部容器槽 (8,138)、玩家背包下移；内/外圈槽位 `isActive()`（没放躯体或超躯干等级不出现） |
| ②R3 颜色集中 | `TechGuiTheme` 新增 DANGER_HI/OK_HI/ACCENT_SELECT/DRAG_BG/ENERGY_MID；RobotControllerScreen 4 处 + RobotInventoryScreen 1 处写死色值换常量 |
| ③机器人皮肤 | 用 `皮肤库/01_旧版任意UV/search_robot.png`（128×64，匹配逐面 UV）覆盖占位灰图 |
| ④径向贴图 | `robot_assembler_radial.png`（256×256）覆盖 `美术材质/GUI/robot_assembler.png`，与 R2 代码同次生效 |
| 升版本 | 0.5.7 → 0.5.8 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `block/entity/custom/RobotAssemblerBlockEntity.java` | `isContainerItem`/`getContainerSlotCount` 改原版方块类白名单；删 ResourceLocation/EntityBlock import |
| `client/screen/RobotInventoryScreen.java` | R0 电量条相对坐标 + R1 blit 夹紧 + R3 ENERGY_MID |
| `menu/custom/RobotAssemblerMenu.java` | R2 径向布局（GUI_HEIGHT=256 + 槽位坐标 + isActive） |
| `client/screen/RobotAssemblerScreen.java` | R2 按钮/标签坐标 + 注释 |
| `client/screen/TechGuiTheme.java` | R3 新增 5 个颜色常量 |
| `client/screen/RobotControllerScreen.java` | R3 写死色值换常量 |
| `美术材质/实体/search_robot.png` | 换成 128×64 皮肤 |
| `美术材质/GUI/robot_assembler.png` | 换成 256×256 径向贴图 |
| `gradle.properties` | mod_version 0.5.7 → 0.5.8 |

**验证状态**：
- [x] BUILD SUCCESSFUL（27s，无新增错误）
- [x] syncArtTextures 同步皮肤 + 径向贴图到资源目录
- [x] jar 归档 `versions/search_bot-0.5.8.jar` + 同步测试游戏 mods（删旧 search_bot-0.5.7.jar）
- [ ] 进游戏验证：径向装配台格子与轨道对齐、放躯体前后槽位增减
- [ ] 进游戏验证：原版箱子可放外部容器槽、AE2 箱子放不进
- [ ] 进游戏验证：电量条不压格子（各背包行数）、机器人皮肤不再纯灰
- [ ] 外部存储接口材质仍缺（0.5.7 遗留），等美术 AI 出图

### 2026-10-31 —— v0.5.7 兼容性（鼠标手势快速移动 + 外部存储接口）

**用户需求**（4 条）：①完善与别的模组的兼容性；②装了鼠标手势，但模组 GUI 不能快捷放入/取出；③新增「外部存储接口」组件，放入装配台后多一个槽位直接放各种容器，机器人带着它当自己仓库；④先兼容原版容器，之后再做 AE2 箱子、旅行背包、更多箱子。

**处理**：

| 项 | 处理 |
|----|------|
| 鼠标手势快速移动 | 根因是装配台/仓库两菜单 `quickMoveStack` 空实现（返回 EMPTY），鼠标手势依赖它。两处全部重写，用 `moveItemStackTo` 在「装配台/机器人背包 ↔ 玩家背包 ↔ 过滤槽」之间转移，自动尊重 `isItemValid` |
| 外部存储接口物品 | `ModItems` 注册 `external_storage_interface`，`isPart`/`getPartType`、创造标签、中文名、模型 JSON |
| 外部容器槽 | 装配台新增 `SLOT_EXTERNAL_CONTAINER=20`，`TOTAL_SLOTS` 20→21；`isActive()` 由 `hasExternalInterface()` 控制，放入接口后解锁 |
| 容器物品读写 | 读/写原版 `BlockEntityTag.Items` 格式（`readContainerItems`/`writeContainerItems`），跨模组通用；判定用 `BlockItem`+`EntityBlock`；各容器槽数查表（箱子/桶/潜影盒 27、漏斗 5、发射器/投掷器 9、熔炉系 3） |
| 组装合并 | 容器物品合并进机器人 Inventory 外部区 `[storageSlots, +externalSlots)`，`inventory_size` 扩为 `storageSlots+externalSlots`，空容器存 `ExternalContainer` NBT；超 90 格上限部分掉落（无损） |
| 拆解还原 | Inventory 按外部区/基础区拆分，外部区写回容器物品、基础区写回储物箱，容器进槽或掉落 |
| 机器人 NBT | `SearchRobotEntity` 新增 `externalContainer` 字段 + `ExternalContainer` 读写 |
| 升版本 | 0.5.6 → 0.5.7 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `item/ModItems.java` | 注册外部存储接口；`isPart`/`getPartType` |
| `item/ModCreativeTabs.java` | 创造标签加入外部存储接口 |
| `menu/custom/RobotAssemblerMenu.java` | 重写 `quickMoveStack`；新增外部容器槽 + `isActive()` + getter |
| `menu/custom/RobotInventoryMenu.java` | 重写 `quickMoveStack`（鼠标手势） |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 外部容器槽 + `isItemValid` + 容器物品读写/槽数查表 + 组装合并 + 拆解还原 |
| `client/screen/RobotAssemblerScreen.java` | 未激活槽不画 + 外部容器槽特化 + 「外部容器」标签 |
| `entity/custom/SearchRobotEntity.java` | `externalContainer` 字段 + `ExternalContainer` NBT 读写 |
| `resources/.../lang/zh_cn.json` | `外部存储接口` 语言条目 |
| `resources/.../models/item/external_storage_interface.json` | 物品模型 JSON（新增） |
| `gradle.properties` | mod_version 0.5.6 → 0.5.7 |

**验证状态**：
- [x] BUILD SUCCESSFUL（19s，仅 3 个既有 ResourceLocation deprecation 警告）
- [x] jar 归档 `versions/search_bot-0.5.7.jar` + 同步测试游戏 mods（删旧 search_bot-0.5.6.jar）
- [ ] 进游戏验证：鼠标手势对装配台/仓库 GUI shift 快速放入取出
- [ ] 进游戏验证：放接口→放带物品容器→组装→机器人带容器当仓库→拆解→容器和物品还原
- [ ] 外部存储接口缺材质（`textures/item/external_storage_interface.png` 未生成），等美术 AI 出图 + syncArtTextures
- [ ] AE2 箱子 / 旅行背包 / 更多箱子（本次只做原版容器，后续逐个适配各自 NBT 格式）

### 2026-10-31 —— v0.5.6 修复（机器人头身分离）

**用户反馈**：机器人的头又开始脱离身子了，按之前的解决方法解决。

**根因**：模型今天凌晨被美术 AI 用 `apply_01.py` 重新生成，`head` 和 `boody`（身体）的旋转中心点（pivot）都被设成了 `y=27.05`（脚底再往下 3.05），导致抬头/转头时头绕脚底下方画大弧、脱离身子。这是 10/30 修过的问题，重生成时又被覆盖回 27.05。

**处理**：

| 项 | 处理 |
|----|------|
| head pivot | `PartPose.offset` y 27.05 → 12.05（头底/脖子），head 内所有 cube 的 y +15 保持外观不变 |
| body pivot | `PartPose.offset` y 27.05 → 18.05（腰），boody 内所有 cube 的 y +9 保持外观不变 |
| 升版本 | 0.5.5 → 0.5.6 |
| 构建归档 | build 成功，归档 + 同步测试游戏 |

**修改文件**：
- `client/model/SearchRobotModel.java`（head/boody 的 PartPose + cube y 坐标）
- `gradle.properties`（mod_version 0.5.6）

**验证状态**：
- [x] BUILD SUCCESSFUL（20s，仅 3 个既有 ResourceLocation deprecation 警告）
- [x] jar 归档 `versions/` + 同步测试游戏 mods（search_bot-0.5.6.jar）
- [ ] 进游戏验证头身分离是否修复（抬头/低头/左右转头都不再脱离）
- [ ] 注：美术 AI 若再跑 apply_01.py 重生成模型，此修复会被覆盖，需再次处理

### 2026-10-31 —— v0.5.5 发布（全面重做材质 + 清除概念图污染）

**用户需求**：又有新材质了，快归档，要进游戏测试。

**处理**：

| 项 | 处理 |
|----|------|
| 新材质检查 | `美术材质/物品/` 根目录全套重做（67 个材质：电池/传感器/发动机/储物箱/机械臂/护盾/躯干/轮子/螺旋桨 × 铜铁金钻下界合金终极 + 通用版 + 传送装置/核心/操控台等） |
| 清理污染 | 移走 8 张预览/放大图（`all_items_preview`、`tiers_preview`、`zoom_copper`、`zoom_round2~5`、`limbs_zoom`）→ `预览图/`；移走 8 个旧子目录（`_backup_replaced` + 传感器/储物箱/发动机/护盾/机械臂/电池/移动部件）→ `备份/旧子目录_20261031/`；移走实体 3 张概念大图 → `预览图/` |
| 清除已污染资源 | 删除 `resources/textures/entity/` 里已同步进去的 3 张中文概念图（充电站概念图/机器人三视图/机器人概念图，合计约 2.3MB，是上一版 jar 从 223KB 涨到 2.5MB 的元凶） |
| 升版本 | `mod_version` 0.5.4 → 0.5.5 |
| 构建归档 | build 成功，jar 归档 `versions/` + 同步测试游戏 mods（删旧 `search_bot-*.jar`） |

**验证状态**：
- [x] BUILD SUCCESSFUL（22s），jar `search_bot-0.5.5.jar`（228913 字节，比 0.5.4 的 2.5MB 干净得多）
- [x] 资源目录干净：item 67 个材质、entity 仅 search_robot、无预览图/中文污染
- [x] 已归档 `versions/` + 同步测试游戏 mods
- [ ] 进游戏验证新材质显示效果

### 2026-10-31 —— v0.5.4 发布（新材质同步 + 美术目录清理）

**用户需求**：检查有没有新材质文件，并把模组更新到 0.5.4。

**处理**：

| 项 | 处理 |
|----|------|
| 新材质检查 | 躯干/轮子/螺旋桨「铜铁金钻下界合金终极」分级材质 + 传送装置 + 机器人核心已就位（`美术材质/物品/` 根目录，且已在资源目录） |
| 美术目录清理 | 5 张物品预览大图 + 6 张 GUI 预览图 → `美术材质/预览图/`；`_backup_replaced` 旧占位图备份 + 旧移动部件 propeller/wheels → `美术材质/备份/`（避免 syncArtTextures 展平时 duplicate 报错或旧材质覆盖新材质） |
| 升版本 | `mod_version` 0.5.1 → 0.5.4（跳过 0.5.2/0.5.3，三轮更新一次发布） |
| 构建归档 | build 成功，jar 归档 `versions/` + 同步测试游戏 mods（删旧 `search_bot-*.jar`） |

**修改/涉及文件**：

| 文件 | 变更 |
|------|------|
| `gradle.properties` | `mod_version` 0.5.1 → 0.5.4 |
| `美术材质/物品/`、`美术材质/GUI/` | 移走预览图、备份、旧同名移动部件材质（整理，非代码） |
| `versions/search_bot-0.5.4.jar` | 新增归档 |
| 测试游戏 mods | 旧 `search_bot-*.jar` 已删，放入 `search_bot-0.5.4.jar` |

**验证状态**：
- [x] BUILD SUCCESSFUL（17s），jar `search_bot-0.5.4.jar`（2564526 字节）
- [x] 资源目录无预览图/备份污染，关键新材质（torso 分级/teleporter/robot_core/propeller/wheels 新版）在位
- [x] 已归档 `versions/` + 同步测试游戏 mods
- [ ] 进游戏验证新材质显示 + 本版本功能（收集/黑白名单/标记物品面板）

### 2026-10-31 —— 收集模式诊断结论 + 找不到货源主动提醒 + 仓库 GUI 面板改「标记物品」

**用户反馈 4 条**：①机器人不收集；②内置 GUI 明明是标记用的却成了储物槽；③shift 右键一般方块应该什么都不触发；④设置收集模式没反应。

**诊断与处理**：

| 项 | 处理 |
|----|------|
| ①④不收集 | **不是 bug**。日志确认机器人已切到收集模式、`CollectGoal` 正常跑，但 `当前源=null`——收集模式语义是「把目标容器(shift右键设的)附近其他有物品的容器，集中搬进目标容器」，玩家只设了目标、附近没放有物品的箱子，所以找不到货源原地不动 |
| ②GUI 像储物槽 | 右侧过滤面板标题「物品过滤」→「标记物品」，12 个过滤槽位改**蓝底**（外框 0xFF5A8FB0 / 内底 0xFF24323F），与左侧灰底储物槽明显区分 |
| ③shift右键方块 | 确认原本就正确：`CommonEventHandler` 对非容器方块直接 return 不触发任何动作；切控制台 GUI 是 shift 右键**空气** |
| 新增提醒 | `CollectGoal` 加 `notifiedNoSource`：找不到货源时通过聊天栏主动告诉主人「附近没找到装了东西的箱子（搜索半径 N 格）」，找到货源后自动重置，不再闷头不动让人误以为坏 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/ai/CollectGoal.java` | 加 `notifiedNoSource` 找不到货源主动提醒（含原有诊断日志） |
| `client/screen/RobotInventoryScreen.java` | 面板标题改「标记物品」+ 过滤槽位蓝底（新增 `drawSlotBox` 四参重载） |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 21s）
- [x] jar 已同步到测试游戏 mods 文件夹（search_bot-0.5.1.jar，2026-10-31 04:31）
- [ ] 进游戏复测收集模式：摆目标容器(shift右键设) + 旁边放有物品的箱子 → 切收集模式，机器人应开始搬运；若旁边没货源，聊天栏应收到「找不到货源」提醒
- [ ] 进游戏看仓库 GUI：右侧面板标题是否为「标记物品」、12 格是否为蓝底

### 2026-10-31 —— 仓库黑白名单过滤 / 全部 GUI 归档

**用户需求**：①机器人的仓库除存储页外，右侧要有黑白名单开关（可选黑名单/白名单）、能选是否忽略物品后缀、可设置物品，从而限制机器人拿取的物品；②所有 GUI 都要归档，移交给美术 AI。

**处理**：

| 项 | 处理 |
|----|------|
| 黑白名单过滤 | `SearchRobotEntity` 新增 `filterMode`(0无/1黑/2白) + `ignoreFilterNbt` + `filterItems`(12 格 ItemStackHandler)，存 NBT；`canPickupItem`/`matchesFilter` 判定；`collectFromContainer`、`transferFromContainerToInventory` 两处取物入口加过滤 |
| 仓库 GUI 右侧面板 | `RobotInventoryMenu` 重写（加 `robot` 引用 + 12 过滤槽位 + `filterData` ContainerData，客户端构造器读过滤状态）；`RobotInventoryScreen` 加宽 176→256，右侧画「物品过滤」标题 + 名单模式按钮（循环无过滤/黑/白）+ 忽略后缀开关 + 12 过滤槽位 |
| 网络 | 新增 `RobotFilterPacket`（CYCLE_MODE=0 / TOGGLE_IGNORE=1）+ `ModNetworking` 注册；`mobInteract` 打开仓库时额外写 filterMode/ignoreNbt |
| GUI 归档 | 新增 `design/gui-assembler-spec.md`、`gui-inventory-spec.md`、`gui-archive-README.md`（操控台 spec 上一轮已有），三界面全部归档 |
| 收集诊断 | `CollectGoal` 每 5 秒打印 `[CollectGoal]` 状态日志，辅助排查「不收集」 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 过滤字段/方法/NBT 读写 + 两处取物入口过滤 + mobInteract 写过滤状态 |
| `menu/custom/RobotInventoryMenu.java` | 重写（robot 引用 + 过滤槽位 + filterData） |
| `client/screen/RobotInventoryScreen.java` | 重写（加宽 256 + 右侧过滤面板 + 点击发包） |
| `network/RobotFilterPacket.java`（新） | 过滤动作包 |
| `network/ModNetworking.java` | 注册 RobotFilterPacket |
| `entity/ai/CollectGoal.java` | 诊断日志 |
| `design/gui-assembler-spec.md` / `gui-inventory-spec.md` / `gui-archive-README.md`（新） | GUI 归档文档 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 21s，仅 4 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 已同步到测试游戏 mods 文件夹（search_bot-0.5.1.jar，2026-10-31 00:13）
- [ ] 进游戏验证：黑名单跳过对应物品 / 白名单只拿 / 忽略后缀开关 / 过滤设置持久化 / 物品可拖进过滤槽
- [ ] 复测收集模式（正确布局：目标容器 + 附近有物品的源容器），若仍不收集看 `[CollectGoal]` 日志
- [ ] 版本号仍为 0.5.1（本次未升，等用户确认黑白名单 OK 后再升 0.5.2 归档）

### 2026-10-30 —— 频道 GUI 行式重做 / 机器人命名系统

**用户需求**（两大块）：

① **操控台 GUI 重做成行式布局**：每一行 = 频道，最左频道名 → 模式按钮（显示模式前两字，**右键**切下一个模式）→ 机器人名称（**拖拽**：拖到某行=改绑该频道、拖到行外=解绑）→ 最右红色 ×（点后该频道**虚线框**、× 变**撤回箭头**，再点撤回恢复，**退出 GUI 后统一执行删除**）；频道列表下方 **+ 号**点击添加频道。

② **机器人命名规则**：核心决定编号——第一个核心做出后是「机器人01」，若某核心完全消失，之后做的核心**先补齐空缺编号**；核心**铁砧单独命名**后机器人显示该名字（命名牌效果）。

**处理**：

| 项 | 处理 |
|----|------|
| 命名编号 | 新增 `util/RobotNameCounter`（WorldSavedData：`nextNumber`+`freed` TreeSet，allocate 优先补空缺、release 回收、format→机器人%02d）；`item/custom/RobotCoreItem`（`onCraftedBy` 服务端分配编号、`getName` 显示机器人NN）；`SearchRobotEntity` 加 `robotNumber`+`getRobotDisplayName()`（有铁砧名优先）、`die()` 释放编号；`RobotAssemblerBlockEntity.assembleRobot` 转移编号到机器人 NBT |
| 频道数据重构 | `RobotControllerItem` 频道机器人从 `RobotIds`(UUID) 改为 `RobotEntry(id,name)`（存名字供拖拽显示），旧数据自动迁移；新增 `getChannelRobots/setChannelRobots/getChannelMode/setChannelMode/moveRobot/unbindRobot/deleteChannels/cycleModeForChannel` |
| 网络 | 新增 `ControllerRobotPacket`(MOVE/UNBIND)、`DeleteChannelsPacket`(批量删频道)；`ControllerActionPacket` 加 `CYCLE_MODE_CHANNEL=7` |
| GUI 重写 | `RobotControllerScreen` 完全重写：自定义绘制行式布局 + 拖拽（mouseClicked/mouseDragged/mouseReleased）+ 右键模式按钮 + × 删除标记/撤回 + + 号添加 + `onClose` 统一发送删除；改名 EditBox 移除（`renameChannel` 方法保留未接） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `util/RobotNameCounter.java`（新） | WorldSavedData 编号分配/释放/补空缺 |
| `item/custom/RobotCoreItem.java`（新） | 核心制作分配编号、getName 显示机器人NN |
| `item/ModItems.java` | ROBOT_CORE 改用 RobotCoreItem |
| `entity/custom/SearchRobotEntity.java` | robotNumber + getRobotDisplayName + die() 释放编号；RobotMode/RobotState.byId 改 public |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 组装时转移核心编号到机器人 |
| `item/custom/RobotControllerItem.java` | 频道数据改 RobotEntry(id,name)；新增 moveRobot/unbindRobot/deleteChannels/cycleModeForChannel |
| `network/ControllerRobotPacket.java` / `DeleteChannelsPacket.java`（新） | 拖拽/删除包 |
| `network/ControllerActionPacket.java` / `ModNetworking.java` | CYCLE_MODE_CHANNEL=7 + 注册两新包 |
| `client/screen/RobotControllerScreen.java` | 完全重写为行式 GUI |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 35s，仅 4 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 已同步到测试游戏 mods 文件夹（22:48 最新版，覆盖同名 0.5.1）
- [ ] 进游戏验证：核心编号 01/02…、铁砧命名生效、编号补空缺；GUI 拖拽换行/解绑、右键切模式、× 标记删除+撤回+退出执行、+ 添加频道
- [ ] 版本号仍为 0.5.1（本次未升版本，等用户确认后再发版本归档）

### 2026-10-30 —— 收集模式修复 / 频道删除解绑 / shift右键设收集容器

**用户 3 条反馈**：①收集模式不收集；②操控台不能删频道、每个频道机器人不能调整绑定；③收集容器设置改为 shift 右键容器（再次 shift 右键取消）。

**根因与处理**：

| 项 | 根因 | 处理 |
|----|------|------|
| ①收集不收集 | 设置收集目标要右键同一容器两次（第一次→运输终点，第二次→收集目标），只右键一次时 `collectTargetPos` 为空、`getModeCycle` 不含「收集」模式，切不进去 | 收集目标改由 shift 右键直接切换（见③）；设好后 `getModeCycle` 自动含「收集」，链路打通 |
| ②不能删频道/调绑定 | `RobotControllerItem` 只有 add/rename，无 delete；GUI 无删除按钮 | 新增 `deleteChannel`（至少保留一个频道，删频道自动解绑其机器人）与 `unbindCurrentChannel`（解绑当前频道全部）；GUI 加「删除频道」「解绑全部」按钮 |
| ③收集容器设置 | 原普通右键是「终点→收集→取消」状态机 | 改为：shift 右键容器=切换收集目标（再 shift 右键取消）；普通右键容器=切换运输终点（再右键取消）；运输与收集互斥 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | `handleControllerRightClick` 改为「切换运输终点」；新增 `toggleCollectTarget`（shift 右键切换收集目标）；运输/收集互斥 |
| `common/CommonEventHandler.java` | 右键容器区分 shift（收集目标）与普通右键（运输终点） |
| `item/custom/RobotControllerItem.java` | 新增 `deleteChannel` / `unbindCurrentChannel`；更新类注释 |
| `network/ControllerActionPacket.java` | 新增 `DELETE_CHANNEL=5` / `UNBIND_ALL=6` 动作 |
| `client/screen/RobotControllerScreen.java` | GUI 加高到 248，第二行加「删除频道」「解绑全部」，改名挪到第三行 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 1m，仅 4 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 已同步到测试游戏 mods 文件夹（22:19 最新版）
- [ ] 进游戏验证：shift 右键设收集目标→切收集模式→机器人搬运；删频道/解绑按钮生效；shift 右键再点取消收集

### 2026-10-30 —— 模型脖子 / 关节 pivot / 大小

**用户 3 点反馈**：①脖子不见了（建模时选错类）；②关节 pivot 不对——抬头时头像「掉下来」，上半身活动中心也奇怪；③机器人再小一点。

**根因与处理**：

| 项 | 根因 | 处理 |
|----|------|------|
| ①脖子不见 | `.bbmodel` 里 `neck1`/`neck2` 是 `type:mesh`，转换脚本只处理 `type:cube` 跳过 | 脚本新增 `mesh_to_cube_el()`，把 mesh vertices（相对 origin）换算成等价 cube；脖子生成（neck1 1.5×1×1.5 y11.5~12.5、neck2 2.5×1×2.5 y10.5~11.5） |
| ②抬头掉下来 | head 组 pivot 在 y=-3.05（脚底），头绕脚底画大弧 | 脚本 head pivot 重定向到 `HEAD_PIVOT_Y=11.95`（头底=脖子顶），子 cube offset 自动重算 |
| ②上半身 pivot | 保持在腰部 y=5.95（上下半身分界，标准做法）；修好 head pivot 后应自然 | 待用户进游戏反馈是否还需上移 |
| ③再小一点 | 渲染缩放 1.2 偏大 | 缩放 1.2→1.0（模型总高≈16.45px≈1 方块） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `D:\Blockbench\mcp\convert_bbmodel.py` | `mesh_to_cube_el()`；`emit_group` 支持 `origin_override`；`HEAD_PIVOT_Y=11.95`；setupAnim `head.xRot` 取负 |
| `client/model/SearchRobotModel.java` | 重新生成（40 cube 含脖子）；head pivot y=11.95 |
| `client/renderer/entity/SearchRobotRenderer.java` | 缩放 1.2→1.0 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 51s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] jar 已同步到测试游戏 mods 文件夹（21:48 最新版）
- [ ] 进游戏验证：脖子显示 / 抬头自然（绕脖子转）/ 大小约一格 / 上半身转身是否自然
- [ ] 上色（用户用 kimi 自理）

### 2026-10-30 —— 机器人 3D 模型 / 正面朝向 / 头身动画 / 看向目标

**用户 6 条反馈**：①机器人不知道正面，应视觉判断（面朝目标）；②头/上半身/下半身锁死，应有灵活度——头左右 ±45°、上半身左右 ±30°、只有头上下 ±30°、下半身锁死（像玩家）；③跟随模式看玩家用雪傀儡思路（有目标就主动看），收集/运输等有目标时都主动看目标；④模型全灰，本次暂不上色（用户之后用 kimi 自理）；⑤大小太大，机器人应一格高（1.5→1.2）；⑥瞬移装置距离判断改 10 格。

**处理**：

| 项 | 处理 |
|----|------|
| ①正面朝向 | 渲染器补原版 `180°−bodyYaw` 朝向旋转，模型脸（-Z）对准实体面朝方向 |
| ②头身动画 | Blockbench `boody` 组拆成 `upper_body`（腰部 pivot y=5.95）/`lower_body`；`setupAnim` 头部 ±45°/±30°、上半身 ±30°、下半身锁死 |
| ③看向目标 | 跟随 `setLookAt(owner)`；收集/运输 `setLookAt(容器)`，用 `getMaxHeadYRot/getMaxHeadXRot` 限位 |
| ④上色 | 未做（保持全灰） |
| ⑤大小 | 渲染缩放 1.5→1.2 |
| ⑥瞬移 10 格 | 代码早已 `10.0*10.0`，本次同步各处注释（5→10 格） |

**关键符号推导**（反编译原版字节码确认真相）：本渲染器直接继承 `EntityRenderer`、没有原版 `scale(-1,-1,1)`，故 `netHeadYaw = bodyYaw − headYaw`、`head.yRot = +netHeadYaw`、`head.xRot = −headPitch`、`upperBody.yRot = +netHeadYaw`。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `client/model/SearchRobotModel.java` | 转换脚本重新生成；拆分 upper_body/lower_body；setupAnim（头部 ±45°/±30°、上半身 ±30°，head.xRot 取负） |
| `client/renderer/entity/SearchRobotRenderer.java` | 缩放 1.2；`180°−bodyYaw`；`netHeadYaw = bodyYaw − headYaw`；`getViewXRot` |
| `entity/custom/SearchRobotEntity.java` | 新增 `getMaxHeadXRot()`=30 / `getMaxHeadYRot()`=45；瞬移注释 5→10 |
| `entity/ai/FollowOwnerGoal.java` | `setLookAt(owner)`；瞬移注释 5→10 |
| `entity/ai/CollectGoal.java` | 源/目标容器两处 `setLookAt`；瞬移注释 5→10 |
| `entity/ai/TransportGoal.java` | `setLookAt(点位)`；瞬移注释 5→10 |
| `entity/ai/CollectItemsGoal.java` | 瞬移注释 5→10 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 20s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [ ] 进游戏验证：正面朝向 / 头部 ±45°±30° / 上半身 ±30°下半身锁死 / 主动看目标 / 一格高 / 瞬移 10 格
- [ ] 上色（用户用 kimi 自理）

### 2026-10-30 —— v0.5.1 躯干等级系统 / 无线充电装置

**用户需求**：①新增「躯干」物品，躯干等级决定非核心组件槽位、也决定机器人血量与伤害减算（铜起步，每升一级功能槽+1/附件槽+1/血量+4/减算+1，减算公式=最终伤害 `max(0,伤害−减算)`）；②躯干和核心是核心组件，GUI 中间 2 格、少一个无法组装；③功能组件（机械臂/传感器/发动机/存储箱/电池）——传感器只取最高等级、装多没用，其余装多则功能叠加增强；④附件（每种只能装一个）——履带、螺旋桨（后续用飞行引擎替代）、传送装置、能量护盾、无线充电装置（有充电站时机器人无需返站、每时每刻由最近充电站耗电给机器人充）；⑤本次为 0.5.1。

**处理**：

| 项 | 处理 |
|----|------|
| 躯干查表 | `RobotStats` 新增功能槽/附件槽/血量/伤害减算 4 张表（铜5/1/8/0 → 终极10/6/28/5）+ 4 个 getter |
| 新物品 | `ModItems` 注册 `wireless_charging`；`PART_NAMES` 加 `torso`（42 件材料零件）；`isPart`/`getPartType` 支持 |
| 装配台重构 | `RobotAssemblerBlockEntity` 重写为 20 槽（2 核心+10 功能+6 附件+输入/输出）；`isItemValid` 按躯干动态解锁槽位、附件去重；`canAssemble` 只需躯干+核心；`assembleRobot` 零件叠加统计（传感器取 max、其余求和）、`max_health=躯干血量+护盾血量`、伤害减算由躯干决定；`disassembleRobot` 逐槽还原 |
| GUI | `RobotAssemblerMenu` 256×230，核心组件居中、功能组件 5×2、附件 6 格；`RobotAssemblerScreen` 拆解/组装按钮 + 区域标签 |
| 无线充电 | `SearchRobotEntity` 新增 `hasWirelessCharging`/`WIRELESS_CHARGE_RATE=500`/`tickWirelessCharging`（每 tick 从最近充电站拉电），`tick()` 调用 |
| 伤害减算 | `SearchRobotEntity.hurt()` 读 `damage_reduction`，最终伤害 `max(0, amount - reduction)` |
| 语言/材质 | lang 新增 6 躯干 + 无线充电装置；wheels→履带、shield→能量护盾；7 个新模型 + 7 个占位材质 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `util/RobotStats.java` | 新增躯干查表 + 4 getter |
| `item/ModItems.java` | 注册无线充电装置；`PART_NAMES` 加 torso；`isPart`/`getPartType` |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 完整重写（20 槽、动态解锁、叠加统计、伤害减算） |
| `menu/custom/RobotAssemblerMenu.java` | 完整重写（GUI 布局） |
| `client/screen/RobotAssemblerScreen.java` | 按钮/标签 |
| `entity/custom/SearchRobotEntity.java` | 无线充电 + 伤害减算 |
| `resources/.../lang/zh_cn.json` | 语言条目 |
| `item/ModCreativeTabs.java` | 创造标签加无线充电装置 |
| `resources/.../models/item/*.json` + `textures/item/*.png` | 7 新模型 + 7 占位材质 |
| `gradle.properties` | 0.4.3 → 0.5.1 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 22s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 版本升至 0.5.1，jar 已归档 `versions/search_bot-0.5.1.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.4.3.jar`）
- [ ] 进游戏验证：躯干+核心少一个无法组装；功能/附件槽随躯干等级解锁；血量/伤害减算按表变化；无线充电（有站全程充、无站不充）；组件叠加（传感器取 max、附件每种一个）
- [ ] 旧存档装配台 20 槽布局迁移兼容（不崩、物品不丢）
- [ ] 躯干×6、无线充电装置真材质（当前为占位色块）

### 2026-10-30 —— v0.4.3 频道模式同步 / 传送装置 5 格瞬移 / 安装 Blockbench+MCP

**用户 4 点反馈**：①绑定机器人后，机器人的模式没法立马和频道同步；②后绑定的机器人也切换不动；③瞬移装置效果应是「无需移动，短距离正常移动、超过五格距离直接瞬移」，且覆盖跟随玩家/探索物资/收集/运输；④好好看一下频道问题（即①②）。

**根因与处理**：

| 项 | 根因 | 处理 |
|----|------|------|
| ①绑定后模式不同步 | 模式是每个机器人各自存的（`DATA_ROBOT_MODE`），`bindRobot` 只加频道 id、不复制同频道已有机器人的模式/标记 | `bindRobot` 绑定成功后调新增 `syncNewRobotToChannel`，新机器人继承同频道第一个机器人的标记 + 模式 |
| ②后绑定的切不动 | `cycleCurrentMode` 对每个机器人各自 `cycleMode()`，各自 `getModeCycle()` 因标记不同而切出不同模式 → 频道内不同步 | `cycleCurrentMode` 改为「第一个机器人切，其余 `switchMode(first.getMode())` 对齐」，频道永远同一模式 |
| ③传送装置效果 | 旧逻辑「有传送装置就无条件瞬移」，没有「短距离正常走、远距离瞬移」的分界 | 新增 `teleportIfFar()`：>5 格瞬移（本 tick 由传送接管）、≤5 格走常规寻路；跟随/探索收集/收集/运输四 Goal 全部改用 |
| ④安装 Blockbench+MCP | 建模前置 | Blockbench 5.2.2 装到 `D:\Blockbench`；MCP 插件 `mcp.js` 放 `%APPDATA%\Blockbench\plugins\mcp\`（待用户授权网络权限激活） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 新增 `copyMarkersFrom()`、`teleportIfFar()` |
| `item/custom/RobotControllerItem.java` | `bindRobot` 调 `syncNewRobotToChannel`；新增 `syncNewRobotToChannel`；`cycleCurrentMode` 统一切模式 |
| `entity/ai/FollowOwnerGoal.java` | `tick()` >5 格先 `teleportIfFar` |
| `entity/ai/CollectGoal.java` / `CollectItemsGoal.java` / `TransportGoal.java` | 寻路入口 `teleportNearTarget` → `teleportIfFar` |
| `gradle.properties` | 0.4.2 → 0.4.3 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 45s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 版本升至 0.4.3，jar 已归档 `versions/search_bot-0.4.3.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.4.2.jar`）
- [ ] 进游戏验证：绑定第二个机器人后模式立即和频道同步；切模式频道内全部同步；传送装置 5 格内正常走、>5 格直接瞬移（四模式）
- [ ] Blockbench MCP 激活：用户授权网络权限后 `claude mcp add blockbench --transport http http://localhost:3000/bb-mcp`

### 2026-10-29 —— v0.4.2 白框框命中框 / 运输一笔画闭环 / 指数减速 / 安装 Blockbench

**用户 4 点反馈**：①白框要框「命中框」（像 F3+B 看到的线），不是所在方块；②运输模式是「就近 + 遍历」，应走成一笔画闭环，不该往返跑、起点经过两次；③到达后速度直接归零太生硬，改为进入交互范围后指数级减速；④安装 Blockbench 准备给机器人建模（建模是后续独立任务）。

**根因与处理**：

| 项 | 根因 | 处理 |
|----|------|------|
| ①白框框方块 | 上一版 `getBoundingBox().inflate(0.05)`，0.8×0.8 实体外扩后接近一格，看着像框方块 | 白框直接用 `getBoundingBox()`（真实碰撞箱 = F3+B 命中框），不再外扩 |
| ②运输往返跑 | 旧版每轮遍历完直接重置 `visitedPositions`，`nearestUnvisited` 从当前位置重选，机器人刚走完的点（当前位置）被立刻再选 → 往返跑/起点走两次 | 重写 `TransportGoal`：点位变化时用「就近贪心」（从当前位置每次选最近未访问点）生成固定遍历顺序，之后按该顺序循环走（取模回绕），每点每圈只走一次，形成一笔画闭环；一整圈无搬运则原地等 2 秒重扫 |
| ③到达瞬间归零 | 三 Goal 到达时调 `brake()` 直接把速度清零 | 新增 `smoothStop()`（停寻路 + 移动控制归零 + 速度 ×0.5 不清零）；飞行 `travel()` 停止分支改指数衰减（×0.6）；三 Goal 到达改 `smoothStop()`；`brake()` 保留给传送装置 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `client/ClientMarkerRenderer.java` | 白框 `getBoundingBox().inflate(0.05)` → `getBoundingBox()` |
| `entity/ai/TransportGoal.java` | 重写为「就近贪心固定顺序 + 循环遍历（一笔画闭环）」；去掉 `visitedPositions`/`nearestUnvisited`；到达改 `smoothStop()` |
| `entity/custom/SearchRobotEntity.java` | 新增 `smoothStop()`；飞行 `travel()` 停止分支改指数衰减 |
| `entity/ai/CollectGoal.java` | 到达 2 处 `brake()` → `smoothStop()` |
| `entity/ai/CollectItemsGoal.java` | 到达 1 处 `brake()` → `smoothStop()` |
| `gradle.properties` | 0.4.1 → 0.4.2 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 17s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 版本升至 0.4.2，jar 已归档 `versions/search_bot-0.4.2.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.4.1.jar`）
- [x] Blockbench 已通过 winget 发起安装（`JannisX11.Blockbench`）
- [ ] 进游戏验证：白框等于 F3+B 命中框；运输按就近顺序走闭环不往返；到达容器平滑减速

### 2026-10-29 —— v0.4.1 白框框实体 / 修复悬停不落地 / 直线稳定移动到位即停

**用户 5 点反馈**：①白框要框住实体（碰撞箱）而不是脚下方块；②机器人一直在空中悬停、改模式也没反应；③不在意重力影响，只要移动稳定、最好直线、到达终点就停；④仔细检查代码别反复出现同类问题；⑤本次算 0.4.1。

**根因与处理**：

| 项 | 根因 | 处理 |
|----|------|------|
| ①白框框脚下方块 | 上一版画白框用 `robot.blockPosition()`（脚下那一格） | 改为 `robot.getBoundingBox().inflate(0.05)`，按机器人真实碰撞箱画白框（框实体本体） |
| ②一直悬停、改模式没反应 | `isNoGravity()` 上一版改成 `return canFly`，导致「装螺旋桨就永远无视重力」，即使不飞也不落地、切模式也不落 | 恢复为 `return flyingActive`（只有「正在飞」才无视重力，步行恢复重力） |
| ③飞行乱飘/不稳 | `FlyingMoveControl` 是 bang-bang 控制（只按符号给 yya、且无水平分量），配合高空气倍率会上下振荡乱飘；且切模式后 `moveControl` 残留旧目标旧速度导致漂移 | 重写 `travel()` 空中分支：绕过 FlyingMoveControl，直接朝 `moveControl.getWantedX/Y/Z()` 计算带阻尼的 3D 速度向量（`speedModifier × MOVEMENT_SPEED`，与陆地同源），越近越慢、到位即停；`clearAllGoals()` 停寻路时同时把 moveControl 目标归零 + 清速度，杜绝切模式残留漂移 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | `isNoGravity()` 改回 `flyingActive`；`travel()` 空中分支重写为阻尼直线 3D 向量控制（绕开 FlyingMoveControl 的 bang-bang）；`clearAllGoals()` 增加 moveControl 归零 + 清速度 |
| `client/ClientMarkerRenderer.java` | 白框从 `robot.blockPosition()` 改为 `robot.getBoundingBox().inflate(0.05)`（框实体碰撞箱） |
| `gradle.properties` | 0.3.5 → 0.4.1 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 20s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 版本升至 0.4.1，jar 已归档 `versions/search_bot-0.4.1.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.3.5.jar`）
- [ ] 进游戏验证：白框贴机器人本体；地面步行不悬空、改模式（含待命）即停；飞行直线稳定、到达目的地停下

### 2026-10-29 —— v0.3.5 白框修复 / 螺旋桨无重力 / 飞行不乱飞 / 新增传送装置

**用户 4 点反馈**：①白框依旧没显现；②乱飞严重，因为机器人还受重力影响，装上螺旋桨后应不受重力；③机器人为了飞到天上目标飞来飞去；④新增「物品传送装置」（和轮子/螺旋桨同一类的移动部件），装上后可直接瞬移到目标容器交互范围内。

**根因与处理**：

| 项 | 根因 | 处理 |
|----|------|------|
| ①白框不显现 | 机器人 `robotId` 只存 NBT（`addAdditionalSaveData`），1.20.1 不自动同步到客户端，客户端 `getRobotId()` 现生成随机 UUID，永远匹配不上操控台 NBT 里的绑定 id | 新增 synched data `DATA_ROBOT_ID`(Optional&lt;UUID&gt;)；`getRobotId()` 客户端读 entityData、服务端懒生成并 `entityData.set` 同步；`loadRobotData` 读到 `RobotId` 时回填 entityData |
| ②螺旋桨仍受重力 | `isNoGravity()` 返回 `flyingActive`（是否「正在飞」），切换时受重力来回坠 | `isNoGravity()` 改为返回 `canFly`（有螺旋桨即永远无重力悬停/飞行，不再受切换影响） |
| ③飞行飞来飞去 | 「飞来飞去」是受重力来回坠、再爬升导致的上下振荡，与速度无关 | 飞行空气倍率保持与陆地同步（直接取 `MOVEMENT_SPEED` 属性 = 速度算子，约 0.36），不减速；振荡由 ② 的 `isNoGravity() = canFly` 消除 |
| ④传送装置 | 无此零件 | 新增无材料等级的移动部件 `teleporter`：注册物品 + `isPart` + 创造标签 + 装配台移动槽校验 + 语言条目 + 模型/材质占位 + `RobotStats` 移速/能耗系数；`SearchRobotEntity` 新增 `hasTeleporter()` / `teleportNearTarget()`（配合螺旋桨无视重力，瞬移到目标容器**四周可悬停的安全空位**，避开固体/岩浆/水、头顶无障碍，耗电 50 FE，10 tick 冷却）/ `findTeleportPosition()`；三 Goal（运输/收集/探索收集）的寻路入口优先瞬移 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 加 `DATA_ROBOT_ID`(Optional&lt;UUID&gt;) 同步；`getRobotId()` 客户端读/服务端同步；`loadRobotData` 回填；`isNoGravity()` 改 `canFly`；`travel()` 飞行空气倍率 = `MOVEMENT_SPEED`（与陆地同步）；`FLYING_SPEED` 基值 0.6→0.25；新增 `teleportCooldown` + `hasTeleporter()` + `teleportNearTarget()` + `findTeleportPosition()`（容器四周找安全空位悬停） |
| `util/RobotStats.java` | `MOVEMENT_SPEED_BONUS`/`MOVEMENT_ENERGY_COST` 加 `teleporter`（0.25 / 1.0） |
| `item/ModItems.java` | 注册 `TELEPORTER`；`isPart` 加 `teleporter` 分支 |
| `item/ModCreativeTabs.java` | 创造标签页加传送装置 |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 移动槽校验加 `teleporter` |
| `entity/ai/TransportGoal.java` | `moveTo()` 有传送装置时优先瞬移 |
| `entity/ai/CollectGoal.java` | `moveTo()` 有传送装置时优先瞬移 |
| `entity/ai/CollectItemsGoal.java` | 寻路入口有传送装置时优先瞬移 |
| `lang/zh_cn.json` | 加 `item.search_bot.teleporter`: 传送装置 |
| `models/item/teleporter.json` + `textures/item/teleporter.png`（新增） | 传送装置模型/材质（暂用螺旋桨材质占位） |
| `gradle.properties` | 0.3.4 → 0.3.5 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 21s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 版本升至 0.3.5，jar 已归档 `versions/search_bot-0.3.5.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.3.4.jar`）
- [ ] 进游戏验证：手持操控台当前频道绑定机器人出现完整格子白框；螺旋桨机器人不受重力、不再乱飞；飞到天上目标稳定且速度与陆地同步；装传送装置后瞬移到目标容器四周安全空位（避开障碍/岩浆）
- [ ] 传送装置真材质（当前为 propeller.png 占位）

### 2026-10-29 —— v0.3.4 运输遍历修复 / 飞行速度与陆地统一 / 白框放大到一格

**用户 3 点反馈**：①运输模式「遍历」没生效，有一个终点根本不去；②飞行速度还是太慢，且应和速度算子/陆地移动一致；③白色框没看见，要放大到一个格子。

**处理**：

| 项 | 根因 | 处理 |
|----|------|------|
| ①运输遍历失效 | 旧 `TransportGoal` 用 `isUsable` 门控（起点需有货、终点需有空），背包卸货到近终点后远端终点被判「不可用」，`pickNextTarget` 重置 visited 后重新偏好近点，远端终点被永久跳过 | 重写 `TransportGoal`：去掉可用性门控，改为 `nearestUnvisited` 纯最近邻 + 遍历（起点/终点一视同仁，每点一轮必去一次）；`doTransfer` 用搬运返回值累计 `movedThisRound`；一轮访问完且有搬运→立即开始下一轮，一轮无搬运→原地等 2 秒重扫 |
| ②飞行速度慢 | 飞行 `travel()` 空气倍率写死 `0.06F`，与陆地 `MOVEMENT_SPEED` 属性（零件算出约 0.36）脱钩，所以飞行比陆地慢 | 飞行空气倍率改用 `(float) getAttributeValue(Attributes.MOVEMENT_SPEED)`（与陆地同源）；`applyStats()` 同步把 `FLYING_SPEED` 也设为 `movement_speed`，保证飞行/陆地/速度算子三者一致 |
| ③白框太小 | 白框用 `getBoundingBox().inflate(0.2)`（贴实体小一圈，几乎看不见） | 改为 `drawBox(robot.blockPosition())`，画完整 1×1×1 方块线框 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/ai/TransportGoal.java` | 重写：去 `isUsable` 门控，改 `nearestUnvisited` 纯遍历 + `movedThisRound` 搬运累计 + 一轮无搬运原地等 2 秒 |
| `entity/custom/SearchRobotEntity.java` | `travel()` 飞行空气倍率 0.06F → `MOVEMENT_SPEED` 属性；`applyStats()` 同步 `FLYING_SPEED` = `movement_speed` |
| `client/ClientMarkerRenderer.java` | 白框从 `getBoundingBox().inflate(0.2)` 改为 `drawBox(robot.blockPosition())`（完整 1×1×1 方块） |
| `gradle.properties` | 0.3.3 → 0.3.4 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 20s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 版本升至 0.3.4，jar 已归档 `versions/search_bot-0.3.4.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.3.3.jar`）
- [ ] 进游戏验证：运输每个终点都会去；飞行速度明显变快且与步行一致；白框是一整个格子清晰可见

### 2026-10-29 —— v0.3.3 频道绑定严格分开 / 已绑定机器人白框显示

**用户 2 点需求**：①保证每个频道的绑定一定要分开（一个机器人只属于一个频道）；②被绑定的机器人用白框框住。

**处理**：

| 项 | 处理 |
|----|------|
| ①绑定严格分开 | `bindRobot` 绑定/解除统一先 `removeRobotFromAllChannels`（从所有频道移除该机器人 id），再写入当前频道，保证一个机器人同一时间只出现在一个频道 |
| ②白框 | `ClientMarkerRenderer` 手持操控台时，读当前频道 `RobotIds`，用 `mc.level.getEntitiesOfClass` 匹配附近机器人，对匹配到的机器人 `getBoundingBox().inflate(0.2)` 画白色线框 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `item/custom/RobotControllerItem.java` | `bindRobot` 解除分支也走 `removeRobotFromAllChannels`，绑定/解除都保证频道间不重叠 |
| `client/ClientMarkerRenderer.java` | 新增白色方框：当前频道已绑定机器人（读 `getCurrentChannelRobotIds` + 匹配实体画白框） |
| `gradle.properties` | 0.3.2 → 0.3.3 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 15s，无新增错误）
- [x] 版本升至 0.3.3，jar 已归档 `versions/search_bot-0.3.3.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.3.2.jar`）
- [ ] 进游戏验证：切换频道后各频道机器人互不重叠；当前频道机器人有白框，切频道白框跟着变

### 2026-10-29 —— v0.3.2 频道多机器人绑定 / 运输收集互斥 / 核心居中 / 核心命名同步

**用户 5 点需求**（v0.3.1 的后续修复）：①机器人总默认绑到 01 频道，应绑到操控台当前所在频道；②同一频道可绑定多个机器人；③设起点应取消同频道收集容器，设收集应取消同频道所有起点和终点（运输与收集互斥）；④机器人核心在装配台应处于中间格；⑤核心可被铁砧命名，组装后同步命名到机器人。

**处理**：

| 项 | 处理 |
|----|------|
| ①绑定到当前频道 | `bindRobot` 从「绑到第一个空频道」改为「绑到 `getCurrentChannelIndex()` 当前频道」 |
| ②一频道多机器人 | 频道 NBT 从单 `RobotId` 改为 `RobotIds` ListTag（兼容旧版单 id 自动迁移）；新增 `getChannelRobotIds`/`findCurrentRobots`（返回列表）；切模式/设标记遍历频道内所有机器人；GUI/tooltip 显示「已绑定 N 个」 |
| ③运输收集互斥 | `toggleTransportStart` 设起点时**总是**清空收集目标；`handleControllerRightClick` 设收集时清空**所有起点和终点**（`transportWaypoints.clear()`） |
| ④核心居中 | `RobotAssemblerMenu` 核心槽从右下 (104,58) 移到中心 (86,36)，移动2 挪到 (104,58) |
| ⑤核心命名同步 | `assembleRobot()` 读核心 `hasCustomHoverName()`/`getHoverName()`，写入机器人 `setHoverName` + `CustomName` JSON；`saveRobotData`/`loadRobotData` 读写 `CustomName`；拾取机器人时 `setHoverName` 保留物品名 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `item/custom/RobotControllerItem.java` | 频道 NBT 改多机器人 `RobotIds`；绑定到当前频道；`findCurrentRobot`→`findCurrentRobots`；切模式/通知/tooltip 多机器人 |
| `common/CommonEventHandler.java` | 左/右键容器遍历当前频道所有机器人；新增 `java.util.List` 导入 |
| `client/screen/RobotControllerScreen.java` | 频道行显示「[N个]」而非「[已绑定]」 |
| `entity/custom/SearchRobotEntity.java` | 运输收集互斥；`saveRobotData`/`loadRobotData` 读写 `CustomName`；拾取时 `setHoverName` |
| `menu/custom/RobotAssemblerMenu.java` | 核心槽移到中心 (86,36) |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | `assembleRobot()` 同步核心命名到机器人 |
| `gradle.properties` | 0.3.1 → 0.3.2 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 17s，仅 3 个既有 `ResourceLocation` deprecation 警告）
- [x] 版本升至 0.3.2，jar 已归档 `versions/search_bot-0.3.2.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.3.1.jar`）
- [ ] 进游戏验证：绑定到当前频道 / 一频道多机器人 / 运输收集互斥 / 核心居中 / 核心命名同步

### 2026-10-29 —— v0.3.1 大更新（待命/运输重做/收集规则/机器人核心/频道系统）

**用户 10 项规则**（大更新，版本 0.2.21 → 0.3.1）：①标记规则修正；②多机器人切模式空白 bug；③待命模式；④运输模式纯最近邻遍历；⑤收集模式规则；⑥0 电速度调慢；⑦记录规则文档；⑧机器人核心；⑨频道系统；⑩版本 0.3.1。

**处理**：

| 项 | 处理 |
|----|------|
| ①标记规则 | 右键容器「终点→收集→取消」；设收集时清空所有运输终点 |
| ②空白模式 | 操控台从「多绑定」改为「单频道绑定单机器人」（频道系统根治），不再有多机器人空白分支 |
| ③待命模式 | `RobotMode.STANDBY`（待命），不注册移动 Goal，原地不动 |
| ④运输重做 | 纯最近邻 + 遍历，起点/终点一视同仁（撤销「每点卸货」分支）；起点停 0.5 秒、终点=(起点数×0.5)÷终点数 |
| ⑤收集规则 | 就近源；背包满返回目标；目标满在目标旁原地不动 |
| ⑥0 电速度 | 0.05f → 0.02f |
| ⑦记录 | 新增 `design/plan-v0.3.1.md` 完整记录规则 |
| ⑧机器人核心 | 新元件 `robot_core`，必选，持有 `RobotId`；装配台 3×3 零件 + 核心槽 |
| ⑨频道系统 | shift 右键空气开 GUI；shift 滚轮切频道；绑定自动建频道；频道按序命名（频道01/02…） |
| ⑩版本 | `mod_version=0.3.1` |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 标记规则修正；STANDBY 模式；0 电 0.02f；getModeCycle 纳入待命；绑定后刷新标记 |
| `entity/ai/TransportGoal.java` | 重写为纯最近邻 + 遍历，起点 0.5 秒、终点均分 |
| `entity/ai/CollectGoal.java` | 背包满返回目标 / 目标满原地待命 |
| `item/ModItems.java` | 新增 ROBOT_CORE + getOrCreateCoreId |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 核心槽 SLOT_CORE、必选、组装读 id、拆解还原 |
| `menu/custom/RobotAssemblerMenu.java` | 9 零件槽 3×3 + 核心槽 |
| `item/custom/RobotControllerItem.java` | 重写为频道系统 |
| `menu/custom/RobotControllerMenu.java` / `client/screen/RobotControllerScreen.java` | 新增频道 GUI |
| `network/ControllerActionPacket.java` / `ControllerSyncPacket.java` / `ModNetworking.java` | 新增频道动作/同步网络包 |
| `client/ClientChannelScrollHandler.java` | shift 滚轮切频道 |
| `common/CommonEventHandler.java` | 单机器人 + syncMarkers |
| `design/plan-v0.3.1.md` | 新增规则/设计文档 |
| `resources/.../robot_core.*` + lang | 新增机器人核心资源（材质占位） |
| `gradle.properties` | 0.2.21 → 0.3.1 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.3.1，jar 已归档 `versions/search_bot-0.3.1.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.21.jar`）
- [x] 修复旧存档装配台崩溃（`ItemStackHandler.deserializeNBT` 读旧 `Size:10` 缩槽 → `load()` 逐槽回填忽略旧 Size）
- [ ] 进游戏验证：标记规则 / 待命 / 运输纯遍历 / 收集规则 / 频道系统 / 机器人核心组装
- [ ] 机器人核心真材质（当前为 engine.png 占位）

### 2026-10-29 —— v0.2.21 运输最近邻遍历 / 多机器人模式名 / 0 电缓慢返航充电

**用户 3 点反馈**：①运输模式同一个终点被去两次，改为「最近邻 + 遍历」；②多机器人绑定时切换模式只显示「已切换 N 个」看不见模式名；③主动充电没实现，且最初设定 0 电也缓慢移动去充电。

**根因与修复**：

| 项 | 处理 |
|----|------|
| ①运输重复去终点 | 顺序遍历改为「最近邻 + 遍历」：背包空→最近的「有货起点」搬货，有货→最近的「有空终点」卸货；每轮 visited 去重，每点一轮只去一次、访问完重置；起点空/终点满跳过 |
| ②多机器人模式名 | `RobotControllerItem.use()` 多绑定分支：若所有机器人切到同一模式，显示「已切换 N 个机器人到「X」模式」 |
| ③0 电返航充电 | `updateRobotState()` 0 电时不再清目标改 OUT_OF_POWER，而是找充电站设 RETURNING_TO_CHARGE（没站才 OUT_OF_POWER）；`ReturnToChargeGoal.canUse()` 去掉 `energy>0` 限制，并在 `tick()` 到达 2 格内直接 `insertRobot`（原版只在 `stop()` 判距离，导致机器人到了站却永不存入） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/ai/TransportGoal.java` | 顺序遍历重写为最近邻 + 遍历（visited 去重 + 有货找终点/无货找起点） |
| `entity/ai/ReturnToChargeGoal.java` | 去掉 `energy>0`；到达 2 格内在 `tick()` 直接存入充电站；`stop()` 只停寻路 |
| `entity/custom/SearchRobotEntity.java` | `updateRobotState()` 0 电返航充电 + 满电清除残留旧充电目标 |
| `item/custom/RobotControllerItem.java` | 多机器人切换后显示模式名 |
| `gradle.properties` | 0.2.20 → 0.2.21 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 19s，无新增警告）
- [x] 版本升至 0.2.21，jar 已归档 `versions/search_bot-0.2.21.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.20.jar`）
- [ ] 进游戏验证：运输最近邻遍历不重复 / 多机器人显示模式名 / 0 电缓慢返航充电

### 2026-10-29 —— v0.2.20 运输循环/到达交互/停留 0.5 秒/模式名反馈

**用户 5 点反馈**：①运输模式没在几个点位循环走动；②要保证抵达后能和容器交互；③停留 0.5 秒；④右键操控台「模式没切换」；⑤看不见切到了什么模式。

**根因与修复**：

| 项 | 处理 |
|----|------|
| ①②到达罚站不循环 | 到达判定 `reach` 从 `interact_radius+0.5`（铜臂 1.5 格）改为 `Math.max(interact_radius+0.5, 2.0)`，至少 2 格，覆盖寻路精度误差（停在容器旁 1.5~2 格仍判未到达） |
| ③停留 0.5 秒 | `TransportGoal.DWELL_TICKS` 40→10 |
| ④⑤模式反馈 | 上一版丢掉了模式名提示，且「跟随/探索」行为相似看不出变化；恢复 `use()` 切换后显示「当前模式：跟随/探索/收集/运输」 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/ai/TransportGoal.java` | `DWELL_TICKS` 40→10；到达判定 `Math.max(interact_radius+0.5, 2.0)` |
| `entity/ai/CollectGoal.java` | 到达判定 `Math.max(interact_radius+0.5, 2.0)`（源/目标 2 处） |
| `item/custom/RobotControllerItem.java` | `use()` 切换后显示「当前模式：XXX」 |
| `gradle.properties` | 0.2.19 → 0.2.20 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 16s，无新增警告）
- [x] 版本升至 0.2.20，jar 已归档 `versions/search_bot-0.2.20.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.19.jar`）
- [ ] 进游戏验证：运输循环 / 到达搬运 / 停留 0.5 秒 / 右键空气显示模式名

### 2026-10-29 —— v0.2.19 六个问题修复（到达交互/多绑定/右键优先/充电台取物/紫色创造站）

**用户 6 点需求**：①到达后保证能交互不罚站；②运输每点停留相等；③操控台多绑定 + 再 shift 右键解绑；④手持机器人右键优先方块功能；⑤充电台充电时右键取出物品；⑥创造充电台秒充电 + 紫色外观。

**修复**：

| 项 | 处理 |
|----|------|
| ①②到达罚站 | 到达判定 `reach = getInteractionRadius() + 0.5`（补偿容器半边长），`TransportGoal` + `CollectGoal`（源/目标两处） |
| ③多绑定 | 操控台改多绑定（NBT `BoundRobots` ListTag），新增机器人稳定标识 `RobotId`（跨充电站存取不变）；`toggleBind`/`findBoundRobots`，切模式/设标记应用到全部绑定机器人 |
| ④右键优先 | `CommonEventHandler.isInteractableBlock()`，容器/GUI 方块放行给方块，普通方块才放机器人 |
| ⑤充电台取物 | `ChargingStationBlockEntity.extractRobotAsItem()` 返回物品；`ChargingStationBlock.use()` 空手右键改塞物品 |
| ⑥创造站紫色 | 生成紫色材质 `creative_charging_station.png` + json 指向 `search_bot:block/creative_charging_station`（秒充逻辑 tick 已有） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/ai/TransportGoal.java` / `CollectGoal.java` | 到达判定 `reach = interact_radius + 0.5` |
| `entity/custom/SearchRobotEntity.java` | 新增 `robotId` + `getRobotId()` + save/load 读写；`mobInteract` 绑定改 `toggleBind` |
| `item/custom/RobotControllerItem.java` | 重写为多绑定（`toggleBind`/`isBound`/`getBoundCount`/`unbindAll`/`findBoundRobots`/`syncMarkersFromRobots`） |
| `common/CommonEventHandler.java` | 新增 `isInteractableBlock`；左/右键容器改遍历所有绑定机器人 |
| `block/custom/ChargingStationBlock.java` | 空手右键「取出物品」而非 `ejectRobot()` |
| `block/entity/custom/ChargingStationBlockEntity.java` | 新增 `extractRobotAsItem()` |
| `resources/models/block/creative_charging_station.json` + 新紫色材质 | 创造站紫色外观 |
| `gradle.properties` | 0.2.18 → 0.2.19 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 23s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.19，jar 已归档 `versions/search_bot-0.2.19.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.18.jar`）
- [ ] 进游戏验证：到达不罚站 / 运输每点 2 秒相等 / 多绑定切换 / 右键优先方块 / 充电台取物 / 创造站紫色+秒充

### 2026-10-29 —— v0.2.18 恢复飞行速度（v0.2.8「又快又稳」）+ 寻路频率恢复 60%

**用户反馈**：
1. 飞行速度「比蜗牛还慢」，要恢复（对应 v0.2.8「又快又稳」的参数）。
2. 寻路频率降太多了，恢复 60%。

**根因**：
- 飞行慢的直接原因是 `travel()` 空气倍率在 v0.2.9 被从 `0.06F` 压回 `0.02F`，`FLYING_SPEED` 也从 `0.6` 压回 `0.25`；v0.2.8 的「又快又稳」正是 `0.06F + 0.6`。
- 寻路：v0.2.15 把 `repathCooldown` 从 15 提到 60（降到原频率 25%），降过头了。

**修复**：

| 项 | 处理 |
|----|------|
| 飞行速度恢复 | `FLYING_SPEED` 0.25→0.6；`travel()` 空气倍率 0.02F→0.06F（回 v0.2.8 又快又稳） |
| 去掉同步覆盖 | 撤销 v0.2.17 里 `applyStats()` 让 `FLYING_SPEED` 跟随 `MOVEMENT_SPEED` 的逻辑（避免把 0.6 覆盖成 0.36） |
| 寻路频率恢复 60% | `TransportGoal` `repathCooldown` 60→25（约 1.25 秒一次，= 原 15 tick 的 60%） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | `FLYING_SPEED` 0.6；`travel()` 空气倍率 0.06F；`applyStats()` 删除 FLYING_SPEED 同步 |
| `entity/ai/TransportGoal.java` | `repathCooldown` 60→25 |
| `gradle.properties` | 0.2.17 → 0.2.18 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 1m11s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.18，jar 已归档 `versions/search_bot-0.2.18.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.17.jar`）
- [ ] 进游戏验证：飞行速度恢复正常（又快又稳）；运输寻路约 1.25 秒一次

### 2026-10-29 —— v0.2.17 修复崩溃：恢复 FLYING_SPEED 属性 + 飞行/陆地速度真正统一

**崩溃**（用户测试反馈）：进游戏后机器人 tick 崩溃。

**根因**：上一轮 v0.2.16 删掉了 `FLYING_SPEED` 属性，但 `FlyingMoveControl` 在空中飞行时（`FlyingMoveControl.java:39`）会读取 `Attributes.FLYING_SPEED`，属性不存在 → `IllegalArgumentException: Can't find attribute minecraft:generic.flying_speed`。

**修复**（`SearchRobotEntity.java`）：

| 项 | 处理 |
|----|------|
| 恢复属性 | `createAttributes()` 加回 `.add(Attributes.FLYING_SPEED, 0.25)` |
| 速度真正统一 | `applyStats()` 里设置 `MOVEMENT_SPEED` 的同时，把 `FLYING_SPEED` 设成同一个值（组装后飞行/陆地都是零件算出的 `movement_speed`） |

> 这也顺带解释了最初「飞行比陆地慢」的真正原因：组装后陆地 `MOVEMENT_SPEED` 被零件数值覆盖（约 0.36），但 `FLYING_SPEED` 一直是固定 0.25，所以飞行比陆地慢。现在两者同步，真正统一。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 恢复 `FLYING_SPEED` 属性；`applyStats()` 里 `FLYING_SPEED` 跟随 `MOVEMENT_SPEED` |
| `gradle.properties` | 0.2.16 → 0.2.17 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 21s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.17，jar 已归档 `versions/search_bot-0.2.17.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.16.jar`）
- [ ] 进游戏验证：不再崩溃；飞行与步行速度一致

### 2026-10-29 —— v0.2.16 飞行速度与陆地统一（撤销 0.6 飞行提速）

**用户澄清**：
1. 飞行速度应和陆地移动速度**统一**（都用同一个 `MOVEMENT_SPEED`），不要单独的更快飞行速度。
2. 确认寻路频率只降了运输模式，没动全局。

**修复**：

| 项 | 处理 |
|----|------|
| 飞行/陆地速度统一 | 撤销上一轮「飞行用 FLYING_SPEED 0.6」；`tick()` 里飞行和步行统一用 `MOVEMENT_SPEED` |
| 移除无用属性 | 删除 `createAttributes()` 里 `FLYING_SPEED`（全项目仅此两处引用，删后无残留） |
| 寻路频率范围确认 | 只有 `TransportGoal`（运输）`repathCooldown=60`；`CollectGoal`（收集）仍是 15，未改全局 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 删 `FLYING_SPEED` 属性；`tick()` 飞行/步行统一用 `MOVEMENT_SPEED` |
| `gradle.properties` | 0.2.15 → 0.2.16 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 20s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.16，jar 已归档 `versions/search_bot-0.2.16.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.15.jar`）
- [ ] 进游戏验证：飞行与步行速度一致

### 2026-10-29 —— v0.2.15 大幅降低运输寻路频率 + 飞行提速

**本次修改目标**（用户反馈）：
1. 运输模式寻路太频繁，要大幅降低寻路频率。
2. 飞行速度太慢，要改回更快的速度。

**根因**：
- 寻路：`TransportGoal.moveTo()` 每 15 tick（0.75 秒）就重算一次路径，太频繁。
- 飞行速度：`FLYING_SPEED` 属性虽然注册了，但飞行实际用的是 `MOVEMENT_SPEED`（和走路一样慢），`FLYING_SPEED` 一直没被接到移动逻辑里。

**修复**：

| 项 | 处理 |
|----|------|
| 降低寻路频率 | `TransportGoal` 的 `repathCooldown` 15 → 60 tick（约 3 秒才重算一次路径） |
| 切点位立即寻路 | 停留结束/跳点后 `repathCooldown = 0`，不干等冷却 |
| 飞行提速 | `FLYING_SPEED` 0.25 → 0.6；`tick()` 里飞行用 `FLYING_SPEED`、步行用 `MOVEMENT_SPEED` |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/ai/TransportGoal.java` | `repathCooldown` 15→60；切点位/跳点后归零立即寻路 |
| `entity/custom/SearchRobotEntity.java` | `FLYING_SPEED` 0.25→0.6；`tick()` 按 `flyingActive` 选飞行/步行速度 |
| `gradle.properties` | 0.2.14 → 0.2.15 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 25s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.15，jar 已归档 `versions/search_bot-0.2.15.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.14.jar`）
- [ ] 进游戏验证：运输模式寻路卡顿减少、飞行速度明显变快

### 2026-10-29 —— v0.2.14 修复「按需飞行」切换丢目标导致飞不上去

**问题**（用户测试反馈）：终点容器在天上时，机器人只在下面蹦弹，不飞上去（明明有螺旋桨）。

**根因**：
1. `updateFlyingActive()` 靠当前导航器的 `getTargetPos()` 判断要不要飞；但 `applyMovementMode()` 切换导航器（地面↔飞行）后，新导航器是空的没有目标，下一 tick 又判定「没目标、不用飞」切回地面，来回抖，永远飞不上去。
2. 飞到目标附近后导航目标被清空（`brake()`），被误判为「不用飞」而坠落。

**修复**：

| 项 | 处理 |
|----|------|
| 切换不丢目标 | 切换前先记住 `navTarget`，切换后 `moveTo(navTarget)` 把目标交给新导航器 |
| 收集/运输加滞回 | 目标在 ±1 格内保持当前状态（飞则悬停、步行则继续走）；目标明显在下方（<-1）才落地 |
| 跟随/探索不动 | `owner != null` 时不重设导航（飞行由 FollowOwnerGoal 用 moveControl 直接控制） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 重写 `updateFlyingActive()`：切换前记住目标、切换后重设、非跟随模式加滞回 |
| `gradle.properties` | 0.2.13 → 0.2.14 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 27s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.14，jar 已归档 `versions/search_bot-0.2.14.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.13.jar`）
- [ ] 进游戏验证：机器人能飞上天去够容器，到达后悬停不再坠落

### 2026-10-29 —— v0.2.13 交互半径归机械臂 + 搜索半径归传感器

**本次修改目标**（用户设计决策）：
1. 交互半径应取决于**机械臂**（手），不该放在传感器上。
2. 传感器**只决定探索/搜索范围**（眼睛），不再管交互距离。

**修复**：

| 项 | 处理 |
|----|------|
| 交互半径归机械臂 | 新增 `ARM_RADIUS` 表，铜 1.0 起步每次 +0.5：铜1.0/铁1.5/金2.0/钻2.5/合金3.0/终极3.5；半径存储从 int 改 double |
| 搜索半径归传感器 | 收集模式找源容器、探索模式找容器，从写死 10/12 改用传感器 `search_radius`（铜6/铁8/金10/钻12/合金14/终极16） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `util/RobotStats.java` | 新增 `ARM_RADIUS`(1.0~3.5) + `getArmRadius()`；`SENSOR_RADIUS` 改 6/8/10/12/14/16 + `getSensorRadius()` |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 机械臂写 `interact_radius`(putDouble)；传感器注释改「只决定探索/搜索范围」 |
| `entity/custom/SearchRobotEntity.java` | `getInteractionRadius()` 改读 `interact_radius`(double)；新增 `getStatDouble()`/`getSearchRadius()`；`findContainerInInteractionRadius` 半径 `Math.ceil` 取整 |
| `entity/ai/CollectGoal.java` | 删写死 `RADIUS=10`，`findSourceContainer` 改用 `getSearchRadius()` |
| `entity/ai/CollectItemsGoal.java` | 删写死 `SEARCH_RADIUS=12`，搜索/判定改用 `getSearchRadius()` |
| `gradle.properties` | 0.2.11 → 0.2.13 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL，仅 4~5 个既有 deprecation 警告）
- [x] 版本升至 0.2.13，jar 已归档 `versions/search_bot-0.2.13.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.12.jar`）
- [ ] 进游戏验证：铜臂 1 格贴脸、铁臂 1.5 格；传感器搜索范围生效

> 传感器搜索半径（铜6/铁8/金10/钻12/合金14/终极16）——以玩家（探索）或收集目标容器（收集）为中心的空间半径。

### 2026-10-28 —— v0.2.11 交互半径内就地交互 + 到达刹停（方案 A）

**本次修改目标**（用户两点）：
1. 只要在交互半径内就不用移动（就地取物/搬运）。
2. 选用方案 A：到达目的地立刻刹停，避免带惯性冲过头。

**修复**：

| 项 | 处理 |
|----|------|
| 交互半径内不移动 | 三 Goal 到达判定从固定 2 格改为 `getInteractionRadius()`（默认 3 格） |
| 到达刹停（方案 A） | 实体新增 `brake()`（停寻路 + moveControl 归零 + 清速度），三 Goal 到达时调用 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 新增 `brake()` |
| `entity/ai/TransportGoal.java` | 删 `ARRIVE_DIST_SQR`；交互半径判定 + `brake()` |
| `entity/ai/CollectGoal.java` | 同上（源/目标两处） |
| `entity/ai/CollectItemsGoal.java` | 同上 |
| `gradle.properties` | 0.2.10 → 0.2.11 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 20s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.11，jar 已归档 `versions/search_bot-0.2.11.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.10.jar`）
- [ ] 进游戏验证：交互半径内就地交互不贴脸 / 到达不再冲过头

### 2026-10-28 —— v0.2.10 撤销降速 + 按需飞行 + 恢复上台阶

**本次修改目标**（用户 4 点反馈，前 3 点改、第 4 点讨论）：
1. 曲线降速导致短距离像乌龟 → 撤销降速。
2. 不需要飞的时候别飞（节能）。
3. 自动上台阶改回来。
4. 「既然没有移动惯性为什么路线还不稳」→ 先讨论、未改。

**修复**：

| 项 | 处理 |
|----|------|
| 撤销降速 | 三 Goal 删 `speedFor()`/`setSpeedModifier`，寻路速度回固定 **1.0** |
| 按需飞行 | 拆 `canFly`(能力) 与 `flyingActive`(当前是否真飞)；平地步行、目标在头顶上方 >1 格才飞；`isNoGravity`/`travel` 改用 `flyingActive` |
| 恢复上台阶 | 删构造器 `setMaxUpStep(0.0F)`，回原版默认 0.6 格 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 删 `setMaxUpStep(0.0F)`；加 `flyingActive`+`FLY_TRIGGER_Y`；`updateCanFly` 只改能力；新增 `applyMovementMode`/`updateFlyingActive`/`getDesiredTargetY`；`isNoGravity`/`travel` 改 `flyingActive`；加 `isFlyingActive()` |
| `entity/ai/FollowOwnerGoal.java` | `canFly()` → `isFlyingActive()`（飞行分支 + 3D/2D 距离） |
| `entity/ai/TransportGoal.java` | 撤销曲线降速 |
| `entity/ai/CollectGoal.java` | 撤销曲线降速 |
| `entity/ai/CollectItemsGoal.java` | 撤销曲线降速（删 repathCooldown/speedFor/常量） |
| `gradle.properties` | 0.2.9 → 0.2.10 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 1m9s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.10，jar 已归档 `versions/search_bot-0.2.10.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.9.jar`）
- [ ] 进游戏验证：短距离不慢 / 平地步行不悬空 / 上台阶恢复
- [ ] 第 4 点（路线不稳）讨论后待改

### 2026-10-28 —— v0.2.9 关闭自动上台阶 + 飞行悬停 + 靠近减速

**本次修改目标**（用户 4 点反馈）：
1. 「上跳瞎窜」没解决，所以不是速度问题 → 把移动速度调回去。
2. 飞行要能悬停的直线运动，不要一上一下。
3. 一跳一跳是「自动上一格高方块」导致的。
4. 按既定最短路线走，用曲线速度，靠近目的地减速。

**修复**：

| 项 | 处理 |
|----|------|
| 移动速度调回 | `FLYING_SPEED` 0.6→**0.25**、飞行空气倍率 0.06F→**0.02F**（回原版默认） |
| 飞行悬停直线 | `FollowOwnerGoal` 飞行分支改为固定飞向主人头顶 `+1.0` 格悬停，删 `canSeeOwner()` 分支 |
| 关自动上台阶 | `SearchRobotEntity` 构造器 `setMaxUpStep(0.0F)`（原版默认 0.6 格） |
| 曲线减速 | `TransportGoal`/`CollectGoal`/`CollectItemsGoal` 统一加 `speedFor()`：4 格外全速、2~4 格线性降到 0.25，用 `setSpeedModifier` 平滑降速不重寻路 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 构造器 `setMaxUpStep(0.0F)`；`FLYING_SPEED` 0.6→0.25；飞行空气倍率 0.06F→0.02F |
| `entity/ai/FollowOwnerGoal.java` | 飞行分支固定高度悬停直线飞；删 `canSeeOwner()` |
| `entity/ai/TransportGoal.java` | 加 `DECEL_DIST_SQR`/`MIN_SPEED` + `speedFor()` + `setSpeedModifier` 平滑减速 |
| `entity/ai/CollectGoal.java` | 同上（源/目标容器两处寻路都减速） |
| `entity/ai/CollectItemsGoal.java` | 加 `repathCooldown` + 常量 + `speedFor()` + `setSpeedModifier` |
| `gradle.properties` | 0.2.8 → 0.2.9 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 41s，仅 4 个既有 deprecation 警告）
- [x] 版本升至 0.2.9，jar 已归档 `versions/search_bot-0.2.9.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.8.jar`）
- [ ] 进游戏验证：步行不再一跳一跳 / 飞行悬停直线 / 靠近减速稳停 / 速度手感

### 2026-10-28 —— v0.2.8 删除爬行肢 + 飞行又快又稳

**本次修改目标**（用户两点要求）：
1. 飞行速度希望「又快又稳」二者兼得（之前 v0.2.7 对齐原版后偏慢）。
2. 删掉爬行肢和相关爬墙代码，放到特殊废案档里。

**修复**：

| 项 | 处理 |
|----|------|
| 飞行又快又稳 | 空气倍率 0.02F→**0.06F**、`FLYING_SPEED` 0.25→**0.6**；比例控制下越近越慢、天然稳停 |
| 删爬行肢 | 删物品注册、属性系数、装配器校验、创造标签、语言条目、模型/贴图 |
| 删爬墙代码 | 删实体 `onClimbable`/`isClimbing`/`climbTimer`/上推/摔落减免 + `FollowOwnerGoal` 整套爬墙分支 |
| 归档 | 全部被删代码写入 `废案档/爬行肢与爬墙功能.md`（含恢复方法） |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 删爬墙相关 + 飞行倍率/速度上调 |
| `entity/ai/FollowOwnerGoal.java` | 简化（去爬墙），距离统一「飞行 3D、步行 2D」 |
| `item/ModItems.java` | 删 `CRAWLING_LIMBS` 注册与 isPart 分支 |
| `util/RobotStats.java` | 删 crawling_limbs 两条系数 |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 移动槽校验删 crawling_limbs |
| `item/ModCreativeTabs.java` | 删爬行肢条目 |
| `lang/zh_cn.json` | 删爬行肢语言条目 |
| 8 个资源文件 | 删 crawling_limbs 模型/贴图 |
| `废案档/爬行肢与爬墙功能.md`（新增） | 归档被删代码 |
| `gradle.properties` | 0.2.7 → 0.2.8 |

**验证状态**：
- [x] 代码完整构建通过（BUILD SUCCESSFUL in 24s，仅既有 deprecation 警告）
- [x] 版本升至 0.2.8，jar 已归档 `versions/search_bot-0.2.8.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.7.jar`）
- [ ] 进游戏验证：飞行又快又稳 / 步行不蹦、到达即停 / 创造标签无爬行肢

### 2026-10-28 —— v0.2.7 修复标记冲突 + 稳定移动

**本次修改目标**（用户反馈的两个问题）：
1. 设起点时同时把收集目标箱子取消记录（同一容器不能既是蓝框起点又是绿框收集目标）。
2. 机器人移动「上蹦瞎窜、到达后蹦来蹦去」，需要稳定移动 + 稳稳停住（含飞行模式）。

**根因与修复**：

| 问题 | 根因 | 修复 |
|------|------|------|
| 蓝框+绿框重叠 | `toggleTransportStart` 设起点没清除 `collectTargetPos` | 设起点前先清同容器收集标记 |
| 上蹦瞎窜 | 蜘蛛爬墙 `onClimbable()` 撞墙即 true，`travel()` 每 tick 上推 0.15 格，与重力来回拉扯 | 暂停爬墙：`onClimbable()` 返回 false、删 `travel()` 上推代码 |
| 飞行抖动/过冲 | 飞行 `travel()` 空气倍率用 `getSpeed()`（0.25，原版 12.5 倍） | 对齐原版 `FlyingMob`：空中 0.02F、落地 0.1F·f1 |
| 构建反复卡网络 | ForgeGradle 动态版本 `[6.0,6.2)` TTL 过期反复重解析 | 固定 `6.0.54` + settings.gradle 把 Forge maven 前置 |

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | `toggleTransportStart` 清除收集标记；`onClimbable()` 暂停爬墙；`travel()` 删步行上推 + 飞行空气分支对齐原版 0.02F |
| `gradle.properties` | `mod_version` 0.2.6 → 0.2.7 |
| `build.gradle` | ForgeGradle `[6.0,6.2)` → 固定 `6.0.54` |
| `settings.gradle` | `pluginManagement` 仓库顺序：MinecraftForge 前置 gradlePluginPortal |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL in 48s，仅既有 deprecation 警告）
- [x] 版本升至 0.2.7，jar 已归档 `versions/search_bot-0.2.7.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.6.jar`）
- [ ] 进游戏验证：标记不重叠 / 移动稳定不蹦、到达即停 / 飞行稳定

### 2026-10-28 —— v0.2.6 五点功能：探索主动收集 / 容器标记方框 / 多点位运输

**本次修改目标**（用户 5 点需求）：
1. 模式切换已正常（0.2.5 修复）。
2. 探索模式要**主动收集玩家附近容器**（之前只跟随 + 路过收集）。
3. 手持操控台显示容器标记方框：起点=天蓝、终点=橙、收集=绿；且收集与运输标记要共存（不再互相覆盖）。
4. 左键起点容器再左键取消；右键收集容器再右键取消。
5. 支持多个起点/终点，运输移动判断改为「每个点位停留时间相等、短暂停留即可」；点位顺序=点击顺序；起点空跳过、终点满跳过；全被跳过则原地等待每 2 秒重扫。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 单点 `transportStartPos/transportEndPos` 改为有序 `List<TransportWaypoint>`（含 isStart）；NBT 用 `TransportWaypoints` ListTag 存取（兼容旧单点迁移）；新增 `getTransportStarts/Ends`、`isTransportStart`、`toggleTransportStart`、`handleControllerRightClick`（右键状态机）、`containerIsFull`；EXPLORE 分支 `CollectItemsGoal` 升为 priority 2、`FollowOwnerGoal` 降到 3 |
| `entity/ai/TransportGoal.java` | 重写为多点位巡游：按点击顺序、每点停留 40 tick（2 秒）、搬空/搬满提前下一、起点空/终点满跳过、全跳过则原地等待每 2 秒重扫 |
| `entity/ai/CollectItemsGoal.java` | 加 20 tick 扫描冷却（无目标时 1 秒重扫，节约算力）；目标离玩家超过 12 格则放弃（防止跑远） |
| `item/custom/RobotControllerItem.java` | 删除「暂存 TransportStart」逻辑；新增 `syncMarkers`（把机器人标记镜像写进操控台 NBT 供客户端渲染）+ `getMarkerStarts/Ends/Collect`；tooltip 显示标记数量 |
| `common/CommonEventHandler.java` | 左键=切换运输起点；右键=终点(橙)→收集(绿，清空所有起点)→取消 状态机 |
| `client/ClientMarkerRenderer.java`（新增） | FORGE 总线 + DIST.CLIENT，`RenderLevelStageEvent.AFTER_TRANSLUCENT_BLOCKS` 渲染天蓝/橙/绿方框 |

**⚠️ 行为变化**：设置/取消标记（左/右键容器）**不再自动切换模式**；设好标记后用「右键空气」循环切到「收集」或「运输」模式让机器人开工（模式循环会自动纳入已设的收集/运输项）。

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL，仅既有 deprecation 警告）
- [x] 版本升至 0.2.6，jar 已归档 `versions/search_bot-0.2.6.jar`
- [x] 已同步到测试游戏 mods 文件夹（删除旧 `search_bot-0.2.5.jar`）
- [ ] 进游戏验证：探索模式主动收集 / 手持操控台显示标记方框 / 多点位运输停留时间

### 2026-10-28 —— v0.2.5 真正修复「切换模式后机器人呆在原地」（GoalSelector.removeAllGoals 不释放 flag 锁）

**本次修改目标**：0.2.4 用户反馈「切模式后呆在原地、和之前没区别」。读测试日志确认 `switchMode` 本身正常，反编译原版 `GoalSelector` 确认根因：1.20.1 的 `removeAllGoals` 只把目标移出集合、不停止运行中的目标、也不释放 flag 锁，导致重注册后新目标全部无法启动。放置时正常是因为放置时目标还没开始运行。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 新增 `clearAllGoals()`：逐个 `removeGoal`（正确 stop 运行中的目标并置 running=false，下一 tick 自动释放 flag 锁）+ `navigation.stop()`；`switchMode` 与 `determineModeAndRegisterGoals` 都改用 `clearAllGoals()` 替代 `removeAllGoals(g -> true)` |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL，仅既有 deprecation 警告）
- [x] 版本升至 0.2.5，jar 已归档 `versions/search_bot-0.2.5.jar`
- [x] 已同步到测试游戏 mods 文件夹 `D:\游戏\mc\PCL\.minecraft\versions\1.20.1-Forge_47.4.20\mods`（删除旧 `search_bot-*.jar`）
- [ ] 进游戏验证：绑定后右键空气切「探索」，机器人仍跟着玩家走（不再呆在原地）

### 2026-10-28 —— v0.2.4 修复「切换模式后机器人呆在原地」（探索模式跟随与跟随模式割裂）

**本次修改目标**（用户反馈）：绑定没问题，但**一切换模式机器人就呆在原地**；刚放下来是正常跟随，放置跟随和操控台切模式两套行为「非常割裂」。根因：探索（EXPLORE）模式里 `FollowOwnerGoal` 参数比跟随（FOLLOW）模式更松（startDistance 6.5 vs 5.5、stopDistance 2.5 vs 1.5）且优先级更低（3 vs 2，被 `CollectItemsGoal` 顶掉），导致切到探索后几乎不跟随。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | `registerGoalsByMode()` 的 EXPLORE 分支重排：`FollowOwnerGoal` 提升到 priority 2（与 FOLLOW 一致），参数从 `(1.0, 6.5f, 2.5f)` 改为 `(1.0, 5.5f, 1.5f)`（与 FOLLOW 完全一致）；`CollectItemsGoal` 降到 priority 3。效果：探索模式「跟随为主、玩家近处时才顺手收集」，切模式后跟随时紧时松不再割裂 |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL，仅既有 deprecation 警告）
- [x] 版本升至 0.2.4，jar 已归档 `versions/search_bot-0.2.4.jar`
- [x] 已同步到测试游戏 mods 文件夹 `D:\游戏\mc\PCL\.minecraft\versions\1.20.1-Forge_47.4.20\mods`（删除旧 `search_bot-*.jar`）
- [ ] 进游戏验证：绑定后右键空气切到「探索」，机器人仍会跟着玩家走（不再呆在原地）

### 2026-10-28 —— v0.2.3 修复「绑定后机器人呆在原地」回归 + 绑定加固

**本次修改目标**（用户反馈 2 个问题）：①不绑定还能跟随、绑定后机器人呆在原地（跟随失效）；②上一次运输模式还好、这次绑定后一直呆在原地（运输失效）。两者都发生在「绑定」之后，根因指向操控台绑定与主人 UUID 匹配链路，同时对放置流程做加固并加诊断日志。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `item/custom/RobotControllerItem.java` | 绑定同时存「主人 UUID」`BoundOwnerUUID`（主，充电后仍有效）+「机器人实体 UUID」`BoundRobotUUID`（兜底，防主人为空时找不到）；`findBoundRobot` 改为先按主人 UUID 匹配、匹配不到再按实体 UUID 兜底；`bindRobot`/`findBoundRobot` 加诊断日志 |
| `common/CommonEventHandler.java` | 放置机器人时**始终调用** `loadRobotData`（tag 为空时传空 CompoundTag，保证默认满电/默认跟随），并把 `setOwner(player)` 移到 `loadRobotData` 之后（避免旧 Owner 标签覆盖当前放置者） |
| `item/custom/RobotItem.java` | 同上：始终 `loadRobotData` + `setOwner` 后置 |
| `entity/custom/SearchRobotEntity.java` | `switchMode` 加诊断日志（输出模式/主人/状态/电量），方便定位绑定后切模式是否成功 |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL，仅既有 deprecation 警告）
- [x] 版本升至 0.2.3，jar 已归档 `versions/search_bot-0.2.3.jar`
- [ ] 绑定后跟随模式仍能跟随（进游戏验证；重点看绑定后是否误切到「探索」模式）
- [ ] 绑定后运输模式正常搬运（进游戏验证；若仍不动，把游戏日志里 `[RobotController]`/`[SearchRobot] switchMode` 行发我）

### 2026-10-28 —— v0.2.2 四个 bug 修复：跟随/探索无反应、跟随不碰容器、充电失绑、刹不住车

**本次修改目标**（用户反馈 4 个问题）：①跟随/探索模式没反应；②跟随模式只该跟随、不该碰容器；③机器人充电后操控台绑定失效；④机器人「刹不住车」追人冲过头（内置惯性，速度快本身可接受）。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 1. `loadRobotData`：无 `Energy` 标签（创造模式直接取出）默认满电，避免 0 电不动（修 bug 1）<br>2. `tickCollectItems` 改为仅 `RobotMode.EXPLORE` 时「路过收集」，跟随模式不碰容器（修 bug 2）<br>3. `mobInteract` 绑定改调 `bindRobot(held, this)`（配合绑定改为存主人 UUID） |
| `item/custom/RobotControllerItem.java` | 绑定从存「机器人实体 UUID」改为存「主人 UUID」`BoundOwnerUUID`；`findBoundRobot` 按 `e.getOwnerUUID()` 匹配、`appendHoverText` 改用 `getBoundOwnerUUID`（修 bug 3：充电站存取会 discard/重建实体导致实体 UUID 变） |
| `entity/ai/FollowOwnerGoal.java` | 惯性修正：到达停止距离后立即刹停（清速度 + 停寻路 `navigation.stop()` + `moveControl` 归零），避免高速逼近时刹不住冲过头；减速用更快的 `DECEL_LERP=0.6`、加速保持 `SPEED_LERP=0.25`（修 bug 4 的「刹不住车」） |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL，仅既有 deprecation 警告）
- [x] 版本升至 0.2.2，jar 已归档 `versions/search_bot-0.2.2.jar`
- [ ] 跟随/探索模式能正常移动（进游戏验证，此前为 0 电不动）
- [ ] 跟随模式不再与容器交互（进游戏验证）
- [ ] 机器人充电后操控台仍能控制（进游戏验证绑定不失效）
- [ ] 追人不冲过头、能刹住车（进游戏验证；若减速仍不理想可调 `FollowOwnerGoal` 的 `DECEL_LERP`/`SPEED_LERP`）

### 2026-10-28 —— v0.2.1 四模式重构 + 机器人操控台

**本次修改目标**：①把机器人工作模式从「跟随/探索」两态重构为「跟随/探索/收集/运输」四态；②删掉定位方块，改为手持「机器人操控台」绑定与控制机器人；③探索模式背包满自动提示并切回跟随。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 1. 新增 `RobotMode` 枚举（FOLLOW/EXPLORE/COLLECT/TRANSPORT，含 `cnName()`）替代原 `boolean explorationMode`；`DATA_ROBOT_MODE`(int) 替代 `DATA_EXPLORATION_MODE`<br>2. 新增字段 `collectTargetPos`/`transportStartPos`/`transportEndPos` 及 getter/setter<br>3. `registerGoalsByMode` 四模式注册：跟随=FollowOwnerGoal，探索=CollectItemsGoal(主动收集)+FollowOwnerGoal，收集=CollectGoal，运输=TransportGoal<br>4. `switchMode`(切模式+重注册AI)/`cycleMode`(循环切换)/`getModeCycle`(按已设容器决定循环)/`notifyOwner`(聊天提示)<br>5. 新增 `transferFromContainerToInventory`/`transferFromInventoryToContainer`(容器间搬运)/`containerHasItems`/`isInventoryEmpty`<br>6. `tickCollectItems` 仅在跟随/探索模式「路过收集」<br>7. `mobInteract` 手持操控台 shift 右键机器人 = 绑定<br>8. NBT：`Mode`(int)、`CollectTarget`/`TransportStart`/`TransportEnd`(long) |
| `entity/ai/CollectItemsGoal.java`（重写） | 探索模式「主动收集」：以玩家为中心 12 格内搜索容器并寻路，背包满时提示「仓库已满」并自动切回跟随；复用 `robot.containerHasItems` |
| `entity/ai/CollectGoal.java`（新增） | 收集模式：把目标容器半径 10 格内其他容器物品搬进目标容器（容器→容器） |
| `entity/ai/TransportGoal.java`（新增） | 运输模式：起点容器 → 终点容器循环搬运 |
| `item/custom/RobotControllerItem.java`（新增） | 手持操控台：存 `BoundRobotUUID`/`TransportStart`(暂存)；`use()` 处理 shift 取消绑定 + 右键空气切换模式；`findBoundRobot` 按 UUID 附近查实体；tooltip 显示绑定/起点状态 |
| `item/ModItems.java` | 删 `LOCATOR_ITEM`，加 `CONTROLLER`(robot_controller) |
| `block/ModBlocks.java` | 删 `LOCATOR` |
| `item/ModCreativeTabs.java` | 删 locator，加 controller |
| `common/CommonEventHandler.java` | 1. 手持操控台右键容器：未设起点→设收集容器+收集模式；已设起点→设终点+运输模式<br>2. 新增 `onLeftClickBlock`：手持操控台左键容器=设运输起点（setCanceled 阻止破坏） |
| 删除 | `LocatorBlock.java`、`LocatorUtil.java`、`ReturnToStorageGoal.java`、4 个 locator 资源文件、lang 的 locator 条目 |
| 资源/lan（新增） | `models/item/robot_controller.json`、`textures/item/robot_controller.png`（暂用 robot 材质）、lang `item.search_bot.robot_controller` |
| `gradle.properties` | `mod_version` 0.1.6 → **0.2.1**；更新 `mod_description` |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL）
- [x] jar 已归档 `versions/search_bot-0.2.1.jar`
- [ ] 操控台绑定/取消绑定/切换模式（进游戏验证）
- [ ] 右键容器设收集容器、左键设起点、右键设终点（进游戏验证）
- [ ] 四模式 AI 行为（跟随/探索主动收集/收集搬运/运输）（进游戏验证）
- [ ] 探索模式背包满 → 提示「仓库已满」+ 自动切跟随（进游戏验证）

### 2026-10-27 —— 三个 bug 修复：电量显示 / 创造充电站 / 拆解丢配件

**本次修改目标**：①机器人背包 GUI 显示电量；②新增「创造充电站」无需供电秒充电；③修复拆解后机器人配件消失。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `menu/custom/RobotInventoryMenu.java`（重写） | 新增 `ContainerData` 同步电量（当前/最大）；服务端构造器改为接收 `SearchRobotEntity`；新增 `getEnergy()` / `getMaxEnergy()` |
| `client/screen/RobotInventoryScreen.java` | `renderLabels` 在玩家背包标签行右侧绘制电量条 + 数字（低红/中黄/高绿） |
| `entity/custom/SearchRobotEntity.java` | 1. `mobInteract` 打开 GUI 时写 3 个 int（背包大小/电量/最大电量）<br>2. `findNearestChargingStation` 同时识别创造充电站<br>3. `loadRobotData` 对 `partsTag`/`statsTag` 深拷贝 `.copy()` |
| `block/custom/CreativeChargingStationBlock.java`（新增） | 继承 `ChargingStationBlock`，复用同一 BE 与交互逻辑 |
| `block/entity/custom/ChargingStationBlockEntity.java` | `tick` 判定创造充电站方块状态，秒充满（机器人/物品直接到 max），普通站仍 5000 FE/tick |
| `block/ModBlocks.java` | 注册 `CREATIVE_CHARGING_STATION` |
| `item/ModItems.java` | 注册 `CREATIVE_CHARGING_STATION_ITEM` |
| `block/entity/ModBlockEntities.java` | 创造充电站方块加入 `CHARGING_STATION` 类型 builder |
| `item/ModCreativeTabs.java` | 创造标签页加入创造充电站 |
| `common/CommonEventHandler.java` | 右键跳过逻辑改为 `instanceof ChargingStationBlock`（覆盖创造充电站） |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | `disassembleRobot` 输入槽为空时回退到输出槽拆解（组装后机器人常留输出槽） |
| 资源文件（新增） | `blockstates/models(block,item)/creative_charging_station.json`（emerald_block 材质）+ lang 条目 |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL，仅 5 个既有 deprecation 警告）
- [ ] 机器人右键后电量条/数字正确显示（进游戏验证）
- [ ] 创造充电站放入机器人秒充满（进游戏验证）
- [ ] 拆解后配件完整还原（重点测「组装→放置→拾取→拆解」全流程）

### 2026-10-27 —— 充电站渲染器（v0.2.0 Step 8，最后一个编码步骤）

**本次修改目标**：补齐 v0.2.0 计划中唯一未实现的编码任务——充电站渲染器，让玩家能直观看到充电站里存着机器人。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `client/renderer/block/ChargingStationRenderer.java`（新增） | 实现 `BlockEntityRenderer<ChargingStationBlockEntity>`；`hasRobot()` 为真时在充电站顶部渲染缩小、缓慢自转、轻微浮动的机器人模型（复用 `SearchRobotModel`）。因方块本体不透明，机器人画在站顶（`ejectRobot` 释放位）而非内部 |
| `client/ClientSetup.java` | `registerRenderers` 新增 `registerBlockEntityRenderer` 注册充电站渲染器；补充两个 import |

**验证状态**：
- [x] 代码完整构建通过（`gradlew.bat build`，BUILD SUCCESSFUL）
- [ ] 存入机器人后站顶出现缩小机器人、释放后消失（需进游戏手动验证）
- [ ] 缩小比例 / 浮动 / 转速视觉观感（需进游戏微调）

### 2026-07-16（第二轮）—— 寻路系统重构 + 螺旋桨崩溃 + 摔落减免

**本次修改目标**：修复螺旋桨崩溃、重构寻路系统（三模式+视线检测+平滑加减速）、添加摔落伤害减免。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 1. `createAttributes()` 添加 `FLYING_SPEED`（修复螺旋桨崩溃）<br>2. 新增 `countCrawlingLimbs()` / `hasWheels()` 方法<br>3. 覆写 `causeFallDamage()`：螺旋桨免疫摔落，爬行肢每个-1格 |
| `entity/ai/FollowOwnerGoal.java` | **完全重写**：三模式寻路（车轮2D/爬行2D+视线/飞行3D）、平滑速度 lerp、滞后余量、视线检测 |

**验证状态**：
- [x] 代码编译通过
- [ ] 螺旋桨机器人不再崩溃
- [ ] 爬行肢机器人无视线时自动爬墙
- [ ] 车轮机器人2D距离跟随，加速减速丝滑
- [ ] 飞行机器人3D跟随
- [ ] 墙上不会上下振荡
- [ ] 摔落伤害正确减免
- [ ] 抽搐抖动解决

### 2026-07-16 —— 爬行肢爬墙 + 螺旋桨飞行 + 静止收集修复

**本次修改目标**：修复三个游戏性问题——爬行肢无法翻墙跟随玩家、螺旋桨无飞行能力、静止时无法收集物品。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `entity/custom/SearchRobotEntity.java` | 1. 新增 `travel()` 覆写：飞行模式使用 `FlyingMob` 风格空中物理；步行模式下 `onClimbable()` 时施加向上速度(+0.2/tick)实现爬墙<br>2. 新增 `groundMoveControl` / `flyingMoveControl` 字段，`updateCanFly()` 同步切换 `MoveControl`<br>3. `isNoGravity()` 改为 `return this.canFly`（装备螺旋桨即无重力）<br>4. `tickCollectItems()` 条件从 `!=WORKING` 改为 `==CHARGING`（低电返航时继续收集）<br>5. `hasCrawlingLimbs()` 改为 public |
| `entity/ai/FollowOwnerGoal.java` | `tick()` 中当 `navigation.moveTo()` 失败且机器人有爬行肢时，使用 `moveControl.setWantedPosition()` 直接朝主人移动（触发撞墙→爬墙） |

**验证状态**：
- [x] 代码编译通过
- [ ] 爬行肢机器人可翻越墙壁跟随玩家
- [ ] 螺旋桨机器人低空悬浮飞行
- [ ] 低电返航途中经停容器时继续收集
- [ ] 非爬行肢/非螺旋桨机器人行为不受影响

### 2026-07-12 —— 新材料零件注册与属性查表重构

**本次修改目标**：让 54 个新材料等级零件（铜/铁/金/钻石/下界合金/终极 × 9种零件）在游戏中真正可用，并将属性系统从公式计算改为直接查表。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `item/ModCreativeTabs.java` | 将 `MATERIAL_PARTS` 中的 54 个新材料零件加入创造模式标签页 |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | 1. `isItemValid()` 改用 `ModItems.isPart()` 验证槽位，支持新材料零件放入<br>2. `assembleRobot()` 新增 stats 计算逻辑：提取每个零件的材料名 → 查 `RobotStats` 表 → 计算并写入 `stats` NBT |
| `util/RobotStats.java` | 全面重构为查表模式：新增 `ARM_COLLECT_AMOUNT`、`ARM_COLLECT_ENERGY_COST`、`STORAGE_SLOTS`、`SHIELD_ARMOR_TYPE`、`MOVEMENT_SPEED_BONUS`、`MOVEMENT_ENERGY_COST` 等直接查表；保留 `getCollectCooldown`、`getMovementSpeed`、`getMoveEnergyCostPerTick` 等交叉计算公式 |
| `entity/custom/SearchRobotEntity.java` | 1. `applyStats()` 现在将完整 stats 存入 `statsTag`<br>2. 新增 `getStatInt()`、`getInteractionRadius()`、`getCollectAmount()`、`getCollectEnergyCost()`、`getCollectCooldown()`、`getMoveEnergyCost()` 供 AI 读取<br>3. `tick()` 中移动耗能从固定 1 FE 改为读取 `getMoveEnergyCost()` |
| `entity/ai/CollectItemsGoal.java` | 1. 移除未使用的 `EnumSet` 导入<br>2. `findContainerInInteractionRadius()` 的搜索半径从硬编码 3 改为 `robot.getInteractionRadius()`<br>3. `collectFromContainer()` 的取物数量从 64 改为 `robot.getCollectAmount()`，耗电从 50 改为 `robot.getCollectEnergyCost()`，插入范围限制为 `robot.getInventorySize()`<br>4. `stop()` 的冷却从固定 5 tick 改为 `robot.getCollectCooldown()` |
| `item/custom/BatteryItem.java` | `BatteryEnergyProvider` 容量不再硬编码 10000；新增 `getCapacity()` 方法，根据 Registry ID 查 `RobotStats.BATTERY_CAPACITY` 并缓存到 NBT；提示文本动态显示实际容量 |
| `entity/custom/SearchRobotEntity.java` | 新增 `getInventorySize()`；`isInventoryFull()` 只检查前 `inventory_size` 个有效槽位，避免 90 格固定背包导致 AI 误判 |

**验证状态**：
- [x] 代码编译通过（已确认）
- [ ] 游戏中创造标签页可见 54 个新材料零件
- [ ] 新材料零件可放入装配台对应槽位
- [ ] 组装后机器人 stats NBT 包含正确数值
- [ ] 机器人交互半径、取物数量、耗电量随零件材料变化
- [ ] 不同材料电池容量上限正确（铜=10万, 铁=50万...）

**待跟进问题**：
- 背包 GUI (`RobotInventoryScreen` / `RobotInventoryMenu`) 仍显示固定格子数，未根据 `inventory_size` 动态调整显示。后端逻辑已限制为 `inventory_size`，但 GUI 可能显示多余的空槽。不影响功能测试。

---

### 2026-07-13 —— 美术材质整理与创造模式排序

**本次修改目标**：建立美术材质源文件夹，实现修改即同步；整理创造模式物品排序。

**修改文件清单**：

| 文件 | 变更内容 |
|------|----------|
| `build.gradle` | 新增 `syncArtTextures` 任务：构建时自动把 `美术材质/` 中的 png 同步到模组 `textures/` 目录。方块→block，物品子文件夹展平→item，实体→entity，GUI→gui |
| `item/ModCreativeTabs.java` | 创造模式排序改为按零件类型分组，每组内从铜到终极：机械臂6个→视觉传感器6个→发动机6个→储物箱6个→电池6个→护盾6个 |
| `item/ModItems.java` | 删除旧版 6 个零件注册，只保留 3 个移动部件（无等级）。`MATERIAL_PARTS` 只注册 36 个材料等级零件 |
| `lang/zh_cn.json` | 删除旧版零件翻译和移动部件材料等级翻译 |

**美术材质文件夹结构**（`美术材质/`）：
```
方块/          —— robot_assembler.png, locator.png, charging_station.png
物品/
  移动部件/    —— wheels.png, crawling_limbs.png, propeller.png
  机械臂/      —— copper/iron/gold/diamond/netherite/ultimate_mechanical_arm.png
  视觉传感器/  —— copper/iron/gold/diamond/netherite/ultimate_visual_sensor.png
  发动机/      —— copper/iron/gold/diamond/netherite/ultimate_engine.png
  储物箱/      —— copper/iron/gold/diamond/netherite/ultimate_storage_box.png
  电池/        —— copper/iron/gold/diamond/netherite/ultimate_battery.png
  护盾/        —— copper/iron/gold/diamond/netherite/ultimate_shield_part.png
  robot.png
实体/          —— search_robot.png
GUI/           —— robot_inventory.png, robot_assembler.png
```
- 36 个缺失的材料等级材质已用基础材质复制作为占位图，用户直接替换同名文件即可。

---

## 7. 关键文件路径速查

| 功能 | 文件 |
|------|------|
| 模组主类 | `src/main/java/com/cheng/search_bot/SearchBotMod.java` |
| 机器人实体 | `src/main/java/com/cheng/search_bot/entity/custom/SearchRobotEntity.java` |
| 跟随 AI | `src/main/java/com/cheng/search_bot/entity/ai/FollowOwnerGoal.java` |
| 收集 AI | `src/main/java/com/cheng/search_bot/entity/ai/CollectItemsGoal.java` |
| 返航充电 AI | `src/main/java/com/cheng/search_bot/entity/ai/ReturnToChargeGoal.java` |
| 充电站 BE | `src/main/java/com/cheng/search_bot/block/entity/custom/ChargingStationBlockEntity.java` |
| 充电站 Block | `src/main/java/com/cheng/search_bot/block/custom/ChargingStationBlock.java` |
| 装配台 BE | `src/main/java/com/cheng/search_bot/block/entity/custom/RobotAssemblerBlockEntity.java` |
| 电池物品 | `src/main/java/com/cheng/search_bot/item/custom/BatteryItem.java` |
| 机器人物品 | `src/main/java/com/cheng/search_bot/item/custom/RobotItem.java` |
| 事件处理 | `src/main/java/com/cheng/search_bot/common/CommonEventHandler.java` |
| v0.2.0 设计 | `design/v0.2.0-follow-and-charge.md` |
| v0.2.0 计划 | `design/plan-v0.2.0.md` |
| 需求文档 | `模组需求文档.md` |

---

## 8. 下次会话快速恢复指南

如果上下文被截断，请按以下顺序恢复状态：

1. **读取本文件**（`PROJECT_STATUS.md`）获取整体进度
2. **读取 design/plan-v0.2.0.md** 查看 9 个实施步骤及验证项
3. **读取 design/v0.2.0-follow-and-charge.md** 查看详细设计文档
4. 根据当前待办，读取对应源代码文件进行修改
5. **每次修改后更新本文件的"进度报告"章节**

---

*本文件由 Claude 在 2026-07-12 生成，后续每次迭代后需手动更新进度。*
