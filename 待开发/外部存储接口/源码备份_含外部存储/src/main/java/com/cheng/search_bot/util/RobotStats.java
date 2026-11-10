package com.cheng.search_bot.util;

import java.util.HashMap;
import java.util.Map;

/**
 * 机器人属性查表工具
 * 所有无需交叉计算的属性直接列表，运行时只查表不计算。
 * 交叉计算属性（取物间隔、移动速度、移动耗电）保留公式。
 */
public class RobotStats {

    /** 材料 → 等级（发动机动力系数） */
    public static final Map<String, Float> MATERIAL_LEVELS = new HashMap<>();
    static {
        MATERIAL_LEVELS.put("copper", 3.0f);
        MATERIAL_LEVELS.put("iron", 3.5f);
        MATERIAL_LEVELS.put("gold", 4.0f);
        MATERIAL_LEVELS.put("diamond", 6.0f);
        MATERIAL_LEVELS.put("netherite", 7.0f);
        MATERIAL_LEVELS.put("ultimate", 10.0f);
    }

    // ========== 直接查表属性（无需交叉计算）==========

    /** 材料 → 机械臂每次取物数量 */
    public static final Map<String, Integer> ARM_COLLECT_AMOUNT = new HashMap<>();
    static {
        ARM_COLLECT_AMOUNT.put("copper", 6);
        ARM_COLLECT_AMOUNT.put("iron", 7);
        ARM_COLLECT_AMOUNT.put("gold", 8);
        ARM_COLLECT_AMOUNT.put("diamond", 12);
        ARM_COLLECT_AMOUNT.put("netherite", 14);
        ARM_COLLECT_AMOUNT.put("ultimate", 20);
    }

    /** 材料 → 机械臂每次取物耗电 (FE) */
    public static final Map<String, Integer> ARM_COLLECT_ENERGY_COST = new HashMap<>();
    static {
        ARM_COLLECT_ENERGY_COST.put("copper", 15);
        ARM_COLLECT_ENERGY_COST.put("iron", 18);   // 17.5 取整
        ARM_COLLECT_ENERGY_COST.put("gold", 20);
        ARM_COLLECT_ENERGY_COST.put("diamond", 30);
        ARM_COLLECT_ENERGY_COST.put("netherite", 35);
        ARM_COLLECT_ENERGY_COST.put("ultimate", 50);
    }

    /** 材料 → 传感器搜索半径（探索/寻找容器范围） */
    public static final Map<String, Integer> SENSOR_RADIUS = new HashMap<>();
    static {
        SENSOR_RADIUS.put("copper", 6);
        SENSOR_RADIUS.put("iron", 8);
        SENSOR_RADIUS.put("gold", 10);
        SENSOR_RADIUS.put("diamond", 12);
        SENSOR_RADIUS.put("netherite", 14);
        SENSOR_RADIUS.put("ultimate", 16);
    }

    /** 材料 → 机械臂交互半径（够得着多远的箱子） */
    public static final Map<String, Double> ARM_RADIUS = new HashMap<>();
    static {
        ARM_RADIUS.put("copper", 1.0);
        ARM_RADIUS.put("iron", 1.5);
        ARM_RADIUS.put("gold", 2.0);
        ARM_RADIUS.put("diamond", 2.5);
        ARM_RADIUS.put("netherite", 3.0);
        ARM_RADIUS.put("ultimate", 3.5);
    }

    /** 材料 → 储物箱背包格数 */
    public static final Map<String, Integer> STORAGE_SLOTS = new HashMap<>();
    static {
        STORAGE_SLOTS.put("copper", 27);   // 3×9
        STORAGE_SLOTS.put("iron", 36);     // 4×9
        STORAGE_SLOTS.put("gold", 45);     // 5×9
        STORAGE_SLOTS.put("diamond", 54);  // 6×9
        STORAGE_SLOTS.put("netherite", 63);// 7×9
        STORAGE_SLOTS.put("ultimate", 90); // 10×9
    }

    /** 材料 → 电池最大容量 (FE) */
    public static final Map<String, Integer> BATTERY_CAPACITY = new HashMap<>();
    static {
        BATTERY_CAPACITY.put("copper", 100_000);
        BATTERY_CAPACITY.put("iron", 500_000);
        BATTERY_CAPACITY.put("gold", 2_100_000);
        BATTERY_CAPACITY.put("diamond", 8_500_000);
        BATTERY_CAPACITY.put("netherite", 10_000_000);
        BATTERY_CAPACITY.put("ultimate", 1_000_000_000);
    }

