# Plan: 搜索机器人 v0.2.0 —— 跟随模式修订与充电站重构

> 基于设计文档：`design/v0.2.0-follow-and-charge.md`
> 范围：仅跟随模式，探索模式后续版本再实现。

---

## Prerequisites

- [ ] v0.1.5 代码已可编译运行，跟随/探索双模式基础逻辑正常。
- [ ] 开发环境可启动 Minecraft 客户端进行手动测试。

---

## Steps

### Step 1: 电池零件 NBT 与组装/拆解台电量传递
- **Files**: `item/custom/BatteryItem.java`（或对应电池类）、`block/entity/custom/RobotAssemblerBlockEntity.java`
- **Action**:
  1. 电池物品默认 NBT 增加 `energy = 0`（新合成/创造模式取出的电池没电）。
  2. 组装台 `assembleRobot()` 中，读取电池槽物品的 `energy` 标签，写入生成的机器人 NBT `Energy` 字段。
  3. 拆解台 `disassembleRobot()` 中，把机器人 NBT 的 `Energy` 写回返还的电池零件 NBT。
  4. 移除 `SearchRobotEntity` 构造器中硬编码的初始电量 `10000`；新放置的机器人若 NBT 无 `Energy`，默认 `0`。
- **Verification**:
  - [ ] 新组装机器人放置后不动（能量为 0）。
  - [ ] 给电池充 500 FE 再组装，机器人初始能量为 500。
  - [ ] 拆解后电池保留剩余电量。

### Step 2: 机器人状态机基础（SearchRobotEntity）
- **Files**: `entity/custom/SearchRobotEntity.java`
- **Action**:
  1. 新增 `RobotState` 枚举（`WORKING`, `RETURNING_TO_CHARGE`, `CHARGING`, `OUT_OF_POWER`）。
  2. 新增同步字段 `DATA_ROBOT_STATE`（`EntityDataAccessor<Integer>`）。
  3. 新增字段 `targetChargingStation: BlockPos`（仅服务端）。
  4. 重写 `tick()`：电量 `<= 0` → `OUT_OF_POWER`；电量 `< 20%` 且世界中有充电站 → `RETURNING_TO_CHARGE`。
  5. 修改 `registerGoalsByMode()`：跟随模式注册 `FollowOwnerGoal`、`CollectItemsGoal`、`ReturnToChargeGoal`、`WaterAvoidingRandomStrollGoal`。
  6. 旧存档兼容：加载 NBT 时若已有 `Energy`，保留原值；若无 `RobotState`，默认 `WORKING`。
- **Verification**:
  - [ ] 调试输出或命令查看机器人状态随电量变化正确切换。

### Step 3: FollowOwnerGoal 调整
- **Files**: `entity/ai/FollowOwnerGoal.java`
- **Action**:
  1. 跟随触发距离从当前值改为 **4.5f**，停止距离保持 **1.5f**。
  2. `canUse()` 增加 `|| robot.getRobotState() == RobotState.OUT_OF_POWER`，确保没电时也跟随。
  3. `OUT_OF_POWER` 状态下速度倍率降至 **0.6**（或更低），体现缓慢跟随。
  4. 可选：`OUT_OF_POWER` 且主人距离 > 16 格时，尝试传送到主人附近（防飞行甩丢）。
- **Verification**:
  - [ ] 玩家行走时机器人保持在 4~5 格内跟随，不会跟丢。
  - [ ] 没电后机器人仍跟随，但明显变慢。

### Step 4: CollectItemsGoal 重构（交互半径 + 并发取物）
- **Files**: `entity/ai/CollectItemsGoal.java`
- **Action**:
  1. 将搜索半径改为 `INTERACTION_RADIUS = 3`（硬编码，后续由感知部件等级替换）。
  2. 修改 `canUse()`：仅在 `RobotState.WORKING`、能量 > 0、背包未满时运行；排除 `OUT_OF_POWER` / `RETURNING_TO_CHARGE` / `CHARGING`。
  3. **移除"专门为取物而寻路"的逻辑**：机器人不再为了远处的容器创建新的导航目标。
  4. `tick()` 中：检测交互半径内是否有方块容器（`BlockEntity` 且支持 `IItemHandler`）。
  5. **并发取物**：发现容器后，在不取消当前跟随导航的前提下，直接打开容器 GUI 取物；取完后立刻关闭，继续移动。取物动作不应阻塞机器人移动超过 1~2 tick。
  6. **过滤**：只处理 `BlockEntity` 类型的容器；**跳过**其他 `SearchRobotEntity`、玩家背包、掉落物实体。
- **Verification**:
  - [ ] 玩家在箱子旁走动，机器人在 3 格内顺手取物，不中断跟随。
  - [ ] 机器人不会因远处的木桶而脱离玩家跑过去。
  - [ ] 没电后机器人不再取物。

