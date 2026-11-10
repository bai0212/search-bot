package com.cheng.search_bot.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * 三个 GUI 共用的「精妙背包风」配色与画法。
 *
 * <p>借鉴精妙背包（Sophisticated Backpacks）的观感：深炭灰面板 + 平坦凹格（无 3D 斜面）
 * + 暖金描边，整体更现代、更干净。颜色与 {@code 美术材质/GUI/风格库/精妙背包风/gen_gui_textures_backpack.py}
 * 里的调色板一一对应：贴图负责静态面板，这里负责动态部分（槽位、按钮、高亮）。
 *
 * <p>类名和方法签名与「原版风」「科技风」完全一致，坐标不涉及，所以三个 Screen 一行都不用改。
 * 凹格仍由代码画（机器人背包的行数会变，不烘进贴图）。
 */
final class TechGuiTheme {
    // ==== 深炭灰 + 暖金 ====
    static final int BORDER = 0xFF0A0A0A;
    static final int BEVEL_HI = 0xFF4A4A4A;
    static final int BEVEL_LO = 0xFF161616;
    static final int BG = 0xFF2B2B2B;
    static final int BG_LIGHT = 0xFF3A3A3A;
    static final int BG_DARK = 0xFF1F1F1F;
    static final int SLOT_FILL = 0xFF1A1A1A;
    static final int SLOT_DK = 0xFF0E0E0E;
    static final int SLOT_LT = 0xFF3A3A3A;
    static final int ACCENT = 0xFFE0B34C;      // 暖金高亮
    static final int ACCENT_DIM = 0xFF6B5A2E;  // 暗金描边
    static final int ACCENT_MUTE = 0xFF3A3320; // 更暗金底
    static final int LABEL = 0xFFD8D8D8;       // 浅灰文字
    static final int LABEL_DIM = 0xFF9A9A9A;   // 暗灰文字
    static final int DANGER = 0xFFE05A5A;
    static final int OK = 0xFF6BC46E;
    static final int DANGER_HI = 0xFFFF8A8A;   // 危险色 hover 提亮
    static final int OK_HI = 0xFF8AD48E;       // 成功色 hover 提亮
    static final int ACCENT_SELECT = 0x30E0B34C; // 当前选中行半透明金
    static final int DRAG_BG = 0xE01A1A1A;     // 拖拽残影底
    static final int ENERGY_MID = 0xFFE0B34C;  // 电量中段金

    private TechGuiTheme() {
    }

    /**
     * 平坦 18x18 凹格（精妙背包那种没斜面的凹格）。参数 (x, y) 是物品图标的左上角
     * （即 {@link net.minecraft.world.inventory.Slot#x/y}），所以 16x16 深底正好垫在物品下面，
     * 1px 细描边再画在四周（无上亮下暗的斜面，四面同色，更平）。
     */
    static void slot(GuiGraphics g, int x, int y) {
        slot(g, x, y, ACCENT_MUTE, SLOT_FILL);
    }

    /** 自定义边框/内底的槽位（用于区分输入/输出/核心等不同用途的槽位）。 */
    static void slot(GuiGraphics g, int x, int y, int border, int fill) {
        g.fill(x, y, x + 16, y + 16, fill);
        g.fill(x - 1, y - 1, x + 17, y, border);        // 上
        g.fill(x - 1, y - 1, x, y + 17, border);        // 左
        g.fill(x - 1, y + 16, x + 17, y + 17, border);  // 下
        g.fill(x + 16, y - 1, x + 17, y + 17, border);  // 右
    }

    /** 主题按钮：深色底 + 暖金描边，hover 变亮。 */
    static void button(GuiGraphics g, Font font, int x, int y, int w, int h,
                       String label, int textColor, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? BG_LIGHT : BG_DARK);
        g.fill(x, y, x + w, y + 1, hover ? ACCENT : ACCENT_DIM);
        g.fill(x, y + h - 1, x + w, y + h, hover ? ACCENT : ACCENT_DIM);
        g.fill(x, y, x + 1, y + h, hover ? ACCENT : ACCENT_DIM);
        g.fill(x + w - 1, y, x + w, y + h, hover ? ACCENT : ACCENT_DIM);
        drawCentered(g, font, label, x, y, w, h, hover ? LABEL : textColor);
    }

    /** 居中文字。 */
    static void drawCentered(GuiGraphics g, Font font, String label, int x, int y, int w, int h, int color) {
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 9) / 2 + 1, color, false);
    }

    /** 鼠标是否落在（相对界面左上角的）矩形内。 */
    static boolean hit(double mx, double my, int left, int top, int x, int y, int w, int h) {
        return mx >= left + x && mx < left + x + w && my >= top + y && my < top + y + h;
    }

    /** 动态高度面板的外框：深底 + 上左亮、下右暗的 1px 内衬，比原版多一点点层次。 */
    static void panelFrame(GuiGraphics g, int left, int top, int w, int h) {
        g.fill(left, top, left + w, top + h, BORDER);
        g.fill(left + 1, top + 1, left + w - 1, top + 2, BEVEL_HI);
        g.fill(left + 1, top + 1, left + 2, top + h - 1, BEVEL_HI);
        g.fill(left + 1, top + h - 2, left + w - 1, top + h - 1, BEVEL_LO);
        g.fill(left + w - 2, top + 1, left + w - 1, top + h - 1, BEVEL_LO);
    }
}
