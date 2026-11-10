package com.cheng.search_bot.menu.custom;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.function.IntSupplier;

/**
 * 机器人仓库（背包）菜单，顶部带类似创造模式的分页栏。
 * 分页顺序：每个储物箱一个分页（箱子1、箱子2…），有外部容器时最后一个分页是「外部」。
 * 左侧：当前分页的容器 + 玩家背包 + 电量条；
 * 右侧：物品黑白名单开关 + 忽略后缀开关 + 过滤物品槽位（12 格，2 列 × 6 行）。
 */
public class RobotInventoryMenu extends AbstractContainerMenu {
    public static final int FILTER_SLOTS = 12;

    private final ItemStackHandler itemHandler;   // 机器人平铺背包（各储物箱按顺序拼接）
    private final ItemStackHandler filterHandler; // 过滤物品列表
    private final int[] boxSizes;                 // 各储物箱的格数
    private final int[] boxStarts;                // 各储物箱在平铺背包里的起始槽位
    private final int inventorySize;              // 所有储物箱格数之和
    private final IItemHandler externalHandler;   // 外部容器（可能为 null）
    private final int externalSize;               // 外部容器格数
    private final int tabCount;                   // 分页总数
    private int bagRows = 1;                      // 所有分页中行数最多者（玩家背包固定其下）
    private final ContainerData tabData;          // 0 = 当前分页
    private final ContainerData energyData;       // 0=电量 1=最大电量
    private final ContainerData filterData;       // 0=名单模式 1=忽略后缀(0/1)
    private final SearchRobotEntity robot;        // 服务端非 null，客户端 null
    private final Runnable externalFlush;         // 服务端关菜单时写回外部容器，客户端 null

    // 客户端构造器：从网络包读取背包大小、箱子拆分、外部容器格数、电量与过滤状态
    public RobotInventoryMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(com.cheng.search_bot.menu.ModMenus.ROBOT_INVENTORY.get(), id);
        this.robot = null;
        this.externalFlush = null;
        this.itemHandler = new ItemStackHandler(extraData.readInt());
        this.filterHandler = new ItemStackHandler(FILTER_SLOTS);
        this.boxSizes = readIntArray(extraData);
        int extSize = extraData.readInt();
        this.externalHandler = extSize > 0 ? new ItemStackHandler(extSize) : null;
        this.externalSize = this.externalHandler == null ? 0 : this.externalHandler.getSlots();
        int energy = extraData.readInt();
        int maxEnergy = extraData.readInt();
        int filterMode = extraData.readInt();
        int ignoreNbt = extraData.readInt();

        this.boxStarts = computeStarts(this.boxSizes);
        this.inventorySize = sum(this.boxSizes);
        this.tabCount = this.boxSizes.length + (this.externalSize > 0 ? 1 : 0);

        SimpleContainerData tab = new SimpleContainerData(1);
        this.tabData = tab;
        SimpleContainerData e = new SimpleContainerData(2);
        e.set(0, energy);
        e.set(1, maxEnergy);
        this.energyData = e;
        SimpleContainerData f = new SimpleContainerData(2);
        f.set(0, filterMode);
        f.set(1, ignoreNbt);
        this.filterData = f;

