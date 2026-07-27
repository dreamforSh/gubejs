package com.github.gubejs.recipe;

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Which recipes a script means when it writes {@code event.remove({ output: 'minecraft:stick' })}.
 *
 * <p>Every key is optional and all of them must match:
 *
 * <ul>
 *   <li>{@code id} — the recipe id, with {@code *} wildcards
 *   <li>{@code mod} — the namespace of the recipe id
 *   <li>{@code type} — the recipe type, e.g. {@code minecraft:crafting_shaped}
 *   <li>{@code output} — an item the recipe produces
 *   <li>{@code input} — an item or tag the recipe consumes
 * </ul>
 *
 * <p>Matching happens against the recipe's JSON rather than against a deserialised recipe object.
 * That is what makes {@code input} and {@code output} work for recipe types this mod has never
 * heard of: a modded serialiser still spells its items as {@code {"item": "..."}} and its tags as
 * {@code {"tag": "..."}}, because that is what the vanilla ingredient codec reads.
 */
public final class RecipeFilter {

    @Nullable
    private final Pattern id;

    @Nullable
    private final String mod;

    @Nullable
    private final List<String> types;

    @Nullable
    private final List<String> outputs;

    @Nullable
    private final List<String> inputs;

    private RecipeFilter(@Nullable Pattern id, @Nullable String mod, @Nullable List<String> types,
                         @Nullable List<String> outputs, @Nullable List<String> inputs) {
        this.id = id;
        this.mod = mod;
        this.types = types;
        this.outputs = outputs;
        this.inputs = inputs;
    }

