package com.cheng.search_bot.network;

import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 机器人仓库 GUI 红石卡分页「红石模式」按钮的网络包。
 * 通知服务器把红石卡模式循环切换（无 → 高信号暂停 → 低信号暂停 → 无）。
 */
public class RobotRedstonePacket {
    public RobotRedstonePacket() {
    }

    public static void encode(RobotRedstonePacket packet, FriendlyByteBuf buffer) {
    }

    public static RobotRedstonePacket decode(FriendlyByteBuf buffer) {
        return new RobotRedstonePacket();
    }

    public static void handle(RobotRedstonePacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null && ctx.getSender().containerMenu instanceof RobotInventoryMenu menu) {
                menu.cycleRedstoneMode();
            }
        });
        ctx.setPacketHandled(true);
    }
}
