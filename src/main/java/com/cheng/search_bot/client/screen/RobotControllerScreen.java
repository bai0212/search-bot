package com.cheng.search_bot.client.screen;

import com.cheng.search_bot.SearchBotMod;
import com.cheng.search_bot.entity.custom.SearchRobotEntity;
import com.cheng.search_bot.item.custom.RobotControllerItem;
import com.cheng.search_bot.menu.custom.RobotControllerMenu;
import com.cheng.search_bot.network.ControllerActionPacket;
import com.cheng.search_bot.network.ControllerRobotPacket;
import com.cheng.search_bot.network.DeleteChannelsPacket;
import com.cheng.search_bot.network.ModNetworking;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 机器人操控台的频道管理 GUI（新版行式布局）。
 *
 * <p>面板底（外框、标题条、8 条频道行、行内凹槽、底部工具条）烘在
 * {@code robot_controller.png} 里；动态状态（当前频道、待删除虚线、
 * 机器人方块、拖拽残影）由代码叠加。行/按钮坐标沿用旧版，没有改动。
 *
 * <p>每一行是一个频道，从左到右依次为：
 * <ul>
 *   <li>频道名称（点击切换到当前频道）；</li>
 *   <li>模式按钮（显示模式前两个字，右键切下一个模式）；</li>
 *   <li>机器人方块（显示机器人名称，可拖拽：拖到某频道行=改绑定该频道，拖到行外=解绑）；</li>
 *   <li>红色 ×（点击标记删除，变撤回箭头；退出 GUI 后统一执行删除）。</li>
 * </ul>
 * 频道列表下方是 + 号，点击添加新频道。
 */
public class RobotControllerScreen extends AbstractContainerScreen<RobotControllerMenu> {
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(SearchBotMod.MOD_ID, "textures/gui/robot_controller.png");

    private static final int ROW_TOP = 24;
    private static final int ROW_HEIGHT = 24;
    private static final int BOTTOM_H = 28;

    // 行内横向布局
    private static final int CH_NAME_X = 8;
    private static final int MODE_X = 62;
    private static final int MODE_W = 40;
    private static final int MODE_H = 16;
    private static final int ROBOT_X = 108;
    private static final int ROBOT_W = 48;
    private static final int ROBOT_H = 18;
    private static final int ROBOT_GAP = 4;
    private static final int DEL_X = 224;
    private static final int DEL_W = 12;

    /** 待删除的频道索引（退出 GUI 后统一执行删除） */
    private final Set<Integer> pendingDelete = new HashSet<>();

    /** 拖拽中的机器人 */
    private UUID draggingRobotId = null;
    private double dragX = 0;
    private double dragY = 0;

    public RobotControllerScreen(RobotControllerMenu menu, Inventory inv, Component title) {
        super(menu, inv, title);
        this.imageWidth = 240;
        this.imageHeight = ROW_TOP + 8 * ROW_HEIGHT + BOTTOM_H; // 支持最多 8 个频道
    }

    @Override
    protected void init() {
        super.init();
        this.titleLabelX = 8;
        this.titleLabelY = 6;
    }

    // ==================== 读取操控台数据 ====================

    private ItemStack controller() {
        if (this.minecraft == null || this.minecraft.player == null) return ItemStack.EMPTY;
        return RobotControllerItem.findController(this.minecraft.player);
    }

    private int channelCount() {
        ItemStack c = controller();
        return c.isEmpty() ? 0 : RobotControllerItem.getChannelCount(c);
    }

    private int currentChannel() {
        ItemStack c = controller();
        return c.isEmpty() ? 0 : RobotControllerItem.getCurrentChannelIndex(c);
    }

    // ==================== 坐标计算 ====================

    private int channelAt(double mouseY) {
        int rel = (int) mouseY - topPos - ROW_TOP;
        if (rel < 0) return -1;
        int idx = rel / ROW_HEIGHT;
        if (idx >= channelCount()) return -1;
        return idx;
    }

    private int robotBlockX(int robotIndex) {
        return leftPos + ROBOT_X + robotIndex * (ROBOT_W + ROBOT_GAP);
    }

