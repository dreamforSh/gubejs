package com.github.gubejs.bindings;

import com.github.gubejs.Gubejs;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.server.ServerLifecycleHooks;
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
}
