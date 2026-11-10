# v0.3.0 实施计划 —— 四模式重构 + 机器人操控台

> 版本：v0.2.1（从 v0.1.6 起；本计划文档命名为 v0.3.0，实际发布版本定为 v0.2.1）  
> 日期：2026-10-28

---

## 1. 需求（用户原话整理）

1. **爬行功能**：暂缓，放之后（本次不做）。
2. **跟随半径**：数据已给用户，等待用户定值后改 `FollowOwnerGoal` 的 stopDistance/startDistance。
3. **机器人模式改为 4 种**：
   - **跟随 FOLLOW**：第一要务跟玩家，收集只是路过（顺路）。
   - **探索 EXPLORE**：第一要务收集，但也要保证跟着玩家。背包满 → 提示玩家「仓库已满」并自动变回跟随。
   - **收集 COLLECT**：在合理范围内（目标容器半径 10 格），把**其他容器里的物品**搬到目标容器。
   - **运输 TRANSPORT**：把起点容器的物品送往终点容器。
4. **删定位方块，改「机器人操控台」手持物件**。

---

## 2. 操控台交互（已与用户确认）

| 操作 | 效果 |
|------|------|
| 手持操控台 **shift 右键机器人** | 绑定该机器人（操控台记住机器人 UUID） |
| 手持操控台 **shift 右键空气** | 取消绑定，机器人保持当前模式 |
| 手持操控台 **右键空气** | 循环切换模式（见下方循环规则） |
| 手持操控台 **右键容器**（未设起点时） | 设置收集目标容器 → 收集模式 |
| 手持操控台 **左键容器** | 设置运输起点 |
| 手持操控台 **右键容器**（已设起点时） | 设置运输终点 → 运输模式 |

**模式切换循环（右键空气）**：
- 没设任何容器：`跟随 ↔ 探索`
- 设了收集容器（无起点）：`跟随 ↔ 探索 ↔ 收集`
- 设了起点+终点：`跟随 ↔ 探索 ↔ 运输`
- 若收集容器与起终点都设了：循环里都出现（跟随/探索/收集/运输）。

**默认**：没有起点容器时，右键容器视为设收集容器 → 收集模式。

---

## 3. 数据模型

### 3.1 机器人实体 `SearchRobotEntity`
- 新增枚举 `RobotMode { FOLLOW, EXPLORE, COLLECT, TRANSPORT }`，替代原 `boolean explorationMode`。
- 用 `EntityDataAccessor<Integer> DATA_ROBOT_MODE` 同步到客户端（替换 `DATA_EXPLORATION_MODE`）。
- 新增字段：
  - `BlockPos collectTargetPos`（收集目标容器，可空）
  - `BlockPos transportStartPos`（运输起点，可空）
  - `BlockPos transportEndPos`（运输终点，可空）
- 保留：`homeStoragePos`（跟随模式放置时绑定，暂保留兼容）、`targetChargingStation`、`RobotState` 运行时状态（WORKING/RETURNING_TO_CHARGE/CHARGING/OUT_OF_POWER 不变，与「工作模式」是两套概念）。
- NBT 保存/加载：`Mode`(int)、`CollectTarget`/`TransportStart`/`TransportEnd`(long 或 absent)。

### 3.2 操控台物品 `RobotControllerItem`
- NBT：
  - `BoundRobotUUID` (UUID) — 绑定的机器人。
  - `TransportStart` (long) — 已左键设的起点（存操控台里，待终点确认后写入机器人）。
- 绑定后右键空气切换模式时，服务端按 UUID 查找附近机器人实体，调用机器人侧的模式切换逻辑。

---

## 4. AI 目标（按模式注册）

| 模式 | 注册的 Goal |
|------|------------|
| FOLLOW | ReturnToChargeGoal(低电返航) → FollowOwnerGoal(跟玩家) → CollectItemsGoal(路过顺手收集) |
| EXPLORE | ReturnToChargeGoal → FollowOwnerGoal → CollectItemsGoal(主动寻路收集，优先于移动) |
| COLLECT | ReturnToChargeGoal → CollectGoal(目标容器10格内搬运) |
| TRANSPORT | ReturnToChargeGoal → TransportGoal(起点→终点搬运) |

- `CollectItemsGoal` 重构：按 `RobotMode` 分派 `tickFollow` / `tickExplore` / `tickCollect`。
- 新增 `TransportGoal`：去起点取物 → 送到终点卸货，循环。
- `ReturnToStorageGoal`（旧探索模式满了返航存储）不再使用，可删除或保留兼容。

---

## 5. 事件处理

- `CommonEventHandler.onRightClickBlock`：
  - 手持操控台右键容器 → 设置收集容器或运输终点。
  - 手持机器人右键 → 放置（现有逻辑保留）。
- 新增 `LeftClickBlock` 处理：手持操控台左键容器 → 设置运输起点（`event.setCanceled(true)` 阻止破坏）。
- `RobotControllerItem.use`（右键空气）：切换模式 / shift 取消绑定。
- 机器人实体 `mobInteract`：手持操控台 shift 右键机器人 → 绑定。

---

## 6. 文件改动清单

**新增**：
- `item/custom/RobotControllerItem.java`
- `entity/ai/TransportGoal.java`
- 资源：操控台模型/材质、lang 条目

**修改**：
- `entity/custom/SearchRobotEntity.java`（RobotMode、字段、模式注册、NBT、getter/setter）
- `entity/ai/CollectItemsGoal.java`（三模式分派）
- `item/ModItems.java`（删 LOCATOR_ITEM，加 CONTROLLER）
- `block/ModBlocks.java`（删 LOCATOR）
- `item/ModCreativeTabs.java`（删 locator，加 controller）
- `common/CommonEventHandler.java`（右键容器/左键容器处理）
- `lang/zh_cn.json`

**删除**：
- `block/custom/LocatorBlock.java`
- `util/LocatorUtil.java`
- locator 资源文件（blockstates/models/item）
- `ReturnToStorageGoal.java`（如确认不再用）

---

## 7. 实施步骤（每步编译验证）

1. 删定位方块（ModBlocks/ModItems/ModCreativeTabs/资源/lang）+ 删 LocatorBlock/LocatorUtil。
2. SearchRobotEntity 新增 `RobotMode` + 字段 + NBT + 替换 explorationMode。
3. 新增 `RobotControllerItem` + 注册 + 资源。
4. 事件处理：右键容器/左键容器/绑定/取消绑定/切换模式。
5. AI 重构：CollectItemsGoal 三模式 + TransportGoal + registerGoalsByMode 四模式。
6. 编译 + 手动测试清单。

---

## 8. 待办/悬置

- 爬行功能（用户要求暂缓）。
- 跟随半径定值（等用户给数）。
- `homeStoragePos` 与新模式的兼容处理（跟随模式放置时仍绑定容器，作为路过收集满了的卸货点，或后续再定）。
