package com.cheng.search_bot.network;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 机器人仓库右侧「标记物品」面板的网络包。
 *
 * <p>参照精妙背包：过滤槽是<b>幽灵槽</b>——点击只是打个标记，不消耗玩家手里的物品。
 * 因此这里除了名单模式/忽略后缀两个开关，还支持「标记某格（SET_SLOT）」和「清空某格（CLEAR_SLOT）」。
 */
public class RobotFilterPacket {
    public static final int ACTION_CYCLE_MODE = 0;    // 名单模式：无过滤 → 黑名单 → 白名单 → 无过滤
    public static final int ACTION_TOGGLE_IGNORE = 1; // 忽略物品后缀（NBT）开关
    public static final int ACTION_SET_SLOT = 2;      // 幽灵标记：把手持物品标记进某过滤槽（不消耗）
    public static final int ACTION_CLEAR_SLOT = 3;    // 清空某过滤槽

    private final int action;
    private final int slot;
    private final ItemStack stack;

    /** 模式/忽略开关用（无槽位、无物品）。 */
    public RobotFilterPacket(int action) {
        this(action, 0, ItemStack.EMPTY);
    }

    public RobotFilterPacket(int action, int slot, ItemStack stack) {
        this.action = action;
        this.slot = slot;
        this.stack = stack;
    }

    public static void encode(RobotFilterPacket packet, FriendlyByteBuf buffer) {
        buffer.writeVarInt(packet.action);
        buffer.writeVarInt(packet.slot);
        buffer.writeNbt(packet.stack.save(new CompoundTag()));
    }

    public static RobotFilterPacket decode(FriendlyByteBuf buffer) {
        int action = buffer.readVarInt();
        int slot = buffer.readVarInt();
        CompoundTag tag = buffer.readNbt();
        ItemStack stack = tag == null ? ItemStack.EMPTY : ItemStack.of(tag);
        return new RobotFilterPacket(action, slot, stack);
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
                    } else if (packet.action == ACTION_SET_SLOT) {
                        if (packet.slot >= 0 && packet.slot < robot.getFilterItems().getSlots()) {
                            ItemStack mark = packet.stack.copy();
                            mark.setCount(1); // 幽灵标记：只存 1 个作参照
                            robot.getFilterItems().setStackInSlot(packet.slot, mark);
                        }
                    } else if (packet.action == ACTION_CLEAR_SLOT) {
                        if (packet.slot >= 0 && packet.slot < robot.getFilterItems().getSlots()) {
                            robot.getFilterItems().setStackInSlot(packet.slot, ItemStack.EMPTY);
                        }
                    }
                }
            }
        });
        ctx.setPacketHandled(true);
    }
}
