# Duel Analyzer (Fabric client mod, Minecraft Java 1.21.11)

Records your PvP duels silently and opens an analysis screen after each fight:
stats, timeline, key moments and 2-3 tips that do not repeat.

## Build
1. Install JDK 21.
2. Open this folder in IntelliJ IDEA (it sets up Gradle for you), or run `gradle build` if you have Gradle 9.x.
   (If you want `./gradlew`, run `gradle wrapper --gradle-version 9.5.1` once.)
3. The jar is in `build/libs/duel-analyzer-0.1.0.jar`.
4. Put it in your `mods` folder together with Fabric Loader and Fabric API (for 1.21.11).

If the versions in `gradle.properties` are outdated, copy the current ones from https://fabricmc.net/develop

## Keys (rebind in Options > Controls > Key Binds > Duel Analyzer)
- K: open the dashboard
- L: last duel analysis
- O: toggle the live overlay (action bar)
- H: turn the whole mod on or off (also in Settings > Mod enabled)

## Data
Stored locally in `config/duelanalyzer/` (one small JSON file per duel + `config.json`).
Delete a file to delete that duel.

## Modes
Sword, Axe, Mace, Spear, Spear Mace, Crystal, Netherite Pot, Diamond Pot, UHC, SMP, Diamond SMP, Cart.
Detected automatically from the items in your inventory.
