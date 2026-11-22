package dev.duelanalyzer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/** Local storage: one small JSON file per duel plus one config file. */
public final class Store {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Path root;
    private static Path duelDir;

    public static Cfg cfg = new Cfg();
    public static final List<DuelRecord> duels = new ArrayList<>();

    public static class Cfg {
        public int theme = 0;
        public int opacity = 80;
        public boolean anims = true;
        public boolean overlay = false;
        public boolean enabled = true;
        public String tipsMode = "offline";
        public List<String> tipHistory = new ArrayList<>();
        public Map<String, String> serverModes = new HashMap<>();
        public List<Goal> goals = new ArrayList<>();
    }

    /** A goal the player sets themselves. metric: 0 hit rate %, 1 win rate %, 2 win streak, 3 best combo, 4 wins. */
    public static class Goal {
        public int metric;
        public double target;
        public String mode = "";
    }

    public static void init() {
        root = FabricLoader.getInstance().getConfigDir().resolve("duelanalyzer");
        duelDir = root.resolve("duels");
        try {
            Files.createDirectories(duelDir);
            Path c = root.resolve("config.json");
            if (Files.exists(c)) {
                Cfg loaded = GSON.fromJson(Files.readString(c), Cfg.class);
                if (loaded != null) cfg = loaded;
                if (cfg.goals == null) cfg.goals = new ArrayList<>();
            }
            try (Stream<Path> s = Files.list(duelDir)) {
                for (Path p : (Iterable<Path>) s::iterator) {
                    if (!p.toString().endsWith(".json")) continue;
                    try {
                        DuelRecord d = GSON.fromJson(Files.readString(p), DuelRecord.class);
                        if (d != null && d.id != null) {
                            if (!d.won() && !d.lost()) d.result = "LOSE";
                            d.modeConfirmed = true;
                            duels.add(d);
                        }
                    } catch (Exception ignored) {
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        duels.sort(Comparator.comparingLong((DuelRecord d) -> d.time).reversed());
    }

    public static void add(DuelRecord d) {
        duels.add(0, d);
        save(d);
    }

    public static void save(DuelRecord d) {
        try {
            Files.writeString(duelDir.resolve(d.id + ".json"), GSON.toJson(d));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveCfg() {
        try {
            Files.writeString(root.resolve("config.json"), GSON.toJson(cfg));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Duels that count toward a mode. Unconfirmed low-confidence duels only count toward "All modes" (mode == null). */
    public static List<DuelRecord> forMode(Mode m) {
        List<DuelRecord> out = new ArrayList<>();
        for (DuelRecord d : duels) {
            if (m == null) {
                out.add(d);
            } else if (d.mode.equals(m.name()) && (d.modeConfirmed || d.modeConf >= 0.6)) {
                out.add(d);
            }
        }
        return out;
    }
}
