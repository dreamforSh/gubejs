package com.github.gubejs.bindings;

import com.github.gubejs.item.IngredientJS;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Ingredient} global, for recipe inputs.
 *
 * <pre>{@code
 * Ingredient.of('#forge:ingots/iron')
 * Ingredient.of(['minecraft:stick', 'minecraft:bone'])
 * Ingredient.all
 * }</pre>
 */
public final class IngredientWrapper {

    private IngredientWrapper() {
    }

    /**
     * Builds an ingredient.
     *
     * @param value anything that names one or more items
     * @return the ingredient, empty if the value names nothing
     */
    public static Ingredient of(@Nullable Object value) {
        return IngredientJS.of(value);
    }

    /**
     * Returns an ingredient matching nothing.
     *
     * @return the empty ingredient
     */
    public static Ingredient getNone() {
        return Ingredient.EMPTY;
    }

    /**
     * Returns an ingredient matching every item there is.
     *
     * @return the ingredient
     */
    public static Ingredient getAll() {
        return IngredientJS.all();
    }

    /**
     * Returns an ingredient matching every item from one mod.
     *
     * @param modId the mod id
     * @return the ingredient
     */
    public static Ingredient ofMod(String modId) {
        return IngredientJS.ofMod(modId);
    }

    /**
     * Returns an ingredient matching every item in a tag.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @return the ingredient
     */
    public static Ingredient ofTag(String tag) {
        return IngredientJS.parse(tag.startsWith("#") ? tag : "#" + tag);
    }
}
