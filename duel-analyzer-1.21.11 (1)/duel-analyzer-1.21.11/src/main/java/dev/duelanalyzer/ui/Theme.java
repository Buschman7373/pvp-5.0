package dev.duelanalyzer.ui;

public enum Theme {
    NIGHT("Night sky", 0x5ad1ff, 0x0b1630, 0x1b2c55),
    EMBER("Ember", 0xff9a4a, 0x2a0f08, 0x5a2412),
    AURORA("Aurora", 0x4fe0c0, 0x07231f, 0x14504a),
    DAY("Day sky", 0xffc24a, 0x2b4a7a, 0x6aa0d8);

    public final String label;
    public final int accent, bg1, bg2;

    Theme(String label, int accent, int bg1, int bg2) {
        this.label = label;
        this.accent = accent;
        this.bg1 = bg1;
        this.bg2 = bg2;
    }

    public static Theme of(int i) {
        Theme[] v = values();
        return v[Math.floorMod(i, v.length)];
    }
}
