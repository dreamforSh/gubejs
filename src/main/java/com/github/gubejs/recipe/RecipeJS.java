/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipeJS.java
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
     * <p>Worth doing for anything a pack expects to find again 鈥?an advancement, a recipe book
     * unlock, or a later script that removes it 鈥?because the generated name is derived from the
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
     * <p>The escape hatch for recipe types with options this mod does not model 鈥?
     * {@code .set('processingTime', 200)}.
     *
     * @param key the JSON key
     * @param value the value, converted the same way {@code JsonIO} converts anything
     * @return this recipe
     */
    public RecipeJS set(String key, @Nullable Object value) {
        json.add(key, JsonUtils.of(value));
        event.countModified(id);
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

        event.countModified(id);
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
    public RecipeJS xp(double xp) {
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
     * Puts the recipe behind a pack stage.
     *
     * <p>Written as a Forge recipe condition, so the recipe is not loaded at all until the stage is
     * set, rather than being hidden at craft time. The condition is
     * {@link StageCondition this mod's own}, so nothing else needs installing.
     *
     * <p>The stage is {@link com.github.gubejs.core.PackStages}, which is the whole pack's, not
     * {@code player.stages}. A condition is asked once as the recipe is read and there is no player
     * to ask about 鈥?a recipe loads for everyone or for no one.
     *
     * @param stage the stage name
     * @return this recipe
     */
    public RecipeJS stage(String stage) {
        var condition = new JsonObject();
        condition.addProperty("type", StageCondition.ID.toString());
        condition.addProperty("stage", stage);

        var conditions = json.has("conditions") && json.get("conditions").isJsonArray()
            ? json.getAsJsonArray("conditions") : new JsonArray();
        conditions.add(condition);
        json.add("conditions", conditions);
        event.countModified(id);
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

    // --- rewriting ---------------------------------------------------------------------------

    /**
     * Replaces one ingredient of this recipe with another.
     *
     * <p>The single-recipe form of {@code event.replaceInput}, for when the recipes to change are
     * easier to pick out in JavaScript than to describe as a filter:
     *
     * <pre>{@code
     * event.forEachRecipe({ mod: 'create' }, r => {
     *     if (r.id.path.includes('crushing')) {
     *         r.replaceInput('minecraft:iron_ore', '#forge:ores/iron')
     *     }
     * })
     * }</pre>
     *
     * @param from the ingredient to look for, as an item id or a {@code #tag}
     * @param to what to put in its place
     * @return this recipe
     */
    public RecipeJS replaceInput(Object from, Object to) {
        if (event.replaceIn(json, from, to, false)) {
            event.countModified(id);
        }

        return this;
    }

    /**
     * Replaces one of this recipe's results with another.
     *
     * @param from the item to look for
     * @param to what to put in its place
     * @return this recipe
     */
    public RecipeJS replaceOutput(Object from, Object to) {
        if (event.replaceIn(json, from, to, true)) {
            event.countModified(id);
        }

        return this;
    }

    /**
     * Whether this recipe consumes an item.
     *
     * <p>A tag is matched as written, not as what it expands to: a recipe asking for
     * {@code #forge:ingots/iron} does not answer to {@code hasInput('minecraft:iron_ingot')}, since
     * at this point the recipe is still JSON and the tag has not been resolved. Ask for the tag.
     *
     * @param ingredient an item id, a {@code #tag}, or a list of either
     * @return whether any of them appears among the recipe's inputs
     */
    public boolean hasInput(@Nullable Object ingredient) {
        return RecipeFilter.contains(json, ingredient, false);
    }

    /**
     * Whether this recipe produces an item.
     *
     * @param item an item id or a list of ids
     * @return whether any of them appears among the recipe's results
     */
    public boolean hasOutput(@Nullable Object item) {
        return RecipeFilter.contains(json, item, true);
    }

    // --- modifiers ---------------------------------------------------------------------------

    /**
     * Leaves an ingredient in the grid instead of consuming it.
     *
     * <pre>{@code
     * event.shaped('minecraft:bread', ['WWW'], { W: 'minecraft:wheat' })
     *     .keepIngredient('minecraft:wheat')
     * }</pre>
     *
     * @param ingredient which ingredient, as an id, a {@code #tag} or a list
     * @return this recipe
     */
    public RecipeJS keepIngredient(@Nullable Object ingredient) {
        return action("keep", ingredient, null);
    }

    /**
     * Damages an ingredient instead of consuming it, the way a crafting tool works.
     *
     * @param ingredient which ingredient
     * @return this recipe
     */
    public RecipeJS damageIngredient(@Nullable Object ingredient) {
        return damageIngredient(ingredient, 1);
    }

    /**
     * Damages an ingredient by an amount instead of consuming it.
     *
     * <p>An ingredient with no durability is kept instead of being damaged, since damaging it
     * would mean consuming it 鈥?the opposite of what the recipe asked for.
     *
     * @param ingredient which ingredient
     * @param amount how much durability it loses
     * @return this recipe
     */
    public RecipeJS damageIngredient(@Nullable Object ingredient, int amount) {
        var action = actionFor("damage", ingredient);
        action.addProperty("amount", amount);
        return addAction(action);
    }

    /**
     * Leaves a different item in the grid in an ingredient's place.
     *
     * @param ingredient which ingredient
     * @param with what to leave behind
     * @return this recipe
     */
    public RecipeJS replaceIngredient(@Nullable Object ingredient, @Nullable Object with) {
        return action("replace", ingredient, with);
    }

    /**
     * Consumes an ingredient outright, whatever it would rather leave behind.
     *
     * <pre>{@code
     * event.shaped('mypack:cake', ['MMM'], { M: 'minecraft:milk_bucket' })
     *     .consumeIngredient('minecraft:milk_bucket')     // no empty buckets back
     * }</pre>
     *
     * <p>The opposite of {@link #keepIngredient}, and not the same as saying nothing: a bucket, a
     * bottle or a modded container leaves its remainder in the grid by default.
     *
     * @param ingredient which ingredient, as an id, a {@code #tag} or a list
     * @return this recipe
     */
    public RecipeJS consumeIngredient(@Nullable Object ingredient) {
        return action("consume", ingredient, null);
    }

    /**
     * Lets a function decide what an ingredient leaves in the grid.
     *
     * <pre>{@code
     * event.shaped('mypack:etched_plate', ['SP'], { S: 'mypack:stamp', P: '#forge:plates' })
     *     .customIngredientAction('mypack:stamp', (stack, original) => {
     *         return stack.count > 1 ? stack.withCount(stack.count - 1) : original
     *     })
     * }</pre>
     *
     * <p>The escape hatch for the case none of the four named actions covers. Server-side only, for
     * the same reason {@link #modifyResult} is: what stays in the grid is decided where the craft
     * happens, and the client is told the answer rather than working it out.
     *
     * @param ingredient which ingredient
     * @param function takes what is in the slot and what would otherwise be left, returns what to
     *     leave
     * @return this recipe
     */
    public RecipeJS customIngredientAction(@Nullable Object ingredient,
                                           org.graalvm.polyglot.Value function) {
        var action = actionFor("custom", ingredient);
        action.addProperty("callback", RecipeCallbacks.register(function));
        return addAction(action);
    }

    /**
     * Runs a function over the result before it is handed to the player.
     *
     * <pre>{@code
     * event.shaped('minecraft:diamond_sword', ...).modifyResult((result, grid) => {
     *     return result.withNbt({ display: { Name: '{"text":"Sharp"}' } })
     * })
     * }</pre>
     *
     * <p>Server-side only, which is where a crafting result is decided anyway. A client reading the
     * same recipe has no such function and shows what the recipe underneath produces.
     *
     * @param function takes the result and the crafting grid, returns the new result
     * @return this recipe
     */
    public RecipeJS modifyResult(org.graalvm.polyglot.Value function) {
        modifiers().addProperty("modify_result", RecipeCallbacks.register(function));
        event.countModified(id);
        return this;
    }

    /**
     * Stops a shaped recipe matching its pattern flipped left-to-right.
     *
     * <p>Vanilla tries both ways round, so a recipe where left and right mean different things
     * cannot be written without this.
     *
     * @return this recipe
     */
    public RecipeJS noMirror() {
        modifiers().addProperty("no_mirror", true);
        event.countModified(id);
        return this;
    }

    /**
     * Keeps the blank rows and columns around a shaped recipe's pattern.
     *
     * <p>Vanilla trims them while reading, so {@code ['   ', ' A ', '   ']} becomes a one-cell
     * recipe that matches anywhere in the grid. With this the position is part of the recipe.
     *
     * @return this recipe
     */
    public RecipeJS noShrink() {
        modifiers().addProperty("no_shrink", true);
        event.countModified(id);
        return this;
    }

    private RecipeJS action(String type, @Nullable Object ingredient, @Nullable Object with) {
        var action = actionFor(type, ingredient);

        if (with != null) {
            action.add("with", RecipeJson.result(with));
        }

        return addAction(action);
    }

    private JsonObject actionFor(String type, @Nullable Object ingredient) {
        var action = new JsonObject();
        action.addProperty("type", type);
        action.add("ingredient", RecipeJson.ingredient(ingredient));
        return action;
    }

    private RecipeJS addAction(JsonObject action) {
        var wrapper = modifiers();
        var actions = wrapper.has("actions") && wrapper.get("actions").isJsonArray()
            ? wrapper.getAsJsonArray("actions") : new JsonArray();
        actions.add(action);
        wrapper.add("actions", actions);
        event.countModified(id);
        return this;
    }

    /**
     * Turns this recipe into a wrapper around itself, if it is not one already.
     *
     * <p>Edited in place rather than replaced, because the event's map holds this exact object and
     * swapping it for a new one would leave the map pointing at the unwrapped recipe.
     *
     * @return the wrapper's JSON, which is this recipe's JSON
     */
    private JsonObject modifiers() {
        if ("gubejs:modified".equals(getType())) {
            return json;
        }

        var inner = json.deepCopy();

        for (var key : new java.util.ArrayList<>(json.keySet())) {
            json.remove(key);
        }

        json.addProperty("type", "gubejs:modified");
        json.add("recipe", inner);
        return json;
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
