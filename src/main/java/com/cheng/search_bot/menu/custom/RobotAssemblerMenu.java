package com.cheng.search_bot.menu.custom;

import com.cheng.search_bot.block.entity.custom.RobotAssemblerBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.SlotItemHandler;

/**
 * 机器人装配台的容器菜单
 * 布局（0.5.8 径向「原子核 + 电子轨道」）：
 * 躯体槽居中作原子核，核心槽在正上方，10 个功能组件槽在内圈、6 个附件槽在外圈；
 * 输入/输出端口在左右两侧；没放躯体时内/外圈槽位不出现（isActive=false），
 * 躯干等级决定内/外圈可用数量。底部是玩家背包。
 */
public class RobotAssemblerMenu extends AbstractContainerMenu {
    public static final int GUI_WIDTH = 256;
    public static final int GUI_HEIGHT = 256;

    private final RobotAssemblerBlockEntity blockEntity;
    private final ContainerData data;

    public RobotAssemblerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv, (RobotAssemblerBlockEntity) inv.player.level().getBlockEntity(extraData.readBlockPos()), new net.minecraft.world.inventory.SimpleContainerData(0));
    }

    public RobotAssemblerMenu(int id, Inventory inv, RobotAssemblerBlockEntity blockEntity, ContainerData data) {
        super(com.cheng.search_bot.menu.ModMenus.ROBOT_ASSEMBLER.get(), id);
        this.blockEntity = blockEntity;
        this.data = data;

        var handler = blockEntity.getItemHandler();

        // === 径向布局（0.5.8）：原子核=躯体、核心在正上方，内圈功能组件、外圈附件 ===
        this.addSlot(new SlotItemHandler(handler, RobotAssemblerBlockEntity.SLOT_ROBOT_INPUT, 8, 84));
        this.addSlot(new SlotItemHandler(handler, RobotAssemblerBlockEntity.SLOT_TORSO, 119, 84));
        this.addSlot(new SlotItemHandler(handler, RobotAssemblerBlockEntity.SLOT_CORE, 119, 20));
        this.addSlot(new SlotItemHandler(handler, RobotAssemblerBlockEntity.SLOT_ROBOT_OUTPUT, 230, 84));

        // === 内圈：功能组件槽（10 格，槽索引 4..13），没放躯体或超出躯干等级时不出现 ===
        int[] functionalX = {119, 143, 157, 157, 143, 119, 95, 81, 81, 95};
        int[] functionalY = {44, 52, 72, 96, 116, 124, 116, 96, 72, 52};
        for (int i = 0; i < functionalX.length; i++) {
            int slotIndex = RobotAssemblerBlockEntity.FUNCTIONAL_START + i;
            this.addSlot(new SlotItemHandler(handler, slotIndex, functionalX[i], functionalY[i]) {
                @Override
                public boolean isActive() {
                    return blockEntity != null
                            && (slotIndex - RobotAssemblerBlockEntity.FUNCTIONAL_START) < blockEntity.getActiveFunctionalSlots();
                }
            });
        }

        // === 外圈：附件槽（6 格，槽索引 14..19），没放躯体或超出躯干等级时不出现 ===
        int[] attachmentX = {169, 181, 147, 91, 57, 69};
        int[] attachmentY = {44, 98, 142, 142, 98, 44};
        for (int i = 0; i < attachmentX.length; i++) {
            int slotIndex = RobotAssemblerBlockEntity.ATTACHMENT_START + i;
            this.addSlot(new SlotItemHandler(handler, slotIndex, attachmentX[i], attachmentY[i]) {
                @Override
                public boolean isActive() {
                    return blockEntity != null
                            && (slotIndex - RobotAssemblerBlockEntity.ATTACHMENT_START) < blockEntity.getActiveAttachmentSlots();
                }
            });
        }

        // === 下方玩家背包（居中于 256 宽 GUI，x 起点 47） ===
        int invX = (GUI_WIDTH - 9 * 18) / 2; // (256 - 162) / 2 = 47
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, invX + col * 18, 176 + row * 18));
            }
        }
        // 快捷栏
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(inv, col, invX + col * 18, 234));
        }
    }

    /**
     * shift 快速移动（鼠标手势/Mouse Tweaks 依赖此方法）。
     * 槽位顺序：装配台 20 格 → 玩家背包(27) + 快捷栏(9)。
     * 装配台各槽的合法性由 isItemValid 限制，moveItemStackTo 会自动尊重。
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int assemblerSlots = RobotAssemblerBlockEntity.TOTAL_SLOTS;
        int playerEnd = assemblerSlots + 36;

        if (index < assemblerSlots) {
            // 装配台 → 玩家背包
            if (!moveItemStackTo(stack, assemblerSlots, playerEnd, true)) return ItemStack.EMPTY;
        } else {
            // 玩家背包 → 装配台（isItemValid 会限制只能放进对应槽）
            if (!moveItemStackTo(stack, 0, assemblerSlots, false)) return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public RobotAssemblerBlockEntity getBlockEntity() {
        return blockEntity;
    }

}
