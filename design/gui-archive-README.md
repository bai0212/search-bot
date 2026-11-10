# 模组 GUI 归档（供美术 AI 交接用）

> 本模组「搜索机器人」共 3 个 GUI 界面，全部已归档成独立说明文档，方便交给负责美术/纹理/图标的 AI。每份文档都写明了：界面作用、尺寸、所有槽位/按钮坐标、当前配色、当前实现方式，以及美术 AI 可以改什么、不能改什么。

---

## 三个界面一览

| 界面 | 打开方式 | 尺寸 | 归档文档 | 有无纹理 |
|------|---------|------|---------|---------|
| 机器人操控台 | 手持操控台 shift+右键空气 | 240 × 244 | [gui-controller-spec.md](gui-controller-spec.md) | 有（固定部件烘贴图，动态状态代码画） |
| 机器人装配台 | 右键装配台方块 | 256 × 230 | [gui-assembler-spec.md](gui-assembler-spec.md) | 有（面板/分区/底板） |
| 机器人仓库（背包） | 空手右键机器人 | 256 × 动态高 | [gui-inventory-spec.md](gui-inventory-spec.md) | 部分（代码+贴图） |

**2026-10-31 更新**：
- GUI 风格已切成**原版箱子风**，科技风整套留档，一条命令可回退 → `美术材质/GUI/风格库/README.md`。
- 电量条压格子（R0）、blit 越界（R1）、装配台原子式布局 + 放入躯体才出格子（R2）
  等**需要改 Java** 的需求，都写在 [gui-code-requirements.md](gui-code-requirements.md)，美术侧不动 src 逻辑。

---

## 纹理文件位置（美术 AI 重点改这里）

所有 GUI 纹理都在这个目录：

```
src/main/resources/assets/search_bot/textures/gui/
```

| 文件 | 尺寸 | 对应界面 | 当前状态 |
|------|------|---------|---------|
| `robot_controller.png` | 220 × 248 | 操控台 | 未使用（操控台是纯代码画） |
| `robot_assembler.png` | 256 × 256 | 装配台 | 占位图，可重画 |
| `robot_inventory.png` | 256 × 256 | 仓库 | 占位图，可重画 |

---

## 给美术 AI 的关键提醒

1. **槽位坐标和按钮位置是功能逻辑，不要改**——改了会导致玩家点不到按钮、物品放不进格。
2. 三个界面的槽位坐标、交互行为都写在各自文档里，改纹理前先看对应文档。
3. 材质/模型方面（机器人 3D 模型、实体贴图）的交接说明在另一个文件：`D:\Blockbench\mcp\给AI的连接说明.md`。

---

## 相关代码路径

| 内容 | 路径 |
|------|------|
| 操控台界面 | `src/main/java/com/cheng/search_bot/client/screen/RobotControllerScreen.java` |
| 装配台界面 | `src/main/java/com/cheng/search_bot/client/screen/RobotAssemblerScreen.java` |
| 仓库界面 | `src/main/java/com/cheng/search_bot/client/screen/RobotInventoryScreen.java` |
| 菜单（容器/槽位） | `src/main/java/com/cheng/search_bot/menu/custom/` |
