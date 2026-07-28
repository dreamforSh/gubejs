/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipesEventJS.java
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

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code ServerEvents.recipes}: add, remove and rewrite recipes.
 *
 * <p>Works on the JSON the game is about to read, not on deserialised recipe objects. That is the
 * one significant design difference from KubeJS, and it is deliberate:
 *
 * <ul>
 *   <li>A modded recipe type works without this mod knowing anything about it. KubeJS needs a
 *       schema per recipe type and falls back to a generic one that cannot see inputs or outputs;
 *       here {@code replaceInput} reaches into any recipe that spells its ingredients the way the
 *       vanilla codec reads them, which every serialiser does.
 *   <li>Nothing is deserialised twice. The game parses each recipe exactly once, afterwards.
 *   <li>A recipe a script adds is indistinguishable from one in a datapack, so recipe viewers,
 *       advancements and the recipe book all behave normally.
 * </ul>
 *
 * <p>What is given up is matching on a deserialised recipe's own fields — a filter can only see
 * what the JSON says. In practice that is what a pack filters on anyway.
 */
public final class RecipesEventJS extends EventJS {

    private final Map<ResourceLocation, JsonElement> recipes;

    private final Map<ResourceLocation, JsonElement> added = new LinkedHashMap<>();

    private final RecipeFunctions functions = new RecipeFunctions(this);

    /**
     * Schemas worked out from the recipes already loaded, so the search happens once per type.
     *
     * <p>Holds a null-valued entry for a type nothing could be learned from, which is what keeps
     * the fallback warning to one per type rather than one per recipe.
     */
    private final Map<ResourceLocation, RecipeSchema> inferred = new HashMap<>();

    private int removedCount;

    private int modifiedCount;

    public RecipesEventJS(Map<ResourceLocation, JsonElement> recipes) {
        this.recipes = recipes;

        // The modifyResult callbacks belong to the run of scripts that is about to happen. The
        // previous run's functions belong to a context that has been closed, and a recipe still
        // carrying one of their numbers would call into it.
        RecipeCallbacks.clear();
    }

    // --- recipe types ------------------------------------------------------------------------

    /**
     * Returns every recipe type, addressed by mod and name.
     *
     * <pre>{@code
     * event.recipes.minecraft.crafting_shaped('minecraft:chest', ['SSS', 'S S', 'SSS'], {
     *     S: '#minecraft:planks'
     * })
     * }</pre>
     *
     * @return the recipe type namespaces
     */
    public RecipeFunctions getRecipes() {
        return functions;
    }

    /**
     * Adds a recipe of the given type from the arguments a script passed.
     *
     * @param type the recipe type
     * @param args the positional arguments
     * @return the recipe, for renaming and further configuration
     */
    public RecipeJS addFromSchema(ResourceLocation type, List<Object> args) {
        var schema = RecipeSchema.find(type);

        if (schema == null) {
            schema = inferSchema(type);
        }

        var json = schema.build(type, args);
        json.addProperty("type", type.toString());
        return register(json, guessId(json, type));
    }

    /**
     * Works out how an unknown recipe type spells itself, from one that is already loaded.
     *
     * <p>A modded recipe type is a type this mod has never seen, but the mod's own datapack is
     * full of examples of it. Reading one tells us which key holds the result and which holds the
     * ingredients, which is the whole of what a schema is for.
     *
     * @param type the recipe type
     * @return a schema for it, never {@code null} — {@link RecipeSchema#GENERIC} when nothing
     *     could be learned
     */
    private RecipeSchema inferSchema(ResourceLocation type) {
        if (inferred.containsKey(type)) {
            var cached = inferred.get(type);
            return cached == null ? RecipeSchema.GENERIC : cached;
        }

        RecipeSchema learned = null;
        var id = type.toString();

        for (var value : recipes.values()) {
            if (value instanceof JsonObject object && object.has("type")
                && id.equals(withNamespace(object.get("type").getAsString()))) {
                learned = fromSample(type, object);
                break;
            }
        }

        if (learned == null) {
            var known = ForgeRegistries.RECIPE_SERIALIZERS.containsKey(type);
            ConsoleJS.SERVER.warn(known
                ? "No recipe schema for '" + id + "' and no example to learn one from; "
                + "writing results and ingredients under their usual names. "
                + "Pass a whole JSON object instead if that is wrong."
                : "Unknown recipe type '" + id + "'. Nothing will read the recipe unless the mod "
                + "that owns it is installed.");
        }

        inferred.put(type, learned);
        return learned == null ? RecipeSchema.GENERIC : learned;
    }