    private int robotBlockY(int channel) {
        return topPos + ROW_TOP + channel * ROW_HEIGHT + (ROW_HEIGHT - ROBOT_H) / 2;
    }

    private boolean inModeButton(double mx, double my, int channel) {
        int y = topPos + ROW_TOP + channel * ROW_HEIGHT + (ROW_HEIGHT - MODE_H) / 2;
        return mx >= leftPos + MODE_X && mx < leftPos + MODE_X + MODE_W
                && my >= y && my < y + MODE_H;
    }

    private boolean inDeleteButton(double mx, double my, int channel) {
        int y = topPos + ROW_TOP + channel * ROW_HEIGHT + (ROW_HEIGHT - DEL_W) / 2;
        return mx >= leftPos + DEL_X && mx < leftPos + DEL_X + DEL_W
                && my >= y && my < y + DEL_W;
    }

    private boolean inAddButton(double mx, double my) {
        int y = topPos + ROW_TOP + channelCount() * ROW_HEIGHT + 4;
        int x = leftPos + imageWidth / 2 - 8;
        return mx >= x && mx < x + 16 && my >= y && my < y + 16;
    }

    // ==================== 网络发送 ====================

    private void send(int action, int value, String text) {
        ModNetworking.CHANNEL.sendToServer(new ControllerActionPacket(action, value, text));
    }

    private void sendRobotMove(UUID id, int channel) {
        ModNetworking.CHANNEL.sendToServer(new ControllerRobotPacket(ControllerRobotPacket.MOVE, id, channel));
    }

    private void sendRobotUnbind(UUID id) {
        ModNetworking.CHANNEL.sendToServer(new ControllerRobotPacket(ControllerRobotPacket.UNBIND, id, 0));
    }

    private void togglePendingDelete(int channel) {
        if (channel < 0 || channel >= channelCount()) return;
        if (!pendingDelete.remove(channel)) {
            pendingDelete.add(channel);
        }
    }

