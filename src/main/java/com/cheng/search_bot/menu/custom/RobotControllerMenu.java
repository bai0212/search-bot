package com.cheng.search_bot.menu.custom;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/**
 * 机器人操控台的频道管理菜单（便携 GUI，无物品槽位）。
 * 频道数据存在玩家手持操控台的 NBT 里，客户端屏幕直接读取手持操控台渲染。
 */
public class RobotControllerMenu extends AbstractContainerMenu {
    private final Player player;

    public RobotControllerMenu(int id, Inventory inv) {
        super(com.cheng.search_bot.menu.ModMenus.ROBOT_CONTROLLER.get(), id);
        this.player = inv.player;
    }

    public RobotControllerMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        this(id, inv);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    public Player getPlayer() {
        return player;
    }
}
