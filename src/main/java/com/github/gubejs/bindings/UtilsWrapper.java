/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/UtilsWrapper.java
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License, version 3, as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.gubejs.bindings;

import com.github.gubejs.Gubejs;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.minecraft.Util;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stat;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryManager;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Utils} global: the odds and ends that do not belong to any one type.
 */
public final class UtilsWrapper {

    /**
     * A shared random for scripts that just want a number.
     *
     * <p>Not the level's own random, deliberately: using that from a script would perturb world
     * generation and mob behaviour in ways that depend on how often the script ran.
     */
    private static final Random RANDOM = new Random();

    private UtilsWrapper() {
    }

    /**
     * Returns the shared random source.
     *
     * @return a random
     */
    public static Random getRandom() {
        return RANDOM;
    }

    /**
     * Returns the running server, if there is one.
     *
     * @return the server, or {@code null} on a client that is not in a world
     */
    @Nullable
    public static MinecraftServer getServer() {
        return ServerLifecycleHooks.getCurrentServer();
    }

    /**
     * Parses a resource location, defaulting the namespace to {@code minecraft}.
     *
     * @param id the id, with or without a namespace
     * @return the resource location, or {@code null} if the text is not a valid id
     */
    @Nullable
    public static ResourceLocation id(@Nullable Object id) {
        var text = ValueUtils.asString(id);
        return text == null ? null : ResourceLocation.tryParse(text);
    }

    /**
     * Prefixes an id with this mod's namespace when it has none.
     *
     * @param id the id
     * @return the namespaced id
     */
    public static String gubejsId(String id) {
        return id.indexOf(':') == -1 ? Gubejs.MOD_ID + ":" + id : id;
    }

    /**
     * Finds a creative tab by the name a script calls it.
     *
     * <pre>{@code
     * Utils.findCreativeTab('misc')
     * Utils.findCreativeTab('kubejs')     // this mod's own, created on first use
     * }</pre>
     *
     * @param name the tab name, or a tab already
     * @return the tab, or {@code null} for {@code null}, {@code 'none'} and an empty name
     */
    @Nullable
    public static net.minecraft.world.item.CreativeModeTab findCreativeTab(@Nullable Object name) {
        return com.github.gubejs.item.CreativeTabs.find(name);
    }

