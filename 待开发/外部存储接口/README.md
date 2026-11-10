# 外部存储接口 —— 已分离归档（待开发）

> 本目录是「外部存储接口」功能的**存档**。该功能已从当前模组代码中**完整移出**，模组回到不含外部存储的干净核心状态。将来要重新开发时，按本说明接手即可。

## 一、这个功能是什么

让机器人通过「外部存储接口」功能组件，额外挂载一个外部容器（原版箱子/木桶/潜影盒等，以及 AE2 便携存储元件），在机器人仓库 GUI 顶部多出一个「外部」分页来读写它；同时给 AE2 便携元件充电。

## 二、目录内容

| 路径 | 内容 |
|------|------|
| `源码备份_含外部存储/` | **移出前**的完整源码快照（`src/` + `build.gradle` + `gradle.properties`），内含全部外部存储代码，可对照恢复 |
| `../design/plan-external-storage.md` | 方案文档（目标、已完成实现、待开发清单、关键决策、接手要点） |

## 三、移出时删掉的代码（对照恢复清单）

| 位置 | 删除内容 |
|------|----------|
| `src/main/java/com/cheng/search_bot/integration/Ae2PortableCell.java` | 整文件删除（AE2 便携元件纯反射适配：检测/读/差值写/充电） |
| `item/ModItems.java` | `EXTERNAL_STORAGE_INTERFACE` 字段 + `isPart`/`getPartType` 里对应分支 |
| `item/ModCreativeTabs.java` | 创造标签里的外部存储接口 |
| `block/entity/custom/RobotAssemblerBlockEntity.java` | `SLOT_EXTERNAL_CONTAINER=20`、`TOTAL_SLOTS` 21→20、`isItemValid` 外部接口去重、`hasExternalInterface()`、`isContainerItem()`、组装/拆解里的外部容器处理 |
| `menu/custom/RobotAssemblerMenu.java` | `externalContainerSlot` 字段 + 外部容器槽 + `hasExternalInterface()`/`getExternalContainerSlot()` |
| `client/screen/RobotAssemblerScreen.java` | 外部容器槽特化绘制 + 「外部容器」标签 |
| `entity/custom/SearchRobotEntity.java` | 外部容器字段/方法（`buildExternalContainerHandler`/`getExternalContainerTabSlots`/`flushExternalContainer`/`flushPortableCell`/`itemHandlerOf`/`flushMountedContainer`/`readExternalContainerItems`/`writeExternalContainerItemsFromHandler`/`externalContainerSlotCount`/`chargePortableCell`/`getExternalContainer`）、相关 import、`ExternalContainer` NBT 读写 |
| `menu/custom/RobotInventoryMenu.java` | 外部容器分页（`externalHandler`/`externalSize`/`externalFlush`、外部分页槽位、`hasExternalTab()`、`removed()` 里的 flush） |
| `client/screen/RobotInventoryScreen.java` | 「外部」分页标签 |
| `resources/.../lang/zh_cn.json` | `item.search_bot.external_storage_interface` 语言条目 |
| `resources/.../models/item/external_storage_interface.json` | 物品模型 JSON |
| `resources/.../textures/item/external_storage_interface.png` | 物品材质（孤儿文件，一并清除） |

## 四、恢复方法

1. 从 `源码备份_含外部存储/` 对照上面清单，把相关代码/资源拷回 `src/` 对应位置；
2. 读 `design/plan-external-storage.md` 里的「待开发清单」，逐项补齐（补材质 → 进游戏实测读写/充电/掉落 → 原生 GUI 高风险暂缓 → ME 箱子方向待拍板）；
3. `gradlew.bat build` 验证编译。

## 五、保留未动的核心功能

以下**不属于**外部存储、已完整保留在当前模组中：

- 多储物箱分页栏（「箱子1/箱子2…」顶部分页）——这是核心功能，**没有删除**，只删掉了分页栏里的「外部」标签。
- 躯干等级系统、功能组件/附件槽位、无线充电、收集/运输/过滤等。
