package com.cheng.search_bot.network;

import com.cheng.search_bot.item.custom.RobotControllerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

/**
 * 操控台机器人拖拽操作包（客户端 → 服务端）。
 * 动作：把机器人移动到目标频道 / 拖到行外解除绑定。
 */
public class ControllerRobotPacket {
    public static final int MOVE = 0;
    public static final int UNBIND = 1;

    private final int action;
    private final UUID robotId;
    private final int channel; // MOVE 用：目标频道

    public ControllerRobotPacket(int action, UUID robotId, int channel) {
        this.action = action;
        this.robotId = robotId;
        this.channel = channel;
    }

    public static void encode(ControllerRobotPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
        buffer.writeUUID(packet.robotId);
        buffer.writeVarInt(packet.channel);
    }

    public static ControllerRobotPacket decode(FriendlyByteBuf buffer) {
        return new ControllerRobotPacket(buffer.readVarInt(), buffer.readUUID(), buffer.readVarInt());
    }

    public static void handle(ControllerRobotPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack controller = RobotControllerItem.findController(player);
            if (controller.isEmpty()) return;

            switch (packet.action) {
                case MOVE -> RobotControllerItem.moveRobot(player, controller, packet.robotId, packet.channel);
                case UNBIND -> RobotControllerItem.unbindRobot(player, controller, packet.robotId);
                default -> { }
            }

            // 操作后把最新操控台 NBT 推回客户端（刷新频道列表 + 名字 + 模式）
            ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new ControllerSyncPacket(controller.getTag())
            );
        });
        ctx.setPacketHandled(true);
    }
}
