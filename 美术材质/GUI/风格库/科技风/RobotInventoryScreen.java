package com.cheng.search_bot.client.screen;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.menu.custom.RobotInventoryMenu;
import com.cheng.search_bot.network.ModNetworking;
import com.cheng.search_bot.network.RobotFilterPacket;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 机器人背包 GUI。
 *
 * <p>0.5.3：右侧「标记物品」面板和标题条烘在 {@code robot_inventory.png} 里，
 * 左侧储物区高度随机器等级变化，由代码补齐底色和外框。
 * 槽位坐标、按钮位置全部沿用 {@link RobotInventoryMenu}，没有改动。
 */
public class RobotInventoryScreen extends AbstractContainerScreen<RobotInventoryMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SearchBotMod.MOD_ID, "textures/gui/robot_inventory.png");

    // 右侧「物品过滤」面板的按钮布局（坐标相对界面左上角）
    private static final int BTN_X = 178;
    private static final int BTN_MODE_Y = 18;
    private static final int BTN_IGNORE_Y = 40;
    private static final int BTN_W = 76;
    private static final int BTN_H = 18;

    // 左侧储物区：首行 y=18，每行 18px（与 RobotInventoryMenu 一致）
    private static final int BAG_TOP = 18;
    private static final int ROW_H = 18;
    // 右侧面板的分界线
    private static final int PANEL_X = 174;

    public RobotInventoryScreen(RobotInventoryMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 256;
        // 高度要同时容纳左侧背包行数，以及右侧过滤面板（槽位到 y=174）
        this.imageHeight = Math.max(116 + menu.getInventoryRows() * ROW_H, 181);
    }

    @Override
    protected void init() {
        super.init();
        // 「背包」标签放在储物区和玩家背包之间的空带上
        this.inventoryLabelX = 8;
        this.inventoryLabelY = BAG_TOP + this.menu.getInventoryRows() * ROW_H + 4;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        // 先铺满主题底色（贴图左侧是透明的，高度随等级变化），再叠固定部件
        graphics.fill(this.leftPos, this.topPos,
                this.leftPos + this.imageWidth, this.topPos + this.imageHeight, TechGuiTheme.BG);
        graphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
        TechGuiTheme.panelFrame(graphics, this.leftPos, this.topPos, this.imageWidth, this.imageHeight);

        // 储物区 / 玩家背包 分隔带
        int sepY = this.topPos + BAG_TOP + this.menu.getInventoryRows() * ROW_H + 1;
        graphics.fill(this.leftPos + 6, sepY, this.leftPos + PANEL_X - 2, sepY + 1, TechGuiTheme.ACCENT_DIM);
        graphics.fill(this.leftPos + 6, sepY, this.leftPos + 26, sepY + 1, TechGuiTheme.ACCENT);

        for (Slot slot : this.menu.slots) {
            // 右侧「标记物品」槽位用蓝调描边，和左侧储物槽区分开
            if (slot.x >= PANEL_X) {
                TechGuiTheme.slot(graphics, this.leftPos + slot.x, this.topPos + slot.y,
                        TechGuiTheme.ACCENT_DIM, TechGuiTheme.BG_DARK);
            } else {
                TechGuiTheme.slot(graphics, this.leftPos + slot.x, this.topPos + slot.y);
            }
        }

        drawFilterPanel(graphics, mouseX, mouseY);
    }

    /** 画右侧「物品过滤」面板：标题 + 名单模式按钮 + 忽略后缀开关 */
    private void drawFilterPanel(GuiGraphics g, int mouseX, int mouseY) {
        int px = this.leftPos + BTN_X;
        int py = this.topPos;

        // 面板标题
        g.drawString(this.font, "标记物品", px, py + 4, TechGuiTheme.ACCENT, false);

        // 名单模式按钮
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
        TechGuiTheme.button(g, this.font, px, py + BTN_MODE_Y, BTN_W, BTN_H, modeLabel, modeColor,
                isHover(mouseX, mouseY, BTN_X, BTN_MODE_Y));

        // 忽略后缀开关
        boolean ignore = this.menu.isIgnoreFilterNbt();
        String ignoreLabel = "忽略后缀:" + (ignore ? "开" : "关");
        int ignoreColor = ignore ? TechGuiTheme.OK : TechGuiTheme.LABEL_DIM;
        TechGuiTheme.button(g, this.font, px, py + BTN_IGNORE_Y, BTN_W, BTN_H, ignoreLabel, ignoreColor,
                isHover(mouseX, mouseY, BTN_X, BTN_IGNORE_Y));
    }

    private boolean isHover(int mouseX, int mouseY, int relX, int relY) {
        return TechGuiTheme.hit(mouseX, mouseY, this.leftPos, this.topPos, relX, relY, BTN_W, BTN_H);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isHover((int) mouseX, (int) mouseY, BTN_X, BTN_MODE_Y)) {
                ModNetworking.CHANNEL.sendToServer(new RobotFilterPacket(RobotFilterPacket.ACTION_CYCLE_MODE));
                return true;
            }
            if (isHover((int) mouseX, (int) mouseY, BTN_X, BTN_IGNORE_Y)) {
                ModNetworking.CHANNEL.sendToServer(new RobotFilterPacket(RobotFilterPacket.ACTION_TOGGLE_IGNORE));
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TechGuiTheme.ACCENT, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY,
                TechGuiTheme.LABEL_DIM, false);

        drawEnergyBar(graphics);
    }

    /** 电量条：分格式科技指示条，位置在玩家背包标签同一行右侧。 */
    private void drawEnergyBar(GuiGraphics graphics) {
        int energy = this.menu.getEnergy();
        int maxEnergy = Math.max(1, this.menu.getMaxEnergy());
        int barX = this.leftPos + 120;
        int barY = this.topPos + this.inventoryLabelY;
        int barWidth = 48;
        int barHeight = 8;

        // 电量数字（右对齐到电量条左侧）
        String energyText = energy + "/" + maxEnergy;
        graphics.drawString(this.font, energyText, barX - 4 - this.font.width(energyText), barY, TechGuiTheme.LABEL, false);

        // 底色 + 外框
        graphics.fill(barX, barY, barX + barWidth, barY + barHeight, TechGuiTheme.SLOT_FILL);
        graphics.fill(barX, barY, barX + barWidth, barY + 1, TechGuiTheme.BEVEL_LO);
        graphics.fill(barX, barY + barHeight - 1, barX + barWidth, barY + barHeight, TechGuiTheme.BEVEL_HI);

        double ratio = Math.min(1.0, (double) energy / maxEnergy);
        int color;
        if (ratio < 0.2) color = TechGuiTheme.DANGER;
        else if (ratio < 0.5) color = 0xFFFFD24A;
        else color = TechGuiTheme.ACCENT;

        // 连续填充 + 每格暗色刻线，读起来像一段段的电量指示
        int inner = barWidth - 2;
        graphics.fill(barX + 1, barY + 2, barX + 1 + (int) (inner * ratio), barY + barHeight - 2, color);
        for (int tick = 1; tick < 8; tick++) {
            int tx = barX + 1 + inner * tick / 8;
            graphics.fill(tx, barY + 2, tx + 1, barY + barHeight - 2, TechGuiTheme.SLOT_FILL);
        }
    }
}
