package com.cheng.search_bot.menu.custom;

import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.item.ModItems;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * 机器人仓库（背包）菜单，顶部两行分页：
 * <ul>
 *   <li>第一行：插件卡功能分页（过滤卡/磁力卡/红石卡/销毁卡/储罐卡/堆叠卡，仅装入的卡显示）。</li>
 *   <li>第二行：储物箱分页（箱子1、箱子2…）。</li>
 * </ul>
 * 布局：左侧一列插件卡槽；中部储物区（一行 9 格，超出可视行数用滚动条）；右侧为当前选中
 * 功能卡分页的配置面板（过滤槽、名单/忽略开关、红石模式等）；底部玩家背包。
 */
public class RobotInventoryMenu extends AbstractContainerMenu {
    public static final int FILTER_SLOTS = 12;
    public static final int CARD_SLOTS_MAX = 6;

    // ===== 布局常量（相对界面左上角）=====
    public static final int SLOT = 18;
    public static final int CARD_X = 8;          // 插件卡槽列（左侧）
    public static final int CARD_Y = 54;
    public static final int STORAGE_X = 30;      // 储物区起点（卡槽列右侧）
    public static final int STORAGE_Y = 54;
    public static final int STORAGE_COLS = 9;
    public static final int VISIBLE_ROWS = 5;    // 储物区可视行数（超出滚动）
    public static final int CONFIG_X = 204;      // 右侧功能卡配置面板起点
    public static final int CONFIG_Y = 54;
    public static final int CONFIG_W = 110;
    public static final int CARD_TAB_Y = 16;     // 卡片分页栏
    public static final int BOX_TAB_Y = 34;      // 储物箱分页栏

    private final ItemStackHandler itemHandler;   // 机器人平铺背包（各储物箱按顺序拼接）
    private final ItemStackHandler filterHandler; // 过滤物品列表
    private final ItemStackHandler cardHandler;   // 插件卡背包（最多 6，实际可用由 cardSlots 决定）
    private final int[] boxSizes;                 // 各储物箱的格数
    private final int[] boxStarts;                // 各储物箱在平铺背包里的起始槽位
    private final int inventorySize;              // 所有储物箱格数之和
    private final int tabCount;                   // 储物箱分页总数
    private final ContainerData tabData;          // 0 = 当前储物箱分页
    private final ContainerData energyData;       // 0=电量 1=最大电量
    private final ContainerData filterData;       // 0=名单模式 1=忽略后缀(0/1)
    private final ContainerData modeData;         // 0 = 工作模式 id（标题处显示 + 点击切换）
    private final ContainerData cardData;         // 0 = 插件卡槽数
    private final ContainerData redstoneData;     // 0 = 红石卡模式（0 无/1 高信号/2 低信号）
    private final ContainerData cardTabData;      // 0 = 当前选中卡片功能分页（-1 = 无）
    private final int tankFluidAmount;            // 储罐卡当前流体量 (mB)
    private final Component tankFluidName;        // 储罐卡当前流体名（空罐为 ""）
    private final SearchRobotEntity robot;        // 服务端非 null，客户端 null

    // 储物区滚动（纯客户端视觉，不存档不联网）
    private int scrollRow = 0;
    private final List<TabSlot> tabSlots = new ArrayList<>();

    // 客户端构造器：从网络包读取背包大小、箱子拆分、电量与过滤状态
    public RobotInventoryMenu(int id, Inventory inv, FriendlyByteBuf extraData) {
        super(com.cheng.search_bot.menu.ModMenus.ROBOT_INVENTORY.get(), id);
        this.robot = null;
        this.itemHandler = new ItemStackHandler(extraData.readInt());
        this.filterHandler = new ItemStackHandler(FILTER_SLOTS);
        this.cardHandler = new ItemStackHandler(CARD_SLOTS_MAX);
        this.boxSizes = readIntArray(extraData);
        int energy = extraData.readInt();
        int maxEnergy = extraData.readInt();
        int filterMode = extraData.readInt();
        int ignoreNbt = extraData.readInt();
        int modeId = extraData.readInt();
        int cardSlots = extraData.readInt();
        int redstoneMode = extraData.readInt();
        int tankAmount = extraData.readInt();
        Component tankName = extraData.readComponent();

        this.boxStarts = computeStarts(this.boxSizes);
        this.inventorySize = sum(this.boxSizes);
        this.tabCount = this.boxSizes.length;

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
        SimpleContainerData m = new SimpleContainerData(1);
        m.set(0, modeId);
        this.modeData = m;
        SimpleContainerData c = new SimpleContainerData(1);
        c.set(0, cardSlots);
        this.cardData = c;
        SimpleContainerData r = new SimpleContainerData(1);
        r.set(0, redstoneMode);
        this.redstoneData = r;
        SimpleContainerData ct = new SimpleContainerData(1);
        ct.set(0, -1); // 默认不选中任何卡片分页
        this.cardTabData = ct;
        this.tankFluidAmount = tankAmount;
        this.tankFluidName = tankName == null ? Component.empty() : tankName;

        this.addDataSlots(tab);
        this.addDataSlots(e);
        this.addDataSlots(f);
        this.addDataSlots(m);
        this.addDataSlots(c);
        this.addDataSlots(r);
        this.addDataSlots(ct);
        addSlots(inv);
    }

