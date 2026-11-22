package dev.duelanalyzer;

public enum Mode {
    SWORD("Sword", 0x8fd3ff, "Trading hits instead of winning them",
            "Hit, then step back out of reach before they swing."),
    AXE("Axe", 0xff9a6b, "Axe swings that miss a shield",
            "Axe first to break the shield, then switch to your damage."),
    MACE("Mace", 0xc8b6ff, "Mistimed smash attacks",
            "Smash only after a full fall, then back off."),
    SPEAR("Spear", 0xb8f26b, "Poor spacing with the spear",
            "Stay at the tip of your reach and back off right after each poke."),
    SPEAR_MACE("Spear Mace", 0x9ef0e0, "Wrong weapon for the moment",
            "Poke with the spear to keep range, switch to the mace only for a full-fall smash."),
    CRYSTAL("Crystal", 0xff7bd5, "Slow crystal placement",
            "Pre-place crystals before the opponent closes in."),
    NETHERITE_POT("Netherite Pot", 0xb3a5d6, "Late potion and gapple use",
            "Heal while they are on cooldown, not while they are on you."),
    DIAMOND_POT("Diamond Pot", 0x5ad1ff, "Armor wearing down too fast",
            "Use speed and strength before you engage, not after."),
    UHC("UHC", 0xffc24a, "Gapples eaten too late",
            "Eat your golden apple around 8 HP, not 3 HP."),
    SMP("SMP", 0x6fe3a0, "Fighting without an escape plan",
            "Keep a pearl or a block ready before you commit."),
    DIAMOND_SMP("Diamond SMP", 0x4fd8e8, "Armor breaking mid-fight",
            "Check your armor durability before a long fight and swap early."),
    CART("Cart", 0xff8a5c, "Carts placed too late",
            "Place the cart before they reach you and punish the blast.");

    public final String label;
    public final int rgb;
    public final String weak;
    public final String tip;

    Mode(String label, int rgb, String weak, String tip) {
        this.label = label;
        this.rgb = rgb;
        this.weak = weak;
        this.tip = tip;
    }

    public static Mode byName(String n) {
        try {
            return valueOf(n);
        } catch (Exception e) {
            return SWORD;
        }
    }
}
