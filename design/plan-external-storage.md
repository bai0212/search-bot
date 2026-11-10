# 外部存储接口 —— 方案（待开发）

> **状态**：🟡 已暂停 / 待开发。用户决定先把模组主体完善，此特性暂缓，后续按本方案继续。
> **归档日期**：2026-11-03
> **相关版本线**：v0.5.7（引入外部存储接口物品）→ v0.6.0（外部容器整件保留 + 右键开原生 GUI）→ v0.6.1（模组容器原生 GUI + 接口限装一个）→ v0.6.3（仓库分页栏 + AE2 箱子可读）→ v0.6.4（AE2 便携元件接入外部存储 + 充电 + 绝不吞物）

---

## 1. 一句话目标

机器人通过「外部存储接口」这个功能组件，挂载一个外部存储介质（原版箱子 / 模组容器 / AE2 便携存储元件），把它当作自己的「随身外部仓库」：在机器人仓库 GUI 里查看和存取，并给 AE2 元件充电。**核心原则：绝不吞物，宁可退回/掉落。**

---

## 2. 相关物品 / 组件

| 物品 / 组件 | 类型 | 作用 | 当前状态 |
|------------|------|------|----------|
| 外部存储接口 `external_storage_interface` | 功能组件（限装一个） | 放进装配台后解锁「外部容器槽」 | ✅ 已注册可用，🔴 缺材质图 |
| 外部容器（任意带方块实体的方块物品） | 挂载物 | 放外部容器槽，机器人带在身上当随身仓库 | ✅ 已实现 |
| AE2 便携存储元件 (Portable Cell) | AE2 物品（非本模组） | 挂外部接口后，元件内容显示在「外部」分页 + 机器人给它充电 | ✅ 反射适配已写，🔴 待进游戏实测 |

> 说明：外部容器不是本模组做的物品，而是「玩家把任何带方块实体的容器方块（原版箱子/木桶/潜影盒/漏斗/投掷器、模组的旅行背包/更多箱子/AE2 箱子等）放进装配台的外部容器槽」。外部存储接口本身只是一个「开关组件」。

---

## 3. 已完成（代码已写，随 v0.6.4 进库）

### 3.1 外部存储接口物品（v0.5.7）
- `ModItems.EXTERNAL_STORAGE_INTERFACE` 已注册，`isPart`/`getPartType` 支持，中文名「外部存储接口」，模型 JSON 已建。
- 装配台功能槽「同类型去重」：外部存储接口和附件一样只能装一个。

### 3.2 外部容器挂载 + 读写（v0.5.7 ~ v0.6.0）
- 装配台新增外部容器槽（放接口后解锁）；组装时容器**整件**存入机器人 `ExternalContainer` NBT，拆解时整件还原，物品不丢。
- 外部容器是独立的「随身隔间」，不并入机器人工作背包（AI 收集/运输只用储物箱）。

### 3.3 仓库「外部」分页（v0.6.3）
- 机器人仓库 GUI 顶部分页栏：每个储物箱一分页，有外部容器时最后一个是「外部」分页。
- 原版容器读 `BlockEntityTag.Items`；模组容器重建方块实体拿 `IItemHandler`（AE2 用反射 `getInternalInventory().toItemHandler()` 兜底）；关菜单写回物品。

### 3.4 AE2 便携存储元件接入（v0.6.4，纯反射，不依赖 AE2 编译）
新增 `integration/Ae2PortableCell.java`：
- **检测**：向上遍历物品类父类链，是否继承 `appeng.items.tools.powered.AbstractPortableCell`（AE2 不在场永远 false，零风险）。
- **读**：`StorageCells.getCellInventory(stack, null)` → `getAvailableStacks()` → 遍历 KeyCounter，把每个 (AEItemKey, 数量) 展开成 ≤64 一叠的 ItemStack，最多 63 格。
- **写（差值，绝不吞物）**：打开时深拷贝快照，关闭时按「快照 vs 关闭内容」算净差值；写入顺序「先全部取出腾空间 → 再尝试放入」；`insert` 返回实际放入量，放不进的余额收集成 leftover 返回，由 `flushPortableCell` 掉落在机器人脚下让玩家捡回。任何失败路径（元件读不到 / 反射整体失败 / 元件塞满放不进）都把玩家新增部分退回，绝不消失。
- **充电**：`Capabilities.FORGE_ENERGY` 拿元件 FE 能力，机器人每 tick 转 200 FE 进元件，元件满 / 机器人没电自动停。
- **存档安全**：`flushPortableCell()` 只在关 GUI 时调用一次，`saveRobotData()` 不调用它，避免存档时重复 apply 造成二次扣减。

### 3.5 机器人背包满的既有保障
- 捡东西前查 `isInventoryFull()`，满了直接不捡。
- 从箱子搬东西用 `insertItem` 只拿装得下的部分，余量留在原处，不会「装不下还硬塞卡没」。

---

## 4. 待开发 / 待验证清单（后续接手从这里开始）

