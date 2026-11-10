package com.cheng.search_bot.network;

import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 机器人仓库 GUI 标题处点击「模式」文字时发送的网络包。
 * 通知服务器把机器人循环切换到下一个可用工作模式（跟随 → 探索 → 收集 → 运输 → 待命）。
 * 无参数：切换逻辑全部由 {@link RobotInventoryMenu#cycleRobotMode()} 在服务端完成。
 */
public class RobotModePacket {
    public RobotModePacket() {
    }

    public static void encode(RobotModePacket packet, FriendlyByteBuf buffer) {
    }

    public static RobotModePacket decode(FriendlyByteBuf buffer) {
        return new RobotModePacket();
    }

    public static void handle(RobotModePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null && ctx.getSender().containerMenu instanceof RobotInventoryMenu menu) {
                menu.cycleRobotMode();
            }
        });
        ctx.setPacketHandled(true);
    }
}
