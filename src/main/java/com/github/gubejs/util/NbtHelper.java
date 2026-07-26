package com.github.gubejs.util;

import com.github.graal.minecraft.NbtProxy;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CollectionTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code NBT} global: turning script values into tags and back.
 *
 * <p>The engine converts a guest object into a compound on its own wherever a host method asks for
 * one, so most scripts never call anything here. What this adds is the explicit constructors a
 * script needs when the automatic choice of tag type is wrong.
 *
 * <p>That case is real and common: {@code {Damage: 1}} produces an int tag, but an item's
 * {@code Damage} has to be an int and its {@code CustomModelData} too, while a potion's
 * {@code CustomPotionColor} is an int and {@code HideFlags} a byte. Where the tag type matters,
 * {@code NBT.byteTag(1)} says so.
 */
public final class NbtHelper {

    private NbtHelper() {
    }

    /**
     * Converts a script value into a compound tag.
     *
     * @param value an object, an SNBT string, or a tag already
     * @return the compound, or {@code null} if the value is not compound-shaped
     */
    @Nullable
    public static CompoundTag compound(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        // A string in this position is SNBT -- '{Damage:1}' -- not a string tag, which is what
        // the general conversion would make of it.
        if (unwrapped instanceof CharSequence text) {
            return parse(text.toString());
        }

        return of(unwrapped) instanceof CompoundTag tag ? tag : null;
    }

    /**
     * Converts a script value into whichever tag fits it.
     *
     * <p>Numbers are the interesting case. JavaScript has one number type, so the tag has to be
     * chosen from the value: a whole number small enough becomes an int, which is what almost
     * every vanilla field wants, and anything fractional becomes a double. Where that guess is
     * wrong, the explicit constructors below say what is meant.
     *
     * @param value anything a script can produce
     * @return the tag, or {@code null} for {@code null}
     */
    @Nullable
    public static Tag of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return null;
        } else if (unwrapped instanceof Tag tag) {
            return tag;
        } else if (unwrapped instanceof Boolean bool) {
            return ByteTag.valueOf(bool);
        } else if (unwrapped instanceof CharSequence text) {
            return StringTag.valueOf(text.toString());
        } else if (unwrapped instanceof Byte b) {
            return ByteTag.valueOf(b);
        } else if (unwrapped instanceof Short s) {
            return ShortTag.valueOf(s);
        } else if (unwrapped instanceof Integer i) {
            return IntTag.valueOf(i);
        } else if (unwrapped instanceof Long l) {
            return LongTag.valueOf(l);
        } else if (unwrapped instanceof Float f) {
            return FloatTag.valueOf(f);
        } else if (unwrapped instanceof Number number) {
            var d = number.doubleValue();
            return d == Math.rint(d) && !Double.isInfinite(d)
                && d >= Integer.MIN_VALUE && d <= Integer.MAX_VALUE
                ? IntTag.valueOf((int) d) : DoubleTag.valueOf(d);
        } else if (unwrapped instanceof java.util.Map<?, ?> map) {
            var compound = new CompoundTag();

            map.forEach((k, v) -> {
                var tag = of(v);

                if (tag != null) {
                    compound.put(String.valueOf(k), tag);
                }
            });

            return compound;
        } else if (unwrapped instanceof Iterable<?> || unwrapped instanceof Object[]) {
            return listTag(unwrapped);
        }

        return StringTag.valueOf(String.valueOf(unwrapped));
    }

    /**
     * Presents a tag to a script as a plain object or array, writing through to the tag itself.
     *
     * @param tag the tag to view
     * @return a view a script can index and assign into
     */
    @Nullable
    public static Object toObject(@Nullable Tag tag) {
        return tag == null ? null : NbtProxy.of(tag);
    }

    /**
     * Parses SNBT, the format {@code /data} and item tooltips use.
     *
     * @param text the text to parse
     * @return the compound, or {@code null} if it does not parse
     */
    @Nullable
    public static CompoundTag parse(String text) {
        try {
            return TagParser.parseTag(text);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Converts a tag to JSON, the way a datapack would spell the same data.
     *
     * @param tag the tag to convert
     * @return the equivalent JSON
     */
    public static JsonElement toJson(@Nullable Tag tag) {
        if (tag == null) {
            return JsonNull.INSTANCE;
        } else if (tag instanceof CompoundTag compound) {
            var object = new JsonObject();

            for (var key : compound.getAllKeys()) {
                object.add(key, toJson(compound.get(key)));
            }

            return object;
        } else if (tag instanceof CollectionTag<?> collection) {
            var array = new JsonArray();
            collection.forEach(element -> array.add(toJson(element)));
            return array;
        } else if (tag instanceof StringTag string) {
            return new JsonPrimitive(string.getAsString());
        } else if (tag instanceof ByteTag b) {
            // Minecraft has no boolean tag; a byte holding 0 or 1 is how one is written, and a
            // datapack that reads it back expects true/false rather than 0/1.
            return b.getAsByte() == 0 || b.getAsByte() == 1
                ? new JsonPrimitive(b.getAsByte() == 1) : new JsonPrimitive(b.getAsByte());
        } else if (tag instanceof NumericTag number) {
            return new JsonPrimitive(number.getAsNumber());
        }

        return new JsonPrimitive(tag.getAsString());
    }

    // --- explicit tag types ------------------------------------------------------------------

    /**
     * Creates a byte tag.
     *
     * @param value the number to store
     * @return the tag
     */
    public static ByteTag byteTag(Number value) {
        return ByteTag.valueOf(value.byteValue());
    }

    public static ShortTag shortTag(Number value) {
        return ShortTag.valueOf(value.shortValue());
    }

    public static IntTag intTag(Number value) {
        return IntTag.valueOf(value.intValue());
    }

    public static LongTag longTag(Number value) {
        return LongTag.valueOf(value.longValue());
    }

    public static FloatTag floatTag(Number value) {
        return FloatTag.valueOf(value.floatValue());
    }

    public static DoubleTag doubleTag(Number value) {
        return DoubleTag.valueOf(value.doubleValue());
    }

    public static StringTag stringTag(Object value) {
        return StringTag.valueOf(String.valueOf(ValueUtils.unwrap(value)));
    }

    /**
     * Creates a byte array tag from a list of numbers.
     *
     * @param value a list or array of numbers
     * @return the tag
     */
    public static ByteArrayTag byteArrayTag(Object value) {
        var values = ValueUtils.listOf(value);
        var array = new byte[values.size()];

        for (var i = 0; i < array.length; i++) {
            array[i] = ((Number) values.get(i)).byteValue();
        }

        return new ByteArrayTag(array);
    }

    public static IntArrayTag intArrayTag(Object value) {
        var values = ValueUtils.listOf(value);
        var array = new int[values.size()];

        for (var i = 0; i < array.length; i++) {
            array[i] = ((Number) values.get(i)).intValue();
        }

        return new IntArrayTag(array);
    }

    public static LongArrayTag longArrayTag(Object value) {
        var values = ValueUtils.listOf(value);
        var array = new long[values.size()];

        for (var i = 0; i < array.length; i++) {
            array[i] = ((Number) values.get(i)).longValue();
        }

        return new LongArrayTag(array);
    }

    /**
     * Creates a list tag from a list of values.
     *
     * @param value a list or array
     * @return the tag
     */
    public static ListTag listTag(Object value) {
        var list = new ListTag();

        for (var element : ValueUtils.listOf(value)) {
            var tag = of(element);

            if (tag != null) {
                list.add(tag);
            }
        }

        return list;
    }
}