    /** 材料 → 护盾血量 */
    public static final Map<String, Integer> SHIELD_HEALTH = new HashMap<>();
    static {
        SHIELD_HEALTH.put("copper", 8);
        SHIELD_HEALTH.put("iron", 9);
        SHIELD_HEALTH.put("gold", 10);
        SHIELD_HEALTH.put("diamond", 14);
        SHIELD_HEALTH.put("netherite", 16);
        SHIELD_HEALTH.put("ultimate", 22);
    }

    /** 材料 → 护盾护甲类型描述（供后续逻辑参考） */
    public static final Map<String, String> SHIELD_ARMOR_TYPE = new HashMap<>();
    static {
        SHIELD_ARMOR_TYPE.put("copper", "copper");
        SHIELD_ARMOR_TYPE.put("iron", "iron");
        SHIELD_ARMOR_TYPE.put("gold", "gold");
        SHIELD_ARMOR_TYPE.put("diamond", "diamond");
        SHIELD_ARMOR_TYPE.put("netherite", "netherite");
        SHIELD_ARMOR_TYPE.put("ultimate", "beacon"); // 信标效果：生命恢复 II
    }

    // ========== 躯干属性（0.5.1 新增）==========

    /** 材料 → 功能组件槽数（铜 5，每次升级 +1） */
    public static final Map<String, Integer> TORSO_FUNCTIONAL_SLOTS = new HashMap<>();
    static {
        TORSO_FUNCTIONAL_SLOTS.put("copper", 5);
        TORSO_FUNCTIONAL_SLOTS.put("iron", 6);
        TORSO_FUNCTIONAL_SLOTS.put("gold", 7);
        TORSO_FUNCTIONAL_SLOTS.put("diamond", 8);
        TORSO_FUNCTIONAL_SLOTS.put("netherite", 9);
        TORSO_FUNCTIONAL_SLOTS.put("ultimate", 10);
    }

    /** 材料 → 附件槽数（铜 1，每次升级 +1） */
    public static final Map<String, Integer> TORSO_ATTACHMENT_SLOTS = new HashMap<>();
    static {
        TORSO_ATTACHMENT_SLOTS.put("copper", 1);
        TORSO_ATTACHMENT_SLOTS.put("iron", 2);
        TORSO_ATTACHMENT_SLOTS.put("gold", 3);
        TORSO_ATTACHMENT_SLOTS.put("diamond", 4);
        TORSO_ATTACHMENT_SLOTS.put("netherite", 5);
        TORSO_ATTACHMENT_SLOTS.put("ultimate", 6);
    }

    /** 材料 → 躯干血量（铜 8，每次升级 +4） */
    public static final Map<String, Integer> TORSO_HEALTH = new HashMap<>();
    static {
        TORSO_HEALTH.put("copper", 8);
        TORSO_HEALTH.put("iron", 12);
        TORSO_HEALTH.put("gold", 16);
        TORSO_HEALTH.put("diamond", 20);
        TORSO_HEALTH.put("netherite", 24);
        TORSO_HEALTH.put("ultimate", 28);
    }

    /** 材料 → 伤害减算（铜 0，每升一级 +1，最终伤害 = max(0, 伤害 - 减算)） */
    public static final Map<String, Integer> TORSO_DAMAGE_REDUCTION = new HashMap<>();
    static {
        TORSO_DAMAGE_REDUCTION.put("copper", 0);
        TORSO_DAMAGE_REDUCTION.put("iron", 1);
        TORSO_DAMAGE_REDUCTION.put("gold", 2);
        TORSO_DAMAGE_REDUCTION.put("diamond", 3);
        TORSO_DAMAGE_REDUCTION.put("netherite", 4);
        TORSO_DAMAGE_REDUCTION.put("ultimate", 5);
    }

    // ========== 交叉计算属性（保留公式）==========

    /**
     * 取物间隔(tick) = max(1, 200 / (发动机动力 × 机械臂等级))
     */
    public static int getCollectCooldown(float armLevel, float enginePower) {
        return Math.max(1, (int) (200.0f / (enginePower * armLevel)));
    }

