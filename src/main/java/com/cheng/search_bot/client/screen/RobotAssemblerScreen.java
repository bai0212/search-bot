package com.cheng.search_bot.client.screen;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.menu.custom.RobotAssemblerMenu;
import com.cheng.search_bot.network.AssemblerActionPacket;
import com.cheng.search_bot.network.ModNetworking;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * 机器人装配台的 GUI 界面
 * 宽度 256×256，原版风格；0.5.8 起改成「原子核 + 电子轨道」径向布局。
 *
 * <p>面板由 {@code robot_assembler.png}（radial 版）提供，槽位与按钮由代码按主题画；
 * 槽位坐标和按钮位置沿用 {@link RobotAssemblerMenu}，没有改动。
 */
public class RobotAssemblerScreen extends AbstractContainerScreen<RobotAssemblerMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SearchBotMod.MOD_ID, "textures/gui/robot_assembler.png");

    // 按钮位置（相对界面左上角）：拆解在左下、组装在右下（0.5.8 径向布局）
    private static final int BUTTON_WIDTH = 36;
    private static final int BUTTON_HEIGHT = 18;
    private static final int DISMANTLE_X = 8;
    private static final int DISMANTLE_Y = 106;
    private static final int ASSEMBLE_X = 212;
    private static final int ASSEMBLE_Y = 106;

    public RobotAssemblerScreen(RobotAssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = RobotAssemblerMenu.GUI_WIDTH;   // 256
        this.imageHeight = RobotAssemblerMenu.GUI_HEIGHT; // 256
    }

    @Override
    protected void init() {
        super.init();

        // 0.5.8 径向布局：标题在左上角，物品栏标签在玩家背包上方
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 163;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 槽位顺序：0 输入 / 1 躯干 / 2 核心 / 3 输出，其后是功能组件与附件
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
            if (!slot.isActive()) continue; // 未激活的槽（如未放躯干时的内/外圈槽）不画
            int x = this.leftPos + slot.x;
            int y = this.topPos + slot.y;
            if (i == 0 || i == 3) {
                TechGuiTheme.slot(graphics, x, y, TechGuiTheme.ACCENT_DIM, TechGuiTheme.SLOT_FILL);
            } else if (i == 1 || i == 2) {
                TechGuiTheme.slot(graphics, x, y, TechGuiTheme.ACCENT_MUTE, TechGuiTheme.BG_DARK);
            } else {
                TechGuiTheme.slot(graphics, x, y);
            }
        }

        drawButton(graphics, mouseX, mouseY, DISMANTLE_X, DISMANTLE_Y, "拆解");
        drawButton(graphics, mouseX, mouseY, ASSEMBLE_X, ASSEMBLE_Y, "组装");
    }

    private void drawButton(GuiGraphics graphics, int mouseX, int mouseY, int x, int y, String label) {
        boolean hover = TechGuiTheme.hit(mouseX, mouseY, this.leftPos, this.topPos,
                x, y, BUTTON_WIDTH, BUTTON_HEIGHT);
        TechGuiTheme.button(graphics, this.font, this.leftPos + x, this.topPos + y,
                BUTTON_WIDTH, BUTTON_HEIGHT, label, hover ? TechGuiTheme.ACCENT : TechGuiTheme.LABEL, hover);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (TechGuiTheme.hit(mouseX, mouseY, this.leftPos, this.topPos,
                    DISMANTLE_X, DISMANTLE_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                sendAssemblerAction(false);
                return true;
            }
            if (TechGuiTheme.hit(mouseX, mouseY, this.leftPos, this.topPos,
                    ASSEMBLE_X, ASSEMBLE_Y, BUTTON_WIDTH, BUTTON_HEIGHT)) {
                sendAssemblerAction(true);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void sendAssemblerAction(boolean assemble) {
        ModNetworking.CHANNEL.sendToServer(
                new AssemblerActionPacket(this.menu.getBlockEntity().getBlockPos(), assemble));
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

        // 端口标签（0.5.8 径向布局）
        graphics.drawString(this.font, Component.literal("输入"), 8, 60, TechGuiTheme.LABEL_DIM, false);
        graphics.drawString(this.font, Component.literal("输出"), 222, 60, TechGuiTheme.LABEL_DIM, false);
        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TechGuiTheme.LABEL_DIM, false);
    }
}
