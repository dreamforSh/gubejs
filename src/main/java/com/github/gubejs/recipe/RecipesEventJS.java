package com.github.gubejs.recipe;

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
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

    private int removedCount;

    private int modifiedCount;

    public RecipesEventJS(Map<ResourceLocation, JsonElement> recipes) {
        this.recipes = recipes;
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

        doomed.forEach(recipes::remove);
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
        removedCount += count;
        return count;
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
        return register(object, guessId(object));
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
        var json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shaped");

        var rows = new JsonArray();

        for (var row : ValueUtils.listOf(pattern)) {
            rows.add(String.valueOf(ValueUtils.unwrap(row)));
        }

        json.add("pattern", rows);

        var keys = new JsonObject();
        var keyMap = ValueUtils.unwrap(key);

        if (keyMap instanceof Map<?, ?> map) {
            map.forEach((k, v) -> keys.add(String.valueOf(k), ingredientJson(v)));
        }

        json.add("key", keys);
        json.add("result", resultJson(output));
        return register(json, guessId(json));
    }

    /**
     * Adds a shapeless crafting recipe.
     *
     * @param output what it makes
     * @param inputs what goes in, in any arrangement
     * @return the recipe
     */
    public RecipeJS shapeless(Object output, Object inputs) {
        var json = new JsonObject();
        json.addProperty("type", "minecraft:crafting_shapeless");

        var ingredients = new JsonArray();

        for (var input : ValueUtils.listOf(inputs)) {
            ingredients.add(ingredientJson(input));
        }

        json.add("ingredients", ingredients);
        json.add("result", resultJson(output));
        return register(json, guessId(json));
    }

    /**
     * Adds a furnace recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS smelting(Object output, Object input) {
        return cooking("minecraft:smelting", output, input, 200);
    }

    /**
     * Adds a blast furnace recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS blasting(Object output, Object input) {
        return cooking("minecraft:blasting", output, input, 100);
    }

    /**
     * Adds a smoker recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS smoking(Object output, Object input) {
        return cooking("minecraft:smoking", output, input, 100);
    }

    /**
     * Adds a campfire recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS campfireCooking(Object output, Object input) {
        return cooking("minecraft:campfire_cooking", output, input, 600);
    }

    private RecipeJS cooking(String type, Object output, Object input, int time) {
        var json = new JsonObject();
        json.addProperty("type", type);
        json.add("ingredient", ingredientJson(input));
        // A cooking result is a bare id, not an object; the vanilla serialiser reads it with
        // ShapedRecipe.itemFromJson and would reject the object form.
        json.addProperty("result", itemId(output));
        json.addProperty("experience", 0F);
        json.addProperty("cookingtime", time);
        return register(json, guessId(json));
    }

    /**
     * Adds a stonecutter recipe.
     *
     * @param output what it makes
     * @param input what goes in
     * @return the recipe
     */
    public RecipeJS stonecutting(Object output, Object input) {
        var json = new JsonObject();
        json.addProperty("type", "minecraft:stonecutting");
        json.add("ingredient", ingredientJson(input));
        json.addProperty("result", itemId(output));
        json.addProperty("count", ItemStackJS.of(output).getCount());
        return register(json, guessId(json));
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
        var json = new JsonObject();
        json.addProperty("type", "minecraft:smithing");
        json.add("base", ingredientJson(base));
        json.add("addition", ingredientJson(addition));
        json.add("result", resultJson(output));
        return register(json, guessId(json));
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
        var target = idOf(from);
        var replacement = ingredientJson(to);
        var changed = 0;

        for (var entry : recipes.entrySet()) {
            if (!(entry.getValue() instanceof JsonObject object) || !parsed.test(entry.getKey(), object)) {
                continue;
            }

            var touched = false;

            for (var field : object.entrySet()) {
                var isResult = field.getKey().equals("result");

                if (field.getKey().equals("type") || isResult != results) {
                    continue;
                }

                var rewritten = rewrite(field.getValue(), target, replacement, results);

                if (rewritten != null) {
                    field.setValue(rewritten);
                    touched = true;
                }
            }

            if (touched) {
                changed++;
            }
        }

        modifiedCount += changed;
        return changed;
    }

    /**
     * Rewrites every occurrence of one id inside a JSON subtree.
     *
     * @return the replacement subtree, or {@code null} if nothing in it matched
     */
    @Nullable
    private JsonElement rewrite(JsonElement element, String target, JsonElement replacement,
                                boolean results) {
        if (element.isJsonPrimitive()) {
            // A bare id, which is how cooking and stonecutting results are written.
            var id = element.getAsString();
            var normalised = id.indexOf(':') == -1 ? "minecraft:" + id : id;

            if (!normalised.equals(target)) {
                return null;
            }

            return replacement.isJsonObject() && replacement.getAsJsonObject().has("item")
                ? replacement.getAsJsonObject().get("item") : replacement;
        } else if (element.isJsonArray()) {
            var array = element.getAsJsonArray();
            var touched = false;

            for (var i = 0; i < array.size(); i++) {
                var rewritten = rewrite(array.get(i), target, replacement, results);

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

            if (object.has(key)) {
                var id = object.get(key).getAsString();

                if ((id.indexOf(':') == -1 ? "minecraft:" + id : id).equals(wanted)) {
                    // The whole ingredient object is replaced, so an item-to-tag swap works and
                    // the count on a result is preserved from the replacement, not the original.
                    return replacement.deepCopy();
                }
            }

            var touched = false;

            for (var field : object.entrySet()) {
                var rewritten = rewrite(field.getValue(), target, replacement, results);

                if (rewritten != null) {
                    field.setValue(rewritten);
                    touched = true;
                }
            }

            return touched ? object : null;
        }

        return null;
    }

    // --- plumbing ----------------------------------------------------------------------------

    /**
     * Returns the recipes as the game will read them, for a script that wants to look around.
     *
     * @return the live map, keyed by recipe id
     */
    public Map<ResourceLocation, JsonElement> getRecipes() {
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
    }

    void rename(ResourceLocation from, ResourceLocation to) {
        var json = added.remove(from);

        if (json != null) {
            recipes.remove(from);
            added.put(to, json);
            recipes.put(to, json);
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
        return new RecipeJS(this, json, unique);
    }

    /** Names a generated recipe after what it makes, which is what a pack author looks for. */
    private ResourceLocation guessId(JsonObject json) {
        var result = json.get("result");
        var ids = new ArrayList<String>();

        if (result != null) {
            if (result.isJsonPrimitive()) {
                ids.add(result.getAsString());
            } else if (result.isJsonObject() && result.getAsJsonObject().has("item")) {
                ids.add(result.getAsJsonObject().get("item").getAsString());
            }
        }

        var name = ids.isEmpty() ? "recipe" : ids.get(0).replace(':', '_').replace('/', '_');
        return new ResourceLocation(Gubejs.MOD_ID, name);
    }

    /** Builds the {@code {"item": ...}} or {@code {"tag": ...}} form of an ingredient. */
    private static JsonElement ingredientJson(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof CharSequence text) {
            var s = text.toString().trim();
            var json = new JsonObject();

            if (s.startsWith("#")) {
                json.addProperty("tag", s.substring(1));
            } else {
                json.addProperty("item", itemId(s));
            }

            return json;
        } else if (unwrapped instanceof List<?> list) {
            var array = new JsonArray();
            list.forEach(v -> array.add(ingredientJson(v)));
            return array;
        } else if (unwrapped instanceof Map<?, ?> || unwrapped instanceof JsonElement) {
            return JsonUtils.of(unwrapped);
        }

        var json = new JsonObject();
        json.addProperty("item", itemId(unwrapped));
        return json;
    }

    /** Builds the {@code {"item": ..., "count": n}} form of a result. */
    private static JsonElement resultJson(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof Map<?, ?> || unwrapped instanceof JsonElement) {
            return JsonUtils.of(unwrapped);
        }

        var stack = ItemStackJS.of(unwrapped);
        var json = new JsonObject();
        json.addProperty("item", String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem())));

        if (stack.getCount() > 1) {
            json.addProperty("count", stack.getCount());
        }

        if (stack.hasTag()) {
            // Vanilla's crafting result has no NBT field; Forge's does, and every recipe viewer
            // reads it, so a pack that wants NBT output at least gets it where it is supported.
            json.addProperty("nbt", String.valueOf(stack.getTag()));
        }

        return json;
    }

    private static String itemId(@Nullable Object value) {
        var stack = ItemStackJS.of(value);
        return stack.isEmpty() ? "minecraft:air"
            : String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));
    }

    /** Normalises an id or {@code #tag} the way {@link #rewrite} compares them. */
    private static String idOf(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);
        var text = String.valueOf(unwrapped).trim();

        if (text.startsWith("#")) {
            var id = text.substring(1);
            return "#" + (id.indexOf(':') == -1 ? "minecraft:" + id : id);
        }

        return itemId(unwrapped);
    }
}
