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

        if (outputs != null && !matchesAny(outputs, collectItems(object.get("result")))) {
            return false;
        }

        return inputs == null || matchesAny(inputs, collectIngredients(object));
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
     * Collects the item ids a result names.
     *
     * <p>{@code result} is a string in some recipe types, an object in others, and an array in a
     * few modded ones, so all three are handled.
     */
    private static List<String> collectItems(@Nullable JsonElement result) {
        var ids = new ArrayList<String>();
        collectItemsInto(result, ids);
        return ids;
    }

    private static void collectItemsInto(@Nullable JsonElement element, List<String> ids) {
        if (element == null || element.isJsonNull()) {
            return;
        } else if (element.isJsonPrimitive()) {
            ids.add(normalise(element.getAsString()));
        } else if (element.isJsonArray()) {
            element.getAsJsonArray().forEach(e -> collectItemsInto(e, ids));
        } else if (element.isJsonObject()) {
            var object = element.getAsJsonObject();

            if (object.has("item")) {
                ids.add(normalise(object.get("item").getAsString()));
            } else if (object.has("id")) {
                ids.add(normalise(object.get("id").getAsString()));
            }
        }
    }

    /**
     * Collects every ingredient id anywhere in a recipe, other than its result.
     *
     * <p>Walked generically because recipe types put their inputs under different keys —
     * {@code ingredients}, {@code key}, {@code ingredient}, {@code base}, {@code addition} — and
     * a modded type will invent another one.
     */
    private static List<String> collectIngredients(JsonObject recipe) {
        var ids = new ArrayList<String>();

        for (var entry : recipe.entrySet()) {
            if (!entry.getKey().equals("result") && !entry.getKey().equals("type")) {
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
                if (!entry.getKey().equals("item") && !entry.getKey().equals("tag")) {
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
