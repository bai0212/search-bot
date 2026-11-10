package com.cheng.search_bot.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * 三个 GUI 共用的「科技银 + 荧光蓝」配色与画法。
 *
 * <p>颜色与 {@code 美术材质/GUI/gen_gui_textures.py} 里的调色板一一对应，
 * 贴图负责静态面板，这里负责动态部分（槽位、按钮、高亮）。
 * 改配色时两边要一起改。
 */
final class TechGuiTheme {
    static final int BORDER = 0xFF080B10;
    static final int BEVEL_HI = 0xFF5E728C;
    static final int BEVEL_LO = 0xFF0E1219;
    static final int BG = 0xFF262F3C;
    static final int BG_LIGHT = 0xFF323E4E;
    static final int BG_DARK = 0xFF181F29;
    static final int SLOT_FILL = 0xFF121821;
    static final int ACCENT = 0xFF60C4EE;
    static final int ACCENT_DIM = 0xFF346278;
    static final int ACCENT_MUTE = 0xFF1E3644;
    static final int LABEL = 0xFFC6D3DF;
    static final int LABEL_DIM = 0xFF8A99AB;
    static final int DANGER = 0xFFFF6B6B;
    static final int OK = 0xFF6BE08A;

    private TechGuiTheme() {
    }

    /** 一个 18x18 的凹陷物品槽位。 */
    static void slot(GuiGraphics g, int x, int y) {
        slot(g, x, y, ACCENT_MUTE, SLOT_FILL);
    }

    /** 自定义边框/内底的槽位（用于区分不同用途的槽位）。 */
    static void slot(GuiGraphics g, int x, int y, int border, int fill) {
        g.fill(x, y, x + 18, y + 18, border);
        g.fill(x + 1, y + 1, x + 17, y + 17, fill);
        g.fill(x + 1, y + 1, x + 17, y + 2, BEVEL_LO);
        g.fill(x + 1, y + 1, x + 2, y + 17, BEVEL_LO);
    }

    /** 一个主题按钮：深色底 + 顶边高光 + 荧光蓝描边，hover 变亮。 */
    static void button(GuiGraphics g, Font font, int x, int y, int w, int h,
                       String label, int textColor, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? BG_LIGHT : BG_DARK);
        g.fill(x, y, x + w, y + 1, hover ? BEVEL_HI : ACCENT_DIM);
        g.fill(x, y + h - 1, x + w, y + h, BEVEL_LO);
        g.fill(x, y, x + 1, y + h, hover ? BEVEL_HI : ACCENT_DIM);
        g.fill(x + w - 1, y, x + w, y + h, BEVEL_LO);
        drawCentered(g, font, label, x, y, w, h, textColor);
    }

    /** 居中文字。 */
    static void drawCentered(GuiGraphics g, Font font, String label, int x, int y, int w, int h, int color) {
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 9) / 2 + 1, color, false);
    }

    /** 鼠标是否落在（相对界面左上角的）矩形内。 */
    static boolean hit(double mx, double my, int left, int top, int x, int y, int w, int h) {
        return mx >= left + x && mx < left + x + w && my >= top + y && my < top + y + h;
    }

    /** 动态高度面板的外框（贴图只烘固定部件时由代码补齐）。 */
    static void panelFrame(GuiGraphics g, int left, int top, int w, int h) {
        g.fill(left, top, left + w, top + h, BORDER);
        g.fill(left + 1, top + 1, left + w - 1, top + 2, BEVEL_HI);
        g.fill(left + 1, top + 1, left + 2, top + h - 1, BEVEL_HI);
        g.fill(left + 1, top + h - 2, left + w - 1, top + h - 1, BEVEL_LO);
        g.fill(left + w - 2, top + 1, left + w - 1, top + h - 1, BEVEL_LO);
    }

    /** 四角荧光蓝短角标。 */
    static void brackets(GuiGraphics g, int x, int y, int w, int h, int color) {
        g.fill(x, y, x + 3, y + 1, color);
        g.fill(x, y, x + 1, y + 3, color);
        g.fill(x + w - 3, y, x + w, y + 1, color);
        g.fill(x + w - 1, y, x + w, y + 3, color);
        g.fill(x, y + h - 1, x + 3, y + h, color);
        g.fill(x, y + h - 3, x + 1, y + h, color);
        g.fill(x + w - 3, y + h - 1, x + w, y + h, color);
        g.fill(x + w - 1, y + h - 3, x + w, y + h, color);
    }
}
