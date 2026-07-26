package com.github.gubejs.recipe;

import com.github.gubejs.item.IngredientJS;
import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Turns what a script writes into the JSON a recipe serialiser reads.
 *
 * <p>Every recipe type spells its ingredients and its results the same way, because they all go
 * through the vanilla {@code Ingredient} codec and {@code ShapedRecipe.itemStackFromJson}. That is
 * what lets one pair of methods serve a recipe type this mod has never heard of.
 */
public final class RecipeJson {

    private RecipeJson() {
    }

    /**
     * Builds the {@code {"item": ...}} or {@code {"tag": ...}} form of an ingredient.
     *
     * <p>A count is written out when there is one. Vanilla's ingredient codec ignores keys it does
     * not know, so it costs nothing there, and the modded serialisers that support stacked inputs
     * read exactly this key.
     *
     * @param value an id, a {@code #tag}, a list of either, an {@link Ingredient}, or an object
     * @return the ingredient JSON
     */
    public static JsonElement ingredient(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return new JsonObject();
        } else if (unwrapped instanceof JsonElement json) {
            return json;
        } else if (unwrapped instanceof Ingredient ingredient) {
            return ingredient.toJson();
        } else if (unwrapped instanceof CharSequence text) {
            return fromString(text.toString().trim());
        } else if (unwrapped instanceof ItemStack stack) {
            return fromStack(stack, false);
        } else if (unwrapped instanceof List<?> list) {
            var array = new JsonArray();
            list.forEach(v -> array.add(ingredient(v)));
            return array;
        } else if (unwrapped instanceof Map<?, ?> map) {
            return JsonUtils.of(map);
        }

        // Anything else names an item -- an Item, a Block, a builder. Going through IngredientJS
        // rather than ItemStackJS keeps tag objects and the other Ingredient shapes working.
        var ingredient = IngredientJS.of(unwrapped);
        return ingredient == Ingredient.EMPTY ? new JsonObject() : ingredient.toJson();
    }

    /**
     * Builds the {@code {"item": ..., "count": n}} form of a result.
     *
     * @param value an id with an optional count and NBT, an {@link ItemStack}, or an object
     * @return the result JSON
     */
    public static JsonElement result(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof JsonElement json) {
            return json;
        } else if (unwrapped instanceof Map<?, ?> map) {
            return JsonUtils.of(map);
        }

        return fromStack(ItemStackJS.of(unwrapped), true);
    }

    /**
     * Builds the bare-id form of a result, which cooking and stonecutting use.
     *
     * <p>Those two read their result with {@code ShapedRecipe.itemFromJson}, which wants a string
     * and rejects an object — so a count or NBT written there has to be dropped, and the count is
     * carried by the recipe's own {@code count} key instead.
     *
     * @param value what names the item
     * @return the item id, {@code minecraft:air} when the value names nothing
     */
    public static String itemId(@Nullable Object value) {
        var stack = ItemStackJS.of(value);
        return stack.isEmpty() ? "minecraft:air"
            : String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /**
     * Normalises an id or {@code #tag} for comparison, adding the {@code minecraft:} a script
     * usually leaves off.
     *
     * @param value an id or a tag
     * @return the normalised form, with the {@code #} kept
     */
    public static String idOf(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof CharSequence text) {
            var s = text.toString().trim();

            if (s.startsWith("#")) {
                var id = s.substring(1);
                return "#" + (id.indexOf(':') == -1 ? "minecraft:" + id : id);
            }
        }

        return itemId(unwrapped);
    }

    /** Reads the {@code '#tag'}, {@code '4x id'} and plain-id spellings. */
    private static JsonElement fromString(String text) {
        var json = new JsonObject();

        if (text.startsWith("#")) {
            json.addProperty("tag", withNamespace(text.substring(1)));
            return json;
        }

        return fromStack(ItemStackJS.parse(text), false);
    }

    /**
     * Writes a stack out, with the count and NBT when they carry information.
     *
     * @param withNbt whether NBT belongs in the output — an ingredient's does not, since vanilla's
     *     codec has no field for it and a modded one that does reads it from its own key
     */
    private static JsonElement fromStack(ItemStack stack, boolean withNbt) {
        var json = new JsonObject();

        if (stack.isEmpty()) {
            json.addProperty("item", "minecraft:air");
            return json;
        }

        json.addProperty("item", String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));

        if (stack.getCount() > 1) {
            json.addProperty("count", stack.getCount());
        }

        if (withNbt && stack.hasTag()) {
            // Vanilla's crafting result has no NBT field; Forge's does, and every recipe viewer
            // reads it, so a pack that wants NBT output gets it wherever it is supported.
            json.addProperty("nbt", String.valueOf(stack.getTag()));
        }

        return json;
    }

    private static String withNamespace(String id) {
        return id.indexOf(':') == -1 ? "minecraft:" + id : id;
    }
}