    /** The keys a serialiser is likely to put its ingredients under. */
    private static final List<String> INGREDIENT_KEYS =
        List.of("ingredient", "ingredients", "input", "inputs", "item", "items");

    /**
     * Builds a schema from one recipe of the same type.
     *
     * @param type the type, for the log line
     * @param sample a recipe already loaded
     * @return the schema
     */
    private static RecipeSchema fromSample(ResourceLocation type, JsonObject sample) {
        String resultKey = null;
        String ingredientKey = null;

        for (var key : RecipeJson.RESULT_KEYS) {
            if (sample.has(key)) {
                resultKey = key;
                break;
            }
        }

        for (var key : INGREDIENT_KEYS) {
            if (sample.has(key)) {
                ingredientKey = key;
                break;
            }
        }

        var resultIsArray = resultKey != null && sample.get(resultKey).isJsonArray();
        var ingredientIsArray = ingredientKey != null && sample.get(ingredientKey).isJsonArray();

        ConsoleJS.SERVER.debug("Learned the shape of '" + type + "' from an existing recipe: "
            + "result -> " + resultKey + ", ingredients -> " + ingredientKey);

        var finalResultKey = resultKey;
        var finalIngredientKey = ingredientKey;

        return (t, args) -> {
            // One object argument still means "this is the whole recipe", whatever was learned.
            if (args.size() == 1 && ValueUtils.unwrap(args.get(0)) instanceof Map<?, ?>) {
                return JsonUtils.objectOf(args.get(0));
            }

            var json = new JsonObject();

            if (finalResultKey != null && !args.isEmpty()) {
                json.add(finalResultKey, resultIsArray
                    ? jsonArray(args.get(0), RecipeJson::result)
                    : RecipeJson.result(args.get(0)));
            }

            if (finalIngredientKey != null && args.size() > 1) {
                json.add(finalIngredientKey, ingredientIsArray
                    ? jsonArray(args.get(1), RecipeJson::ingredient)
                    : RecipeJson.ingredient(args.get(1)));
            }

            return json;
        };
    }

    private static JsonArray jsonArray(@Nullable Object value,
                                       java.util.function.Function<Object, JsonElement> converter) {
        var array = new JsonArray();
        ValueUtils.listOf(value).forEach(v -> array.add(converter.apply(v)));
        return array;
    }

    // --- removing ----------------------------------------------------------------------------

    /**
     * Removes every recipe matching a filter.
     *
     * <pre>{@code
     * event.remove({ output: 'minecraft:stick' })
     * event.remove({ mod: 'minecraft', type: 'minecraft:crafting_shaped' })
     * event.remove({ input: '#forge:ingots/iron' })
     * }</pre>
     *
     * @param filter what to remove
     * @return how many recipes were removed
     */
    public int remove(@Nullable Object filter) {
        var parsed = RecipeFilter.of(filter);
        var doomed = new ArrayList<ResourceLocation>();

        recipes.forEach((id, json) -> {
            if (parsed.test(id, json)) {
                doomed.add(id);
            }
        });

        if (com.github.gubejs.DevProperties.get().logRemovedRecipes) {
            doomed.forEach(id -> ConsoleJS.SERVER.info("- " + id + " (matched " + parsed + ")"));
        }

        doomed.forEach(recipes::remove);
        doomed.forEach(added::remove);
        removedCount += doomed.size();
        return doomed.size();
    }