    /**
     * 移动速度 = (移动部件移速系数之和) × (1.0 + 0.2 × 发动机动力) × 玩家走路速度
     * 注：玩家走路速度基准约 0.215，但机器人应明显快于玩家，故使用 0.3 作为基准倍率。
     */
    public static double getMovementSpeed(double movementPartSum, float enginePower) {
        return movementPartSum * (1.0 + 0.2 * enginePower) * 0.3;
    }

    /**
     * 移动耗电/tick = (移动部件能耗系数之和) × (1.0 + 0.1 × 发动机动力) FE
     */
    public static int getMoveEnergyCostPerTick(double energyPartSum, float enginePower) {
        return (int) (energyPartSum * (1.0 + 0.1 * enginePower));
    }

    // ========== 移动部件系数（按部件类型固定）==========

    /** 移动部件 → 移速系数 */
    public static final Map<String, Double> MOVEMENT_SPEED_BONUS = new HashMap<>();
    static {
        MOVEMENT_SPEED_BONUS.put("wheels", 0.5);
        MOVEMENT_SPEED_BONUS.put("propeller", 0.25);
        MOVEMENT_SPEED_BONUS.put("teleporter", 0.25);
    }

    /** 移动部件 → 能耗系数 */
    public static final Map<String, Double> MOVEMENT_ENERGY_COST = new HashMap<>();
    static {
        MOVEMENT_ENERGY_COST.put("wheels", 0.5);
        MOVEMENT_ENERGY_COST.put("propeller", 1.0);
        MOVEMENT_ENERGY_COST.put("teleporter", 1.0);
    }

    // ========== 工具方法 ==========

    /** 从物品注册 ID（如 "copper_mechanical_arm"）中提取材料名 */
    public static String extractMaterial(String itemId) {
        for (String material : MATERIAL_LEVELS.keySet()) {
            if (itemId.startsWith(material + "_")) {
                return material;
            }
        }
        return "copper"; // 默认回退
    }

    /** 从物品注册 ID 中提取等级 */
    public static float extractLevel(String itemId) {
        String material = extractMaterial(itemId);
        return MATERIAL_LEVELS.getOrDefault(material, 3.0f);
    }

    /** 便捷查表：取物数量 */
    public static int getCollectAmount(String material) {
        return ARM_COLLECT_AMOUNT.getOrDefault(material, 6);
    }

    /** 便捷查表：取物耗电 */
    public static int getCollectEnergyCost(String material) {
        return ARM_COLLECT_ENERGY_COST.getOrDefault(material, 15);
    }

    /** 便捷查表：搜索半径 */
    public static int getSensorRadius(String material) {
        return SENSOR_RADIUS.getOrDefault(material, 6);
    }

    /** 便捷查表：机械臂交互半径 */
    public static double getArmRadius(String material) {
        return ARM_RADIUS.getOrDefault(material, 1.0);
    }

    /** 便捷查表：背包格数 */
    public static int getStorageSlots(String material) {
        return STORAGE_SLOTS.getOrDefault(material, 27);
    }

    /** 便捷查表：电池容量 */
    public static int getBatteryCapacity(String material) {
        return BATTERY_CAPACITY.getOrDefault(material, 100_000);
    }

    /** 便捷查表：护盾血量 */
    public static int getShieldHealth(String material) {
        return SHIELD_HEALTH.getOrDefault(material, 8);
    }

    /** 便捷查表：躯干功能组件槽数 */
    public static int getTorsoFunctionalSlots(String material) {
        return TORSO_FUNCTIONAL_SLOTS.getOrDefault(material, 5);
    }

    /** 便捷查表：躯干附件槽数 */
    public static int getTorsoAttachmentSlots(String material) {
        return TORSO_ATTACHMENT_SLOTS.getOrDefault(material, 1);
    }

    /** 便捷查表：躯干血量 */
    public static int getTorsoHealth(String material) {
        return TORSO_HEALTH.getOrDefault(material, 8);
    }

    /** 便捷查表：躯干伤害减算 */
    public static int getTorsoDamageReduction(String material) {
        return TORSO_DAMAGE_REDUCTION.getOrDefault(material, 0);
    }
}