    /**
     * Builds a box from two corners.
     *
     * @param x1 first corner x
     * @param y1 first corner y
     * @param z1 first corner z
     * @param x2 second corner x
     * @param y2 second corner y
     * @param z2 second corner z
     * @return the box
     */
    public static AABB newAABB(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AABB(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Rolls a percentage chance.
     *
     * @param chance the chance, 0 to 100
     * @return {@code true} that often
     */
    public static boolean rollChance(double chance) {
        return chance > 0 && (chance >= 100 || RANDOM.nextDouble() * 100D < chance);
    }

    /**
     * Returns a random element of a list.
     *
     * @param value a list or array
     * @return one element, or {@code null} if there are none
     */
    @Nullable
    public static Object randomOf(@Nullable Object value) {
        var list = ValueUtils.listOf(value);
        return list.isEmpty() ? null : list.get(RANDOM.nextInt(list.size()));
    }

    /**
     * Parses a UUID in either the hyphenated or the compact form.
     *
     * @param value the text to parse
     * @return the UUID, or {@code null} if it does not parse
     */
    @Nullable
    public static UUID uuid(@Nullable Object value) {
        var text = ValueUtils.asString(value);

        if (text == null) {
            return null;
        }

        try {
            return text.length() == 32
                ? UUID.fromString(text.replaceFirst(
                "(.{8})(.{4})(.{4})(.{4})(.{12})", "$1-$2-$3-$4-$5"))
                : UUID.fromString(text);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    /**
     * Converts a guest value into plain Java collections.
     *
     * <p>Occasionally needed when a script wants to hand a structure to a host method typed on
     * {@code Object} and have it arrive as a {@link Map} rather than as a guest object.
     *
     * @param value what to convert
     * @return maps, lists and primitives
     */
    @Nullable
    public static Object toJava(@Nullable Object value) {
        return ValueUtils.unwrap(value);
    }

    /**
     * Returns a value as a list, treating a lone value as a list of one.
     *
     * @param value one or several values
     * @return the list
     */
    public static List<Object> asList(@Nullable Object value) {
        return new ArrayList<>(ValueUtils.listOf(value));
    }

    /**
     * Returns a value as a map.
     *
     * @param value an object
     * @return the map, empty if the value is not object-shaped
     */
    public static Map<String, Object> asMap(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof Map<?, ?> map) {
            var result = new LinkedHashMap<String, Object>();
            map.forEach((k, v) -> result.put(String.valueOf(k), v));
            return result;
        }

        return new LinkedHashMap<>();
    }

    /**
     * Returns a new mutable list.
     *
     * @return the list, empty
     */
    public static List<Object> newList() {
        return new ArrayList<>();
    }

    /**
     * Returns a new mutable list sized for what is about to go into it.
     *
     * @param size how many elements to make room for
     * @return the list, empty
     */
    public static List<Object> newList(int size) {
        return new ArrayList<>(Math.max(size, 0));
    }

    /**
     * Returns a new mutable map that keeps the order things were put into it.
     *
     * @return the map, empty
     */
    public static Map<String, Object> newMap() {
        return new LinkedHashMap<>();
    }

    // --- the game's own tables and registries --------------------------------------------------

    /**
     * Rolls a loot table the way opening a chest would.
     *
     * <pre>{@code
     * Utils.rollChestLoot('minecraft:chests/simple_dungeon')
     * }</pre>
     *
     * <p>Only tables that need no context, which is what chest tables are — a table for a block or
     * an entity asks for the block, the entity and the tool that broke it, and there is nothing
     * here to answer with. Such a table rolls to nothing rather than throwing.
     *
     * @param tableId the loot table's id
     * @return the items rolled, empty if there is no server or no such table
     */
    public static List<ItemStack> rollChestLoot(@Nullable Object tableId) {
        var server = getServer();
        var id = id(tableId);

        if (server == null || id == null) {
            console().warn("Utils.rollChestLoot needs a running server and a loot table id, got "
                + tableId);
            return new ArrayList<>();
        }

        var table = server.getLootTables().get(id);

        if (table == LootTable.EMPTY) {
            console().warn("No loot table " + id);
            return new ArrayList<>();
        }

        var context = new LootContext.Builder(server.overworld())
            .withRandom(RandomSource.create())
            .create(LootContextParamSets.EMPTY);

        try {
            return new ArrayList<>(table.getRandomItems(context));
        } catch (Exception ex) {
            console().error("Loot table " + id + " wants more context than a chest table does", ex);
            return new ArrayList<>();
        }
    }

    /**
     * Looks up a custom statistic, the kind {@code /scoreboard} and advancements count.
     *
     * @param id the statistic's id, e.g. {@code 'minecraft:jump'}
     * @return the statistic, or {@code null} if nothing registered that id
     */
    @Nullable
    public static Stat<?> getStat(@Nullable Object id) {
        var location = id(id);

        // Asking the stat type for an unregistered id would quietly invent a statistic that nothing
        // ever increments, which is worse than saying no.
        if (location == null || !Registry.CUSTOM_STAT.containsKey(location)) {
            console().warn("No custom statistic " + id);
            return null;
        }

        return Stats.CUSTOM.get(location);
    }

    /**
     * Looks up a sound event.
     *
     * @param id the sound's id, e.g. {@code 'minecraft:entity.ghast.scream'}
     * @return the sound, or {@code null} if nothing registered that id
     */
    @Nullable
    public static SoundEvent getSound(@Nullable Object id) {
        var location = id(id);
        return location == null ? null : ForgeRegistries.SOUND_EVENTS.getValue(location);
    }

    /**
     * Lists every id in a registry.
     *
     * <pre>{@code
     * Utils.getRegistryIds('item')
     * Utils.getRegistryIds('minecraft:mob_effect')
     * Utils.getRegistryIds('forge:biome_modifiers')
     * }</pre>
     *
     * <p>Both registry systems are asked, vanilla's first: what a script calls a registry may be
     * one the game owns or one a mod added through Forge, and a script has no way of knowing which.
     *
     * @param registryId the registry's id, with or without a namespace
     * @return the ids, in the registry's own order, empty if there is no such registry
     */
    public static List<String> getRegistryIds(@Nullable Object registryId) {
        var id = id(registryId);
        var ids = new ArrayList<String>();

        if (id == null) {
            console().warn("Utils.getRegistryIds needs a registry id, got " + registryId);
            return ids;
        }

        var vanilla = Registry.REGISTRY.get(id);

        if (vanilla != null) {
            vanilla.keySet().forEach(key -> ids.add(key.toString()));
            return ids;
        }

        var forge = RegistryManager.ACTIVE.getRegistry(id);

        if (forge != null) {
            forge.getKeys().forEach(key -> ids.add(key.toString()));
            return ids;
        }

        console().warn("No registry " + id);
        return ids;
    }

    // --- text and numbers ----------------------------------------------------------------------

    /**
     * Builds a pattern from whatever a script has at hand.
     *
     * <pre>{@code
     * Utils.regex(/^minecraft:.*_slab$/i)
     * Utils.regex('/^minecraft:.*_slab$/i')   // the same, written as a string
     * Utils.regex('^minecraft:.*_slab$', 'i')
     * }</pre>
     *
     * <p>The string forms exist because a pattern that travelled through JSON or through a config
     * file is no longer a {@code /x/}, and a pack that assembled one by hand still expects it to
     * work.
     *
     * @param value a pattern, a regular expression, or the text of one
     * @return the pattern, or {@code null} if there was nothing there or Java cannot read it
     */
    @Nullable
    public static Pattern regex(@Nullable Object value) {
        return regex(value, "");
    }

    /**
     * Builds a pattern with flags stated separately.
     *
     * <p>Only {@code i}, {@code s} and {@code m} are carried over. The rest of JavaScript's flags
     * are about where a search resumes and what a match returns, which is the caller's business
     * rather than the pattern's.
     *
     * @param value a pattern, a regular expression, or the text of one
     * @param flags JavaScript regular expression flags; these win over any the value carries
     * @return the pattern, or {@code null} if there was nothing there or Java cannot read it
     */
    @Nullable
    public static Pattern regex(@Nullable Object value, String flags) {
        // Before unwrapping, because a guest regular expression keeps its pattern in members that
        // converting to a map does not carry.
        if (value instanceof Value guest && !guest.isNull() && !guest.isString()
            && guest.hasMembers() && guest.hasMember("source") && guest.hasMember("flags")) {
            var source = guest.getMember("source");
            var own = guest.getMember("flags");

            if (source != null && source.isString() && own != null && own.isString()) {
                return compileRegex(source.asString(), flags.isEmpty() ? own.asString() : flags);
            }
        }

        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof Pattern pattern) {
            return flags.isEmpty() ? pattern : compileRegex(pattern.pattern(), flags);
        }

        var text = ValueUtils.asString(unwrapped);

        if (text == null) {
            return null;
        }

        if (text.length() >= 3 && text.charAt(0) == '/') {
            var end = text.lastIndexOf('/');
            var literalFlags = end < 2 ? null : text.substring(end + 1);

            if (literalFlags != null && isRegexFlags(literalFlags)) {
                return compileRegex(text.substring(1, end),
                    flags.isEmpty() ? literalFlags : flags);
            }
        }

        return compileRegex(text, flags);
    }

    /**
     * Reads a whole number, giving up rather than throwing.
     *
     * @param value a number or the text of one
     * @param defaultValue what to answer when the value is not a number
     * @return the number, or {@code defaultValue}
     */
    public static int parseInt(@Nullable Object value, int defaultValue) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof Number number) {
            return number.intValue();
        }

        var text = ValueUtils.asString(unwrapped);

        if (text == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException ex) {
            // '3.0' reaches here from a script that built the text by arithmetic, and refusing it
            // would be a surprise when the same value passed as a number works.
            var asDouble = parseDouble(text, Double.NaN);
            return Double.isNaN(asDouble) ? defaultValue : (int) asDouble;
        }
    }

