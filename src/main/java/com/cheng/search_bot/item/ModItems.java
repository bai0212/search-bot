package com.cheng.search_bot.item;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.block.ModBlocks;
import com.cheng.search_bot.item.custom.BatteryItem;
import com.cheng.search_bot.item.custom.RobotCardItem;
import com.cheng.search_bot.item.custom.RobotControllerItem;
import com.cheng.search_bot.item.custom.RobotCoreItem;
import com.cheng.search_bot.item.custom.RobotItem;
import com.cheng.search_bot.item.custom.SignalControllerItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, SearchBotMod.MOD_ID);

    // Block items
    public static final RegistryObject<Item> ROBOT_ASSEMBLER_ITEM = ITEMS.register("robot_assembler",
            () -> new BlockItem(ModBlocks.ROBOT_ASSEMBLER.get(), new Item.Properties()));

    public static final RegistryObject<Item> CHARGING_STATION_ITEM = ITEMS.register("charging_station",
            () -> new BlockItem(ModBlocks.CHARGING_STATION.get(), new Item.Properties()));

    public static final RegistryObject<Item> CREATIVE_CHARGING_STATION_ITEM = ITEMS.register("creative_charging_station",
            () -> new BlockItem(ModBlocks.CREATIVE_CHARGING_STATION.get(), new Item.Properties()));

    // Robot item (item form of the entity)
    public static final RegistryObject<Item> ROBOT_ITEM = ITEMS.register("robot",
            () -> new RobotItem(new Item.Properties().stacksTo(1)));

    // Robot controller (handheld, binds/controls a robot)
    public static final RegistryObject<Item> CONTROLLER = ITEMS.register("robot_controller",
            () -> new RobotControllerItem(new Item.Properties().stacksTo(1)));

    // ====== 移动部件（无材料等级，固定 3 种）======
    public static final RegistryObject<Item> WHEELS = ITEMS.register("wheels",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> PROPELLER = ITEMS.register("propeller",
            () -> new Item(new Item.Properties()));
    public static final RegistryObject<Item> TELEPORTER = ITEMS.register("teleporter",
            () -> new Item(new Item.Properties()));

    // 无线充电装置（附件，每种只能装一个）
    public static final RegistryObject<Item> WIRELESS_CHARGING = ITEMS.register("wireless_charging",
            () -> new Item(new Item.Properties()));

    // 机器人核心（组装必选，持有稳定 RobotId；合成时自动分配「机器人NN」编号）
    public static final RegistryObject<Item> ROBOT_CORE = ITEMS.register("robot_core",
            () -> new RobotCoreItem(new Item.Properties()));

    // ====== 插件卡（精妙背包式升级，不装在躯干上，装在机器人 GUI 里）======
    // 卡类型标识（注册 id 同时用作功能判定的 key）
    public static final String CARD_FILTER = "filter_card";   // 过滤卡：启用标记物品黑白名单
    public static final String CARD_MAGNET = "magnet_card";   // 磁力卡：吸取周围掉落物
    public static final String CARD_REDSTONE = "redstone_card"; // 红石卡：配合信号控制器
    public static final String CARD_VOID = "void_card";       // 销毁卡：销毁被过滤掉的物品
    public static final String CARD_TANK = "tank_card";       // 储罐卡：获得流体储罐

    // 堆叠卡按材质分六档（每档一张卡），单格堆叠上限递增
    public static final String CARD_STACKING_COPPER = "stacking_card_copper";
    public static final String CARD_STACKING_IRON = "stacking_card_iron";
    public static final String CARD_STACKING_GOLD = "stacking_card_gold";
    public static final String CARD_STACKING_DIAMOND = "stacking_card_diamond";
    public static final String CARD_STACKING_NETHERITE = "stacking_card_netherite";
    public static final String CARD_STACKING_ULTIMATE = "stacking_card_ultimate";

    /** 堆叠卡各材质档 → 单格堆叠上限（64 的倍数） */
    public static final Map<String, Integer> STACKING_LIMITS = Map.of(
            CARD_STACKING_COPPER, 128,     // 2×64
            CARD_STACKING_IRON, 256,       // 4×64
            CARD_STACKING_GOLD, 512,       // 8×64
            CARD_STACKING_DIAMOND, 1024,   // 16×64
            CARD_STACKING_NETHERITE, 2048, // 32×64
            CARD_STACKING_ULTIMATE, 4096   // 64×64
    );

    /** 机器人最多同时生效的堆叠卡数量 */
    public static final int MAX_STACKING_CARDS = 4;

    public static final String[] CARD_IDS = {
            CARD_FILTER, CARD_MAGNET, CARD_REDSTONE, CARD_VOID, CARD_TANK,
            CARD_STACKING_COPPER, CARD_STACKING_IRON, CARD_STACKING_GOLD,
            CARD_STACKING_DIAMOND, CARD_STACKING_NETHERITE, CARD_STACKING_ULTIMATE
    };

    /** 卡 id → 中文功能说明（tooltip 用） */
    public static final Map<String, String> CARD_DESCRIPTIONS = Map.ofEntries(
            Map.entry(CARD_FILTER, "装上后启用「标记物品」黑白名单过滤"),
            Map.entry(CARD_MAGNET, "自动吸取周围掉落物进背包"),
            Map.entry(CARD_REDSTONE, "配合信号控制器接收红石信号"),
            Map.entry(CARD_VOID, "销毁被过滤掉的物品（不捡进背包）"),
            Map.entry(CARD_TANK, "获得 8 桶流体储罐"),
            Map.entry(CARD_STACKING_COPPER, "单格堆叠上限提升到 128"),
            Map.entry(CARD_STACKING_IRON, "单格堆叠上限提升到 256"),
            Map.entry(CARD_STACKING_GOLD, "单格堆叠上限提升到 512"),
            Map.entry(CARD_STACKING_DIAMOND, "单格堆叠上限提升到 1024"),
            Map.entry(CARD_STACKING_NETHERITE, "单格堆叠上限提升到 2048"),
            Map.entry(CARD_STACKING_ULTIMATE, "单格堆叠上限提升到 4096")
    );

    public static final Map<String, RegistryObject<Item>> CARDS = new HashMap<>();
    static {
        for (String id : CARD_IDS) {
            String desc = CARD_DESCRIPTIONS.getOrDefault(id, "");
            CARDS.put(id, ITEMS.register(id, () -> new RobotCardItem(new Item.Properties(), desc)));
        }
    }

    /** 是否属于堆叠卡（任意材质档） */
    public static boolean isStackingCard(String cardId) {
        return cardId != null && STACKING_LIMITS.containsKey(cardId);
    }

    /** 堆叠卡的单格上限；非堆叠卡返回 null */
    public static Integer getStackingLimit(String cardId) {
        return cardId == null ? null : STACKING_LIMITS.get(cardId);
    }

    /** 卡片功能分类（GUI 顶部分页用）：filter/magnet/redstone/void/tank/stacking */
    public static String getCardCategory(String cardId) {
        if (isStackingCard(cardId)) return "stacking";
        return switch (cardId) {
            case "filter_card" -> "filter";
            case "magnet_card" -> "magnet";
            case "redstone_card" -> "redstone";
            case "void_card" -> "void";
            case "tank_card" -> "tank";
            default -> cardId;
        };
    }

    /** 卡片功能分类的固定顺序（GUI 顶部分页按此顺序排列） */
    public static final String[] CARD_CATEGORIES = {"filter", "magnet", "redstone", "void", "tank", "stacking"};

    /** 卡片功能分类 → 中文名（顶部分页标签用） */
    public static final Map<String, String> CARD_CATEGORY_NAMES = Map.of(
            "filter", "过滤卡",
            "magnet", "磁力卡",
            "redstone", "红石卡",
            "void", "销毁卡",
            "tank", "储罐卡",
            "stacking", "堆叠卡"
    );

    public static String getCardCategoryName(String category) {
        return CARD_CATEGORY_NAMES.getOrDefault(category, category);
    }

    // ====== 信号控制器（配合红石卡）======
    public static final RegistryObject<Item> SIGNAL_CONTROLLER = ITEMS.register("signal_controller",
            () -> new SignalControllerItem(ModBlocks.SIGNAL_CONTROLLER.get(), new Item.Properties()));

    // ====== 材料等级零件（6 种材料 × 7 类 = 42 个）======
    // 7 类：功能组件 5 类（机械臂/传感器/发动机/存储箱/电池）+ 能量护盾 + 躯干
    public static final String[] MATERIALS = {"copper", "iron", "gold", "diamond", "netherite", "ultimate"};
    public static final String[] PART_NAMES = {
            "mechanical_arm", "sensor", "engine",
            "storage_box", "battery", "shield_part", "torso"
    };

    /** 新材料零件映射：key = "material_partName"，如 "copper_mechanical_arm" */
    public static final Map<String, RegistryObject<Item>> MATERIAL_PARTS = new HashMap<>();

    static {
        for (String material : MATERIALS) {
            for (String part : PART_NAMES) {
                String id = material + "_" + part;
                boolean isBattery = part.equals("battery");
                MATERIAL_PARTS.put(id, ITEMS.register(id, () ->
                        isBattery ? new BatteryItem(new Item.Properties()) : new Item(new Item.Properties())
                ));
            }
        }
    }

    /**
     * 判断 ItemStack 是否属于某类零件。
     * 无等级附件（履带 wheels/螺旋桨 propeller/传送装置 teleporter/无线充电 wireless_charging）只匹配单个物品。
     * 其余（功能组件/护盾/躯干）匹配材料等级版。
     */
    public static boolean isPart(ItemStack stack, String partName) {
        return switch (partName) {
            case "wheels" -> stack.is(WHEELS.get());
            case "propeller" -> stack.is(PROPELLER.get());
            case "teleporter" -> stack.is(TELEPORTER.get());
            case "wireless_charging" -> stack.is(WIRELESS_CHARGING.get());
            case "robot_core" -> stack.is(ROBOT_CORE.get());
            // 材料等级零件：功能组件 / 护盾 / 躯干
            case "mechanical_arm", "sensor", "engine",
                    "storage_box", "battery", "shield_part", "torso" -> {
                for (String material : MATERIALS) {
                    RegistryObject<Item> partItem = MATERIAL_PARTS.get(material + "_" + partName);
                    if (partItem != null && stack.is(partItem.get())) {
                        yield true;
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    /**
     * 返回零件的规范类型名（如 "mechanical_arm"、"torso"、"wireless_charging"）。
     * 不是任何已知零件时返回 null。
     */
    public static String getPartType(ItemStack stack) {
        for (String part : PART_NAMES) {
            if (isPart(stack, part)) return part;
        }
        if (stack.is(WHEELS.get())) return "wheels";
        if (stack.is(PROPELLER.get())) return "propeller";
        if (stack.is(TELEPORTER.get())) return "teleporter";
        if (stack.is(WIRELESS_CHARGING.get())) return "wireless_charging";
        if (stack.is(ROBOT_CORE.get())) return "robot_core";
        return null;
    }

    /** 判断物品是否属于插件卡。 */
    public static boolean isCard(ItemStack stack) {
        return getCardType(stack) != null;
    }

    /** 返回插件卡的类型 id（如 "filter_card"）；不是插件卡时返回 null。 */
    public static String getCardType(ItemStack stack) {
        for (String id : CARD_IDS) {
            RegistryObject<Item> card = CARDS.get(id);
            if (card != null && stack.is(card.get())) return id;
        }
        return null;
    }

    /**
     * 读取（或首次生成）机器人核心持有的稳定 RobotId，并写回核心 NBT。
     * 同一核心反复组装/拆解都保持同一个 id，操控台绑定因此不会断。
     */
    public static UUID getOrCreateCoreId(ItemStack core) {
        CompoundTag tag = core.getOrCreateTag();
        if (tag.hasUUID("RobotId")) {
            return tag.getUUID("RobotId");
        }
        UUID id = UUID.randomUUID();
        tag.putUUID("RobotId", id);
        return id;
    }
}
