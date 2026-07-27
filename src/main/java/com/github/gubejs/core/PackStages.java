package com.github.gubejs.core;

import com.github.gubejs.Gubejs;
import com.github.gubejs.GubejsPaths;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The stages the whole pack has reached, as opposed to the ones one player has.
 *
 * <pre>{@code
 * PackStages.add('nether_open')     // then /reload, and the gated recipes appear
 * PackStages.has('nether_open')
 * }</pre>
 *
 * <p>This is what {@code recipe.stage(name)} gates on, and it is deliberately <em>not</em>
 * {@link StageManager}. The two answer different questions and neither can answer the other's:
 *
 * <ul>
 *   <li>{@code player.stages} is per player, changes at any moment, and is synced to that player's
 *       client. Use it for anything a script decides while the game is running.
 *   <li>These are per installation and are read once, while the datapacks load. Use them for the
 *       things that are decided by a recipe file existing or not existing.
 * </ul>
 *
 * <p>The reason for the split is not a preference. A Forge recipe condition is evaluated once, on
 * the server, as the recipe is read — there is no player in scope and no later moment to ask again.
 * A recipe either loads or it does not, for everyone. So a per-player recipe condition is not a
 * thing this or any other mod can offer; what a pack gets instead is a switch it can throw for the
 * whole save, and {@code /reload} is what makes a change take effect.
 *
 * <h2>Where these live, and why not in the world save</h2>
 *
 * <p>In {@code local/gubejs/stages.txt}, one name per line. Not in the world's saved data, which
 * would be the obvious place: on the first load of a world the recipes are read before a
 * {@code MinecraftServer} exists at all — the resources are built to hand to its constructor — so
 * saved data cannot be consulted from a condition. Not in {@code kubejs/config/} either, since that
 * directory is part of a pack a modpack author ships and this is state a play-through accumulates.
 *
 * <p>The consequence is worth knowing: these are per installation, not per save. Two worlds in one
 * single-player game share them.
 */
public final class PackStages {

    /** One name per line, in the order they were added. */
    private static final Set<String> STAGES = new LinkedHashSet<>();

    private static boolean loaded;

    private PackStages() {
    }

    /**
     * Reports whether the pack has reached a stage.
     *
     * @param stage the stage name
     * @return {@code true} if it is set
     */
    public static synchronized boolean has(String stage) {
        return stages().contains(stage);
    }

    /**
     * Sets a stage for the whole pack.
     *
     * <p>Recipes gated on it appear at the next datapack reload, not immediately — see the class
     * note above for why there is no way to make it immediate.
     *
     * @param stage the stage name
     * @return {@code true} if it was not already set
     */
    public static synchronized boolean add(String stage) {
        if (!stages().add(stage)) {
            return false;
        }

        save();
        return true;
    }

    /**
     * Takes a stage back off the pack.
     *
     * @param stage the stage name
     * @return {@code true} if it had been set
     */
    public static synchronized boolean remove(String stage) {
        if (!stages().remove(stage)) {
            return false;
        }

        save();
        return true;
    }

    /**
     * Returns every stage the pack has reached.
     *
     * @return the names, in the order they were added
     */
    public static synchronized List<String> getAll() {
        return List.copyOf(stages());
    }

    /**
     * Forgets every stage.
     *
     * @return how many were removed
     */
    public static synchronized int clear() {
        var count = stages().size();

        if (count > 0) {
            STAGES.clear();
            save();
        }

        return count;
    }

    /**
     * Re-reads the file, for a pack that edits it from outside the game.
     *
     * <p>Called by {@code /gubejs reload config} along with everything else that is read from disk.
     */
    public static synchronized void reload() {
        loaded = false;
        stages();
    }

    /**
     * The set, read from disk on first use.
     *
     * <p>Lazily rather than at class initialisation, because the first thing to ask is a recipe
     * condition and that runs on whichever thread is building the server's resources.
     */
    private static Set<String> stages() {
        if (loaded) {
            return STAGES;
        }

        loaded = true;

        try {
            if (Files.exists(GubejsPaths.PACK_STAGES)) {
                for (var line : Files.readAllLines(GubejsPaths.PACK_STAGES, StandardCharsets.UTF_8)) {
                    var name = line.trim();

                    // '#' starts a comment, so a pack author can leave a note next to a stage they
                    // turned off rather than deleting the line and forgetting what it was.
                    if (!name.isEmpty() && !name.startsWith("#")) {
                        STAGES.add(name);
                    }
                }
            }
        } catch (Exception ex) {
            Gubejs.LOGGER.error("Could not read {}", GubejsPaths.PACK_STAGES, ex);
        }

        return STAGES;
    }

    private static void save() {
        try {
            Files.writeString(GubejsPaths.PACK_STAGES, String.join("\n", STAGES) + "\n",
                StandardCharsets.UTF_8);
        } catch (Exception ex) {
            Gubejs.LOGGER.error("Could not write {}", GubejsPaths.PACK_STAGES, ex);
        }
    }
}
