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
 * 宽度 256，原版风格（0.5.1：核心组件 2 格 + 功能组件 + 附件）
 *
 * <p>0.5.3：面板由 {@code robot_assembler.png} 提供，槽位与按钮由代码按主题画；
 * 槽位坐标和按钮位置沿用 {@link RobotAssemblerMenu}，没有改动。
 */
public class RobotAssemblerScreen extends AbstractContainerScreen<RobotAssemblerMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SearchBotMod.MOD_ID, "textures/gui/robot_assembler.png");

    // 按钮位置（相对界面左上角）：拆解在输入槽右侧，组装在核心槽右侧
    private static final int BUTTON_WIDTH = 36;
    private static final int BUTTON_HEIGHT = 18;
    private static final int DISMANTLE_X = 28;
    private static final int DISMANTLE_Y = 20;
    private static final int ASSEMBLE_X = 190;
    private static final int ASSEMBLE_Y = 20;

    public RobotAssemblerScreen(RobotAssemblerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = RobotAssemblerMenu.GUI_WIDTH;   // 256
        this.imageHeight = RobotAssemblerMenu.GUI_HEIGHT; // 230
    }

    @Override
    protected void init() {
        super.init();

        // 标题放在底部铭牌里（顶部留给「输入 / 核心组件 / 输出」三个分区标签）
        this.titleLabelX = 10;
        this.titleLabelY = 211;
        // 玩家背包标签：左侧空列，避开组件舱
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 90;
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);

        // 槽位顺序：0 输入 / 1 躯干 / 2 核心 / 3 输出，其后是功能组件与附件
        for (int i = 0; i < this.menu.slots.size(); i++) {
            Slot slot = this.menu.slots.get(i);
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

        // 区域标签（顶部三个放在标题条里，别压住下面的凹槽线）
        graphics.drawString(this.font, Component.literal("输入"), 8, 6, TechGuiTheme.LABEL_DIM, false);
        graphics.drawString(this.font, Component.literal("核心组件"), 102, 6, TechGuiTheme.LABEL_DIM, false);
        graphics.drawString(this.font, Component.literal("输出"), 222, 6, TechGuiTheme.LABEL_DIM, false);
        graphics.drawString(this.font, Component.literal("功能组件"), 66, 25, TechGuiTheme.LABEL_DIM, false);
        graphics.drawString(this.font, Component.literal("附件"), 8, 78, TechGuiTheme.LABEL_DIM, false);

        graphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, TechGuiTheme.LABEL_DIM, false);
    }
}