    // ==================== 渲染 ====================

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1, 1, 1, 1);
        graphics.blit(GUI_TEXTURE, this.leftPos, this.topPos, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, delta);

        ItemStack c = controller();
        int count = channelCount();
        int current = currentChannel();

        for (int i = 0; i < count; i++) {
            int rowY = topPos + ROW_TOP + i * ROW_HEIGHT;

            // 当前频道：整行铺一层半透明荧光蓝
            if (i == current) {
                graphics.fill(leftPos + 3, rowY + 1, leftPos + imageWidth - 3, rowY + ROW_HEIGHT - 1, TechGuiTheme.ACCENT_SELECT);
                graphics.fill(leftPos + 3, rowY + 1, leftPos + 4, rowY + ROW_HEIGHT - 1, TechGuiTheme.ACCENT);
            }

            // 待删除频道：虚线框
            if (pendingDelete.contains(i)) {
                drawDashedRect(graphics, leftPos + 2, rowY, imageWidth - 4, ROW_HEIGHT - 2, TechGuiTheme.DANGER);
            }

            // 拖拽机器人时：鼠标所在行作为放置目标高亮
            if (draggingRobotId != null && channelAt(mouseY) == i) {
                graphics.fill(leftPos + 2, rowY, leftPos + imageWidth - 2, rowY + 1, TechGuiTheme.ACCENT);
                graphics.fill(leftPos + 2, rowY + ROW_HEIGHT - 2, leftPos + imageWidth - 2, rowY + ROW_HEIGHT - 1, TechGuiTheme.ACCENT);
            }

            // 频道名称（当前频道高亮）
            String name = RobotControllerItem.getChannelName(c, i);
            int nameColor = (i == current) ? TechGuiTheme.ACCENT : TechGuiTheme.LABEL;
            graphics.drawString(this.font, name, leftPos + CH_NAME_X, rowY + 8, nameColor, false);

            // 模式按钮（显示模式前两个字）
            int modeId = RobotControllerItem.getChannelMode(c, i);
            String modeName = SearchRobotEntity.RobotMode.byId(modeId).cnName();
            if (modeName.length() > 2) modeName = modeName.substring(0, 2);
            int my = rowY + (ROW_HEIGHT - MODE_H) / 2;
            boolean modeHover = inModeButton(mouseX, mouseY, i);
            graphics.fill(leftPos + MODE_X, my, leftPos + MODE_X + MODE_W, my + MODE_H,
                    modeHover ? TechGuiTheme.BG_LIGHT : TechGuiTheme.BG_DARK);
            graphics.fill(leftPos + MODE_X, my, leftPos + MODE_X + MODE_W, my + 1,
                    modeHover ? TechGuiTheme.ACCENT : TechGuiTheme.ACCENT_DIM);
            graphics.drawString(this.font, modeName,
                    leftPos + MODE_X + (MODE_W - this.font.width(modeName)) / 2, my + 4,
                    modeHover ? TechGuiTheme.ACCENT : TechGuiTheme.LABEL, false);

            // 机器人方块（最多显示 2 个，超出显示 +N）
            List<RobotControllerItem.RobotEntry> robots = RobotControllerItem.getChannelRobots(c, i);
            int shown = Math.min(robots.size(), 2);
            for (int ri = 0; ri < shown; ri++) {
                RobotControllerItem.RobotEntry e = robots.get(ri);
                boolean dragging = draggingRobotId != null && draggingRobotId.equals(e.id());
                int bx = robotBlockX(ri);
                int by = robotBlockY(i);
                graphics.fill(bx, by, bx + ROBOT_W, by + ROBOT_H,
                        dragging ? TechGuiTheme.ACCENT_DIM : TechGuiTheme.BG_DARK);
                graphics.fill(bx, by, bx + ROBOT_W, by + 1,
                        dragging ? TechGuiTheme.ACCENT : TechGuiTheme.BEVEL_HI);
                String robotName = e.name() == null || e.name().isEmpty() ? "机器人" : e.name();
                // 名字过长截断
                if (this.font.width(robotName) > ROBOT_W - 4) {
                    robotName = this.font.plainSubstrByWidth(robotName, ROBOT_W - 8) + "…";
                }
                graphics.drawString(this.font, robotName, bx + 2, by + 5, TechGuiTheme.LABEL, false);
            }
            if (robots.size() > shown) {
                int bx = robotBlockX(shown);
                graphics.drawString(this.font, "+" + (robots.size() - shown), bx, rowY + 8,
                        TechGuiTheme.LABEL_DIM, false);
            }

            // 删除按钮：待删显示撤回箭头，否则红色 ×
            int dy = rowY + (ROW_HEIGHT - DEL_W) / 2;
            if (pendingDelete.contains(i)) {
                graphics.drawString(this.font, "↺", leftPos + DEL_X - 1, dy + 1, TechGuiTheme.OK, false);
            } else {
                graphics.drawString(this.font, "✕", leftPos + DEL_X + 1, dy + 1,
                        inDeleteButton(mouseX, mouseY, i) ? TechGuiTheme.DANGER_HI : TechGuiTheme.DANGER, false);
            }
        }

        // + 号（最后一行频道下方）
        int addY = topPos + ROW_TOP + count * ROW_HEIGHT + 4;
        int addX = leftPos + imageWidth / 2 - 8;
        boolean addHover = inAddButton(mouseX, mouseY);
        graphics.fill(addX, addY, addX + 16, addY + 16, addHover ? TechGuiTheme.OK_HI : TechGuiTheme.BG_DARK);
        graphics.fill(addX, addY, addX + 16, addY + 1, addHover ? TechGuiTheme.ACCENT : TechGuiTheme.ACCENT_DIM);
        graphics.fill(addX, addY, addX + 1, addY + 16, addHover ? TechGuiTheme.ACCENT : TechGuiTheme.ACCENT_DIM);
        graphics.drawString(this.font, "+", addX + 5, addY + 4, TechGuiTheme.LABEL, false);

        // 拖拽中：在鼠标位置绘制被拖拽机器人名字
        if (draggingRobotId != null) {
            String dragName = "机器人";
            for (int i = 0; i < count; i++) {
                for (RobotControllerItem.RobotEntry e : RobotControllerItem.getChannelRobots(c, i)) {
                    if (e.id().equals(draggingRobotId)) {
                        dragName = (e.name() == null || e.name().isEmpty()) ? "机器人" : e.name();
                    }
                }
            }
            int w = this.font.width(dragName) + 6;
            graphics.fill((int) dragX, (int) dragY, (int) dragX + w, (int) dragY + 12, TechGuiTheme.DRAG_BG);
            graphics.fill((int) dragX, (int) dragY, (int) dragX + w, (int) dragY + 1, TechGuiTheme.ACCENT);
            graphics.drawString(this.font, dragName, (int) dragX + 3, (int) dragY + 3, TechGuiTheme.ACCENT, false);
        }

        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {
        graphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, TechGuiTheme.LABEL, false);
    }

    /** 画一个虚线矩形边框。 */
    private void drawDashedRect(GuiGraphics g, int x, int y, int w, int h, int color) {
        for (int i = 0; i < w; i += 4) {
            g.fill(x + i, y, Math.min(x + i + 2, x + w), y + 1, color);
            g.fill(x + i, y + h - 1, Math.min(x + i + 2, x + w), y + h, color);
        }
        for (int i = 0; i < h; i += 4) {
            g.fill(x, y + i, x + 1, Math.min(y + i + 2, y + h), color);
            g.fill(x + w - 1, y + i, x + w, Math.min(y + i + 2, y + h), color);
        }
    }

    // ==================== 交互 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        ItemStack c = controller();
        int count = channelCount();

        if (button == 1) {
            // 右键模式按钮 → 切下一个模式
            int ch = channelAt(mouseY);
            if (ch >= 0 && inModeButton(mouseX, mouseY, ch)) {
                send(ControllerActionPacket.CYCLE_MODE_CHANNEL, ch, null);
                return true;
            }
        }

        if (button == 0) {
            int ch = channelAt(mouseY);

            // × 按钮 → 标记/取消删除
            if (ch >= 0 && inDeleteButton(mouseX, mouseY, ch)) {
                togglePendingDelete(ch);
                return true;
            }

            // + 号 → 添加频道
            if (inAddButton(mouseX, mouseY)) {
                send(ControllerActionPacket.ADD_CHANNEL, 0, null);
                return true;
            }

            // 机器人方块 → 开始拖拽
            for (int i = 0; i < count; i++) {
                List<RobotControllerItem.RobotEntry> robots = RobotControllerItem.getChannelRobots(c, i);
                int shown = Math.min(robots.size(), 2);
                for (int ri = 0; ri < shown; ri++) {
                    int bx = robotBlockX(ri);
                    int by = robotBlockY(i);
                    if (mouseX >= bx && mouseX < bx + ROBOT_W && mouseY >= by && mouseY < by + ROBOT_H) {
                        draggingRobotId = robots.get(ri).id();
                        dragX = mouseX;
                        dragY = mouseY;
                        return true;
                    }
                }
            }

            // 频道名称 → 切换到当前频道
            if (ch >= 0 && mouseX >= leftPos + CH_NAME_X && mouseX < leftPos + MODE_X) {
                send(ControllerActionPacket.SET_CURRENT, ch, null);
                return true;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && draggingRobotId != null) {
            int target = channelAt(mouseY);
            boolean inside = mouseX >= leftPos && mouseX < leftPos + imageWidth && target >= 0;
            if (inside) {
                sendRobotMove(draggingRobotId, target);
            } else {
                sendRobotUnbind(draggingRobotId);
            }
            draggingRobotId = null;
            return true;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (draggingRobotId != null) {
            this.dragX = mouseX;
            this.dragY = mouseY;
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public void onClose() {
        // 退出 GUI 时执行待删除的频道
        if (!pendingDelete.isEmpty()) {
            ModNetworking.CHANNEL.sendToServer(new DeleteChannelsPacket(new ArrayList<>(pendingDelete)));
        }
        super.onClose();
    }
}
