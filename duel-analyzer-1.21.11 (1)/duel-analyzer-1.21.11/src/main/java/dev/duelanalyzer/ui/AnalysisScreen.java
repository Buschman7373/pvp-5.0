package dev.duelanalyzer.ui;

import dev.duelanalyzer.DuelRecord;
import dev.duelanalyzer.Mode;
import dev.duelanalyzer.Store;
import dev.duelanalyzer.Tips;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

import java.util.ArrayList;

/** Post-fight analysis: result, stats, timeline, key moments, tips and the "Set mode" prompt. */
public class AnalysisScreen extends GlassScreen {
    private final DuelRecord d;
    private final Screen parent;

    public AnalysisScreen(DuelRecord d, Screen parent) {
        super("Duel analysis");
        this.d = d;
        this.parent = parent;
        if (d.tips == null || d.tips.isEmpty()) {
            d.tips = new ArrayList<>(Tips.make(d));
            Store.save(d);
        }
    }

    private void back() {
        Minecraft.getInstance().setScreen(parent);
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my) {
        int x = px + 14, y = py + 12, w = pw - 28;
        int resCol = d.won() ? 0x6fe3a0 : d.lost() ? 0xff7b7b : 0xffd36a;
        String res = d.won() ? "WIN" : "LOSE";

        Mode m = d.modeEnum();
        Icons.draw(g, m, x, y - 2, 2, fa(m.rgb, 255));
        text(g, res, x + 30, y, resCol);
        textDim(g, "vs " + d.opponent + "  |  " + m.label + "  |  " + d.duration(), x + 30, y + 12);

        // back / close
        String bl = parent != null ? "Back" : "Close";
        chip(g, bl, px + pw - 58, py + 10, 46, false, this::back);

        // stat cards
        int cy = y + 32;
        int cw = (w - 15) / 4;
        String[][] cards = {
                {"Hit rate", (int) Math.round(d.hitRate() * 100) + "%"},
                {"Hits / swings", d.hits + " / " + d.swings},
                {"Best combo", String.valueOf(d.bestCombo)},
                {"Damage taken", String.format("%.0f", d.damageTaken)},
        };
        for (int i = 0; i < 4; i++) {
            int cx = x + i * (cw + 5);
            box(g, cx, cy, cw, 30, 22);
            textDim(g, cards[i][0], cx + 6, cy + 4);
            text(g, cards[i][1], cx + 6, cy + 16, 0xFFFFFF);
        }
        String[][] cards2 = {
                {"Weak swings", String.valueOf(d.weakHits)},
                {"Sprint-hits", String.valueOf(d.sprintHits)},
                {"Pearls / gapples", d.pearls + " / " + d.gapples},
                {"Potions / blocks", d.potions + " / " + d.blockedByOpp},
        };
        cy += 35;
        for (int i = 0; i < 4; i++) {
            int cx = x + i * (cw + 5);
            box(g, cx, cy, cw, 30, 22);
            textDim(g, cards2[i][0], cx + 6, cy + 4);
            text(g, cards2[i][1], cx + 6, cy + 16, 0xFFFFFF);
        }

        // timeline + key moments
        cy += 38;
        int half = (w - 8) / 2;
        box(g, x, cy, half, 86, 18);
        text(g, "Timeline", x + 6, cy + 5, accent());
        int total = Math.max(1, d.ticks);
        g.fill(x + 6, cy + 22, x + half - 6, cy + 24, fa(0xFFFFFF, 60));
        for (DuelRecord.Ev e : d.events) {
            int ex = x + 6 + (int) ((half - 12) * Math.min(1.0, e.t / (double) total));
            int col = e.s.contains("damage") ? 0xff7b7b : e.s.contains("combo") ? 0x6fe3a0 : 0xffffff;
            g.fill(ex - 1, cy + 18, ex + 1, cy + 28, fa(col, 255));
        }
        int ly = cy + 34;
        int shown = 0;
        for (int i = Math.max(0, d.events.size() - 4); i < d.events.size() && shown < 4; i++, shown++) {
            DuelRecord.Ev e = d.events.get(i);
            textDim(g, DuelRecord.clock(e.t) + "  " + fit(e.s, half - 50), x + 6, ly);
            ly += 11;
        }

        int tx = x + half + 8;
        box(g, tx, cy, half, 86, 18);
        text(g, "Tips for your next fight", tx + 6, cy + 5, accent());
        int ty = cy + 18;
        for (String tip : d.tips) {
            String s = fit(tip, half - 20);
            // wrap onto two lines when needed
            if (font.width(tip) > half - 20) {
                String a = tip;
                int cut = a.length();
                while (cut > 1 && font.width(a.substring(0, cut)) > half - 20) cut--;
                int sp = a.lastIndexOf(' ', cut);
                if (sp > 0) cut = sp;
                textLine(g, "- " + a.substring(0, cut), tx + 6, ty);
                ty += 10;
                textLine(g, "  " + fit(a.substring(cut).trim(), half - 20), tx + 6, ty);
            } else {
                textLine(g, "- " + s, tx + 6, ty);
            }
            ty += 12;
        }

        int by = cy + 92;
        if (by + 14 < py + ph - 14) {
            textDim(g, "Mode (detected from your inventory): " + m.label, x, by + 3);
        }
    }

    private void textLine(GuiGraphics g, String s, int x, int y) {
        text(g, s, x, y, 0xFFFFFF);
    }

    private void setMode(Mode m) {
        d.mode = m.name();
        d.modeConf = 1.0;
        d.modeConfirmed = true;
        Store.save(d);
        if (!d.server.isEmpty()) {
            Store.cfg.serverModes.put(d.server, m.name());
            Store.saveCfg();
        }
    }
}