    /**
     * Reads a filter from what a script passed.
     *
     * <p>A bare string is taken as an id pattern, so {@code event.remove('minecraft:*')} works.
     *
     * @param value an object, a string, or {@code null} for "everything"
     * @return the filter
     */
    public static RecipeFilter of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return new RecipeFilter(null, null, null, null, null);
        } else if (unwrapped instanceof CharSequence text) {
            return new RecipeFilter(toPattern(text.toString()), null, null, null, null);
        } else if (unwrapped instanceof Map<?, ?> map) {
            return new RecipeFilter(
                map.get("id") == null ? null : toPattern(String.valueOf(map.get("id"))),
                map.get("mod") == null ? null : String.valueOf(map.get("mod")),
                ids(map.get("type")),
                ids(map.get("output")),
                ids(map.get("input")));
        }

        throw new IllegalArgumentException("Not a recipe filter: " + unwrapped);
    }

    /**
     * Reports whether a recipe matches.
     *
     * @param recipeId the recipe's id
     * @param json the recipe's JSON
     * @return {@code true} if every stated condition holds
     */
    public boolean test(ResourceLocation recipeId, JsonElement json) {
        if (id != null && !id.matcher(recipeId.toString()).matches()) {
            return false;
        }

        if (mod != null && !recipeId.getNamespace().equals(mod)) {
            return false;
        }

        if (!(json instanceof JsonObject object)) {
            return types == null && outputs == null && inputs == null;
        }

        if (types != null) {
            var type = object.has("type") ? object.get("type").getAsString() : "";

            if (!types.contains(normalise(type))) {
                return false;
            }
        }

        if (outputs != null && !matchesAny(outputs, collectResults(object))) {
            return false;
        }

        return inputs == null || matchesAny(inputs, collectIngredients(object));
    }

    /**
     * Reports whether a recipe that has already been read matches.
     *
     * <p>The same conditions as {@link #test(ResourceLocation, JsonElement)}, asked of a loaded
     * recipe rather than of its JSON. One thing differs and cannot be made not to: a tag written as
     * {@code input: '#minecraft:planks'} is matched here against the items the tag expanded to,
     * because by this point the ingredient no longer remembers it was written as a tag.
     *
     * @param recipe the loaded recipe
     * @return {@code true} if every stated condition holds
     */
    public boolean test(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        var recipeId = recipe.getId();

        if (id != null && !id.matcher(recipeId.toString()).matches()) {
            return false;
        }

        if (mod != null && !recipeId.getNamespace().equals(mod)) {
            return false;
        }

        if (types != null && !matchesLoadedType(recipe)) {
            return false;
        }

        if (outputs != null && !matchesAny(outputs, itemIds(recipe.getResultItem()))) {
            return false;
        }

        if (inputs == null) {
            return true;
        }

        var found = new ArrayList<String>();

        for (var ingredient : recipe.getIngredients()) {
            for (var stack : ingredient.getItems()) {
                found.addAll(itemIds(stack));
            }
        }

        return matchesAny(inputs, found);
    }

    /**
     * Whether a loaded recipe is one of the wanted types.
     *
     * <p>Two ids are tried, because {@code type} means one thing in a recipe file and another in a
     * loaded recipe. What the file spells {@code minecraft:crafting_shaped} is a serialiser; the
     * loaded recipe's {@code getType()} is {@code minecraft:crafting}, shared by every shape of
     * crafting recipe there is. A filter written once has to mean the same thing on both sides, so
     * the serialiser is checked first and the recipe type after it.
     */
    private boolean matchesLoadedType(net.minecraft.world.item.crafting.Recipe<?> recipe) {
        var serializer =
            net.minecraft.core.Registry.RECIPE_SERIALIZER.getKey(recipe.getSerializer());

        if (serializer != null && types.contains(serializer.toString())) {
            return true;
        }

        var type = net.minecraft.core.Registry.RECIPE_TYPE.getKey(recipe.getType());
        return type != null && types.contains(type.toString());
    }

    /** The id of one stack's item, or nothing at all when the stack is empty. */
    private static List<String> itemIds(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }

        return List.of(String.valueOf(
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem())));
    }

    /** Whether any wanted id appears among the ids found in the recipe. */
    private static boolean matchesAny(List<String> wanted, List<String> found) {
        for (var w : wanted) {
            if (found.contains(w)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Collects the item ids a recipe's results name.
     *
     * <p>Every key in {@link RecipeJson#RESULT_KEYS}, not just {@code result}: a modded type spells
     * its output {@code results} or {@code output} as often as not, and an {@code output:} filter
     * that only understood the vanilla spelling matched none of them.
     *
     * <p>Walked to any depth for the same reason {@link #collectIngredients} is — a modded type is
     * free to nest its whole operation under a key of its own, and a recipe this mod has wrapped
     * keeps the original one level down. The two walkers agree on where the line is, so a recipe
     * cannot be invisible to {@code output:} while its inputs are visible to {@code input:}.
     */
    private static List<String> collectResults(JsonObject recipe) {
        var ids = new ArrayList<String>();
        collectResultsInto(recipe, false, ids);
        return ids;
    }

    /**
     * @param inResult whether this subtree is part of the recipe's result — turned on by a key from
     *     {@link RecipeJson#RESULT_KEYS} and never turned off again, since an input cannot be
     *     nested inside an output
     */
    private static void collectResultsInto(@Nullable JsonElement element, boolean inResult,
                                           List<String> ids) {
        if (element == null || element.isJsonNull()) {
            return;
        } else if (element.isJsonPrimitive()) {
            // A bare id, which is how cooking and stonecutting write their result. Strings only:
            // a count recursed into would otherwise be collected as if it named something.
            if (inResult && element.getAsJsonPrimitive().isString()) {
                ids.add(normalise(element.getAsString()));
            }
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> collectResultsInto(e, inResult, ids));
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();

            if (inResult) {
                for (var key : List.of("item", "id")) {
                    if (object.has(key) && object.get(key).isJsonPrimitive()) {
                        // Stops here rather than descending: the rest of a result object is its
                        // count and NBT, and neither names an item.
                        ids.add(normalise(object.get(key).getAsString()));
                        return;
                    }
                }
            }

            for (var entry : object.entrySet()) {
                if (!entry.getKey().equals("type")) {
                    collectResultsInto(entry.getValue(),
                        inResult || RecipeJson.RESULT_KEYS.contains(entry.getKey()), ids);
                }
            }
        }
    }

    /**
     * Collects every ingredient id anywhere in a recipe, other than in its results.
     *
     * <p>Walked generically because recipe types put their inputs under different keys —
     * {@code ingredients}, {@code key}, {@code ingredient}, {@code base}, {@code addition} — and
     * a modded type will invent another one. That generality is exactly why the results have to be
     * excluded by name at every level rather than only at the top: a walker that collects whatever
     * it finds would report the diamond a recipe <em>produces</em> as one it consumes, and
     * {@code event.remove({ input: 'minecraft:diamond' })} would take that recipe with it.
     */
    private static List<String> collectIngredients(JsonObject recipe) {
        var ids = new ArrayList<String>();

        for (var entry : recipe.entrySet()) {
            if (!entry.getKey().equals("type") && !RecipeJson.RESULT_KEYS.contains(entry.getKey())) {
                collectIngredientsInto(entry.getValue(), ids);
            }
        }

        return ids;
    }

    private static void collectIngredientsInto(@Nullable JsonElement element, List<String> ids) {
        if (element == null || element.isJsonNull() || element.isJsonPrimitive()) {
            return;
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> collectIngredientsInto(e, ids));
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();

            if (object.has("item")) {
                ids.add(normalise(object.get("item").getAsString()));
            }

            if (object.has("tag")) {
                ids.add("#" + normalise(object.get("tag").getAsString()));
            }

            for (var entry : object.entrySet()) {
                if (!entry.getKey().equals("item") && !entry.getKey().equals("tag")
                    && !RecipeJson.RESULT_KEYS.contains(entry.getKey())) {
                    collectIngredientsInto(entry.getValue(), ids);
                }
            }
        }
    }

    /**
     * Reads one or several ids, keeping the {@code #} that marks a tag.
     *
     * @param value a string or a list of strings
     * @return the normalised ids, or {@code null} if the key was absent
     */
    @Nullable
    private static List<String> ids(@Nullable Object value) {
        if (value == null) {
            return null;
        }

        var list = new ArrayList<String>();

        for (var element : ValueUtils.listOf(value)) {
            var text = String.valueOf(ValueUtils.unwrap(element)).trim();

            if (text.startsWith("#")) {
                list.add("#" + normalise(text.substring(1)));
            } else {
                // Accepts a stack too, so a filter can reuse whatever names the item elsewhere.
                var stack = ItemStackJS.of(text);
                list.add(stack.isEmpty() ? normalise(text)
                    : String.valueOf(net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(stack.getItem())));
            }
        }

        return list;
    }

    /** Adds the {@code minecraft:} a script usually leaves off. */
    private static String normalise(String id) {
        return id.indexOf(':') == -1 ? "minecraft:" + id : id;
    }

    /** Turns a {@code *} wildcard pattern into a regex, escaping everything else. */
    private static Pattern toPattern(String text) {
        var builder = new StringBuilder();

        for (var part : text.split("\\*", -1)) {
            if (builder.length() > 0) {
                builder.append(".*");
            }

            builder.append(Pattern.quote(part));
        }

        return Pattern.compile(text.indexOf(':') == -1
            ? "(minecraft:)?" + builder : builder.toString());
    }
}