    /**
     * Removes every recipe there is.
     *
     * <p>Almost always followed by adding back the ones a pack wants. Removing crafting entirely
     * and forgetting to is a common way to make a pack unplayable, so the count is logged.
     *
     * @return how many recipes were removed
     */
    public int removeAll() {
        var count = recipes.size();
        recipes.clear();
        added.clear();
        removedCount += count;
        return count;
    }

    // --- finding -----------------------------------------------------------------------------

    /**
     * Runs a function over every recipe matching a filter.
     *
     * <pre>{@code
     * event.forEachRecipe({ type: 'minecraft:crafting_shaped' }, recipe => {
     *     recipe.set('group', 'my_pack')
     * })
     * }</pre>
     *
     * @param filter which recipes, {@code {}} for all of them
     * @param consumer what to do with each
     */
    public void forEachRecipe(@Nullable Object filter, Consumer<RecipeJS> consumer) {
        for (var recipe : findRecipes(filter)) {
            consumer.accept(recipe);
        }
    }

    /**
     * Returns every recipe matching a filter, as objects a script can edit.
     *
     * @param filter which recipes
     * @return the matches
     */
    public Collection<RecipeJS> findRecipes(@Nullable Object filter) {
        var parsed = RecipeFilter.of(filter);
        var found = new ArrayList<RecipeJS>();

        // Over a copy of the keys: a script is free to rename or remove what it is handed, and
        // either would otherwise fail the iteration it happened during.
        for (var id : new ArrayList<>(recipes.keySet())) {
            var json = recipes.get(id);

            if (json instanceof JsonObject object && parsed.test(id, object)) {
                found.add(new RecipeJS(this, object, id));
            }
        }

        return found;
    }

    /**
     * Returns the ids of every recipe matching a filter.
     *
     * @param filter which recipes
     * @return the ids
     */
    public Collection<ResourceLocation> findRecipeIds(@Nullable Object filter) {
        var parsed = RecipeFilter.of(filter);
        var found = new ArrayList<ResourceLocation>();

        recipes.forEach((id, json) -> {
            if (parsed.test(id, json)) {
                found.add(id);
            }
        });

        return found;
    }

    /**
     * Counts the recipes matching a filter, without building objects for them.
     *
     * @param filter which recipes
     * @return how many match
     */
    public int countRecipes(@Nullable Object filter) {
        var parsed = RecipeFilter.of(filter);
        var count = 0;

        for (var entry : recipes.entrySet()) {
            if (parsed.test(entry.getKey(), entry.getValue())) {
                count++;
            }
        }

        return count;
    }

    /**
     * Reports whether any recipe matches a filter.
     *
     * @param filter which recipes
     * @return {@code true} if at least one matches
     */
    public boolean containsRecipe(@Nullable Object filter) {
        var parsed = RecipeFilter.of(filter);

        for (var entry : recipes.entrySet()) {
            if (parsed.test(entry.getKey(), entry.getValue())) {
                return true;
            }
        }

        return false;
    }

    // --- adding ------------------------------------------------------------------------------

    /**
     * Adds a recipe from raw JSON, exactly as a datapack file would spell it.
     *
     * @param json the recipe
     * @return the recipe, for renaming
     */
    public RecipeJS custom(@Nullable Object json) {
        var object = JsonUtils.objectOf(json);
        return register(object, guessId(object, null));
    }

    /**
     * Adds a shaped crafting recipe.
     *
     * <pre>{@code
     * event.shaped('minecraft:chest', ['SSS', 'S S', 'SSS'], { S: '#minecraft:planks' })
     * }</pre>
     *
     * @param output what it makes
     * @param pattern the rows of the grid
     * @param key what each character in the pattern stands for
     * @return the recipe
     */
    public RecipeJS shaped(Object output, Object pattern, Object key) {
        return addFromSchema(new ResourceLocation("minecraft", "crafting_shaped"),
            List.of(output, pattern, key));
    }

