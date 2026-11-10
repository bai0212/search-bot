package com.cheng.search_bot.network;

import com.cheng.search_bot.item.custom.RobotControllerItem;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 操控台状态同步包（服务端 → 客户端）。
 * 服务端每次改动操控台 NBT 后，把完整 NBT 推回客户端，
 * 客户端据此刷新频道管理 GUI 与容器标记方框。
 */
public class ControllerSyncPacket {
    private final CompoundTag tag; // 可能为 null（无 NBT）

    public ControllerSyncPacket(CompoundTag tag) {
        this.tag = tag;
    }

    public static void encode(ControllerSyncPacket packet, FriendlyByteBuf buffer) {
        buffer.writeNbt(packet.tag);
    }

    public static ControllerSyncPacket decode(FriendlyByteBuf buffer) {
        return new ControllerSyncPacket(buffer.readNbt());
    }

    public static void handle(ControllerSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                ItemStack controller = RobotControllerItem.findController(mc.player);
                if (!controller.isEmpty()) {
                    controller.setTag(packet.tag);
                }
            }
        }));
        ctx.setPacketHandled(true);
    }
}
