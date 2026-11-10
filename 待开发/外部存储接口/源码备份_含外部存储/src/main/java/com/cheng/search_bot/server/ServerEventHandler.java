package com.cheng.search_bot.server;

import com.cheng.search_bot.SearchBotMod;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 服务端事件处理器（仅运行在服务端/单人模式 integrated server）
 */
@Mod.EventBusSubscriber(modid = SearchBotMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ServerEventHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ServerEventHandler.class);

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Search Bot 模组服务端启动中");
    }
}