    /**
     * Adds a shapeless crafting recipe.
     *
     * @param output what it makes
     * @param inputs what goes in, in any arrangement
     * @return the recipe
     */
    public RecipeJS shapeless(Object output, Object inputs) {
        return addFromSchema(new ResourceLocation("minecraft", "crafting_shapeless"),
            List.of(output, inputs));
    }

    /**
     * Adds a furnace recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS smelting(Object output, Object input) {
        return addFromSchema(new ResourceLocation("minecraft", "smelting"), List.of(output, input));
    }

    /**
     * Adds a blast furnace recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS blasting(Object output, Object input) {
        return addFromSchema(new ResourceLocation("minecraft", "blasting"), List.of(output, input));
    }

    /**
     * Adds a smoker recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS smoking(Object output, Object input) {
        return addFromSchema(new ResourceLocation("minecraft", "smoking"), List.of(output, input));
    }

    /**
     * Adds a campfire recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS campfireCooking(Object output, Object input) {
        return addFromSchema(new ResourceLocation("minecraft", "campfire_cooking"),
            List.of(output, input));
    }

    /**
     * Adds a stonecutter recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS stonecutting(Object output, Object input) {
        return addFromSchema(new ResourceLocation("minecraft", "stonecutting"),
            List.of(output, input));
    }

    /**
     * Adds a smithing table recipe.
     *
     * @param output what it makes
     * @param base the item being upgraded
     * @param addition what upgrades it
     * @return the recipe
     */
    public RecipeJS smithing(Object output, Object base, Object addition) {
        return addFromSchema(new ResourceLocation("minecraft", "smithing"),
            List.of(output, base, addition));
    }

    // --- rewriting ---------------------------------------------------------------------------

    /**
     * Replaces one ingredient with another, everywhere a filter matches.
     *
     * <pre>{@code
     * event.replaceInput({}, '#forge:ingots/iron', 'minecraft:gold_ingot')
     * }</pre>
     *
     * @param filter which recipes to touch, {@code {}} for all of them
     * @param from the ingredient to look for, as an item id or a {@code #tag}
     * @param to what to put in its place
     * @return how many recipes changed
     */
    public int replaceInput(@Nullable Object filter, Object from, Object to) {
        return replace(filter, from, to, false);
    }

    /**
     * Replaces one result with another, everywhere a filter matches.
     *
     * @param filter which recipes to touch
     * @param from the item to look for
     * @param to what to put in its place
     * @return how many recipes changed
     */
    public int replaceOutput(@Nullable Object filter, Object from, Object to) {
        return replace(filter, from, to, true);
    }

    private int replace(@Nullable Object filter, Object from, Object to, boolean results) {
        var parsed = RecipeFilter.of(filter);
        var changed = 0;

        for (var entry : recipes.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject object) || !parsed.test(entry.getKey(), object)) {
                continue;
            }

