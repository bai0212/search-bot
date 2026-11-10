package com.cheng.search_bot.network;

import com.cheng.search_bot.item.custom.RobotControllerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 操控台动作网络包（客户端 → 服务端）。
 * 动作：切换频道 / 设置当前频道 / 循环模式 / 添加频道 / 改名。
 */
public class ControllerActionPacket {
    public static final int SWITCH_CHANNEL = 0;
    public static final int SET_CURRENT = 1;
    public static final int CYCLE_MODE = 2;
    public static final int ADD_CHANNEL = 3;
    public static final int RENAME = 4;
    public static final int DELETE_CHANNEL = 5;
    public static final int UNBIND_ALL = 6;
    public static final int CYCLE_MODE_CHANNEL = 7;

    private final int action;
    private final int value;      // SWITCH_CHANNEL 用 delta；SET_CURRENT / RENAME 用 index
    private final String text;    // RENAME 用新名字

    public ControllerActionPacket(int action, int value, String text) {
        this.action = action;
        this.value = value;
        this.text = text;
    }

    public static void encode(ControllerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
        buffer.writeVarInt(packet.value);
        buffer.writeUtf(packet.text == null ? "" : packet.text);
    }

    public static ControllerActionPacket decode(FriendlyByteBuf buffer) {
        return new ControllerActionPacket(buffer.readVarInt(), buffer.readVarInt(), buffer.readUtf());
    }

    public static void handle(ControllerActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack controller = RobotControllerItem.findController(player);
            if (controller.isEmpty()) return;

            switch (packet.action) {
                case SWITCH_CHANNEL -> RobotControllerItem.switchChannel(player, controller, packet.value);
                case SET_CURRENT -> RobotControllerItem.setCurrentChannel(player, controller, packet.value);
                case CYCLE_MODE -> RobotControllerItem.cycleCurrentMode(player, controller);
                case ADD_CHANNEL -> RobotControllerItem.addChannel(player, controller);
                case RENAME -> RobotControllerItem.renameChannel(player, controller, packet.value, packet.text);
                case DELETE_CHANNEL -> RobotControllerItem.deleteChannel(player, controller);
                case UNBIND_ALL -> RobotControllerItem.unbindCurrentChannel(player, controller);
                case CYCLE_MODE_CHANNEL -> RobotControllerItem.cycleModeForChannel(player, controller, packet.value);
                default -> { }
            }
            // 动作后把最新操控台 NBT 推回客户端（同步频道列表 + 标记方框）
            ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new ControllerSyncPacket(controller.getTag())
            );
        });
        ctx.setPacketHandled(true);
    }
}
