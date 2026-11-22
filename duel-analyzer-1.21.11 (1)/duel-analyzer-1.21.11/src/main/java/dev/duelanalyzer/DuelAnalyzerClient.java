package dev.duelanalyzer;

import com.mojang.blaze3d.platform.InputConstants;
import dev.duelanalyzer.ui.AnalysisScreen;
import dev.duelanalyzer.ui.DashboardScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

public class DuelAnalyzerClient implements ClientModInitializer {
    private static void msg(Minecraft mc, String text) {
        if (mc.player != null) mc.player.displayClientMessage(net.minecraft.network.chat.Component.literal(text), true);
    }

    public static final String MOD_ID = "duelanalyzer";
    public static KeyMapping menuKey, lastKey, overlayKey, toggleKey;

    @Override
    public void onInitializeClient() {
        Store.init();
        Recorder.init();

        KeyMapping.Category cat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
        menuKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.duelanalyzer.menu", InputConstants.Type.KEYSYM, InputConstants.KEY_K, cat));
        lastKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.duelanalyzer.last", InputConstants.Type.KEYSYM, InputConstants.KEY_L, cat));
        overlayKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.duelanalyzer.overlay", InputConstants.Type.KEYSYM, InputConstants.KEY_O, cat));
        toggleKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping("key.duelanalyzer.toggle", InputConstants.Type.KEYSYM, InputConstants.KEY_H, cat));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            Recorder.tick(mc);
            while (menuKey.consumeClick()) {
                if (mc.screen == null) mc.setScreen(new DashboardScreen());
            }
            while (lastKey.consumeClick()) {
                if (mc.screen != null) continue;
                if (Store.duels.isEmpty()) {
                    msg(mc, "Duel Analyzer: no duels recorded yet. Fight a player first.");
                } else {
                    mc.setScreen(new AnalysisScreen(Store.duels.get(0), null));
                }
            }
            while (toggleKey.consumeClick()) {
                Store.cfg.enabled = !Store.cfg.enabled;
                Store.saveCfg();
                msg(mc, "Duel Analyzer: " + (Store.cfg.enabled ? "ON (recording)" : "OFF (paused)"));
            }
            while (overlayKey.consumeClick()) {
                Store.cfg.overlay = !Store.cfg.overlay;
                Store.saveCfg();
                msg(mc, "Duel Analyzer: live overlay " + (Store.cfg.overlay ? "ON" : "OFF"));
            }
        });
    }
}
