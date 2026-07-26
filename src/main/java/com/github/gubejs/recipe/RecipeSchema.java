package com.github.gubejs.recipe;

import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * How one recipe type reads the arguments a script hands it.
 *
 * <p>{@code event.recipes.minecraft.crafting_shaped(output, pattern, key)} has to become the JSON
 * the shaped serialiser expects, and every recipe type spells that differently. A schema is the
 * one piece of knowledge that cannot be derived from the JSON alone: which argument means what.
 *
 * <p>Three sources answer that question, in order:
 *
 * <ol>
 *   <li>a schema registered here — every vanilla type, plus whatever a plugin adds;
 *   <li>a shape learned from a recipe of the same type already in the datapacks, which is what
 *       makes an unknown modded type work without anyone writing an integration for it;
 *   <li>{@link #GENERIC}, which writes results and ingredients under the names the majority of
 *       serialisers use.
 * </ol>
 *
 * <p>The second one is the difference from KubeJS, where a modded recipe type needs a schema
 * shipped by a plugin before a script can create one.
 */
@FunctionalInterface
public interface RecipeSchema {

    /** The schemas registered so far, keyed by recipe type id. */
    Map<ResourceLocation, RecipeSchema> REGISTRY = new ConcurrentHashMap<>();

    /**
     * Builds one recipe.
     *
     * @param type the recipe type being created
     * @param args what the script passed, already converted to plain Java values
     * @return the recipe JSON, without its {@code type} key — the caller adds that
     */
    JsonObject build(ResourceLocation type, List<Object> args);

    /**
     * Registers a schema, replacing any earlier one for the same type.
     *
     * <p>For a mod adding its own recipe types from a {@link com.github.gubejs.GubejsPlugin}.
     *
     * @param type the recipe type id
     * @param schema how to read its arguments
     */
    static void register(String type, RecipeSchema schema) {
        var id = ResourceLocation.tryParse(type);

        if (id != null) {
            REGISTRY.put(id, schema);
        }
    }

    /**
     * Looks up a registered schema.
     *
     * @param type the recipe type id
     * @return the schema, or {@code null} if none was registered
     */
    @Nullable
    static RecipeSchema find(ResourceLocation type) {
        return REGISTRY.get(type);
    }

    // --- built-in schemas ------------------------------------------------------------------

    /**
     * The fallback: results first, then ingredients.
     *
     * <p>Singular or plural depending on whether a list was passed, since that is the split
     * nearly every serialiser makes — {@code "ingredient"} for one, {@code "ingredients"} for
     * several.
     */
    RecipeSchema GENERIC = (type, args) -> {
        var json = new JsonObject();

        if (args.isEmpty()) {
            return json;
        }

        // One object argument is the raw form: event.recipes.some.type({ ... }).
        if (args.size() == 1 && args.get(0) instanceof Map<?, ?>) {
            return com.github.gubejs.util.JsonUtils.objectOf(args.get(0));
        }

        putResults(json, args.get(0));

        if (args.size() > 1) {
            putIngredients(json, args.get(1));
        }

        return json;
    };

    /** {@code crafting_shaped(result, pattern, key)}. */
    RecipeSchema SHAPED = (type, args) -> {
        var json = new JsonObject();
        var rows = new JsonArray();

        for (var row : ValueUtils.listOf(arg(args, 1))) {
            rows.add(String.valueOf(ValueUtils.unwrap(row)));
        }

        json.add("pattern", rows);

        var keys = new JsonObject();

        if (ValueUtils.unwrap(arg(args, 2)) instanceof Map<?, ?> map) {
            map.forEach((k, v) -> keys.add(String.valueOf(k), RecipeJson.ingredient(v)));
        }

        json.add("key", keys);
        json.add("result", RecipeJson.result(arg(args, 0)));
        return json;
    };

    /** {@code crafting_shapeless(result, ingredients)}. */
    RecipeSchema SHAPELESS = (type, args) -> {
        var json = new JsonObject();
        var ingredients = new JsonArray();

        for (var input : ValueUtils.listOf(arg(args, 1))) {
            ingredients.add(RecipeJson.ingredient(input));
        }

        json.add("ingredients", ingredients);
        json.add("result", RecipeJson.result(arg(args, 0)));
        return json;
    };

    /** {@code stonecutting(result, ingredient)}. */
    RecipeSchema STONECUTTING = (type, args) -> {
        var json = new JsonObject();
        json.add("ingredient", RecipeJson.ingredient(arg(args, 1)));
        json.addProperty("result", RecipeJson.itemId(arg(args, 0)));
        json.addProperty("count", com.github.gubejs.item.ItemStackJS.of(arg(args, 0)).getCount());
        return json;
    };

    /** {@code smithing(result, base, addition)}. */
    RecipeSchema SMITHING = (type, args) -> {
        var json = new JsonObject();
        json.add("base", RecipeJson.ingredient(arg(args, 1)));
        json.add("addition", RecipeJson.ingredient(arg(args, 2)));
        json.add("result", RecipeJson.result(arg(args, 0)));
        return json;
    };

    /** A crafting type with no arguments at all, like {@code crafting_special_bookcloning}. */
    RecipeSchema SPECIAL = (type, args) -> new JsonObject();

    /**
     * Builds a cooking schema — {@code smelting(result, ingredient, xp?, time?)}.
     *
     * <p>Each cooking type has a different default time, which is why this is a factory rather
     * than a constant: a smoker recipe left at the furnace's 200 ticks would be no faster than
     * the furnace, and a pack author would have to know to say so.
     *
     * @param defaultTime how long this type takes when the script does not say
     * @return the schema
     */
    static RecipeSchema cooking(int defaultTime) {
        return (type, args) -> {
            var json = new JsonObject();
            json.add("ingredient", RecipeJson.ingredient(arg(args, 1)));
            json.addProperty("result", RecipeJson.itemId(arg(args, 0)));
            json.addProperty("experience", ValueUtils.unwrap(arg(args, 2)) instanceof Number xp
                ? xp.floatValue() : 0F);
            json.addProperty("cookingtime", ValueUtils.unwrap(arg(args, 3)) instanceof Number time
                ? time.intValue() : defaultTime);
            return json;
        };
    }

    /** Registers the vanilla recipe types. Called once, while the mod is constructed. */
    static void registerBuiltIn() {
        register("minecraft:crafting_shaped", SHAPED);
        register("minecraft:crafting_shapeless", SHAPELESS);
        register("minecraft:stonecutting", STONECUTTING);
        register("minecraft:smithing", SMITHING);
        register("minecraft:smelting", cooking(200));
        register("minecraft:blasting", cooking(100));
        register("minecraft:smoking", cooking(100));
        register("minecraft:campfire_cooking", cooking(600));

        for (var special : List.of("armordye", "bannerduplicate", "bookcloning", "firework_rocket",
            "firework_star", "firework_star_fade", "mapcloning", "mapextending", "repairitem",
            "shielddecoration", "shulkerboxcoloring", "tippedarrow", "suspiciousstew")) {
            register("minecraft:crafting_special_" + special, SPECIAL);
        }
    }

    // --- helpers ---------------------------------------------------------------------------

    /**
     * Reads one positional argument.
     *
     * @param args what the script passed
     * @param index which argument
     * @return the argument, or {@code null} if the script passed fewer than that
     */
    @Nullable
    static Object arg(List<Object> args, int index) {
        return index < args.size() ? args.get(index) : null;
    }

    /** Writes a result under {@code result} or {@code results}, depending on how many there are. */
    static void putResults(JsonObject json, @Nullable Object value) {
        if (ValueUtils.unwrap(value) instanceof List<?> list) {
            var array = new JsonArray();
            list.forEach(v -> array.add(RecipeJson.result(v)));
            json.add("results", array);
        } else {
            json.add("result", RecipeJson.result(value));
        }
    }

    /** Writes ingredients under {@code ingredient} or {@code ingredients}. */
    static void putIngredients(JsonObject json, @Nullable Object value) {
        if (ValueUtils.unwrap(value) instanceof List<?> list) {
            var array = new JsonArray();
            list.forEach(v -> array.add(RecipeJson.ingredient(v)));
            json.add("ingredients", array);
        } else {
            json.add("ingredient", RecipeJson.ingredient(value));
        }
    }
}