### Step 5: 新增 ReturnToChargeGoal
- **Files**: `entity/ai/ReturnToChargeGoal.java`（新增）
- **Action**:
  1. `canUse()`：`robot.getRobotState() == RETURNING_TO_CHARGE` 且 `targetChargingStation != null` 且能量 > 0。
  2. `tick()`：每 tick 向 `targetChargingStation` 导航移动（速度可用 `1.5x` 倍率， urgency）。
  3. `stop()`：到达充电站（距离 < 2 格）后，调用 `ChargingStationBlockEntity.insertRobot(robot)`；若成功，机器人实体 `discard()`，状态转为 `CHARGING`。
- **Verification**:
  - [ ] 电量降至 20% 以下时，机器人自动走向最近的充电站。
  - [ ] 到达后实体消失，站内开始有机器人。

### Step 6: ChargingStationBlockEntity 核心重构
- **Files**: `block/entity/custom/ChargingStationBlockEntity.java`
- **Action**:
  1. 移除 `MenuProvider` 实现和 `createMenu()` 方法（或返回 `null`），彻底去掉 GUI。
  2. 新增字段：`storedRobot: CompoundTag`、`hasRobot: boolean`。
  3. 实现 `insertRobot(SearchRobotEntity robot)`：序列化机器人 NBT，存入 `storedRobot`，`discard()` 实体，`hasRobot = true`。
  4. 实现 `insertRobotFromItem(ItemStack robotItem)`：玩家手动放入物品形态机器人，解析 NBT 存入。
  5. 实现 `ejectRobot()`：充满后从 `storedRobot` 重建实体，恢复电量为满，强制状态 `WORKING`，在充电站上方生成实体，清空内部存储。
  6. 重写 `tick()`：若 `hasRobot`，每 tick 给 `storedRobot` 的 `Energy` 快速增加（`+5000` 或直接充满）；充满后调用 `ejectRobot()`。
  7. NBT 序列化：`saveAdditional` / `load` 读写 `HasRobot` 和 `StoredRobot`。
  8. 破坏时吐出：提供 `onStationDestroyed()` 方法，若 `hasRobot` 则在原地生成实体。
- **Verification**:
  - [ ] 机器人存入后实体从世界消失，调试信息或 NBT 编辑器可见站内数据完整。
  - [ ] 充满后机器人自动出现在充电站上方，状态为 `WORKING`，继续跟随。
  - [ ] 破坏充电站时吐出机器人，数据不丢失。

### Step 7: ChargingStationBlock 交互调整
- **Files**: `block/custom/ChargingStationBlock.java`
- **Action**:
  1. 移除 `MenuProvider` 相关代码，右击不再打开 GUI。
  2. 重写 `use()`：玩家手持物品形态机器人右键充电站 → 调用 `insertRobotFromItem()`，成功则消耗手中物品。
  3. 若站内已有机器人，放入失败（可播放音效或粒子提示）。
  4. `onRemove()` / `playerWillDestroy()`：调用 `ChargingStationBlockEntity.onStationDestroyed()`，确保破坏时吐出机器人。
- **Verification**:
  - [ ] 空手右键充电站无反应（不打开 GUI）。
  - [ ] 手持机器人物品右键充电站，物品被消耗，站内出现机器人。
  - [ ] 破坏充电站时站内机器人掉落到地面。

### Step 8: 充电站渲染器（可选，可最后实现）
- **Files**: `client/renderer/block/ChargingStationRenderer.java`（新增）
- **Action**:
  1. 实现 `BlockEntityRenderer<ChargingStationBlockEntity>`。
  2. `render()`：若 `hasRobot()`，在充电站内部渲染一个缩小的机器人模型（或复用 `SearchRobotRenderer` 的静态渲染方法）。
  3. 在客户端事件/初始化中注册该渲染器。
- **Verification**:
  - [ ] 机器人存入后，充电站内部可见机器人模型；释放后消失。

### Step 9: 综合测试与验证
- **Files**: N/A（手动测试）
- **Action**:
  1. 完整走一遍 v0.2.0 测试策略中的所有场景。
  2. 检查日志无异常、无崩溃。
  3. 确认旧存档（v0.1.5）加载后机器人行为正常（兼容）。
- **Verification**:
  - [ ] 测试策略 6.1 ~ 6.4 全部通过（见设计文档）。

---

## Completion Criteria

- [ ] 所有 9 个步骤的验证项均通过手动测试。
- [ ] 跟随模式下机器人 4~5 格触发跟随，3 格交互半径内并发取物，不阻塞移动。
- [ ] 低电量（<20%）自动返航充电，充满后自动释放并恢复跟随。
- [ ] 新电池没电 → 新机器人没电；拆解后电池保留剩余电量。
- [ ] 充电站无 GUI，支持玩家手动放入和自动充电。
- [ ] 代码可编译，客户端/服务端无崩溃。
