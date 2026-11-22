package dev.duelanalyzer.ui;

import dev.duelanalyzer.DuelAnalyzerClient;
import dev.duelanalyzer.DuelRecord;
import dev.duelanalyzer.Mode;
import dev.duelanalyzer.Stats;
import dev.duelanalyzer.Store;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Main menu: glass dashboard with a left sidebar and eleven sections. */
public class DashboardScreen extends GlassScreen {
    private static final String[] SECS = {"Overview", "Trends", "All duels", "By mode", "Opponents",
            "Weaknesses", "Habits", "Records", "Goals", "Settings", "Information"};
    private static final String[] TIERS = {"Bronze", "Silver", "Gold", "Diamond"};
    private static final int[] TIER_COL = {0xcd8a4a, 0xc9d3dd, 0xffd36a, 0x6fe8ff};

    private int sec = 0;
    private Mode modeSel = null;   // null = All modes (used by Weaknesses/Habits/Records/Goals)
    private Mode detail = null;    // mode detail view in "By mode"
    private int sort = 0;          // 0 win rate, 1 fights, 2 A-Z
    private int scroll = 0;

    public DashboardScreen() {
        super("Duel Analyzer");
    }

    private void go(int s) {
        sec = s;
        scroll = 0;
        detail = null;
    }

