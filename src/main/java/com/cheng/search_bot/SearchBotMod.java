package com.cheng.search_bot;

import com.cheng.search_bot.block.ModBlocks;
import com.cheng.search_bot.block.entity.ModBlockEntities;
import com.cheng.search_bot.entity.ModEntities;
import com.cheng.search_bot.entity.ModEntities;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.item.ModCreativeTabs;
import com.cheng.search_bot.item.ModItems;
import com.cheng.search_bot.menu.ModMenus;
import com.cheng.search_bot.network.ModNetworking;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(SearchBotMod.MOD_ID)
public class SearchBotMod {
    public static final String MOD_ID = "search_bot";

    public SearchBotMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        ModBlocks.BLOCKS.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModEntities.ENTITY_TYPES.register(modEventBus);
        ModMenus.MENUS.register(modEventBus);
        ModCreativeTabs.CREATIVE_MODE_TABS.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onAttributeCreate);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModNetworking.register();
        });
    }

    private void onAttributeCreate(final EntityAttributeCreationEvent event) {
        event.put(ModEntities.SEARCH_ROBOT.get(), SearchRobotEntity.createAttributes().build());
    }
}
