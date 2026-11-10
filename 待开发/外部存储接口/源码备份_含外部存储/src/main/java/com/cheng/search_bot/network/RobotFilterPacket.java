package com.cheng.search_bot.network;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 机器人仓库右侧「物品过滤」面板的网络包。
 * 客户端点击名单模式按钮 / 忽略后缀开关时发送，通知服务器改机器人的过滤设置。
 */
public class RobotFilterPacket {
    public static final int ACTION_CYCLE_MODE = 0;    // 名单模式：无过滤 → 黑名单 → 白名单 → 无过滤
    public static final int ACTION_TOGGLE_IGNORE = 1; // 忽略物品后缀（NBT）开关

    private final int action;

    public RobotFilterPacket(int action) {
        this.action = action;
    }

    public static void encode(RobotFilterPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
    }

    public static RobotFilterPacket decode(FriendlyByteBuf buffer) {
        return new RobotFilterPacket(buffer.readVarInt());
    }

    public static void handle(RobotFilterPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null && ctx.getSender().containerMenu instanceof RobotInventoryMenu menu) {
                SearchRobotEntity robot = menu.getRobot();
                if (robot != null) {
                    if (packet.action == ACTION_CYCLE_MODE) {
                        robot.setFilterMode((robot.getFilterMode() + 1) % 3);
                    } else if (packet.action == ACTION_TOGGLE_IGNORE) {
                        robot.setIgnoreFilterNbt(!robot.isIgnoreFilterNbt());
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
