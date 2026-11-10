package com.cheng.search_bot.network;

import com.cheng.search_bot.item.custom.RobotControllerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * 批量删除频道包（客户端 → 服务端）。
 * GUI 里点击 × 标记待删频道，关闭 GUI 时一次性发送待删索引，服务端统一执行删除。
 */
public class DeleteChannelsPacket {
    private final List<Integer> channels;

    public DeleteChannelsPacket(List<Integer> channels) {
        this.channels = channels;
    }

    public static void encode(DeleteChannelsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.channels.size());
        for (int c : packet.channels) {
            buffer.writeVarInt(c);
        }
    }

    public static DeleteChannelsPacket decode(FriendlyByteBuf buffer) {
        int size = buffer.readVarInt();
        List<Integer> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(buffer.readVarInt());
        }
        return new DeleteChannelsPacket(list);
    }

    public static void handle(DeleteChannelsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null) return;
            ItemStack controller = RobotControllerItem.findController(player);
            if (controller.isEmpty()) return;

            RobotControllerItem.deleteChannels(player, controller, packet.channels);

            ModNetworking.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new ControllerSyncPacket(controller.getTag())
            );
        });
        ctx.setPacketHandled(true);
    }
}
