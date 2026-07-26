package com.github.gubejs.item;

import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonElement;
import java.util.ArrayList;
import java.util.Map;
import java.util.stream.Stream;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Reads the several ways a script can name a recipe input.
 *
 * <pre>{@code
 * 'minecraft:diamond'          // one item
 * '#forge:ingots/iron'         // everything in a tag
 * ['#forge:rods/wooden', 'minecraft:stick']   // any of these
 * { tag: 'forge:ingots/iron' }
 * '*'                          // anything at all
 * }</pre>
 */
public final class IngredientJS {

    private IngredientJS() {
    }

    /**
     * Reads an ingredient from whatever a script passed.
     *
     * @param value a string, an array, an object, an {@link Ingredient} already, or {@code null}
     * @return the ingredient, {@link Ingredient#EMPTY} when the value names nothing
     */
    public static Ingredient of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return Ingredient.EMPTY;
        } else if (unwrapped instanceof Ingredient ingredient) {
            return ingredient;
        } else if (unwrapped instanceof ItemStack stack) {
            return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
        } else if (unwrapped instanceof ItemLike itemLike) {
            return Ingredient.of(itemLike);
        } else if (unwrapped instanceof TagKey<?> tag) {
            return ofTag(castTag(tag));
        } else if (unwrapped instanceof CharSequence text) {
            return parse(text.toString());
        } else if (unwrapped instanceof Iterable<?> || unwrapped instanceof Object[]) {
            return union(ValueUtils.listOf(unwrapped));
        } else if (unwrapped instanceof Map<?, ?> map) {
            return fromMap(map);
        } else if (unwrapped instanceof JsonElement json) {
            return Ingredient.fromJson(json);
        }

        ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Not an ingredient: " + unwrapped);
        return Ingredient.EMPTY;
    }

    /**
     * Reports whether a string names something that can be an ingredient, quietly.
     *
     * @param text the text to test
     * @return {@code true} if {@link #parse} would produce an ingredient
     */
    public static boolean looksLikeIngredient(String text) {
        var s = text.trim();
        return s.isEmpty() || s.equals("*") || s.startsWith("#") || s.startsWith("@")
            || ItemStackJS.looksLikeItem(s);
    }

    /**
     * Parses the string form.
     *
     * @param text an item id, a {@code #tag}, or {@code *}
     * @return the ingredient
     */
    public static Ingredient parse(String text) {
        var s = text.trim();

        if (s.isEmpty() || s.equals("-") || s.equals("minecraft:air") || s.equals("air")) {
            return Ingredient.EMPTY;
        } else if (s.equals("*")) {
            return all();
        } else if (s.startsWith("#")) {
            var id = ResourceLocation.tryParse(s.substring(1));

            if (id == null) {
                ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn("Not a tag id: '" + s + "'");
                return Ingredient.EMPTY;
            }

            return ofTag(TagKey.create(Registry.ITEM_REGISTRY, id));
        } else if (s.startsWith("@")) {
            return ofMod(s.substring(1));
        }

        var stack = ItemStackJS.parse(s);
        return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
    }

    /**
     * Builds an ingredient matching everything in a tag.
     *
     * @param tag the tag to match
     * @return the ingredient
     */
    public static Ingredient ofTag(TagKey<Item> tag) {
        return Ingredient.of(tag);
    }

    /**
     * Builds an ingredient matching every item registered by one mod.
     *
     * <p>Expanded to a fixed list here rather than kept as a live predicate, so the result is an
     * ordinary vanilla ingredient that any recipe type and any recipe viewer understands.
     *
     * @param namespace the mod id
     * @return the ingredient, empty if the mod registered no items
     */
    public static Ingredient ofMod(String namespace) {
        return fromStacks(ForgeRegistries.ITEMS.getEntries().stream()
            .filter(e -> e.getKey().location().getNamespace().equals(namespace))
            .map(e -> new ItemStack(e.getValue())));
    }

    /**
     * Builds an ingredient matching every item there is, minus air.
     *
     * @return the ingredient
     */
    public static Ingredient all() {
        return fromStacks(ForgeRegistries.ITEMS.getValues().stream()
            .filter(item -> item != net.minecraft.world.item.Items.AIR)
            .map(ItemStack::new));
    }

    /**
     * Combines several ingredients into one that matches any of them.
     *
     * @param values the ingredients to combine
     * @return the combined ingredient
     */
    public static Ingredient union(Iterable<?> values) {
        var parts = new ArrayList<Ingredient.Value>();

        for (var value : values) {
            var ingredient = of(value);

            if (!ingredient.isEmpty()) {
                // Merging the value lists rather than nesting ingredients: a nested one would need
                // Forge's compound ingredient, which not every recipe serialiser round-trips.
                for (var stack : ingredient.getItems()) {
                    parts.add(new Ingredient.ItemValue(stack));
                }
            }
        }

        return parts.isEmpty() ? Ingredient.EMPTY : Ingredient.fromValues(parts.stream());
    }

    private static Ingredient fromMap(Map<?, ?> map) {
        if (map.containsKey("tag")) {
            var id = ResourceLocation.tryParse(String.valueOf(map.get("tag")));
            return id == null ? Ingredient.EMPTY : ofTag(TagKey.create(Registry.ITEM_REGISTRY, id));
        } else if (map.containsKey("item") || map.containsKey("id")) {
            var stack = ItemStackJS.of(map);
            return stack.isEmpty() ? Ingredient.EMPTY : Ingredient.of(stack);
        } else if (map.containsKey("mod")) {
            return ofMod(String.valueOf(map.get("mod")));
        }

        ConsoleJS.getCurrent(ConsoleJS.STARTUP)
            .warn("An ingredient object needs an 'item', 'tag' or 'mod' key: " + map);
        return Ingredient.EMPTY;
    }

    private static Ingredient fromStacks(Stream<ItemStack> stacks) {
        var values = stacks.<Ingredient.Value>map(Ingredient.ItemValue::new).toList();
        return values.isEmpty() ? Ingredient.EMPTY : Ingredient.fromValues(values.stream());
    }

    @SuppressWarnings("unchecked")
    private static TagKey<Item> castTag(TagKey<?> tag) {
        return (TagKey<Item>) tag;
    }
}
