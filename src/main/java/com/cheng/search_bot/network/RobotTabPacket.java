package com.cheng.search_bot.network;

import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 机器人仓库顶部「分页栏」切换包。
 * 客户端点击某个储物箱分页时发送，通知服务器切换当前分页。
 */
public class RobotTabPacket {
    private final int tab;

    public RobotTabPacket(int tab) {
        this.tab = tab;
    }

    public static void encode(RobotTabPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.tab);
    }

    public static RobotTabPacket decode(FriendlyByteBuf buffer) {
        return new RobotTabPacket(buffer.readVarInt());
    }

    public static void handle(RobotTabPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null && ctx.getSender().containerMenu instanceof RobotInventoryMenu menu) {
                menu.setCurrentTab(packet.tab);
            }
        });
        ctx.setPacketHandled(true);
    }
}
