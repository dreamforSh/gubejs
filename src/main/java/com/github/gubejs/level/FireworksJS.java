package com.github.gubejs.level;

import com.github.gubejs.util.ValueUtils;
import java.util.Locale;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.Nullable;

/**
 * Turns what a script says about a firework into the rocket the game launches.
 *
 * <pre>{@code
 * event.level.spawnFireworks(x, y, z, {
 *     flight: 2,
 *     type: 'large_ball',
 *     colors: [0xFF0000, 0xFFAA00],
 *     fadeColors: [0x000000],
 *     trail: true,
 *     flicker: true
 * })
 * }</pre>
 *
 * <p>A firework is entirely NBT on an item — there is no other way to describe one, and no vanilla
 * builder for it. So this writes that NBT, and a script never has to know the shape of it.
 *
 * <p>Passing nothing gives a plain white ball with one second of flight, which is a firework rather
 * than an error.
 */
public final class FireworksJS {

    /** The explosion shapes, in the order the game numbers them. */
    private static final String[] TYPES = {
        "small_ball", "large_ball", "star", "creeper", "burst"
    };

    private FireworksJS() {
    }

    /**
     * Builds the rocket item one description means.
     *
     * @param description an object with {@code flight}, {@code type}, {@code colors},
     *     {@code fadeColors}, {@code trail} and {@code flicker}, or {@code null} for the default
     * @return a firework rocket stack
     */
    public static ItemStack createStack(@Nullable Object description) {
        var stack = new ItemStack(Items.FIREWORK_ROCKET);
        var unwrapped = ValueUtils.unwrap(description);
        var map = unwrapped instanceof Map<?, ?> m ? m : Map.of();

        var fireworks = new CompoundTag();
        fireworks.putByte("Flight", (byte) intOf(map.get("flight"), 1));

        var explosion = new CompoundTag();
        explosion.putByte("Type", (byte) typeOf(map.get("type")));
        explosion.putBoolean("Trail", booleanOf(map.get("trail")));
        explosion.putBoolean("Flicker", booleanOf(map.get("flicker")));
        explosion.put("Colors", colours(map.get("colors"), 0xFFFFFF));
        explosion.put("FadeColors", colours(map.get("fadeColors"), null));

        var explosions = new ListTag();
        explosions.add(explosion);
        fireworks.put("Explosions", explosions);

        stack.getOrCreateTag().put("Fireworks", fireworks);
        return stack;
    }

    /**
     * Reads the colour list, which is an int array of packed RGB and nothing else.
     *
     * @param value what the script wrote — one colour or several
     * @param fallback the colour to use when the script wrote none, or {@code null} to leave the
     *     list empty, which is what an absent fade means
     */
    private static Tag colours(@Nullable Object value, @Nullable Integer fallback) {
        var list = ValueUtils.listOf(value);

        if (list.isEmpty()) {
            return new IntArrayTag(fallback == null ? new int[0] : new int[]{fallback});
        }

        var colours = new int[list.size()];

        for (var i = 0; i < colours.length; i++) {
            colours[i] = intOf(list.get(i), 0xFFFFFF);
        }

        return new IntArrayTag(colours);
    }

    /**
     * Reads the explosion shape, by name or by the number the game uses.
     *
     * <p>An unknown name becomes a small ball rather than an error: a firework that comes out the
     * wrong shape is obvious in the sky, and one that refuses to launch is not.
     */
    private static int typeOf(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof Number number) {
            return Math.max(0, Math.min(TYPES.length - 1, number.intValue()));
        } else if (unwrapped == null) {
            return 0;
        }

        var name = String.valueOf(unwrapped).toLowerCase(Locale.ROOT);

        for (var i = 0; i < TYPES.length; i++) {
            if (TYPES[i].equals(name)) {
                return i;
            }
        }

        return 0;
    }

    private static int intOf(@Nullable Object value, int fallback) {
        var unwrapped = ValueUtils.unwrap(value);
        return unwrapped instanceof Number number ? number.intValue() : fallback;
    }

    private static boolean booleanOf(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);
        return unwrapped instanceof Boolean flag && flag;
    }
}
