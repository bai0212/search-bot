package com.cheng.search_bot.item;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.block.ModBlocks;
import com.cheng.search_bot.item.custom.BatteryItem;
import com.cheng.search_bot.item.custom.RobotControllerItem;
import com.cheng.search_bot.item.custom.RobotCoreItem;
import com.cheng.search_bot.item.custom.RobotItem;
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

    // 外部存储接口（功能组件：放入装配台后多一个外部容器槽，机器人可把外部容器当自己的仓库）
    public static final RegistryObject<Item> EXTERNAL_STORAGE_INTERFACE = ITEMS.register("external_storage_interface",
            () -> new Item(new Item.Properties()));

    // 机器人核心（组装必选，持有稳定 RobotId；合成时自动分配「机器人NN」编号）
    public static final RegistryObject<Item> ROBOT_CORE = ITEMS.register("robot_core",
            () -> new RobotCoreItem(new Item.Properties()));

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
            case "external_storage_interface" -> stack.is(EXTERNAL_STORAGE_INTERFACE.get());
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
        if (stack.is(EXTERNAL_STORAGE_INTERFACE.get())) return "external_storage_interface";
        if (stack.is(ROBOT_CORE.get())) return "robot_core";
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
