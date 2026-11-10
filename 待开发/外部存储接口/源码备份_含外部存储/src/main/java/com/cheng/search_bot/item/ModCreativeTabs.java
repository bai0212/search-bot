package com.cheng.search_bot.item;

import com.cheng.search_bot.SearchBotMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 模组的创造模式物品栏标签页
 */
public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, SearchBotMod.MOD_ID);

    public static final RegistryObject<CreativeModeTab> SEARCH_BOT_TAB = CREATIVE_MODE_TABS.register("search_bot_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack(ModItems.ROBOT_ITEM.get()))
                    .title(Component.translatable("itemGroup.search_bot.search_bot_tab"))
                    .displayItems((parameters, output) -> {
                        // 方块物品
                        output.accept(ModItems.ROBOT_ASSEMBLER_ITEM.get());
                        output.accept(ModItems.CHARGING_STATION_ITEM.get());
                        output.accept(ModItems.CREATIVE_CHARGING_STATION_ITEM.get());

                        // 机器人操控台
                        output.accept(ModItems.CONTROLLER.get());

                        // 附件（无材料等级）
                        output.accept(ModItems.WHEELS.get());
                        output.accept(ModItems.PROPELLER.get());
                        output.accept(ModItems.TELEPORTER.get());
                        output.accept(ModItems.WIRELESS_CHARGING.get());
                        output.accept(ModItems.EXTERNAL_STORAGE_INTERFACE.get());

                        // 机器人核心
                        output.accept(ModItems.ROBOT_CORE.get());

                        // 材料等级零件：按零件类型分组，每组内从铜到终极排序
                        for (String part : ModItems.PART_NAMES) {
                            for (String material : ModItems.MATERIALS) {
                                var partItem = ModItems.MATERIAL_PARTS.get(material + "_" + part);
                                if (partItem != null) {
                                    output.accept(partItem.get());
                                }
                            }
                        }

                        // 机器人
                        output.accept(ModItems.ROBOT_ITEM.get());
                    })
                    .build());
}
