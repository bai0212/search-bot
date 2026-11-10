package com.cheng.search_bot.client;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.block.entity.ModBlockEntities;
import com.cheng.search_bot.client.model.SearchRobotModel;
import com.cheng.search_bot.client.renderer.block.ChargingStationRenderer;
import com.cheng.search_bot.client.renderer.entity.SearchRobotRenderer;
import com.cheng.search_bot.client.screen.RobotAssemblerScreen;
import com.cheng.search_bot.client.screen.RobotControllerScreen;
import com.cheng.search_bot.client.screen.RobotInventoryScreen;
import com.cheng.search_bot.entity.ModEntities;
import com.cheng.search_bot.menu.ModMenus;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SearchBotMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientSetup {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.register(ModMenus.ROBOT_ASSEMBLER.get(), RobotAssemblerScreen::new);
            MenuScreens.register(ModMenus.ROBOT_INVENTORY.get(), RobotInventoryScreen::new);
            MenuScreens.register(ModMenus.ROBOT_CONTROLLER.get(), RobotControllerScreen::new);
        });
    }

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(SearchRobotModel.LAYER_LOCATION, SearchRobotModel::createBodyLayer);
    }

    @SubscribeEvent
    public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SEARCH_ROBOT.get(), SearchRobotRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntities.CHARGING_STATION.get(), ChargingStationRenderer::new);
    }
}