| # | 待办 | 优先级 | 说明 |
|---|------|--------|------|
| 1 | **外部存储接口材质** | 🔴 高 | `textures/item/external_storage_interface.png` 仍缺，等美术 AI 出图后跑 `./gradlew syncArtTextures` |
| 2 | **AE2 元件读/写/充电实测** | 🔴 高 | 反射路径无法编译期验证，最需确认写路径：取出/放入后关 GUI 再开数据不丢 |
| 3 | **塞满掉落实测** | 🔴 高 | 往满元件塞新物品，关 GUI 后新物品应掉在机器人脚下（验证「绝不吞物」） |
| 4 | **AE2 元件原生 GUI** | 🟡 中（高风险，已决定暂缓） | 见 §5.2，技术上可做但很脆弱，用户若坚持再单独做 |
| 5 | **模组容器原生 GUI 兼容测试** | 🟡 中 | 旅行背包 / 更多箱子等作外部容器，右键打开其原生 GUI 存取不丢；个别容器菜单有 `getBlockEntity(pos)==this` 自检会打开即关（AE2 ME 箱子），需逐一确认 |
| 6 | **ME 箱子 / AE2 机器方向** | 🟡 中（待用户拍板） | ME 箱子是「必须摆世界 + 接入网络 + 通电」的网络机器，物品态打不开原生 GUI、充不上电，方向见 §5.3 |
| 7 | **63 格截断体验** | 🟢 低 | 超过 63 格的类型不显示但不丢（差值只针对可见槽位），可接受 |

---

## 5. 关键决策记录

### 5.1 为什么用「自己画界面」而不是「打开 AE2 元件原生 GUI」
AE2 原生打开入口（`MenuOpener.open` + `MenuLocator`）只认「玩家手上 / 背包 / 快捷栏」这些标准位置；而我们的元件存在机器人自定义字段 `externalContainer` 里，AE2 不认。要做只能：
- **自定义 `MenuLocator`**：`MenuLocators.register` 要真实类，反射塞不进我们的机器人类，会崩；
- **临时挪走 hack**：开 GUI 前把元件塞玩家背包 → `openFromInventory` → 关掉拿回，中间任何一步出错元件可能卡在玩家背包或消失，很危险。

结论：暂不冒这个险，用「机器人『外部』分页显示同样内容」这个最稳的做法。用户若坚持，作为单独任务进游戏实测后再决定。

### 5.2 绝不吞物的实现依据
- AE2 `BasicCellInventory.insert` 的返回值 = **实际放入量**（放不进时为 0 或小于请求量），这是「放不进的余额退还」的关键依据。
- 差值在「碰元件之前」先算清（拿快照和关闭内容对比），所以即使反射整体失败，也能算清玩家到底放进了多少，全部退回。

### 5.3 ME 箱子 / AE2 机器的硬限制（方向待定）
ME 箱子不是普通容器，是「必须放在世界里、接入 AE 网络、通电才能用」的网络机器。物品态把它带在身上开 GUI 时，它没有位置、没有网络、没有电，所以：
- 打不开原生 GUI（AE2 菜单按坐标找真实方块，`getBlockEntity(pos)==this` 自检过不了）；
- 充不上电（幽灵方块实体不 tick、收不到 FE）。

**可选方向**：A) 接受现状，外部容器只完整支持原版箱子类 + 便携元件，AE 箱子当普通方块放世界里用；B) 重构为「把容器真正放到世界里并关联」，工作量大。未拍板。

---

## 6. 关键文件路径

| 功能 | 文件 |
|------|------|
| 外部存储接口物品注册 | `item/ModItems.java`（`EXTERNAL_STORAGE_INTERFACE`）、`item/ModCreativeTabs.java` |
| AE2 便携元件反射适配 | `integration/Ae2PortableCell.java`（`isPortableCell` / `readItems` / `applyDelta` / `getEnergyStorage` / `stackify`） |
| 机器人外部容器 + 便携元件分支 + 充电 + 掉落退还 | `entity/custom/SearchRobotEntity.java`（`buildExternalContainerHandler` / `getExternalContainerTabSlots` / `flushExternalContainer` / `flushPortableCell` / `chargePortableCell` / `itemHandlerOf`） |
| 装配台外部容器槽 + 组装拆解 | `block/entity/custom/RobotAssemblerBlockEntity.java` |
| 仓库分页栏 + TabSlot | `menu/custom/RobotInventoryMenu.java`、`client/screen/RobotInventoryScreen.java` |
| 槽位绘制 / 电量条 | `client/screen/TechGuiTheme.java`、`client/screen/RobotInventoryScreen.java` |
| 语言条目 | `resources/assets/search_bot/lang/zh_cn.json`（`item.search_bot.external_storage_interface`） |
| 缺的材质 | `resources/assets/search_bot/textures/item/external_storage_interface.png`（待美术出图） |
| AE2 反编译参考（临时） | `.ae2ref/` 目录 |

---

## 7. 后续接手要点

1. 先补 **#1 材质**（最省事、最影响观感）。
2. 进游戏跑 **#2/#3 实测**，这是 v0.6.4 写路径最需要确认的两条；若写路径不生效，重点查 `Ae2PortableCell.applyDelta` 的 `insert`/`extract` 反射方法签名。
3. **#4 原生 GUI** 只在用户明确坚持时再做，且必须进游戏实测防丢元件。
4. **#6 ME 箱子方向** 等用户拍板 A/B 再动。