            if (replaceIn(object, from, to, results)) {
                changed++;
                logModified(entry.getKey());
            }
        }

        modifiedCount += changed;
        return changed;
    }

    /**
     * Rewrites one side of a single recipe, in place.
     *
     * <p>Shared with {@link RecipeJS#replaceInput}, so a script rewriting one recipe it picked out
     * in JavaScript gets the same walk as a filtered {@code event.replaceInput} — including the
     * depth and the input/output line, which are the parts that are easy to get subtly wrong.
     *
     * @param results whether to rewrite the recipe's results rather than its inputs
     * @return whether anything was rewritten
     */
    boolean replaceIn(JsonObject object, Object from, Object to, boolean results) {
        var target = RecipeJson.idOf(from);
        var replacement = results ? RecipeJson.result(to) : RecipeJson.ingredient(to);
        var touched = false;

        for (var field : object.entrySet()) {
            if (field.getKey().equals("type")) {
                continue;
            }

            // Descended into rather than skipped, whichever direction this is: a field that is
            // not itself a result can still contain one -- a modded type nesting its whole
            // operation under a key of its own, or a recipe this mod has wrapped. Which half
            // gets rewritten is decided per subtree by rewrite(), from the keys on the way in.
            var rewritten = rewrite(field.getValue(), target, replacement,
                RecipeJson.RESULT_KEYS.contains(field.getKey()), results);

            if (rewritten != null) {
                field.setValue(rewritten);
                touched = true;
            }
        }

        return touched;
    }

    /**
     * Rewrites every occurrence of one id inside a JSON subtree, on one side of the recipe.
     *
     * <p>{@code inResult} is what keeps {@code replaceInput} off a recipe's output and
     * {@code replaceOutput} off its inputs. It is worked out on the way down rather than at the
     * top: a key from {@link RecipeJson#RESULT_KEYS} turns it on, and it stays on for everything
     * below — because {@code "result": {"item": "x"}} has an {@code item} key that is still part of
     * the result. Nothing turns it back off, since there is no such thing as an input nested inside
     * an output.
     *
     * @param inResult whether this subtree is part of the recipe's result
     * @param results which side the caller asked to rewrite
     * @return the replacement subtree, or {@code null} if nothing in it was rewritten
     */
    @Nullable
    private JsonElement rewrite(JsonElement element, String target, JsonElement replacement,
                                boolean inResult, boolean results) {
        if (element.isJsonPrimitive()) {
            // A bare id, which is how cooking and stonecutting results are written.
            var id = element.getAsString();

            if (inResult != results || !withNamespace(id).equals(target)) {
                return null;
            }

            return replacement.isJsonObject() && replacement.getAsJsonObject().has("item")
                ? replacement.getAsJsonObject().get("item") : replacement;
        } else if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            var touched = false;

            for (var i = 0; i < array.size(); i++) {
                var rewritten = rewrite(array.get(i), target, replacement, inResult, results);

                if (rewritten != null) {
                    array.set(i, rewritten);
                    touched = true;
                }
            }

            return touched ? array : null;
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();
            var key = target.startsWith("#") ? "tag" : "item";
            var wanted = target.startsWith("#") ? target.substring(1) : target;

            if (inResult == results && object.has(key) && object.get(key).isJsonPrimitive()
                && withNamespace(object.get(key).getAsString()).equals(wanted)) {
                // The whole ingredient object is replaced, so an item-to-tag swap works and the
                // count on a result is preserved from the replacement, not the original.
                return replacement.deepCopy();
            }

            var touched = false;

            for (var field : object.entrySet()) {
                var rewritten = rewrite(field.getValue(), target, replacement,
                    inResult || RecipeJson.RESULT_KEYS.contains(field.getKey()), results);

                if (rewritten != null) {
                    field.setValue(rewritten);
                    touched = true;
                }
            }

            return touched ? object : null;
        }

        return null;
    }

    /**
     * Puts every matching recipe behind a pack stage.
     *
     * <pre>{@code
     * event.stage({ output: 'minecraft:netherite_ingot' }, 'nether_open')
     * }</pre>
     *
     * <p>The condition written is {@link StageCondition this mod's own}, so no other mod has to be
     * installed. It gates on {@link com.github.gubejs.core.PackStages} — the whole pack's stages,
     * not one player's; see that class for why a recipe condition cannot be per player.
     *
     * @param filter which recipes
     * @param stage the stage name
     * @return how many recipes were staged
     */
    public int stage(@Nullable Object filter, String stage) {
        var count = 0;

        for (var recipe : findRecipes(filter)) {
            recipe.stage(stage);
            count++;
        }

        return count;
    }

    // --- plumbing ----------------------------------------------------------------------------

    /**
     * Returns the recipes as the game will read them, keyed by id.
     *
     * <p>The live map — removing from it removes the recipe. {@link #findRecipes} is the friendlier
     * way in; this is here for a script that wants to do something no filter expresses.
     *
     * @return the live map
     */
    public Map<ResourceLocation, JsonElement> getAllRecipes() {
        return recipes;
    }

    /**
     * Called once every listener has run, to report what changed.
     *
     * @param result what the event decided
     */
    @Override
    protected void afterPosted(com.github.gubejs.event.EventResult result) {
        ConsoleJS.SERVER.info("Recipes: " + added.size() + " added, " + removedCount
            + " removed, " + modifiedCount + " modified; " + recipes.size() + " total");

        var dev = com.github.gubejs.DevProperties.get();

        if (dev.logSkippedRecipes) {
            reportSkipped();
        }

        if (dev.dataPackOutput) {
            writeDataPack();
        }
    }

    /**
     * Names the recipes the game is about to throw away.
     *
     * <p>A recipe whose type no serialiser owns is dropped while the datapacks load, with one line
     * in the game's log that names the file and not the reason. From here the reason is obvious —
     * the mod that owns the type is not installed — and a pack author looking for a recipe that
     * "does not work" is usually looking at one of these.
     */
    private void reportSkipped() {
        var reported = 0;

        for (var entry : recipes.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject object)) {
                ConsoleJS.SERVER.warn("skipped " + entry.getKey() + ": not a JSON object");
                reported++;
                continue;
            }

            var type = object.has("type") ? object.get("type").getAsString() : "";
            var id = ResourceLocation.tryParse(withNamespace(type));

            if (id == null || !ForgeRegistries.RECIPE_SERIALIZERS.containsKey(id)) {
                ConsoleJS.SERVER.warn("skipped " + entry.getKey() + ": nothing can read type '"
                    + type + "'");
                reported++;
            }
        }

        ConsoleJS.SERVER.info(reported == 0
            ? "Every recipe has a serialiser that can read it"
            : reported + " recipe(s) will not load");
    }

    /**
     * Writes the recipes the game ends up with, as a datapack on disk.
     *
     * <p>One file per recipe under {@code local/gubejs/export/datapack/}, in the layout a datapack
     * has, so the output can be read as files or dropped into {@code datapacks/} of a world to
     * reproduce exactly what a pack produced. Written after every listener, which is the only moment
     * the answer is final.
     */
    private void writeDataPack() {
        var root = com.github.gubejs.GubejsPaths.EXPORT.resolve("datapack");
        var written = 0;

        try {
            for (var entry : recipes.entrySet()) {
                var id = entry.getKey();
                var file = root.resolve("data").resolve(id.getNamespace())
                    .resolve("recipes").resolve(id.getPath() + ".json");
                java.nio.file.Files.createDirectories(file.getParent());
                java.nio.file.Files.writeString(file,
                    JsonUtils.toPrettyString(entry.getValue()));
                written++;
            }

            java.nio.file.Files.writeString(root.resolve("pack.mcmeta"), """
                {
                  "pack": {
                    "description": "Recipes as Gubejs left them",
                    "pack_format": 10
                  }
                }""");
        } catch (Exception ex) {
            ConsoleJS.SERVER.error("Could not write the recipe datapack to " + root, ex);
            return;
        }

        ConsoleJS.SERVER.info("Wrote " + written + " recipe file(s) to " + root);
    }

    /**
     * Moves a recipe to a new id.
     *
     * <p>Works for a recipe that was already in a datapack as well as one a script added, since
     * {@code forEachRecipe} hands out both and renaming either is a reasonable thing to ask for.
     */
    void rename(ResourceLocation from, ResourceLocation to) {
        var json = recipes.remove(from);

        if (json == null) {
            return;
        }

        recipes.put(to, json);

        if (added.remove(from) != null) {
            added.put(to, json);
        }
    }

    /** Drops a recipe the script was handed by {@code forEachRecipe}. */
    void removeById(ResourceLocation id) {
        if (recipes.remove(id) != null) {
            added.remove(id);
            removedCount++;
        }
    }

    void countModified() {
        modifiedCount++;
    }

    /**
     * Counts a rewrite and, when {@code dev.properties} asks, says which recipe it was.
     *
     * @param id the recipe that changed
     */
    void countModified(ResourceLocation id) {
        modifiedCount++;
        logModified(id);
    }

    private void logModified(ResourceLocation id) {
        if (com.github.gubejs.DevProperties.get().logModifiedRecipes) {
            ConsoleJS.SERVER.info("~ " + id + " " + recipes.get(id));
        }
    }

    private RecipeJS register(JsonObject json, ResourceLocation id) {
        var unique = id;

        // Two recipes for the same output would otherwise silently overwrite each other, and the
        // pack author would see one of their two recipes simply not exist.
        for (var i = 2; recipes.containsKey(unique); i++) {
            unique = new ResourceLocation(id.getNamespace(), id.getPath() + "_" + i);
        }

        recipes.put(unique, json);
        added.put(unique, json);

        if (com.github.gubejs.DevProperties.get().logAddedRecipes) {
            ConsoleJS.SERVER.info("+ " + unique + " " + json);
        }

        return new RecipeJS(this, json, unique);
    }

    /**
     * Names a generated recipe after what it makes, which is what a pack author looks for.
     *
     * @param type the recipe type, used when the result names nothing
     */
    private ResourceLocation guessId(JsonObject json, @Nullable ResourceLocation type) {
        String name = null;

        for (var key : RecipeJson.RESULT_KEYS) {
            name = nameOf(json.get(key));

            if (name != null) {
                break;
            }
        }

        if (name == null) {
            name = type == null ? "recipe" : type.getPath();
        }

        return new ResourceLocation(Gubejs.MOD_ID, asPath(name));
    }

    /**
     * Reduces whatever named the result to something a resource location will accept.
     *
     * <p>Every character outside the allowed set, not just {@code :} and {@code /}: a result read
     * from raw JSON can be a tag ({@code "#minecraft:planks"}) or anything else a script wrote, and
     * one stray {@code #} would have {@code new ResourceLocation} throw — from inside the recipe
     * event, which takes the whole {@code ServerEvents.recipes} listener down with it and leaves a
     * pack with no recipes and one puzzling exception.
     *
     * @param name what the result named
     * @return a legal path, never empty
     */
    private static String asPath(String name) {
        var builder = new StringBuilder(name.length());

        for (var c : name.toLowerCase(java.util.Locale.ROOT).toCharArray()) {
            builder.append(c >= 'a' && c <= 'z' || c >= '0' && c <= '9'
                || c == '_' || c == '.' || c == '-' ? c : '_');
        }

        // A name made entirely of rejected characters would leave nothing at all, and an empty
        // path is illegal too.
        return builder.length() == 0 ? "recipe" : builder.toString();
    }

    /** Reads an item id out of whichever shape a result was written in. */
    @Nullable
    private static String nameOf(@Nullable JsonElement result) {
        if (result == null || result.isJsonNull()) {
            return null;
        } else if (result.isJsonPrimitive()) {
            return result.getAsString();
        } else if (result.isJsonArray()) {
            return result.getAsJsonArray().isEmpty() ? null
                : nameOf(result.getAsJsonArray().get(0));
        } else if (result.isJsonObject()) {
            var object = result.getAsJsonObject();

            for (var key : List.of("item", "id", "tag")) {
                if (object.has(key) && object.get(key).isJsonPrimitive()) {
                    return object.get(key).getAsString();
                }
            }
        }

        return null;
    }

    private static String withNamespace(String id) {
        return id.indexOf(':') == -1 ? "minecraft:" + id : id;
    }
}