        this.addDataSlots(tab);
        this.addDataSlots(e);
        this.addDataSlots(f);
        addSlots(inv);
    }

    // 服务端构造器：绑定机器人实体，实时同步电量与过滤状态
    public RobotInventoryMenu(int id, Inventory inv, SearchRobotEntity robot) {
        super(com.cheng.search_bot.menu.ModMenus.ROBOT_INVENTORY.get(), id);
        this.robot = robot;
        this.itemHandler = robot.getInventory();
        this.filterHandler = robot.getFilterItems();
        this.boxSizes = robot.getBoxSlots();
        this.boxStarts = computeStarts(this.boxSizes);
        this.inventorySize = sum(this.boxSizes);
        this.externalHandler = robot.buildExternalContainerHandler();
        this.externalSize = this.externalHandler == null ? 0 : this.externalHandler.getSlots();
        this.externalFlush = robot::flushExternalContainer;
        this.tabCount = this.boxSizes.length + (this.externalSize > 0 ? 1 : 0);

        this.tabData = new SimpleContainerData(1);
        this.energyData = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? robot.getEnergyStorage().getEnergyStored()
                                  : robot.getEnergyStorage().getMaxEnergyStored();
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
        this.filterData = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? robot.getFilterMode()
                                  : (robot.isIgnoreFilterNbt() ? 1 : 0);
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };

        this.addDataSlots(this.tabData);
        this.addDataSlots(this.energyData);
        this.addDataSlots(this.filterData);
        addSlots(inv);
    }

    private void addSlots(Inventory inv) {
        int bagTop = 36; // 顶部标题 + 分页栏（约 36px）之下开始放容器槽位

        // 玩家背包固定在「所有分页里行数最多者」下方，切页时不会上下跳动
        int maxRows = 1;
        for (int size : this.boxSizes) maxRows = Math.max(maxRows, (size + 8) / 9);
        if (this.externalSize > 0) maxRows = Math.max(maxRows, (this.externalSize + 8) / 9);
        this.bagRows = maxRows;

        int playerInvStartY = bagTop + maxRows * 18 + 14;

        // 储物箱分页：每个箱子一个分页，槽位统一从顶部开始排列
        for (int box = 0; box < this.boxSizes.length; box++) {
            int size = this.boxSizes[box];
            int base = this.boxStarts[box];
            for (int r = 0; r < size; r++) {
                int col = r % 9;
                int row = r / 9;
                this.addSlot(new TabSlot(this.itemHandler, base + r,
                        8 + col * 18, bagTop + row * 18, box, this::getCurrentTab));
            }
        }

        // 外部容器分页
        if (this.externalHandler != null && this.externalSize > 0) {
            int tab = this.boxSizes.length;
            for (int r = 0; r < this.externalSize; r++) {
                int col = r % 9;
                int row = r / 9;
                this.addSlot(new TabSlot(this.externalHandler, r,
                        8 + col * 18, bagTop + row * 18, tab, this::getCurrentTab));
            }
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, playerInvStartY + row * 18));
            }
        }
        // 快捷栏
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, 8 + i * 18, playerInvStartY + 58));
        }

        // 过滤物品槽位（右侧 2 列 × 6 行，共 12 格）
        for (int i = 0; i < FILTER_SLOTS; i++) {
            int col = i % 2;
            int row = i / 2;
            this.addSlot(new SlotItemHandler(this.filterHandler, i, 178 + col * 18, 66 + row * 18));
        }
    }

    /** shift 快速移动（鼠标手势/Mouse Tweaks 依赖此方法）。 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int robotEnd = this.inventorySize + this.externalSize; // 所有机器人容器槽
        int playerStart = robotEnd;
        int playerEnd = playerStart + 36;
        int filterStart = playerEnd;
        int filterEnd = filterStart + FILTER_SLOTS;

        if (index < robotEnd) {
            // 机器人容器槽 → 玩家背包
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if (index < playerEnd) {
            // 玩家背包 → 优先当前分页的容器，满了再放过滤槽
            int[] target = currentTabSlotRange();
            if (!moveItemStackTo(stack, target[0], target[1], false)) {
                if (!moveItemStackTo(stack, filterStart, filterEnd, false)) return ItemStack.EMPTY;
            }
        } else {
            // 过滤槽 → 玩家背包
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
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

    /** 当前分页对应的槽位区间（[起, 止)，按 this.slots 的下标） */
    private int[] currentTabSlotRange() {
        int tab = getCurrentTab();
        if (tab < this.boxSizes.length) {
            int start = this.boxStarts[tab];
            return new int[]{start, start + this.boxSizes[tab]};
        }
        int start = this.inventorySize; // 外部容器槽排在所有箱子槽之后
        return new int[]{start, start + this.externalSize};
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        // 服务端关菜单时，把重建的模组容器方块实体改动写回外部容器物品
        if (this.externalFlush != null && !player.level().isClientSide) {
            this.externalFlush.run();
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 只渲染当前分页的槽位（其它分页的槽位背景/图标都不画） */
    private static class TabSlot extends SlotItemHandler {
        private final int tab;
        private final IntSupplier currentTab;

        TabSlot(IItemHandler handler, int index, int x, int y, int tab, IntSupplier currentTab) {
            super(handler, index, x, y);
            this.tab = tab;
            this.currentTab = currentTab;
        }

        @Override
        public boolean isActive() {
            return this.tab == this.currentTab.getAsInt();
        }
    }

    private static int[] readIntArray(FriendlyByteBuf buf) {
        int n = buf.readInt();
        int[] arr = new int[Math.max(0, n)];
        for (int i = 0; i < arr.length; i++) arr[i] = buf.readInt();
        return arr;
    }

    private static int[] computeStarts(int[] sizes) {
        int[] starts = new int[sizes.length];
        int sum = 0;
        for (int i = 0; i < sizes.length; i++) {
            starts[i] = sum;
            sum += sizes[i];
        }
        return starts;
    }

    private static int sum(int[] a) {
        int s = 0;
        for (int v : a) s += v;
        return s;
    }

    public ItemStackHandler getItemHandler() {
        return itemHandler;
    }

    public ItemStackHandler getFilterHandler() {
        return filterHandler;
    }

    public int getBagRows() {
        return bagRows;
    }

    public int getBoxCount() {
        return boxSizes.length;
    }

    public boolean hasExternalTab() {
        return externalSize > 0;
    }

    public int getTabCount() {
        return tabCount;
    }

    public int getCurrentTab() {
        return tabData.get(0);
    }

    public void setCurrentTab(int tab) {
        int max = Math.max(0, tabCount - 1);
        tabData.set(0, Math.max(0, Math.min(tab, max)));
    }

    public int getEnergy() {
        return energyData.get(0);
    }

    public int getMaxEnergy() {
        return energyData.get(1);
    }

    public int getFilterMode() {
        return filterData.get(0);
    }

    public boolean isIgnoreFilterNbt() {
        return filterData.get(1) != 0;
    }

    public SearchRobotEntity getRobot() {
        return robot;
    }
}
