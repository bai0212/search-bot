package com.cheng.search_bot.network;

import com.cheng.search_bot.block.entity.custom.RobotAssemblerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 装配台按钮操作的网络包
 * 客户端点击"组装"或"拆解"按钮时，发送此包通知服务器执行
 */
public class AssemblerActionPacket {
    private final BlockPos pos;
    private final boolean assemble; // true=组装, false=拆解

    public AssemblerActionPacket(BlockPos pos, boolean assemble) {
        this.pos = pos;
        this.assemble = assemble;
    }

    public static void encode(AssemblerActionPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeBoolean(packet.assemble);
    }

    public static AssemblerActionPacket decode(FriendlyByteBuf buffer) {
        return new AssemblerActionPacket(buffer.readBlockPos(), buffer.readBoolean());
    }

    public static void handle(AssemblerActionPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            if (ctx.getSender() != null && ctx.getSender().level() != null) {
                BlockEntity be = ctx.getSender().level().getBlockEntity(packet.pos);
                if (be instanceof RobotAssemblerBlockEntity assembler) {
                    if (packet.assemble) {
                        assembler.assembleRobot();
                    } else {
                        assembler.disassembleRobot();
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