    // 服务端构造器：绑定机器人实体，实时同步电量与过滤状态
    public RobotInventoryMenu(int id, Inventory inv, SearchRobotEntity robot) {
        super(com.cheng.search_bot.menu.ModMenus.ROBOT_INVENTORY.get(), id);
        this.robot = robot;
        this.itemHandler = robot.getInventory();
        this.filterHandler = robot.getFilterItems();
        this.cardHandler = robot.getCardInventory();
        this.boxSizes = robot.getBoxSlots();
        this.boxStarts = computeStarts(this.boxSizes);
        this.inventorySize = sum(this.boxSizes);
        this.tabCount = this.boxSizes.length;

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
        this.modeData = new ContainerData() {
            @Override
            public int get(int index) {
                return robot.getMode().id;
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        this.cardData = new ContainerData() {
            @Override
            public int get(int index) {
                return robot.getCardSlots();
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        this.redstoneData = new ContainerData() {
            @Override
            public int get(int index) {
                return robot.getRedstoneMode();
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 1;
            }
        };
        SimpleContainerData ct = new SimpleContainerData(1);
        ct.set(0, -1);
        this.cardTabData = ct;
        this.tankFluidAmount = robot.getTankFluid().getAmount();
        this.tankFluidName = robot.getTankFluid().isEmpty()
                ? Component.empty()
                : robot.getTankFluid().getDisplayName();

        this.addDataSlots(this.tabData);
        this.addDataSlots(this.energyData);
        this.addDataSlots(this.filterData);
        this.addDataSlots(this.modeData);
        this.addDataSlots(this.cardData);
        this.addDataSlots(this.redstoneData);
        this.addDataSlots(this.cardTabData);
        addSlots(inv);
    }

    private void addSlots(Inventory inv) {
        // 玩家背包固定在储物区可视行数之下（不再随箱子行数跳动，超高箱子走滚动）
        int playerInvStartY = STORAGE_Y + VISIBLE_ROWS * SLOT + 14;

        // 储物箱分页：每个箱子一个分页，槽位统一从顶部开始排列，一行 9 格
        for (int box = 0; box < this.boxSizes.length; box++) {
            int size = this.boxSizes[box];
            int base = this.boxStarts[box];
            for (int r = 0; r < size; r++) {
                int col = r % STORAGE_COLS;
                int row = r / STORAGE_COLS;
                TabSlot s = new TabSlot(this.itemHandler, base + r,
                        STORAGE_X + col * SLOT, STORAGE_Y + row * SLOT, STORAGE_Y + row * SLOT, box, this::getCurrentTab);
                this.addSlot(s);
                this.tabSlots.add(s);
            }
        }

        // 玩家背包
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(inv, col + row * 9 + 9, STORAGE_X + col * SLOT, playerInvStartY + row * SLOT));
            }
        }
        // 快捷栏
        for (int i = 0; i < 9; i++) {
            this.addSlot(new Slot(inv, i, STORAGE_X + i * SLOT, playerInvStartY + 58));
        }

        // 过滤物品槽位（右侧配置面板内，2 列 × 6 行；幽灵槽——只做标记，不存实物；
        // 只在选中「过滤卡」分页时激活）
        for (int i = 0; i < FILTER_SLOTS; i++) {
            int col = i % 2;
            int row = i / 2;
            this.addSlot(new FilterSlot(this.filterHandler, i,
                    CONFIG_X + 4 + col * SLOT, CONFIG_Y + 20 + row * SLOT, () -> isFilterTabSelected() ? 1 : 0));
        }

        // 插件卡槽位（左侧 1 列，最多 6 格，实际数量由传感器等级决定）
        int cardSlots = getCardSlots();
        for (int i = 0; i < cardSlots; i++) {
            this.addSlot(new CardSlot(this.cardHandler, i, CARD_X, CARD_Y + i * SLOT));
        }
    }

    /** shift 快速移动（鼠标手势/Mouse Tweaks 依赖此方法）。 */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();

        int robotEnd = this.inventorySize; // 所有机器人容器槽
        int playerStart = robotEnd;
        int playerEnd = playerStart + 36;
        int filterStart = playerEnd;
        int filterEnd = filterStart + FILTER_SLOTS;
        int cardStart = filterEnd;
        int cardEnd = cardStart + getCardSlots();

        if (index < robotEnd) {
            // 机器人容器槽 → 玩家背包
            if (!moveItemStackTo(stack, playerStart, playerEnd, true)) return ItemStack.EMPTY;
        } else if (index < playerEnd) {
            // 玩家背包 → 优先当前分页的容器，再插件卡槽（过滤槽是幽灵槽，不作为 shift 目标）
            int[] target = currentTabSlotRange();
            if (!moveItemStackTo(stack, target[0], target[1], false)) {
                if (!moveItemStackTo(stack, cardStart, cardEnd, false)) return ItemStack.EMPTY;
            }
        } else if (index < filterEnd) {
            // 过滤槽（幽灵标记）→ shift 点击清空该格，不回玩家背包
            slot.set(ItemStack.EMPTY);
            return ItemStack.EMPTY;
        } else {
            // 插件卡槽 → 玩家背包
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

    /** 当前储物箱分页对应的槽位区间（[起, 止)，按 this.slots 的下标） */
    private int[] currentTabSlotRange() {
        int tab = getCurrentTab();
        int start = this.boxStarts[tab];
        return new int[]{start, start + this.boxSizes[tab]};
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    /** 只渲染当前分页、且在可视行数内的储物槽位 */
    private static class TabSlot extends SlotItemHandler {
        private final int tab;
        private final int originY;      // 未滚动时的原始 y（永不改变，滚动偏移都基于它计算）
        private final IntSupplier currentTab;

        TabSlot(IItemHandler handler, int handlerIndex, int x, int originY, int displayY, int tab, IntSupplier currentTab) {
            super(handler, handlerIndex, x, displayY);
            this.originY = originY;
            this.tab = tab;
            this.currentTab = currentTab;
        }

        @Override
        public boolean isActive() {
            if (this.tab != this.currentTab.getAsInt()) return false;
            return this.y >= STORAGE_Y && this.y < STORAGE_Y + VISIBLE_ROWS * SLOT;
        }
    }

    /** 插件卡槽位：只接受插件卡物品；堆叠卡最多放 4 张。 */
    private static class CardSlot extends SlotItemHandler {
        CardSlot(IItemHandler handler, int handlerIndex, int x, int y) {
            super(handler, handlerIndex, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            if (!ModItems.isCard(stack)) return false;
            String type = ModItems.getCardType(stack);
            if (ModItems.isStackingCard(type)) {
                int count = 0;
                IItemHandler cards = getItemHandler();
                for (int i = 0; i < cards.getSlots(); i++) {
                    ItemStack existing = cards.getStackInSlot(i);
                    if (!existing.isEmpty() && ModItems.isStackingCard(ModItems.getCardType(existing))) count++;
                }
                if (count >= ModItems.MAX_STACKING_CARDS) return false;
            }
            return true;
        }
    }

    /**
     * 过滤槽位（幽灵槽）：只做「标记物品」参照，不存实物。
     * 点击标记/清空由 {@code RobotInventoryScreen} 拦截并走 {@code RobotFilterPacket}。
     */
    public static class FilterSlot extends SlotItemHandler {
        private final IntSupplier visible;

        public FilterSlot(IItemHandler handler, int handlerIndex, int x, int y, IntSupplier visible) {
            super(handler, handlerIndex, x, y);
            this.visible = visible;
        }

        @Override
        public boolean isActive() {
            return this.visible.getAsInt() != 0;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false; // 不接收真实物品；标记走网络包，不经过普通放置逻辑
        }

        @Override
        public boolean mayPickup(Player player) {
            return false; // 幽灵标记不可被当作真实物品拾取，避免「标记被拿走/复制」
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

    public ItemStackHandler getCardHandler() {
        return cardHandler;
    }

    /** 当前传感器等级决定的插件卡槽数（最多 6） */
    public int getCardSlots() {
        return Math.max(0, Math.min(CARD_SLOTS_MAX, cardData.get(0)));
    }

    public int getBoxCount() {
        return boxSizes.length;
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
        setScrollRow(0);
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

    /** 红石卡模式：0=无，1=高信号(有信号暂停)，2=低信号(无信号暂停) */
    public int getRedstoneMode() {
        return redstoneData.get(0);
    }

    public SearchRobotEntity getRobot() {
        return robot;
    }

    /** 当前工作模式（标题处显示，点击可循环切换）。 */
    public SearchRobotEntity.RobotMode getRobotMode() {
        return SearchRobotEntity.RobotMode.byId(this.modeData.get(0));
    }

    /** 循环切换到下一个可用模式（仅服务端，由 RobotModePacket 调用）。 */
    public void cycleRobotMode() {
        if (this.robot != null) {
            this.robot.cycleMode();
        }
    }

    /** 循环切换红石卡模式（仅服务端，由 RobotRedstonePacket 调用）。 */
    public void cycleRedstoneMode() {
        if (this.robot != null) {
            this.robot.cycleRedstoneMode();
        }
    }

    /** 储罐卡当前流体量 (mB) */
    public int getTankFluidAmount() {
        return tankFluidAmount;
    }

    /** 储罐卡当前流体名（空罐为 ""） */
    public Component getTankFluidName() {
        return tankFluidName;
    }

    /** 储罐卡容量（与实体 TANK_CAPACITY=8000 一致） */
    public int getTankCapacity() {
        return 8000;
    }

    // ==================== 卡片功能分页 ====================

    /** 当前装入的卡片功能分类（按 {@link ModItems#CARD_CATEGORIES} 顺序），动态计算。 */
    public List<String> getInstalledCategories() {
        List<String> cats = new ArrayList<>();
        for (String category : ModItems.CARD_CATEGORIES) {
            for (int i = 0; i < cardHandler.getSlots(); i++) {
                ItemStack card = cardHandler.getStackInSlot(i);
                if (!card.isEmpty() && category.equals(ModItems.getCardCategory(ModItems.getCardType(card)))) {
                    cats.add(category);
                    break;
                }
            }
        }
        return cats;
    }

    /** 当前选中的卡片功能分类（无选中返回 null）。 */
    public String getSelectedCategory() {
        List<String> cats = getInstalledCategories();
        int idx = cardTabData.get(0);
        if (idx < 0 || idx >= cats.size()) return null;
        return cats.get(idx);
    }

    public void setCardTab(int idx) {
        cardTabData.set(0, idx);
    }

    /** 当前是否选中「过滤卡」分页（决定右侧过滤槽是否激活） */
    public boolean isFilterTabSelected() {
        return "filter".equals(getSelectedCategory());
    }

    /** 当前是否选中「红石卡」分页（决定红石模式按钮是否显示） */
    public boolean isRedstoneTabSelected() {
        return "redstone".equals(getSelectedCategory());
    }

    /** 客户端侧直接按卡片内容算出的单格堆叠上限（储罐卡面板展示用）。 */
    public int getDisplayStackingLimit() {
        int best = 0;
        int count = 0;
        for (int i = 0; i < cardHandler.getSlots(); i++) {
            ItemStack card = cardHandler.getStackInSlot(i);
            if (card.isEmpty()) continue;
            Integer limit = ModItems.getStackingLimit(ModItems.getCardType(card));
            if (limit != null) {
                count++;
                if (count <= ModItems.MAX_STACKING_CARDS) best = Math.max(best, limit);
            }
        }
        return best > 64 ? best : 64;
    }

    // ==================== 储物区滚动 ====================

    public int getScrollRow() {
        return scrollRow;
    }

    public int getMaxScrollRow() {
        int tab = getCurrentTab();
        if (tab < 0 || tab >= boxSizes.length) return 0;
        int rows = (boxSizes[tab] + STORAGE_COLS - 1) / STORAGE_COLS;
        return Math.max(0, rows - VISIBLE_ROWS);
    }

    public void setScrollRow(int row) {
        int max = getMaxScrollRow();
        int clamped = Math.max(0, Math.min(row, max));
        if (clamped == scrollRow) return;
        scrollRow = clamped;
        repositionStorageSlots();
    }

    public void scrollBy(int delta) {
        setScrollRow(scrollRow + delta);
    }

    /**
     * {@link Slot#x}/{@link Slot#y} 是 final，无法原地挪动；滚动时用同样的 handler/index
     * 重建槽位并替换回 {@link #slots}，让原版渲染、悬停和点击命中都用上新坐标。
     */
    private void repositionStorageSlots() {
        for (int i = 0; i < tabSlots.size(); i++) {
            TabSlot old = tabSlots.get(i);
            // 关键：始终用「未滚动的 originY」计算显示位置，避免滚动偏移逐次累加导致槽位越滚越偏、无法回顶。
            TabSlot fresh = new TabSlot(old.getItemHandler(), old.getSlotIndex(), old.x,
                    old.originY, old.originY - scrollRow * SLOT, old.tab, old.currentTab);
            ((Slot) fresh).index = ((Slot) old).index;
            tabSlots.set(i, fresh);
            this.slots.set(((Slot) old).index, fresh);
        }
    }
}
