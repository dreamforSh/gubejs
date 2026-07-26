package com.github.gubejs.recipe;

import com.github.gubejs.bindings.UtilsWrapper;
import com.github.gubejs.util.JsonUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One recipe, as a script sees it.
 *
 * <p>Returned by every {@code event.recipes.*} call so the script can keep configuring it, and
 * handed out by {@code event.forEachRecipe} for the recipes that were already there:
 *
 * <pre>{@code
 * event.recipes.minecraft.crafting_shaped('minecraft:chest', ['SSS', 'S S', 'SSS'], {
 *     S: '#minecraft:planks'
 * }).id('mypack:wooden_chest')
 * }</pre>
 *
 * <p>Nothing is deserialised here. The recipe stays JSON until the game reads it, which is what
 * lets a script build and edit a recipe for a type this mod knows nothing about.
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
        var parsed = ResourceLocation.tryParse(UtilsWrapper.gubejsId(String.valueOf(newId)));

        if (parsed != null && !parsed.equals(id)) {
            event.rename(this.id, parsed);
            this.id = parsed;
        }

        return this;
    }

    /**
     * Sets any other key in the recipe's JSON.
     *
     * <p>The escape hatch for recipe types with options this mod does not model —
     * {@code .set('processingTime', 200)}.
     *
     * @param key the JSON key
     * @param value the value, converted the same way {@code JsonIO} converts anything
     * @return this recipe
     */
    public RecipeJS set(String key, @Nullable Object value) {
        json.add(key, JsonUtils.of(value));
        event.countModified();
        return this;
    }

    /**
     * Copies every key of an object into the recipe.
     *
     * @param values the keys to set
     * @return this recipe
     */
    public RecipeJS merge(@Nullable Object values) {
        for (var entry : JsonUtils.objectOf(values).entrySet()) {
            json.add(entry.getKey(), entry.getValue());
        }

        event.countModified();
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
     * Sets how much experience a cooking recipe gives.
     *
     * @param xp the experience
     * @return this recipe
     */
    public RecipeJS xp(float xp) {
        return set("experience", xp);
    }

    /**
     * Sets how long a cooking recipe takes.
     *
     * @param ticks the time in ticks
     * @return this recipe
     */
    public RecipeJS cookingTime(int ticks) {
        return set("cookingtime", ticks);
    }

    /**
     * Puts the recipe behind a game stage.
     *
     * <p>Written as a Forge recipe condition, so the recipe is dropped at load time for a player
     * without the stage rather than hidden at craft time. Needs GameStages installed to supply the
     * condition; {@code event.stage(filter, name)} checks for that before calling this.
     *
     * @param stage the stage name
     * @return this recipe
     */
    public RecipeJS stage(String stage) {
        var condition = new JsonObject();
        condition.addProperty("type", "gamestages:stage");
        condition.addProperty("stage", stage);

        var conditions = json.has("conditions") && json.get("conditions").isJsonArray()
            ? json.getAsJsonArray("conditions") : new JsonArray();
        conditions.add(condition);
        json.add("conditions", conditions);
        event.countModified();
        return this;
    }

    /**
     * Removes the recipe.
     *
     * <p>For the recipes {@code event.forEachRecipe} hands out, when the condition that picks them
     * is easier to write in JavaScript than as a filter.
     */
    public void remove() {
        event.removeById(id);
    }

    /**
     * Reads a key out of the recipe's JSON, as plain JavaScript values.
     *
     * @param key the JSON key
     * @return the value, or {@code null} if the recipe has no such key
     */
    @Nullable
    public Object get(String key) {
        return JsonUtils.toObject(json.get(key));
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
     * Returns the recipe's type, which is what its {@code type} key says.
     *
     * @return the type, or an empty string for a recipe with no type
     */
    public String getType() {
        return json.has("type") ? json.get("type").getAsString() : "";
    }

    /**
     * Returns the recipe's JSON, for a script that wants to inspect or patch it directly.
     *
     * @return the live JSON object
     */
    public JsonObject getJson() {
        return json;
    }

    /**
     * Returns the recipe as plain JavaScript values, for reading with {@code .} and {@code []}.
     *
     * @return a map of the recipe's keys
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getData() {
        return (Map<String, Object>) JsonUtils.toObject(json);
    }

    @Override
    public String toString() {
        return id + " " + json;
    }
}
