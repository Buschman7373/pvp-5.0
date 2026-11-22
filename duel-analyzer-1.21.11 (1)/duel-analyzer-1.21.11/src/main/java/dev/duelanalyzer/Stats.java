package dev.duelanalyzer;

import java.util.ArrayList;
import java.util.List;

/** Aggregated numbers. The list must be sorted newest first. */
public final class Stats {
    public int fights, wins, losses;
    public double hitRate, weakRate, critRate, sprintRate, avgTicks, avgPearls, avgGaps, avgPots, avgStart, avgDmg;
    public double avgGapHp = -1, avgPearlDist = -1;
    public int bestCombo, bestStreak, curStreak, mostHits, fastestWinTicks = -1, blocked;

    public double winRate() { return fights == 0 ? 0 : wins / (double) fights; }
    public double kd() { return wins / (double) Math.max(1, losses); }

    public static List<DuelRecord> last(List<DuelRecord> l, int n) {
        return new ArrayList<>(l.subList(0, Math.min(n, l.size())));
    }

    public static Stats of(List<DuelRecord> list) {
        Stats s = new Stats();
        int hits = 0, swings = 0, weak = 0, crit = 0, sprint = 0, pearls = 0, gaps = 0, pots = 0, gapN = 0, pearlN = 0;
        double ticks = 0, start = 0, dmg = 0, gapHpSum = 0, pearlDist = 0;
        for (DuelRecord d : list) {
            if (!d.won() && !d.lost()) continue;
            s.fights++;
            if (d.won()) s.wins++; else s.losses++;
            hits += d.hits; swings += d.swings; weak += d.weakHits; crit += d.critHits; sprint += d.sprintHits;
            pearls += d.pearls; gaps += d.gapples; pots += d.potions;
            ticks += d.ticks; start += d.startDist; dmg += d.damageTaken;
            s.bestCombo = Math.max(s.bestCombo, d.bestCombo);
            s.mostHits = Math.max(s.mostHits, d.hits);
            s.blocked += d.blockedByOpp;
            for (float h : d.gappleHp) { gapHpSum += h; gapN++; }
            if (d.pearls > 0) { pearlDist += d.pearlDistSum; pearlN += d.pearls; }
            if (d.won() && (s.fastestWinTicks < 0 || d.ticks < s.fastestWinTicks)) s.fastestWinTicks = d.ticks;
        }
        if (s.fights == 0) return s;
        s.hitRate = swings == 0 ? 0 : Math.min(1.0, hits / (double) swings);
        s.weakRate = hits == 0 ? 0 : weak / (double) hits;
        s.critRate = hits == 0 ? 0 : crit / (double) hits;
        s.sprintRate = hits == 0 ? 0 : sprint / (double) hits;
        s.avgTicks = ticks / s.fights;
        s.avgPearls = pearls / (double) s.fights;
        s.avgGaps = gaps / (double) s.fights;
        s.avgPots = pots / (double) s.fights;
        s.avgStart = start / s.fights;
        s.avgDmg = dmg / s.fights;
        if (gapN > 0) s.avgGapHp = gapHpSum / gapN;
        if (pearlN > 0) s.avgPearlDist = pearlDist / pearlN;

        // streaks (list is newest first)
        for (DuelRecord d : list) {
            if (d.won()) s.curStreak++; else if (d.lost()) break;
        }
        int run = 0;
        for (int i = list.size() - 1; i >= 0; i--) {
            DuelRecord d = list.get(i);
            if (d.won()) { run++; s.bestStreak = Math.max(s.bestStreak, run); }
            else if (d.lost()) run = 0;
        }
        return s;
    }
}
