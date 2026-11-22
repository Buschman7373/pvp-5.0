package dev.duelanalyzer.ui;

import dev.duelanalyzer.Store;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Shared glass look: gradient sky, dim, frosted panels, slide/fade opening animation, simple click regions. */
public abstract class GlassScreen extends Screen {
    private final long opened = System.currentTimeMillis();
    private final List<int[]> boxes = new ArrayList<>();
    private final List<Runnable> actions = new ArrayList<>();
    private int[] clip;

    protected float p = 1f;
    protected double clickX;
    protected int px, py, pw, ph;

    protected GlassScreen(String title) {
        super(Component.literal(title));
    }

    protected Theme theme() { return Theme.of(Store.cfg.theme); }
    protected int accent() { return theme().accent; }

    protected abstract void drawContent(GuiGraphics g, int mx, int my);

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        boxes.clear();
        actions.clear();
        clip = null;
        if (Store.cfg.anims) {
            float t = Math.min(1f, (System.currentTimeMillis() - opened) / 450f);
            p = 1f - (1f - t) * (1f - t) * (1f - t);
        } else {
            p = 1f;
        }
        Theme th = theme();

        pw = Math.min(width - 16, 580);
        ph = Math.min(height - 16, 310);
        px = (width - pw) / 2;
        py = (height - ph) / 2 + (int) ((1f - p) * 16);
        g.fill(px, py, px + pw, py + ph, fa(th.bg1, Store.cfg.opacity * 255 / 100));
        box(g, px, py, pw, ph, 12);
        g.renderOutline(px, py, pw, ph, fa(0xFFFFFF, 60));
        drawContent(g, mx, my);
        String made = "Made by Buschman48";
        textDim(g, made, px + pw / 2 - tw(made) / 2, py + ph - 12);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ---------- helpers ----------

    protected int fa(int rgb, int alpha) {
        int a = Math.max(5, Math.min(255, (int) (alpha * p)));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    protected void box(GuiGraphics g, int x, int y, int w, int h, int alpha) {
        g.fill(x, y, x + w, y + h, fa(0xFFFFFF, alpha));
        g.renderOutline(x, y, w, h, fa(0xFFFFFF, Math.min(255, alpha + 25)));
    }

    protected void text(GuiGraphics g, String s, int x, int y, int rgb) {
        g.drawString(font, s, x, y, fa(rgb, 255), true);
    }

    protected void textDim(GuiGraphics g, String s, int x, int y) {
        g.drawString(font, s, x, y, fa(0xFFFFFF, 150), true);
    }

    protected void textCenter(GuiGraphics g, String s, int cx, int y, int rgb) {
        text(g, s, cx - font.width(s) / 2, y, rgb);
    }

    protected int tw(String s) { return font.width(s); }

    /** Cuts a long string so it fits into maxW pixels. */
    protected String fit(String s, int maxW) {
        if (font.width(s) <= maxW) return s;
        while (s.length() > 1 && font.width(s + "...") > maxW) s = s.substring(0, s.length() - 1);
        return s + "...";
    }

    protected void chip(GuiGraphics g, String label, int x, int y, int w, boolean active, Runnable r) {
        box(g, x, y, w, 14, active ? 70 : 22);
        if (active) g.renderOutline(x, y, w, 14, fa(accent(), 220));
        textCenter(g, label, x + w / 2, y + 3, active ? accent() : 0xFFFFFF);
        hit(x, y, w, 14, r);
    }

    protected void bar(GuiGraphics g, int x, int y, int w, int h, double frac, int rgb) {
        g.fill(x, y, x + w, y + h, fa(0xFFFFFF, 28));
        int fw = (int) (w * Math.max(0, Math.min(1, frac)) * p);
        if (fw > 0) g.fill(x, y, x + fw, y + h, fa(rgb, 230));
    }

    protected void setClip(GuiGraphics g, int x, int y, int w, int h) {
        g.enableScissor(x, y, x + w, y + h);
        clip = new int[]{x, y, x + w, y + h};
    }

    protected void endClip(GuiGraphics g) {
        g.disableScissor();
        clip = null;
    }

    protected void hit(int x, int y, int w, int h, Runnable r) {
        if (clip != null) {
            int x1 = Math.max(x, clip[0]), y1 = Math.max(y, clip[1]);
            int x2 = Math.min(x + w, clip[2]), y2 = Math.min(y + h, clip[3]);
            if (x2 <= x1 || y2 <= y1) return;
            x = x1; y = y1; w = x2 - x1; h = y2 - y1;
        }
        boxes.add(new int[]{x, y, x + w, y + h});
        actions.add(r);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double mx = event.x(), my = event.y();
            clickX = mx;
            for (int i = boxes.size() - 1; i >= 0; i--) {
                int[] b = boxes.get(i);
                if (mx >= b[0] && mx < b[2] && my >= b[1] && my < b[3]) {
                    actions.get(i).run();
                    return true;
                }
            }
        }
        return super.mouseClicked(event, doubleClick);
    }
}
