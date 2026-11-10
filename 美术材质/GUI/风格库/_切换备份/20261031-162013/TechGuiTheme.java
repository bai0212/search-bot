package com.cheng.search_bot.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * 原版箱子风主题（留档参考，未接入 src）。
 *
 * <p>方法签名与 {@code TechGuiTheme} 完全一致，可以直接改名替换：
 * 三个 Screen 里只需要把类名换掉（或把这个文件改名成 TechGuiTheme.java 覆盖），
 * 坐标一个都不用动。要切回科技风，用
 * {@code 美术材质/GUI/风格库/apply_gui_style.py 科技风}（贴图）
 * + {@code 风格库/科技风/TechGuiTheme.java}（代码）。
 *
 * <p>配色取自原版 container/*.png：面板 #C6C6C6、槽底 #8B8B8B、
 * 凹槽上/左 #373737、凹槽下/右 #FFFFFF、描边 #000000、文字 #404040。
 * 凹格仍然由代码画（不烘进贴图），因为机器人背包的行数会变。
 */
final class TechGuiTheme {
    // ==== 与 TechGuiTheme 同名同位，值换成原版 ====
    static final int BORDER = 0xFF000000;
    static final int BEVEL_HI = 0xFFFFFFFF;
    static final int BEVEL_LO = 0xFF5F5F5F;
    static final int BG = 0xFFC6C6C6;
    static final int BG_LIGHT = 0xFFE0E0E0;
    static final int BG_DARK = 0xFFC6C6C6;
    static final int SLOT_FILL = 0xFF8B8B8B;
    static final int SLOT_DK = 0xFF373737;
    static final int SLOT_LT = 0xFFFFFFFF;
    static final int ACCENT = 0xFF404040;
    static final int ACCENT_DIM = 0xFF000000;
    static final int ACCENT_MUTE = 0xFFA0A0A0;
    static final int LABEL = 0xFF404040;
    static final int LABEL_DIM = 0xFF5F5F5F;
    static final int DANGER = 0xFFA02020;
    static final int OK = 0xFF207030;

    private TechGuiTheme() {
    }

    /** 原版 18x18 凹格：外圈留 1px 面板色，内侧 16x16 灰底，上左压暗、下右提亮。 */
    static void slot(GuiGraphics g, int x, int y) {
        g.fill(x + 1, y + 1, x + 17, y + 17, SLOT_FILL);
        g.fill(x + 1, y + 1, x + 17, y + 2, SLOT_DK);
        g.fill(x + 1, y + 1, x + 2, y + 17, SLOT_DK);
        g.fill(x + 1, y + 16, x + 17, y + 17, SLOT_LT);
        g.fill(x + 16, y + 1, x + 17, y + 17, SLOT_LT);
    }

    /**
     * 原版不区分槽位用途（所有凹格长得一样），所以带颜色的重载只是画同一个凹格。
     * 想保留「输入/输出/核心」的视觉差别，就只在这里加描边，不要改坐标。
     */
    static void slot(GuiGraphics g, int x, int y, int border, int fill) {
        slot(g, x, y);
    }

    /** 原版按钮：灰底 + 1px 黑描边 + 上左白高光 + 下右灰暗，hover 只提亮底色。 */
    static void button(GuiGraphics g, Font font, int x, int y, int w, int h,
                       String label, int textColor, boolean hover) {
        g.fill(x, y, x + w, y + h, hover ? BG_LIGHT : BG_DARK);
        g.fill(x, y, x + w, y + 1, BORDER);
        g.fill(x, y + h - 1, x + w, y + h, BORDER);
        g.fill(x, y, x + 1, y + h, BORDER);
        g.fill(x + w - 1, y, x + w, y + h, BORDER);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, BEVEL_HI);
        g.fill(x + 1, y + 1, x + 2, y + h - 1, BEVEL_HI);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, BEVEL_LO);
        g.fill(x + w - 2, y + 1, x + w - 1, y + h - 1, BEVEL_LO);
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

    /** 动态高度面板的外框：原版就是一条 1px 黑边，没有倒角。 */
    static void panelFrame(GuiGraphics g, int left, int top, int w, int h) {
        g.fill(left, top, left + w, top + 1, BORDER);
        g.fill(left, top + h - 1, left + w, top + h, BORDER);
        g.fill(left, top, left + 1, top + h, BORDER);
        g.fill(left + w - 1, top, left + w, top + h, BORDER);
    }
}
