package dev.duelanalyzer;

import java.util.ArrayList;
import java.util.List;

public class DuelRecord {
    public String id;
    public long time;
    public String opponent = "?";
    public String server = "";
    public String mode = "SWORD";
    public double modeConf;
    public boolean modeConfirmed;
    public String result = "LOSE";
    public int ticks;
    public int hits, swings, weakHits, critHits, sprintHits, blockedByOpp, bestCombo;
    public int pearls, gapples, potions;
    public float damageTaken, lowestHp = 20f, oppEndHp;
    public double startDist, pearlDistSum;
    public List<Float> gappleHp = new ArrayList<>();
    public List<Ev> events = new ArrayList<>();
    public List<String> tips;

    public static class Ev {
        public int t;
        public String s;

        public Ev(int t, String s) {
            this.t = t;
            this.s = s;
        }
    }

    public boolean won() { return "WIN".equals(result); }
    public boolean lost() { return !"WIN".equals(result); }
    public Mode modeEnum() { return Mode.byName(mode); }

    public double hitRate() {
        return swings <= 0 ? 0 : Math.min(1.0, hits / (double) swings);
    }

    public static String clock(int ticks) {
        int s = ticks / 20;
        return (s / 60) + ":" + String.format("%02d", s % 60);
    }

    public String duration() { return clock(ticks); }
}
