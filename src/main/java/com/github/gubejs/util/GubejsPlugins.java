package com.github.gubejs.util;

import com.github.gubejs.Gubejs;
import com.github.gubejs.GubejsPlugin;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.fml.ModList;

/**
 * Finds and holds the {@link GubejsPlugin}s installed alongside this mod.
 *
 * <p>A mod declares one by shipping {@code gubejs.plugins.txt} in the root of its jar, listing one
 * fully qualified class name per line. Deliberately not {@link java.util.ServiceLoader}: Forge
 * loads mods into a module layer where a service declared in a mod jar is not visible to another
 * mod's loader, and a plain file read sidesteps the whole question.
 */
public final class GubejsPlugins {

    private static final String PLUGINS_FILE = "gubejs.plugins.txt";

    private static final List<GubejsPlugin> PLUGINS = new ArrayList<>();

    private GubejsPlugins() {
    }

    /**
     * Loads every plugin declared by an installed mod.
     *
     * <p>This mod's own plugin is added first, so that anything it registers can be replaced by
     * another plugin rather than the other way round.
     *
     * @param builtin the plugin belonging to this mod
     */
    public static void load(GubejsPlugin builtin) {
        PLUGINS.clear();
        PLUGINS.add(builtin);

        for (var mod : ModList.get().getModFiles()) {
            for (var className : readDeclaredPlugins(mod)) {
                try {
                    var type = Class.forName(className, true, GubejsPlugins.class.getClassLoader());
                    PLUGINS.add((GubejsPlugin) type.getDeclaredConstructor().newInstance());
                    Gubejs.LOGGER.info("Loaded plugin {}", className);
                } catch (Throwable ex) {
                    // One broken plugin should not take the game down with it; the pack simply
                    // loses whatever that mod added.
                    Gubejs.LOGGER.error("Could not load plugin {} declared by {}",
                        className, mod.moduleName(), ex);
                }
            }
        }
    }

    private static List<String> readDeclaredPlugins(IModFileInfo mod) {
        try {
            var file = mod.getFile().findResource(PLUGINS_FILE);

            if (file == null || Files.notExists(file)) {
                return List.of();
            }

            var names = new ArrayList<String>();

            for (var line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                var trimmed = line.trim();

                if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                    names.add(trimmed);
                }
            }

            return names;
        } catch (Throwable ex) {
            // findResource throws rather than returning null for some mod file types.
            return List.of();
        }
    }

    /**
     * Returns every loaded plugin.
     *
     * @return the plugins, this mod's own first
     */
    public static List<GubejsPlugin> getAll() {
        return PLUGINS;
    }

    /**
     * Runs {@code action} against every plugin, reporting failures rather than propagating them.
     *
     * @param action what to do with each plugin
     */
    public static void forEachPlugin(Consumer<GubejsPlugin> action) {
        for (var plugin : PLUGINS) {
            try {
                action.accept(plugin);
            } catch (Throwable ex) {
                Gubejs.LOGGER.error("Plugin {} failed", plugin, ex);
            }
        }
    }
}