    @Override
    protected void drawContent(GuiGraphics g, int mx, int my) {
        int sw = 108;
        text(g, "Duel Analyzer", px + 12, py + 10, accent());
        g.fill(px + sw, py + 8, px + sw + 1, py + ph - 8, fa(0xFFFFFF, 40));
        int iy = py + 28;
        int rowH = Math.min(17, (ph - 46) / SECS.length);
        for (int i = 0; i < SECS.length; i++) {
            final int idx = i;
            boolean on = sec == i;
            if (on) {
                g.fill(px + 6, iy, px + sw - 6, iy + rowH - 1, fa(accent(), 60));
                g.fill(px + 6, iy, px + 8, iy + rowH - 1, fa(accent(), 255));
            }
            text(g, SECS[i], px + 14, iy + (rowH - 8) / 2, on ? accent() : 0xFFFFFF);
            hit(px + 6, iy, sw - 12, rowH - 1, () -> go(idx));
            iy += rowH;
        }

        int cx = px + sw + 12, cy = py + 10, cw = pw - sw - 22, ch = ph - 32;
        text(g, SECS[sec], cx, cy, 0xFFFFFF);
        cy += 16;
        ch -= 16;
        switch (sec) {
            case 0 -> overview(g, cx, cy, cw, ch);
            case 1 -> trends(g, cx, cy, cw, ch);
            case 2 -> allDuels(g, cx, cy, cw, ch);
            case 3 -> byMode(g, cx, cy, cw, ch);
            case 4 -> opponents(g, cx, cy, cw, ch);
            case 5, 6, 7, 8 -> {
                modeBar(g, cx, cy, cw);
                cy += 20;
                ch -= 20;
                if (sec == 5) weaknesses(g, cx, cy, cw, ch);
                else if (sec == 6) habits(g, cx, cy, cw, ch);
                else if (sec == 7) records(g, cx, cy, cw, ch);
                else goals(g, cx, cy, cw, ch);
            }
            case 9 -> settings(g, cx, cy, cw, ch);
            default -> information(g, cx, cy, cw, ch);
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) (sy * 14));
        return true;
    }

    // ---------- shared pieces ----------

    private void empty(GuiGraphics g, int x, int y, String msg) {
        textDim(g, msg, x, y + 4);
    }

    private void open(DuelRecord d) {
        Minecraft.getInstance().setScreen(new AnalysisScreen(d, this));
    }

    private void modeBar(GuiGraphics g, int x, int y, int w) {
        int cnt = Store.forMode(modeSel).size();
        chip(g, "<", x, y, 16, false, () -> cycle(-1));
        String label = modeSel == null ? "All modes" : modeSel.label;
        int lw = 150;
        box(g, x + 19, y, lw, 14, 22);
        if (modeSel != null) Icons.draw(g, modeSel, x + 23, y + 1, 1, fa(modeSel.rgb, 255));
        text(g, label + "  (" + cnt + " fights)", x + 38, y + 3, modeSel == null ? 0xFFFFFF : modeSel.rgb);
        chip(g, ">", x + 22 + lw, y, 16, false, () -> cycle(1));
    }

    private void cycle(int dir) {
        Mode[] all = Mode.values();
        int i = modeSel == null ? -1 : modeSel.ordinal();
        i += dir;
        if (i < -1) i = all.length - 1;
        if (i >= all.length) i = -1;
        modeSel = i < 0 ? null : all[i];
        scroll = 0;
    }

    private void card(GuiGraphics g, int x, int y, int w, String title, String body, int rgb) {
        box(g, x, y, w, 34, 20);
        g.fill(x, y, x + 2, y + 34, fa(rgb, 255));
        text(g, fit(title, w - 14), x + 8, y + 5, rgb);
        textDim(g, fit(body, w - 14), x + 8, y + 18);
    }

    private void cards(GuiGraphics g, List<String[]> items, int x, int y, int w, int h, int rgb, String emptyMsg) {
        if (items.isEmpty()) {
            empty(g, x, y, emptyMsg);
            return;
        }
        int total = items.size() * 38;
        scroll = Math.min(scroll, Math.max(0, total - h));
        setClip(g, x, y, w, h);
        int yy = y - scroll;
        for (String[] it : items) {
            card(g, x, yy, w, it[0], it[1], rgb);
            yy += 38;
        }
        endClip(g);
    }

    // ---------- sections ----------

    private void overview(GuiGraphics g, int x, int y, int w, int h) {
        List<DuelRecord> all = Store.duels;
        Stats s = Stats.of(all);
        if (all.isEmpty()) {
            empty(g, x, y, "No duels yet. Hit a player and the analysis opens after the fight.");
            return;
        }
        int heroW = w * 45 / 100;
        box(g, x, y, heroW, 70, 24);
        textDim(g, "Win rate", x + 8, y + 6);
        int shown = (int) Math.round(s.winRate() * 100 * p);
        text(g, shown + "%", x + 8, y + 20, accent());
        textDim(g, s.fights + " fights  |  " + s.wins + "W " + s.losses + "L", x + 8, y + 36);
        bar(g, x + 8, y + 52, heroW - 16, 5, s.winRate(), accent());

        int qx = x + heroW + 6, qw = (w - heroW - 12) / 2;
        String[][] q = {
                {"K/D", String.format(Locale.ROOT, "%.1f", s.kd())},
                {"Avg length", DuelRecord.clock((int) s.avgTicks)},
                {"Hit rate", (int) Math.round(s.hitRate * 100) + "%"},
                {"Win streak", String.valueOf(s.curStreak)},
        };
        for (int i = 0; i < 4; i++) {
            int bx = qx + (i % 2) * (qw + 6), by = y + (i / 2) * 36;
            box(g, bx, by, qw, 32, 20);
            textDim(g, q[i][0], bx + 6, by + 4);
            text(g, q[i][1], bx + 6, by + 17, 0xFFFFFF);
        }

        DuelRecord last = all.get(0);
        int ly = y + 78;
        box(g, x, ly, w, 28, 24);
        Icons.draw(g, last.modeEnum(), x + 6, ly + 4, 1, fa(last.modeEnum().rgb, 255));
        text(g, last.won() ? "Win" : "Lose", x + 24, ly + 5, last.won() ? 0x6fe3a0 : last.lost() ? 0xff7b7b : 0xffd36a);
        textDim(g, "Last duel: " + last.opponent + "  " + last.duration(), x + 24, ly + 16);
        String key = "Reopen (" + DuelAnalyzerClient.lastKey.getTranslatedKeyMessage().getString() + ")";
        chip(g, key, x + w - tw(key) - 16, ly + 7, tw(key) + 10, false, () -> open(last));
        hit(x, ly, w - tw(key) - 20, 28, () -> open(last));
    }

    private void trends(GuiGraphics g, int x, int y, int w, int h) {
        List<DuelRecord> l = Stats.last(Store.duels, 20);
        if (l.isEmpty()) {
            empty(g, x, y, "Nothing to show yet.");
            return;
        }
        textDim(g, "Hit rate of your last " + l.size() + " duels (green = win, red = lose)", x, y);
        int bh = Math.min(120, h - 40);
        int bw = Math.max(6, Math.min(22, (w - 4) / l.size() - 3));
        g.fill(x, y + 14 + bh, x + w, y + 15 + bh, fa(0xFFFFFF, 60));
        for (int i = 0; i < l.size(); i++) {
            DuelRecord d = l.get(l.size() - 1 - i);
            int hh = (int) (bh * d.hitRate() * p);
            int bx = x + i * (bw + 3);
            int col = d.won() ? 0x6fe3a0 : d.lost() ? 0xff7b7b : 0xffd36a;
            g.fill(bx, y + 14 + bh - hh, bx + bw, y + 14 + bh, fa(col, 220));
            final DuelRecord dd = d;
            hit(bx, y + 14, bw, bh, () -> open(dd));
        }
    }

    private void allDuels(GuiGraphics g, int x, int y, int w, int h) {
        List<DuelRecord> l = Store.duels;
        if (l.isEmpty()) {
            empty(g, x, y, "No duels recorded yet.");
            return;
        }
        int total = l.size() * 26;
        scroll = Math.min(scroll, Math.max(0, total - h));
        setClip(g, x, y, w, h);
        int yy = y - scroll;
        for (DuelRecord d : l) {
            final DuelRecord dd = d;
            box(g, x, yy, w, 23, 20);
            int col = d.won() ? 0x6fe3a0 : d.lost() ? 0xff7b7b : 0xffd36a;
            g.fill(x, yy, x + 2, yy + 23, fa(col, 255));
            Icons.draw(g, d.modeEnum(), x + 7, yy + 5, 1, fa(d.modeEnum().rgb, 255));
            text(g, fit(d.opponent, 110), x + 24, yy + 3, 0xFFFFFF);
            textDim(g, d.modeEnum().label + (d.modeConfirmed ? "" : " (?)") + "  |  " + d.duration(), x + 24, yy + 13);
            String r = d.won() ? "WIN" : "LOSE";
            text(g, r, x + w - tw(r) - 8, yy + 8, col);
            hit(x, yy, w, 23, () -> open(dd));
            yy += 26;
        }
        endClip(g);
    }

    private void byMode(GuiGraphics g, int x, int y, int w, int h) {
        if (detail != null) {
            modeDetail(g, x, y, w, h);
            return;
        }
        String[] names = {"Win rate", "Fights", "A-Z"};
        int cx = x;
        textDim(g, "Sort:", cx, y + 3);
        cx += tw("Sort:") + 6;
        for (int i = 0; i < 3; i++) {
            final int idx = i;
            int cw = tw(names[i]) + 12;
            chip(g, names[i], cx, y, cw, sort == i, () -> sort = idx);
            cx += cw + 4;
        }
        List<Mode> modes = new ArrayList<>(List.of(Mode.values()));
        Map<Mode, Stats> st = new HashMap<>();
        for (Mode m : modes) st.put(m, Stats.of(Store.forMode(m)));
        modes.sort(switch (sort) {
            case 0 -> Comparator.comparingDouble((Mode m) -> st.get(m).winRate()).reversed();
            case 1 -> Comparator.comparingInt((Mode m) -> st.get(m).fights).reversed();
            default -> Comparator.comparing((Mode m) -> m.label);
        });
        int ly = y + 20, lh = h - 20;
        int total = modes.size() * 26;
        scroll = Math.min(scroll, Math.max(0, total - lh));
        setClip(g, x, ly, w, lh);
        int yy = ly - scroll;
        for (Mode m : modes) {
            final Mode mm = m;
            Stats s = st.get(m);
            box(g, x, yy, w, 23, 20);
            Icons.draw(g, m, x + 6, yy + 5, 1, fa(m.rgb, 255));
            text(g, m.label, x + 24, yy + 3, m.rgb);
            textDim(g, s.fights == 0 ? "No fights yet" : s.wins + "W " + s.losses + "L  |  hit rate " + (int) Math.round(s.hitRate * 100) + "%", x + 24, yy + 13);
            if (s.fights > 0) {
                String wr = (int) Math.round(s.winRate() * 100) + "%";
                text(g, wr, x + w - tw(wr) - 8, yy + 8, 0xFFFFFF);
                bar(g, x + w - 100, yy + 18, 60, 3, s.winRate(), m.rgb);
            }
            hit(x, yy, w, 23, () -> { detail = mm; scroll = 0; });
            yy += 26;
        }
        endClip(g);
    }

    private void modeDetail(GuiGraphics g, int x, int y, int w, int h) {
        Mode m = detail;
        chip(g, "Back", x, y, 40, false, () -> { detail = null; scroll = 0; });
        Icons.draw(g, m, x + 48, y - 1, 1, fa(m.rgb, 255));
        text(g, m.label, x + 66, y + 3, m.rgb);
        Stats s = Stats.of(Store.forMode(m));
        int yy = y + 22;
        if (s.fights == 0) {
            empty(g, x, yy, "No confirmed fights in this mode yet.");
        } else {
            String[][] q = {
                    {"Win rate", (int) Math.round(s.winRate() * 100) + "%"},
                    {"Fights", String.valueOf(s.fights)},
                    {"Hit rate", (int) Math.round(s.hitRate * 100) + "%"},
                    {"Best combo", String.valueOf(s.bestCombo)},
            };
            int qw = (w - 15) / 4;
            for (int i = 0; i < 4; i++) {
                int bx = x + i * (qw + 5);
                box(g, bx, yy, qw, 30, 22);
                textDim(g, q[i][0], bx + 6, yy + 4);
                text(g, q[i][1], bx + 6, yy + 16, 0xFFFFFF);
            }
            yy += 36;
        }
        card(g, x, yy, w, "Weakness: " + m.weak, m.tip, m.rgb);
        yy += 40;
        int bx = x;
        String[] tabs = {"Weaknesses", "Habits", "Records", "Goals"};
        for (int i = 0; i < 4; i++) {
            final int target = 5 + i;
            int cw = tw(tabs[i]) + 12;
            chip(g, tabs[i] + " >", bx, yy, cw + 10, false, () -> { modeSel = m; go(target); });
            bx += cw + 14;
        }
    }

    private void opponents(GuiGraphics g, int x, int y, int w, int h) {
        Map<String, int[]> map = new HashMap<>();
        for (DuelRecord d : Store.duels) {
            int[] a = map.computeIfAbsent(d.opponent, k -> new int[3]);
            if (d.won()) a[0]++; else if (d.lost()) a[1]++; else a[2]++;
        }
        if (map.isEmpty()) {
            empty(g, x, y, "No opponents yet.");
            return;
        }
        List<Map.Entry<String, int[]>> l = new ArrayList<>(map.entrySet());
        l.sort((a, b) -> Integer.compare(b.getValue()[0] + b.getValue()[1] + b.getValue()[2], a.getValue()[0] + a.getValue()[1] + a.getValue()[2]));
        List<String[]> items = new ArrayList<>();
        for (var e : l) {
            int[] a = e.getValue();
            items.add(new String[]{e.getKey(), a[0] + "W " + a[1] + "L " + (a[2] > 0 ? a[2] + "D" : "") + " (" + (a[0] + a[1] + a[2]) + " fights)"});
        }
        cards(g, items, x, y, w, h, accent(), "No opponents yet.");
    }

    private void weaknesses(GuiGraphics g, int x, int y, int w, int h) {
        Stats s = Stats.of(Stats.last(Store.forMode(modeSel), 20));
        List<String[]> l = new ArrayList<>();
        if (s.fights >= 1) {
            if (s.weakRate > 0.2) l.add(new String[]{"Swinging before the cooldown", (int) Math.round(s.weakRate * 100) + "% of your hits were weak swings."});
            if (s.hitRate < 0.45) l.add(new String[]{"Low hit rate", "Only " + (int) Math.round(s.hitRate * 100) + "% of swings connect. Close the distance first."});
            if (s.avgGapHp >= 0 && s.avgGapHp < 5) l.add(new String[]{"Late gapples", String.format(Locale.ROOT, "You eat at %.1f HP on average. Aim for about 8 HP.", s.avgGapHp)});
            if (s.avgPearlDist > 10) l.add(new String[]{"Late pearls", String.format(Locale.ROOT, "You throw at %.0f blocks on average. Throw sooner.", s.avgPearlDist)});
            if (s.fights > 0 && s.blocked / (double) s.fights >= 2) l.add(new String[]{"Hitting into shields", "Many of your hits get blocked. Axe first or bait the shield."});
            if (s.sprintRate < 0.4 && s.hitRate > 0) l.add(new String[]{"Few sprint-hits", (int) Math.round(s.sprintRate * 100) + "% of hits were sprint-hits."});
            if (s.critRate < 0.15 && s.hitRate > 0) l.add(new String[]{"Rare crits", (int) Math.round(s.critRate * 100) + "% crit rate. Time a jump with your swing."});
            if (s.avgDmg > 14) l.add(new String[]{"Taking too much damage", String.format(Locale.ROOT, "%.0f damage taken per fight on average.", s.avgDmg)});
            if (s.avgStart > 8) l.add(new String[]{"Starting too far away", String.format(Locale.ROOT, "Average start distance %.1f blocks.", s.avgStart)});
            if (s.avgStart > 0 && s.avgStart < 2.5) l.add(new String[]{"Starting too close", String.format(Locale.ROOT, "Average start distance %.1f blocks.", s.avgStart)});
        }
        if (modeSel != null) l.add(new String[]{modeSel.weak, modeSel.tip});
        cards(g, l, x, y, w, h, 0xff9a8a, "Not enough data yet. Play a few fights.");
    }

    private void habits(GuiGraphics g, int x, int y, int w, int h) {
        Stats s = Stats.of(Stats.last(Store.forMode(modeSel), 20));
        List<String[]> l = new ArrayList<>();
        if (s.fights >= 1) {
            l.add(new String[]{"Sprint-hits", (int) Math.round(s.sprintRate * 100) + "% of your hits are sprint-hits."});
            l.add(new String[]{"Crits", (int) Math.round(s.critRate * 100) + "% of your hits are crits."});
            l.add(new String[]{"Pearls per fight", String.format(Locale.ROOT, "%.1f on average.", s.avgPearls)});
            l.add(new String[]{"Gapples per fight", String.format(Locale.ROOT, "%.1f on average.", s.avgGaps)});
            l.add(new String[]{"Potions per fight", String.format(Locale.ROOT, "%.1f on average.", s.avgPots)});
            l.add(new String[]{"Start distance", String.format(Locale.ROOT, "%.1f blocks on average.", s.avgStart)});
            l.add(new String[]{"Fight length", DuelRecord.clock((int) s.avgTicks) + " on average."});
            l.add(new String[]{"Shield blocks against you", s.blocked + " hits blocked in total."});
        }
        cards(g, l, x, y, w, h, accent(), "Not enough data yet. Play a few fights.");
    }

    private void records(GuiGraphics g, int x, int y, int w, int h) {
        Stats s = Stats.of(Store.forMode(modeSel));
        List<String[]> l = new ArrayList<>();
        if (s.fights >= 1) {
            l.add(new String[]{"Best combo", s.bestCombo + " hits in a row"});
            l.add(new String[]{"Longest win streak", String.valueOf(s.bestStreak)});
            l.add(new String[]{"Most hits in one duel", String.valueOf(s.mostHits)});
            if (s.fastestWinTicks > 0) l.add(new String[]{"Fastest win", DuelRecord.clock(s.fastestWinTicks)});
        }
        cards(g, l, x, y, w, h, 0xffd36a, "No records yet.");
    }

    private int gMetric = 0;
    private double gTarget = 55;
    private static final String[] G_NAMES = {"Hit rate %", "Win rate %", "Win streak", "Best combo", "Wins"};
    private static final double[] G_DEF = {55, 55, 5, 5, 10};

    private double goalValue(int metric, String modeKey) {
        Mode m = modeKey.isEmpty() ? null : Mode.byName(modeKey);
        List<DuelRecord> l = Store.forMode(m);
        Stats a = Stats.of(l);
        Stats r = Stats.of(Stats.last(l, 20));
        return switch (metric) {
            case 0 -> r.hitRate * 100;
            case 1 -> r.winRate() * 100;
            case 2 -> a.bestStreak;
            case 3 -> a.bestCombo;
            default -> a.wins;
        };
    }

    private void goals(GuiGraphics g, int x, int y, int w, int h) {
        final String key = modeSel == null ? "" : modeSel.name();
        final boolean pct = gMetric < 2;
        final double max = pct ? 100 : 999;
        text(g, "New goal:", x, y + 3, 0xFFFFFF);
        int cx = x + tw("New goal:") + 6;
        int mw = tw(G_NAMES[gMetric]) + 14;
        chip(g, G_NAMES[gMetric], cx, y, mw, true, () -> { gMetric = (gMetric + 1) % G_NAMES.length; gTarget = G_DEF[gMetric]; });
        cx += mw + 6;
        chip(g, "-5", cx, y, 20, false, () -> gTarget = Math.max(1, gTarget - 5));
        cx += 23;
        chip(g, "-", cx, y, 16, false, () -> gTarget = Math.max(1, gTarget - 1));
        cx += 19;
        String tv = String.format(Locale.ROOT, "%.0f", gTarget) + (pct ? "%" : "");
        int tvw = Math.max(34, tw(tv) + 10);
        box(g, cx, y, tvw, 14, 22);
        textCenter(g, tv, cx + tvw / 2, y + 3, 0xFFFFFF);
        cx += tvw + 3;
        chip(g, "+", cx, y, 16, false, () -> gTarget = Math.min(max, gTarget + 1));
        cx += 19;
        chip(g, "+5", cx, y, 20, false, () -> gTarget = Math.min(max, gTarget + 5));
        cx += 26;
        chip(g, "Add goal", cx, y, tw("Add goal") + 14, true, () -> {
            Store.Goal gl = new Store.Goal();
            gl.metric = gMetric;
            gl.target = gTarget;
            gl.mode = key;
            Store.cfg.goals.add(gl);
            Store.saveCfg();
        });

        List<Store.Goal> mine = new ArrayList<>();
        for (Store.Goal gl : Store.cfg.goals) if (gl.mode.equals(key)) mine.add(gl);
        int ly = y + 22, lh = h - 22;
        if (mine.isEmpty()) {
            empty(g, x, ly, "No goals yet. Pick a stat and a target, then press Add goal.");
            return;
        }
        int total = mine.size() * 40;
        scroll = Math.min(scroll, Math.max(0, total - lh));
        setClip(g, x, ly, w, lh);
        int by = ly - scroll;
        for (Store.Goal gl : mine) {
            double v = goalValue(gl.metric, gl.mode);
            boolean pg = gl.metric < 2;
            boolean done = v >= gl.target;
            String tgt = String.format(Locale.ROOT, "%.0f", gl.target) + (pg ? "%" : "");
            String cur = String.format(Locale.ROOT, "%.0f", v) + (pg ? "%" : "");
            box(g, x, by, w, 36, 20);
            g.fill(x, by, x + 2, by + 36, fa(done ? 0x6fe3a0 : accent(), 255));
            text(g, G_NAMES[gl.metric] + ": reach " + tgt, x + 8, by + 5, done ? 0x6fe3a0 : 0xFFFFFF);
            String st = cur + " / " + tgt + (done ? "  Done!" : "");
            textDim(g, st, x + w - tw(st) - 30, by + 5);
            bar(g, x + 8, by + 22, w - 16, 5, v / Math.max(1, gl.target), done ? 0x6fe3a0 : accent());
            chip(g, "x", x + w - 22, by + 17, 14, false, () -> { Store.cfg.goals.remove(gl); Store.saveCfg(); });
            by += 40;
        }
        endClip(g);
    }

    private void settings(GuiGraphics g, int x, int y, int w, int h) {
        Store.Cfg c = Store.cfg;
        text(g, "Theme", x, y, 0xFFFFFF);
        int cx = x;
        for (int i = 0; i < Theme.values().length; i++) {
            final int idx = i;
            Theme t = Theme.of(i);
            int cw = tw(t.label) + 14;
            chip(g, t.label, cx, y + 12, cw, c.theme == i, () -> { c.theme = idx; Store.saveCfg(); });
            cx += cw + 4;
        }
        int yy = y + 36;
        text(g, "Panel opacity: " + c.opacity + "%", x, yy, 0xFFFFFF);
        int bw = Math.min(200, w - 10);
        bar(g, x, yy + 14, bw, 8, c.opacity / 100.0, accent());
        hit(x, yy + 10, bw, 16, () -> {
            c.opacity = (int) Math.max(20, Math.min(100, (clickX - x) * 100.0 / bw));
            Store.saveCfg();
        });
        yy += 34;
        text(g, "Animations", x, yy, 0xFFFFFF);
        chip(g, "Full", x, yy + 12, 40, c.anims, () -> { c.anims = true; Store.saveCfg(); });
        chip(g, "Off", x + 44, yy + 12, 40, !c.anims, () -> { c.anims = false; Store.saveCfg(); });
        yy += 36;
        text(g, "Menu key", x, yy, 0xFFFFFF);
        textDim(g, DuelAnalyzerClient.menuKey.getTranslatedKeyMessage().getString() + "  (change it in Options, Controls, Key Binds)", x, yy + 12);
        yy += 30;
        text(g, "Mod enabled", x, yy, 0xFFFFFF);
        chip(g, "On", x, yy + 12, 40, c.enabled, () -> { c.enabled = true; Store.saveCfg(); });
        chip(g, "Off", x + 44, yy + 12, 40, !c.enabled, () -> { c.enabled = false; Store.saveCfg(); });
        textDim(g, "Off = no recording and no popups (good for SMP).", x + 92, yy + 15);
        yy += 30;
        text(g, "Tips mode", x, yy, 0xFFFFFF);
        chip(g, "Offline rules", x, yy + 12, 80, true, () -> { });
        textDim(g, "AI mode may come later as an optional setting.", x + 86, yy + 15);
    }

    private void information(GuiGraphics g, int x, int y, int w, int h) {
        String[][] items = {
                {"About", "Duel Analyzer v0.1.7. A client-side mod that records your PvP duels and explains them."},
                {"How it works", "1) Fight a player. 2) The mod records hits, damage and items silently. 3) The analysis opens after the fight."},
                {"Keybinds", "K opens the menu, L shows the last duel analysis, O toggles the live overlay, H switches the whole mod on or off. Rebind them in Options, Controls, Key Binds."},
                {"What is new", "v0.1.7: mod on/off key (H) and setting. v0.1.6: overlay shows crits instead of swings. v0.1.5: L and O show a message. v0.1.4: new tips (losing streak warning, rematch comparison, session summary). v0.1.3: a result only appears when a player really dies. v0.1.2: deaths are always recorded as a loss. v0.1.1: modes are detected from your inventory, Win or Lose only, your own goals, new Spear Mace mode, see-through menu. v0.1.0: first preview."},
        };
        int total = items.length * 50;
        scroll = Math.min(scroll, Math.max(0, total - h));
        setClip(g, x, y, w, h);
        int yy = y - scroll;
        for (String[] it : items) {
            box(g, x, yy, w, 46, 20);
            text(g, it[0], x + 8, yy + 5, accent());
            wrapDim(g, it[1], x + 8, yy + 18, w - 16, 2);
            yy += 50;
        }
        endClip(g);
    }

    private void wrapDim(GuiGraphics g, String s, int x, int y, int maxW, int maxLines) {
        String rest = s;
        for (int line = 0; line < maxLines && !rest.isEmpty(); line++) {
            int cut = rest.length();
            while (cut > 1 && tw(rest.substring(0, cut)) > maxW) cut--;
            if (cut < rest.length()) {
                int sp = rest.lastIndexOf(' ', cut);
                if (sp > 0) cut = sp;
            }
            textDim(g, rest.substring(0, cut).trim(), x, y + line * 11);
            rest = rest.substring(cut).trim();
        }
    }
}
