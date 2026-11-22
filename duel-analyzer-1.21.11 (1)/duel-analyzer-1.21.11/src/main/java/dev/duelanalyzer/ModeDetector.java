package dev.duelanalyzer;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.DisplaySlot;

import java.util.Locale;

/** Guesses the game mode from gear, scoreboard/chat clues and saved per-server rules. Every guess has a confidence. */
public final class ModeDetector {
    public record Guess(Mode mode, double conf) {}

    public static volatile String lastChatClue = "";

    static String id(ItemStack s) {
        if (s == null || s.isEmpty()) return "";
        return BuiltInRegistries.ITEM.getKey(s.getItem()).getPath();
    }

    public static String serverKey(Minecraft mc) {
        return mc.getCurrentServer() == null ? "" : mc.getCurrentServer().ip;
    }

    public static Guess detect(Minecraft mc, Player p) {
        String server = serverKey(mc);

        boolean mace = false, spear = false, crystal = false, obsidian = false, cart = false, tnt = false;
        boolean sword = false, axe = false, bow = false, gapple = false, shield = false;
        int potions = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            String n = id(p.getInventory().getItem(i));
            if (n.isEmpty()) continue;
            if (n.equals("mace")) mace = true;
            else if (n.endsWith("_spear")) spear = true;
            else if (n.equals("end_crystal")) crystal = true;
            else if (n.equals("obsidian")) obsidian = true;
            else if (n.equals("tnt_minecart")) cart = true;
            else if (n.equals("tnt")) tnt = true;
            else if (n.endsWith("_sword")) sword = true;
            else if (n.endsWith("_axe")) axe = true;
            else if (n.equals("bow") || n.equals("crossbow")) bow = true;
            else if (n.equals("golden_apple") || n.equals("enchanted_golden_apple")) gapple = true;
            else if (n.equals("shield")) shield = true;
            else if (n.contains("potion")) potions++;
        }
        if (id(p.getOffhandItem()).equals("shield")) shield = true;

        int netherite = 0, diamond = 0;
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (EquipmentSlot sl : slots) {
            String n = id(p.getItemBySlot(sl));
            if (n.startsWith("netherite_")) netherite++;
            else if (n.startsWith("diamond_")) diamond++;
        }

        String clue = scoreboardClue(mc) + " " + lastChatClue;
        Guess byClue = fromText(clue);

        Guess byGear;
        if (mace && spear) byGear = new Guess(Mode.SPEAR_MACE, 0.9);
        else if (mace) byGear = new Guess(Mode.MACE, 0.8);
        else if (spear) byGear = new Guess(Mode.SPEAR, 0.8);
        else if (crystal && obsidian) byGear = new Guess(Mode.CRYSTAL, 0.85);
        else if (cart && tnt) byGear = new Guess(Mode.CART, 0.85);
        else if (cart) byGear = new Guess(Mode.CART, 0.7);
        else if (netherite >= 3 && potions >= 3) byGear = new Guess(Mode.NETHERITE_POT, 0.75);
        else if (diamond >= 3 && potions >= 3) byGear = new Guess(Mode.DIAMOND_POT, 0.7);
        else if (axe && shield) byGear = new Guess(Mode.AXE, 0.7);
        else if (gapple && netherite + diamond == 0) byGear = new Guess(Mode.UHC, 0.5);
        else if (diamond >= 3 && !sword) byGear = new Guess(Mode.DIAMOND_SMP, 0.4);
        else if (diamond >= 3) byGear = new Guess(Mode.DIAMOND_SMP, 0.45);
        else if (sword && (bow || gapple)) byGear = new Guess(Mode.SMP, 0.35);
        else if (sword) byGear = new Guess(Mode.SWORD, 0.5);
        else if (axe) byGear = new Guess(Mode.AXE, 0.45);
        else byGear = new Guess(Mode.SMP, 0.25);

        if (byClue != null && byGear.conf < 0.5) return byClue;
        return byGear;
    }

    private static String scoreboardClue(Minecraft mc) {
        try {
            if (mc.level == null) return "";
            var obj = mc.level.getScoreboard().getDisplayObjective(DisplaySlot.SIDEBAR);
            if (obj == null) return "";
            return obj.getDisplayName().getString();
        } catch (Throwable t) {
            return "";
        }
    }

    public static Guess fromText(String text) {
        if (text == null) return null;
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("mace")) return new Guess(Mode.MACE, 0.7);
        if (t.contains("spear")) return new Guess(Mode.SPEAR, 0.7);
        if (t.contains("crystal")) return new Guess(Mode.CRYSTAL, 0.7);
        if (t.contains("netherite") || t.contains("nethpot") || t.contains("neth pot")) return new Guess(Mode.NETHERITE_POT, 0.7);
        if (t.contains("diamond smp") || t.contains("dsmp")) return new Guess(Mode.DIAMOND_SMP, 0.7);
        if (t.contains("diamond") && t.contains("pot")) return new Guess(Mode.DIAMOND_POT, 0.7);
        if (t.contains("uhc")) return new Guess(Mode.UHC, 0.7);
        if (t.contains("cart")) return new Guess(Mode.CART, 0.7);
        if (t.contains("smp")) return new Guess(Mode.SMP, 0.6);
        if (t.contains("axe")) return new Guess(Mode.AXE, 0.65);
        if (t.contains("sword")) return new Guess(Mode.SWORD, 0.65);
        return null;
    }
}
