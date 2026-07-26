package com.github.gubejs.script;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraftforge.fml.ModList;
import org.jetbrains.annotations.Nullable;

/**
 * One script file, and what its header comments asked for.
 *
 * <p>A script can open with directives:
 *
 * <pre>{@code
 * // priority: 100
 * // requires: create
 * // ignored: false
 * // packmode: hard
 * }</pre>
 *
 * <p>Higher priority loads first, {@code requires} skips the file when a mod is absent, and
 * {@code ignored} skips it outright. They are comments so that a file carrying them is still
 * valid JavaScript in an editor.
 */
public final class ScriptFileInfo {

    private static final Pattern PROPERTY = Pattern.compile("^(\\w+)\\s*[:=]?\\s*(-?[\\w.:/-]+)$");

    /** Which pack this file came from. */
    public final ScriptPackInfo pack;

    /** The path inside the pack, e.g. {@code recipes/shaped.js}. */
    public final String file;

    /** How the file is named in log lines: {@code server_scripts:recipes/shaped.js}. */
    public final String location;

    private final Map<String, List<String>> properties = new HashMap<>();

    private final Set<String> requiredMods = new HashSet<>();

    private int priority;

    private boolean ignored;

    private String packMode = "";

    /** The file's text, dropped once it has been evaluated. */
    public String[] lines = new String[0];

    public ScriptFileInfo(ScriptPackInfo pack, String file) {
        this.pack = pack;
        this.file = file;
        this.location = pack.namespace() + ":" + pack.pathStart() + file;
    }

    /**
     * Reads the file and parses its directives.
     *
     * @param source where to read it from
     * @return {@code null} on success, otherwise what went wrong
     */
    @Nullable
    public Throwable preload(ScriptSource source) {
        properties.clear();
        requiredMods.clear();
        priority = 0;
        ignored = false;
        packMode = "";

        try {
            lines = source.readSource(this).toArray(new String[0]);
        } catch (Throwable ex) {
            return ex;
        }

        for (var i = 0; i < lines.length; i++) {
            var trimmed = lines[i].trim();

            if (trimmed.startsWith("//")) {
                var matcher = PROPERTY.matcher(trimmed.substring(2).trim());

                if (matcher.find()) {
                    properties.computeIfAbsent(matcher.group(1).trim(), k -> new ArrayList<>())
                        .add(matcher.group(2).trim());
                }
            } else if (trimmed.startsWith("import ")) {
                // Packs written against the TypeScript definitions open with import statements
                // that exist only to make an editor happy. Blanked rather than removed so that
                // every later line still reports its original number in a stack trace.
                lines[i] = "";
            }
        }

        try {
            priority = Integer.parseInt(getProperty("priority", "0"));
            ignored = getProperty("ignored", "false").equals("true")
                || getProperty("ignore", "false").equals("true");
            packMode = getProperty("packmode", "");
            requiredMods.addAll(getProperties("requires"));
        } catch (Exception ex) {
            return ex;
        }

        return null;
    }

    /**
     * Returns every value given for a directive.
     *
     * @param key the directive name
     * @return the values, in the order they appeared
     */
    public List<String> getProperties(String key) {
        return properties.getOrDefault(key, List.of());
    }

    /**
     * Returns the last value given for a directive.
     *
     * @param key the directive name
     * @param defaultValue what to return when the directive is absent
     * @return the value
     */
    public String getProperty(String key, String defaultValue) {
        var values = getProperties(key);
        return values.isEmpty() ? defaultValue : values.get(values.size() - 1);
    }

    /** Higher loads first. */
    public int getPriority() {
        return priority;
    }

    /**
     * Explains why this file should not run.
     *
     * @param currentPackMode the pack mode in effect
     * @return the reason, or an empty string if the file should run
     */
    public String skipLoading(String currentPackMode) {
        if (ignored) {
            return "Ignored";
        }

        if (!packMode.isEmpty() && !packMode.equals(currentPackMode)) {
            return "Pack mode is '" + currentPackMode + "', not '" + packMode + "'";
        }

        for (var mod : requiredMods) {
            if (!ModList.get().isLoaded(mod)) {
                return "Mod '" + mod + "' is not installed";
            }
        }

        return "";
    }

    @Override
    public String toString() {
        return location;
    }
}
