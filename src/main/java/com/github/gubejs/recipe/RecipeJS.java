package com.github.gubejs.recipe;

import com.github.gubejs.util.JsonUtils;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * A recipe a script created, before it is handed to the game.
 *
 * <p>Returned by every {@code event.shaped(...)}-style call so the script can keep configuring it:
 *
 * <pre>{@code
 * event.shaped('minecraft:chest', ['SSS', 'S S', 'SSS'], { S: '#minecraft:planks' })
 *     .id('mypack:wooden_chest')
 * }</pre>
 *
 * <p>Nothing is deserialised here. The recipe stays JSON until the game reads it, which is what
 * lets a script build a recipe for a type this mod knows nothing about.
 */
public final class RecipeJS {

    private final RecipesEventJS event;

    private final JsonObject json;

    private ResourceLocation id;

    RecipeJS(RecipesEventJS event, JsonObject json, ResourceLocation id) {
        this.event = event;
        this.json = json;
        this.id = id;
    }

    /**
     * Renames the recipe.
     *
     * <p>Worth doing for anything a pack expects to find again — an advancement, a recipe book
     * unlock, or a later script that removes it — because the generated name is derived from the
     * output and changes if the output does.
     *
     * @param newId the id to use, with {@code gubejs:} assumed when no namespace is given
     * @return this recipe
     */
    public RecipeJS id(Object newId) {
        var parsed = ResourceLocation.tryParse(
            com.github.gubejs.bindings.UtilsWrapper.gubejsId(String.valueOf(newId)));

        if (parsed != null) {
            event.rename(this.id, parsed);
            this.id = parsed;
        }

        return this;
    }

    /**
     * Sets any other key in the recipe's JSON.
     *
     * <p>The escape hatch for recipe types with options this mod does not model —
     * {@code .set('experience', 0.7)}.
     *
     * @param key the JSON key
     * @param value the value, converted the same way {@code JsonIO} converts anything
     * @return this recipe
     */
    public RecipeJS set(String key, @Nullable Object value) {
        json.add(key, JsonUtils.of(value));
        return this;
    }

    /**
     * Sets the group recipes are stacked under in the recipe book.
     *
     * @param group the group name
     * @return this recipe
     */
    public RecipeJS group(String group) {
        return set("group", group);
    }

    /**
     * Returns the recipe's id.
     *
     * @return the id
     */
    public ResourceLocation getId() {
        return id;
    }

    /**
     * Returns the recipe's JSON, for a script that wants to inspect or patch it directly.
     *
     * @return the live JSON object
     */
    public JsonObject getJson() {
        return json;
    }

    @Override
    public String toString() {
        return id + " " + json;
    }
}
