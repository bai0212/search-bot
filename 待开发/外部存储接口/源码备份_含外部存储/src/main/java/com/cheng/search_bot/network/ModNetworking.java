package com.cheng.search_bot.network;

import com.cheng.search_bot.SearchBotMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetworking {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(SearchBotMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                AssemblerActionPacket.class,
                AssemblerActionPacket::encode,
                AssemblerActionPacket::decode,
                AssemblerActionPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                ControllerActionPacket.class,
                ControllerActionPacket::encode,
                ControllerActionPacket::decode,
                ControllerActionPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                ControllerSyncPacket.class,
                ControllerSyncPacket::encode,
                ControllerSyncPacket::decode,
                ControllerSyncPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                ControllerRobotPacket.class,
                ControllerRobotPacket::encode,
                ControllerRobotPacket::decode,
                ControllerRobotPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                DeleteChannelsPacket.class,
                DeleteChannelsPacket::encode,
                DeleteChannelsPacket::decode,
                DeleteChannelsPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                RobotFilterPacket.class,
                RobotFilterPacket::encode,
                RobotFilterPacket::decode,
                RobotFilterPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                RobotTabPacket.class,
                RobotTabPacket::encode,
                RobotTabPacket::decode,
                RobotTabPacket::handle
        );
    }
}
