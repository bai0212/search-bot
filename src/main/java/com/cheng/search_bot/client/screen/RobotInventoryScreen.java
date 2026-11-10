package com.cheng.search_bot.client.screen;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.item.ModItems;
import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import com.cheng.search_bot.network.ModNetworking;
import com.cheng.search_bot.network.RobotFilterPacket;
import com.cheng.search_bot.network.RobotModePacket;
import com.cheng.search_bot.network.RobotRedstonePacket;
import com.cheng.search_bot.network.RobotTabPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * 机器人背包 GUI（0.8.0 重构版）。
 *
 * <p>顶部两行分页：
 * <ul>
 *   <li>第一行：插件卡功能分页（过滤卡/磁力卡/红石卡/销毁卡/储罐卡/堆叠卡，仅装入的卡显示）。</li>
 *   <li>第二行：储物箱分页（箱子1、箱子2…）。</li>
 * </ul>
 * 左侧一列插件卡槽；中部储物区（一行 9 格，超高箱子用滚动条）；右侧是当前选中功能卡
 * 分页的配置面板（过滤槽 + 名单/忽略开关、红石模式按钮、储罐显示等）；底部玩家背包。
 */
public class RobotInventoryScreen extends AbstractContainerScreen<RobotInventoryMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SearchBotMod.MOD_ID, "textures/gui/robot_inventory.png");

    private static final int IMAGE_WIDTH = 320;
    private static final int IMAGE_HEIGHT = 241;
    private static final int TAB_H = 14;

    // 右侧配置面板按钮（相对界面左上角）
    private static final int CFG_X = RobotInventoryMenu.CONFIG_X + 6;  // 210
    private static final int CFG_BTN_W = RobotInventoryMenu.CONFIG_W - 12; // 98
    private static final int CFG_BTN_H = 16;
    private static final int CFG_BTN_Y1 = 186;  // 名单模式 / 红石模式
    private static final int CFG_BTN_Y2 = 206;  // 忽略后缀

    // 储物区滚动条
    private static final int SB_X = 195;
    private static final int SB_W = 6;

    private boolean scrolling = false;

    public RobotInventoryScreen(RobotInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = IMAGE_WIDTH;
        this.imageHeight = IMAGE_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        // 「物品栏」标签放在储物区与玩家背包之间的空带
        this.inventoryLabelX = RobotInventoryMenu.STORAGE_X;
        this.inventoryLabelY = RobotInventoryMenu.STORAGE_Y + RobotInventoryMenu.VISIBLE_ROWS * RobotInventoryMenu.SLOT + 4;
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        g.fill(this.leftPos, this.topPos, this.leftPos + this.imageWidth, this.topPos + this.imageHeight, TechGuiTheme.BG);
        g.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, Math.min(this.imageHeight, 256));
        TechGuiTheme.panelFrame(g, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        drawCardTabBar(g, mouseX, mouseY);
        drawBoxTabBar(g, mouseX, mouseY);

        // 储物区 / 玩家背包分隔带
        int sepY = this.topPos + RobotInventoryMenu.STORAGE_Y + RobotInventoryMenu.VISIBLE_ROWS * RobotInventoryMenu.SLOT + 1;
        g.fill(this.leftPos + RobotInventoryMenu.STORAGE_X, sepY,
                this.leftPos + RobotInventoryMenu.STORAGE_X + RobotInventoryMenu.STORAGE_COLS * RobotInventoryMenu.SLOT, sepY + 1,
                TechGuiTheme.ACCENT_DIM);

        for (Slot slot : this.menu.slots) {
            if (!slot.isActive()) continue;
            int sx = this.leftPos + slot.x;
            int sy = this.topPos + slot.y;
            if (slot instanceof RobotInventoryMenu.FilterSlot || slot.x < RobotInventoryMenu.STORAGE_X) {
                // 过滤槽 / 插件卡槽：用强调描边（原版主题下与普通槽一致）
                TechGuiTheme.slot(g, sx, sy, TechGuiTheme.ACCENT, TechGuiTheme.BG_DARK);
            } else {
                TechGuiTheme.slot(g, sx, sy);
            }
        }

        drawScrollbar(g);
        drawConfigPanel(g, mouseX, mouseY);
    }

    /** 第一行：插件卡功能分页。 */
    private void drawCardTabBar(GuiGraphics g, int mouseX, int mouseY) {
        List<String> cats = this.menu.getInstalledCategories();
        int y = this.topPos + RobotInventoryMenu.CARD_TAB_Y;
        int x = this.leftPos + 8;
        for (int t = 0; t < cats.size(); t++) {
            String label = ModItems.getCardCategoryName(cats.get(t));
            int w = Math.max(this.font.width(label) + 14, 30);
            boolean active = t == selectedCardTab();
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TAB_H;
            int bg = active ? TechGuiTheme.ACCENT : (hover ? TechGuiTheme.ACCENT_DIM : TechGuiTheme.BG_DARK);
            g.fill(x, y, x + w, y + TAB_H, bg);
            g.renderOutline(x, y, w, TAB_H, active ? TechGuiTheme.ACCENT : TechGuiTheme.ACCENT_DIM);
            int color = active ? TechGuiTheme.BG : TechGuiTheme.LABEL;
            g.drawString(this.font, label, x + (w - this.font.width(label)) / 2, y + (TAB_H - 8) / 2, color, false);
            x += w + 2;
        }
    }

    /** 第二行：储物箱分页。 */
    private void drawBoxTabBar(GuiGraphics g, int mouseX, int mouseY) {
        int count = this.menu.getTabCount();
        int y = this.topPos + RobotInventoryMenu.BOX_TAB_Y;
        int x = this.leftPos + 8;
        for (int t = 0; t < count; t++) {
            String label = "箱子" + (t + 1);
            int w = Math.max(this.font.width(label) + 14, 30);
            boolean active = t == this.menu.getCurrentTab();
            boolean hover = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TAB_H;
            int bg = active ? TechGuiTheme.ACCENT : (hover ? TechGuiTheme.ACCENT_DIM : TechGuiTheme.BG_DARK);
            g.fill(x, y, x + w, y + TAB_H, bg);
            g.renderOutline(x, y, w, TAB_H, active ? TechGuiTheme.ACCENT : TechGuiTheme.ACCENT_DIM);
            int color = active ? TechGuiTheme.BG : TechGuiTheme.LABEL;
            g.drawString(this.font, label, x + (w - this.font.width(label)) / 2, y + (TAB_H - 8) / 2, color, false);
            x += w + 2;
        }
    }

    private int selectedCardTab() {
        List<String> cats = this.menu.getInstalledCategories();
        String sel = this.menu.getSelectedCategory();
        return sel == null ? -1 : cats.indexOf(sel);
    }

    private int cardTabX(int t) {
        List<String> cats = this.menu.getInstalledCategories();
        int x = 8;
        for (int i = 0; i < t; i++) {
            x += Math.max(this.font.width(ModItems.getCardCategoryName(cats.get(i))) + 14, 30) + 2;
        }
        return x;
    }

    private int cardTabWidth(int t) {
        List<String> cats = this.menu.getInstalledCategories();
        return Math.max(this.font.width(ModItems.getCardCategoryName(cats.get(t))) + 14, 30);
    }

    private int boxTabX(int t) {
        int x = 8;
        for (int i = 0; i < t; i++) x += boxTabWidth(i) + 2;
        return x;
    }

    private int boxTabWidth(int t) {
        return Math.max(this.font.width("箱子" + (t + 1)) + 14, 30);
    }

    /** 储物区滚动条（超高箱子时才出现）。 */
    private void drawScrollbar(GuiGraphics g) {
        int max = this.menu.getMaxScrollRow();
        if (max <= 0) return;
        int trackY = this.topPos + RobotInventoryMenu.STORAGE_Y;
        int trackH = RobotInventoryMenu.VISIBLE_ROWS * RobotInventoryMenu.SLOT;
        int x = this.leftPos + SB_X;
        g.fill(x, trackY, x + SB_W, trackY + trackH, TechGuiTheme.SLOT_FILL);
        int totalRows = RobotInventoryMenu.VISIBLE_ROWS + max;
        int thumbH = Math.max(12, trackH * RobotInventoryMenu.VISIBLE_ROWS / totalRows);
        int thumbY = trackY + (trackH - thumbH) * this.menu.getScrollRow() / max;
        g.fill(x, thumbY, x + SB_W, thumbY + thumbH, TechGuiTheme.ACCENT);
        g.renderOutline(x, trackY, SB_W, trackH, TechGuiTheme.ACCENT_DIM);
    }

    /** 右侧配置面板：面板边框 + 标题 + 各功能卡对应的配置内容。 */
    private void drawConfigPanel(GuiGraphics g, int mouseX, int mouseY) {
        int px = this.leftPos + RobotInventoryMenu.CONFIG_X;
        int py = this.topPos + RobotInventoryMenu.CONFIG_Y;
        int ph = this.imageHeight - 7 - RobotInventoryMenu.CONFIG_Y;
        g.renderOutline(px, py, RobotInventoryMenu.CONFIG_W, ph, TechGuiTheme.ACCENT_DIM);

        String category = this.menu.getSelectedCategory();
        if (category == null) {
            g.drawString(this.font, "功能卡", px + 6, py + 6, TechGuiTheme.ACCENT, false);
            g.drawString(this.font, "左侧装入插件卡", px + 6, py + 22, TechGuiTheme.LABEL_DIM, false);
            g.drawString(this.font, "后在此分页配置", px + 6, py + 34, TechGuiTheme.LABEL_DIM, false);
            return;
        }
        g.drawString(this.font, ModItems.getCardCategoryName(category), px + 6, py + 6, TechGuiTheme.ACCENT, false);

        switch (category) {
            case "filter" -> drawFilterConfig(g, mouseX, mouseY);
            case "redstone" -> drawRedstoneConfig(g, mouseX, mouseY);
            case "stacking" -> drawStackingConfig(g, px, py);
            case "tank" -> drawTankConfig(g, px, py);
            case "magnet" -> drawHint(g, px, py, "自动吸取掉落物", "范围固定 6 格");
            case "void" -> drawHint(g, px, py, "配合过滤卡", "销毁被过滤的物品");
            default -> {
            }
        }
    }

    private void drawFilterConfig(GuiGraphics g, int mouseX, int mouseY) {
        int mode = this.menu.getFilterMode();
        String modeLabel = switch (mode) {
            case 1 -> "黑名单";
            case 2 -> "白名单";
            default -> "无过滤";
        };
        int modeColor = switch (mode) {
            case 1 -> TechGuiTheme.DANGER;
            case 2 -> TechGuiTheme.OK;
            default -> TechGuiTheme.LABEL;
        };
        TechGuiTheme.button(g, this.font, this.leftPos + CFG_X, this.topPos + CFG_BTN_Y1, CFG_BTN_W, CFG_BTN_H,
                modeLabel, modeColor, cfgHover(mouseX, mouseY, CFG_BTN_Y1));

        boolean ignore = this.menu.isIgnoreFilterNbt();
        String ignoreLabel = "忽略后缀:" + (ignore ? "开" : "关");
        int ignoreColor = ignore ? TechGuiTheme.OK : TechGuiTheme.LABEL_DIM;
        TechGuiTheme.button(g, this.font, this.leftPos + CFG_X, this.topPos + CFG_BTN_Y2, CFG_BTN_W, CFG_BTN_H,
                ignoreLabel, ignoreColor, cfgHover(mouseX, mouseY, CFG_BTN_Y2));
    }

    private void drawRedstoneConfig(GuiGraphics g, int mouseX, int mouseY) {
        int mode = this.menu.getRedstoneMode();
        String label = switch (mode) {
            case 1 -> "高信号暂停";
            case 2 -> "低信号暂停";
            default -> "红石关闭";
        };
        int color = mode == 0 ? TechGuiTheme.LABEL_DIM : TechGuiTheme.OK;
        TechGuiTheme.button(g, this.font, this.leftPos + CFG_X, this.topPos + CFG_BTN_Y1, CFG_BTN_W, CFG_BTN_H,
                label, color, cfgHover(mouseX, mouseY, CFG_BTN_Y1));
    }

    private void drawStackingConfig(GuiGraphics g, int px, int py) {
        int limit = this.menu.getDisplayStackingLimit();
        g.drawString(this.font, "单格堆叠上限", px + 6, py + 22, TechGuiTheme.LABEL_DIM, false);
        g.drawString(this.font, String.valueOf(limit), px + 6, py + 38, TechGuiTheme.ACCENT, false);
        g.drawString(this.font, "最多 4 张堆叠卡", px + 6, py + 56, TechGuiTheme.LABEL_DIM, false);
    }

    private void drawTankConfig(GuiGraphics g, int px, int py) {
        Component name = this.menu.getTankFluidName();
        int amount = this.menu.getTankFluidAmount();
        int cap = this.menu.getTankCapacity();
        String fluidText = name.getString().isEmpty() ? "空" : name.getString();
        g.drawString(this.font, "流体:" + fluidText, px + 6, py + 22, TechGuiTheme.LABEL, false);
        g.drawString(this.font, amount + " / " + cap + " mB", px + 6, py + 38, TechGuiTheme.LABEL, false);
        // 小填充条
        int barW = RobotInventoryMenu.CONFIG_W - 12;
        g.fill(px + 6, py + 56, px + 6 + barW, py + 62, TechGuiTheme.SLOT_FILL);
        double ratio = cap <= 0 ? 0 : Math.min(1.0, (double) amount / cap);
        g.fill(px + 6, py + 56, px + 6 + (int) (barW * ratio), py + 62, TechGuiTheme.ACCENT);
    }

    private void drawHint(GuiGraphics g, int px, int py, String line1, String line2) {
        g.drawString(this.font, line1, px + 6, py + 22, TechGuiTheme.LABEL, false);
        g.drawString(this.font, line2, px + 6, py + 34, TechGuiTheme.LABEL_DIM, false);
    }

    private boolean cfgHover(int mouseX, int mouseY, int relY) {
        return TechGuiTheme.hit(mouseX, mouseY, this.leftPos, this.topPos, CFG_X, relY, CFG_BTN_W, CFG_BTN_H);
    }

    private boolean overStorage(double mouseX, double mouseY) {
        return mouseX >= this.leftPos + RobotInventoryMenu.STORAGE_X
                && mouseX < this.leftPos + RobotInventoryMenu.STORAGE_X + RobotInventoryMenu.STORAGE_COLS * RobotInventoryMenu.SLOT
                && mouseY >= this.topPos + RobotInventoryMenu.STORAGE_Y
                && mouseY < this.topPos + RobotInventoryMenu.STORAGE_Y + RobotInventoryMenu.VISIBLE_ROWS * RobotInventoryMenu.SLOT;
    }

    private boolean overScrollbar(double mouseX, double mouseY) {
        if (this.menu.getMaxScrollRow() <= 0) return false;
        return mouseX >= this.leftPos + SB_X && mouseX < this.leftPos + SB_X + SB_W
                && mouseY >= this.topPos + RobotInventoryMenu.STORAGE_Y
                && mouseY < this.topPos + RobotInventoryMenu.STORAGE_Y + RobotInventoryMenu.VISIBLE_ROWS * RobotInventoryMenu.SLOT;
    }

    private void scrollFromMouseY(double mouseY) {
        int max = this.menu.getMaxScrollRow();
        if (max <= 0) return;
        int trackY = this.topPos + RobotInventoryMenu.STORAGE_Y;
        int trackH = RobotInventoryMenu.VISIBLE_ROWS * RobotInventoryMenu.SLOT;
        int totalRows = RobotInventoryMenu.VISIBLE_ROWS + max;
        int thumbH = Math.max(12, trackH * RobotInventoryMenu.VISIBLE_ROWS / totalRows);
        int avail = Math.max(1, trackH - thumbH);
        double rel = (mouseY - trackY - thumbH / 2.0) / avail;
        this.menu.setScrollRow((int) Math.round(rel * max));
    }

    /** 过滤槽点击：手上有物品就标记（幽灵复制，不消耗），空手就清空该格。 */
    private void handleFilterSlotClick(RobotInventoryMenu.FilterSlot slot) {
        ItemStack carried = this.minecraft.player.containerMenu.getCarried();
        if (carried.isEmpty()) {
            ModNetworking.CHANNEL.sendToServer(new RobotFilterPacket(
                    RobotFilterPacket.ACTION_CLEAR_SLOT, slot.getSlotIndex(), ItemStack.EMPTY));
        } else {
            ModNetworking.CHANNEL.sendToServer(new RobotFilterPacket(
                    RobotFilterPacket.ACTION_SET_SLOT, slot.getSlotIndex(), carried));
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        // 过滤槽（幽灵标记）：参照精妙背包，点击只打标记/清空，不消耗手里物品
        Slot under = this.getSlotUnderMouse();
        if (under instanceof RobotInventoryMenu.FilterSlot filterSlot) {
            handleFilterSlotClick(filterSlot);
            return true;
        }

        if (button == 0) {
            // 标题点击 → 循环切换工作模式
            int titleW = this.font.width(this.title);
            if (mouseX >= this.leftPos + this.titleLabelX && mouseX < this.leftPos + this.titleLabelX + titleW
                    && mouseY >= this.topPos + this.titleLabelY && mouseY < this.topPos + this.titleLabelY + 9) {
                ModNetworking.CHANNEL.sendToServer(new RobotModePacket());
                return true;
            }

            // 插件卡功能分页点击（纯客户端 UI 状态）
            List<String> cats = this.menu.getInstalledCategories();
            for (int t = 0; t < cats.size(); t++) {
                int x = this.leftPos + cardTabX(t);
                int y = this.topPos + RobotInventoryMenu.CARD_TAB_Y;
                int w = cardTabWidth(t);
                if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TAB_H) {
                    this.menu.setCardTab(t);
                    return true;
                }
            }

            // 储物箱分页点击
            int count = this.menu.getTabCount();
            for (int t = 0; t < count; t++) {
                int x = this.leftPos + boxTabX(t);
                int y = this.topPos + RobotInventoryMenu.BOX_TAB_Y;
                int w = boxTabWidth(t);
                if (mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TAB_H) {
                    if (t != this.menu.getCurrentTab()) {
                        this.menu.setCurrentTab(t); // 客户端先切，避免等待服务端回包
                        ModNetworking.CHANNEL.sendToServer(new RobotTabPacket(t));
                    }
                    return true;
                }
            }

            // 滚动条点击/拖拽
            if (overScrollbar(mouseX, mouseY)) {
                this.scrolling = true;
                scrollFromMouseY(mouseY);
                return true;
            }

            // 配置面板按钮
            String category = this.menu.getSelectedCategory();
            if ("filter".equals(category)) {
                if (cfgHover((int) mouseX, (int) mouseY, CFG_BTN_Y1)) {
                    ModNetworking.CHANNEL.sendToServer(new RobotFilterPacket(RobotFilterPacket.ACTION_CYCLE_MODE));
                    return true;
                }
                if (cfgHover((int) mouseX, (int) mouseY, CFG_BTN_Y2)) {
                    ModNetworking.CHANNEL.sendToServer(new RobotFilterPacket(RobotFilterPacket.ACTION_TOGGLE_IGNORE));
                    return true;
                }
            } else if ("redstone".equals(category)) {
                if (cfgHover((int) mouseX, (int) mouseY, CFG_BTN_Y1)) {
                    ModNetworking.CHANNEL.sendToServer(new RobotRedstonePacket());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.scrolling) {
            scrollFromMouseY(mouseY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        this.scrolling = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (delta != 0 && overStorage(mouseX, mouseY)) {
            this.menu.scrollBy(delta > 0 ? -1 : 1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TechGuiTheme.LABEL, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                TechGuiTheme.LABEL_DIM, false);
        drawEnergyBar(graphics);
    }

    /** 电量条：分格式科技指示条，位置在储物区与玩家背包之间的空带右侧。 */
    private void drawEnergyBar(GuiGraphics graphics) {
        int energy = this.menu.getEnergy();
        int maxEnergy = Math.max(1, this.menu.getMaxEnergy());
        int barX = RobotInventoryMenu.STORAGE_X + 82;
        int barY = this.inventoryLabelY;
        int barWidth = 52;
        int barHeight = 6;

        String energyText = compactEnergy(energy) + "/" + compactEnergy(maxEnergy);
        graphics.drawString(this.font, energyText, barX - 4 - this.font.width(energyText), barY, TechGuiTheme.LABEL, false);

        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, TechGuiTheme.SLOT_FILL);
        graphics.fill(barX, barY, barX + barWidth, barY + 1, TechGuiTheme.BEVEL_LO);
        graphics.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, TechGuiTheme.BEVEL_HI);

        double ratio = Math.min(1.0, (double) energy / maxEnergy);
        int color;
        if (ratio < 0.2) color = TechGuiTheme.DANGER;
        else if (ratio < 0.5) color = TechGuiTheme.ENERGY_MID;
        else color = TechGuiTheme.ACCENT;

        int inner = barWidth - 2;
        graphics.fill(barX + 1, barY + 2, barX + 1 + (int) (inner * ratio), barY + barHeight - 2, color);
        for (int tick = 1; tick < 8; tick++) {
            int tx = barX + 1 + inner * tick / 8;
            graphics.fill(tx, barY + 2, tx + 1, barY + barHeight - 2, TechGuiTheme.SLOT_FILL);
        }
    }

    /** 大电量缩写：1 000 000 000→1G、8 500 000→8.5M、100 000→100K，小于 1000 原样显示。 */
    private static String compactEnergy(int value) {
        if (value >= 1_000_000_000) return withDecimal(value, 1_000_000_000) + "G";
        if (value >= 1_000_000) return withDecimal(value, 1_000_000) + "M";
        if (value >= 1_000) return withDecimal(value, 1_000) + "K";
        return String.valueOf(value);
    }

    private static String withDecimal(int value, int unit) {
        int whole = value / unit;
        int frac = (value % unit) / (unit / 10);
        return frac == 0 ? String.valueOf(whole) : whole + "." + frac;
    }
}