    /**
     * Reads a number, giving up rather than throwing.
     *
     * @param value a number or the text of one
     * @param defaultValue what to answer when the value is not a number
     * @return the number, or {@code defaultValue}
     */
    public static double parseDouble(@Nullable Object value, double defaultValue) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof Number number) {
            return number.doubleValue();
        }

        var text = ValueUtils.asString(unwrapped);

        if (text == null) {
            return defaultValue;
        }

        try {
            return Double.parseDouble(text.trim());
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * Turns an id or a sentence into a title.
     *
     * <pre>{@code
     * Utils.toTitleCase('steel_ingot')   // 'Steel Ingot'
     * }</pre>
     *
     * @param value the text, with words separated by spaces or underscores
     * @return the titled text, empty for {@code null}
     */
    public static String toTitleCase(@Nullable Object value) {
        var text = ValueUtils.asString(value);

        if (text == null) {
            return "";
        }

        var builder = new StringBuilder(text.length());
        var startOfWord = true;

        for (var i = 0; i < text.length(); i++) {
            var c = text.charAt(i);

            if (c == '_' || Character.isWhitespace(c)) {
                builder.append(' ');
                startOfWord = true;
            } else {
                builder.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
                startOfWord = false;
            }
        }

        return builder.toString();
    }

    // --- deferring work ------------------------------------------------------------------------

    /**
     * Wraps a function so that it runs once and then keeps answering the same thing.
     *
     * <pre>{@code
     * const allOres = Utils.lazy(() => Ingredient.of('#forge:ores').itemIds)
     * allOres()   // computed here, and only here
     * }</pre>
     *
     * <p>Worth having for anything a script would otherwise compute at startup and cannot, because
     * what it needs — tags, recipes, a server — does not exist yet.
     *
     * @param supplier what to call, at most once
     * @return a function taking no arguments
     */
    public static Object lazy(Value supplier) {
        return expiringLazy(supplier, 0L);
    }

    /**
     * Wraps a function so that it runs at most once in each window of time.
     *
     * @param supplier what to call
     * @param durationMs how long an answer stays good for, in milliseconds; zero means forever
     * @return a function taking no arguments
     */
    public static Object expiringLazy(Value supplier, long durationMs) {
        return new ProxyExecutable() {

            private Object cached;

            private boolean computed;

            private long computedAt;

            @Override
            public Object execute(Value... arguments) {
                synchronized (this) {
                    var now = System.currentTimeMillis();

                    if (!computed || (durationMs > 0 && now - computedAt >= durationMs)) {
                        cached = supplier.execute();
                        computedAt = now;
                        computed = true;
                    }

                    return cached;
                }
            }
        };
    }

    /**
     * Runs a function off the game thread.
     *
     * <pre>{@code
     * Utils.runAsync(() => console.info(Utils.getRegistryIds('item').length))
     * }</pre>
     *
     * <p>What this buys is concurrency with the game, not with other scripts: GraalJS lets one
     * thread at a time into a context, so the function still waits for whatever else is running in
     * the script context it came from, and a script that blocks the game thread while holding that
     * lock is not helped by this at all.
     *
     * <p>Touching the world from here is unsafe for the usual reason — the game is halfway through
     * a tick — so the useful shape is to compute something and hand it back through a server task.
     *
     * @param function what to run
     */
    public static void runAsync(Value function) {
        var type = ScriptType.getCurrent();
        var console = console();

        Util.backgroundExecutor().execute(() -> {
            try {
                var manager = type == null ? null : type.getManager();

                if (manager == null) {
                    function.executeVoid();
                } else {
                    manager.inContext(() -> {
                        function.executeVoid();
                        return null;
                    });
                }
            } catch (Throwable ex) {
                console.error("Error in a Utils.runAsync callback", ex);
            }
        });
    }

    // --- reading the pieces --------------------------------------------------------------------

    /**
     * Compiles a JavaScript pattern, keeping the flags Java has an answer for.
     *
     * @return the pattern, or {@code null} if it is not one Java can read
     */
    @Nullable
    private static Pattern compileRegex(String source, String flags) {
        var bits = (flags.indexOf('i') == -1 ? 0 : Pattern.CASE_INSENSITIVE)
            | (flags.indexOf('s') == -1 ? 0 : Pattern.DOTALL)
            | (flags.indexOf('m') == -1 ? 0 : Pattern.MULTILINE);

        try {
            return Pattern.compile(source, bits);
        } catch (PatternSyntaxException ex) {
            console().error("Not a pattern Java understands: /" + source + "/" + flags
                + " (" + ex.getDescription() + ")");
            return null;
        }
    }

    /**
     * Whether a trailing segment is flags rather than the rest of the text.
     *
     * <p>Decides whether {@code 'a/b/c'} was a regular expression or an id with slashes in it,
     * which has to keep being read literally.
     */
    private static boolean isRegexFlags(String flags) {
        for (var i = 0; i < flags.length(); i++) {
            if ("igmsuyd".indexOf(flags.charAt(i)) == -1) {
                return false;
            }
        }

        return true;
    }

    private static ConsoleJS console() {
        return ConsoleJS.getCurrent(ConsoleJS.SERVER);
    }
}
