/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/filter/RecipeFilter.java
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

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import net.minecraft.resources.ResourceLocation;
import org.graalvm.polyglot.Value;
import org.jetbrains.annotations.Nullable;

/**
 * Which recipes a script means when it writes {@code event.remove({ output: 'minecraft:stick' })}.
 *
 * <p>Every key is optional and all of them must match:
 *
 * <ul>
 *   <li>{@code id} — the recipe id, with {@code *} wildcards, or a regular expression
 *   <li>{@code mod} — the namespace of the recipe id
 *   <li>{@code type} — the recipe type, e.g. {@code minecraft:crafting_shaped}
 *   <li>{@code output} — an item the recipe produces
 *   <li>{@code input} — an item or tag the recipe consumes
 *   <li>{@code group} — the recipe book group, with {@code *} wildcards or a regular expression
 * </ul>
 *
 * <p>And three that take filters of their own, so a condition can be written the way it is meant
 * rather than as several calls:
 *
 * <pre>{@code
 * event.remove({ type: 'minecraft:crafting_shaped', not: { mod: 'minecraft' } })
 * event.remove({ or: [{ output: 'minecraft:stick' }, { output: 'minecraft:bowl' }] })
 * event.remove([{ mod: 'create' }, { mod: 'mekanism' }])   // a list reads as "any of these"
 * event.remove(/^minecraft:.*_slab$/)
 * event.remove('/^minecraft:.*_slab$/i')       // the same, written as a string
 * }</pre>
 *
 * <p>Matching happens against the recipe's JSON rather than against a deserialised recipe object.
 * That is what makes {@code input} and {@code output} work for recipe types this mod has never
 * heard of: a modded serialiser still spells its items as {@code {"item": "..."}} and its tags as
 * {@code {"tag": "..."}}, because that is what the vanilla ingredient codec reads.
 */
public abstract class RecipeFilter {

    /** Matches every recipe, which is what an empty filter means. */
    public static final RecipeFilter ALL = new RecipeFilter() {
        @Override
        public boolean test(ResourceLocation recipeId, JsonElement json) {
            return true;
        }

        @Override
        public boolean test(net.minecraft.world.item.crafting.Recipe<?> recipe) {
            return true;
        }

        @Override
        public String toString() {
            return "all";
        }
    };

    /**
     * Reports whether a recipe matches.
     *
     * @param recipeId the recipe's id
     * @param json the recipe's JSON
     * @return {@code true} if every stated condition holds
     */
    public abstract boolean test(ResourceLocation recipeId, JsonElement json);

    /**
     * Reports whether a recipe that has already been read matches.
     *
     * <p>The same conditions as {@link #test(ResourceLocation, JsonElement)}, asked of a loaded
     * recipe rather than of its JSON. Tags are the one thing that answers differently, and it
     * cannot be made not to: a loaded recipe holds items and has forgotten whatever spelling the
     * file used, so a filter naming a tag is answered by asking each item whether it is in that tag.
     *
     * <p>Which is to say {@code '#minecraft:planks'} matches here whenever the item in hand is a
     * plank, on either side — including {@code output:}, where the JSON form can never match one,
     * since a result in a file is always written as an item.
     *
     * @param recipe the loaded recipe
     * @return {@code true} if every stated condition holds
     */
    public abstract boolean test(net.minecraft.world.item.crafting.Recipe<?> recipe);

    /**
     * Reads a filter from what a script passed.
     *
     * <p>A bare string is taken as an id pattern, so {@code event.remove('minecraft:*')} works; a
     * regular expression is taken as one too. A list is read as "any of these".
     *
     * @param value an object, a string, a regular expression, a list, or {@code null} for
     *     "everything"
     * @return the filter
     */
    public static RecipeFilter of(@Nullable Object value) {
        // Before unwrapping, because a regular expression is a guest object whose pattern lives in
        // members that converting to a map does not keep. Everything else goes the ordinary way.
        if (value instanceof Value v && !v.isNull() && !v.isHostObject()) {
            var regex = regexOf(v);

            if (regex != null) {
                return new Conditions(regex, null, null, null, null, null);
            }

            if (v.hasMembers() && !v.hasArrayElements() && !v.canExecute()) {
                return fromKeys(v.getMemberKeys(), key -> v.hasMember(key) ? v.getMember(key) : null);
            }
        }

        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return ALL;
        } else if (unwrapped instanceof Pattern pattern) {
            return new Conditions(pattern, null, null, null, null, null);
        } else if (unwrapped instanceof CharSequence text) {
            return new Conditions(toPattern(text.toString()), null, null, null, null, null);
        } else if (unwrapped instanceof List<?> list) {
            return anyOf(list);
        } else if (unwrapped instanceof Map<?, ?> map) {
            var keys = new ArrayList<String>();
            map.keySet().forEach(k -> keys.add(String.valueOf(k)));
            return fromKeys(keys, map::get);
        }

        throw new IllegalArgumentException("Not a recipe filter: " + unwrapped);
    }

    /**
     * Builds a filter from an object's keys, however that object is being read.
     *
     * <p>The reader is passed in rather than the object because the two callers differ in exactly
     * one way: one has a guest object, where a regular expression is still recognisable, and the
     * other has a plain map. The keys and their meanings are the same either way.
     *
     * @param keys the keys the object has
     * @param reader returns a key's value, or {@code null} if it has none
     */
    private static RecipeFilter fromKeys(Collection<String> keys, Function<String, Object> reader) {
        var parts = new ArrayList<RecipeFilter>();

        if (keys.contains("not")) {
            parts.add(new Not(of(reader.apply("not"))));
        }

        if (keys.contains("or")) {
            parts.add(anyOf(ValueUtils.listOf(reader.apply("or"))));
        }

        if (keys.contains("and")) {
            parts.add(allOf(ValueUtils.listOf(reader.apply("and"))));
        }

        var conditions = conditionsOf(keys, reader);

        if (conditions != null) {
            parts.add(conditions);
        }

        if (parts.isEmpty()) {
            // An object with none of the keys this understands. Answering "everything" would have
            // event.remove({ outputs: 'x' }) -- one letter wrong -- delete every recipe in the game.
            if (!keys.isEmpty()) {
                ConsoleJS.SERVER.warn("Recipe filter " + keys + " states no condition this "
                    + "understands, so it matches nothing. Keys: id, mod, type, output, input, "
                    + "group, not, or, and");
                return new Not(ALL);
            }

            return ALL;
        }

        return parts.size() == 1 ? parts.get(0) : new Group(parts, true);
    }

    /**
     * Reads the plain conditions out of an object.
     *
     * @return the conditions, or {@code null} if the object states none
     */
    @Nullable
    private static RecipeFilter conditionsOf(Collection<String> keys,
                                             Function<String, Object> reader) {
        var id = keys.contains("id") ? patternOf(reader.apply("id")) : null;
        var mod = keys.contains("mod") ? ValueUtils.asString(reader.apply("mod")) : null;
        var types = keys.contains("type") ? ids(reader.apply("type")) : null;
        var outputs = keys.contains("output") ? ids(reader.apply("output")) : null;
        var inputs = keys.contains("input") ? ids(reader.apply("input")) : null;
        var group = keys.contains("group") ? patternOf(reader.apply("group")) : null;

        if (id == null && mod == null && types == null && outputs == null && inputs == null
            && group == null) {
            return null;
        }

        return new Conditions(id, mod, types, outputs, inputs, group);
    }

    private static RecipeFilter anyOf(List<?> filters) {
        return combine(filters, false);
    }

    private static RecipeFilter allOf(List<?> filters) {
        return combine(filters, true);
    }

    private static RecipeFilter combine(List<?> filters, boolean all) {
        var parts = new ArrayList<RecipeFilter>(filters.size());

        for (var filter : filters) {
            parts.add(of(filter));
        }

        if (parts.isEmpty()) {
            // An empty 'or' matches nothing and an empty 'and' matches everything, which is what
            // the words mean -- and either way it is what a filter built from an empty list asked
            // for, so it is not second-guessed here.
            return all ? ALL : new Not(ALL);
        }

        return parts.size() == 1 ? parts.get(0) : new Group(parts, all);
    }

    /**
     * Reports whether one recipe names an item on one of its sides.
     *
     * <p>Shared with {@link RecipeJS#hasInput}, so asking about a single recipe answers by the same
     * walk an {@code input:} or {@code output:} filter uses. A separate implementation would be the
     * kind that disagrees with the filter on exactly the recipes that are hard to read — a modded
     * type nesting its operation, or a recipe this mod has wrapped.
     *
     * @param recipe the recipe's JSON
     * @param wanted an item id, a {@code #tag}, or a list of either
     * @param results whether to look at the recipe's results rather than its inputs
     * @return whether any wanted id appears on that side
     */
    static boolean contains(JsonObject recipe, @Nullable Object wanted, boolean results) {
        var ids = ids(wanted);
        return ids != null
            && matchesAny(ids, results ? collectResults(recipe) : collectIngredients(recipe));
    }

    // --- the filters ---------------------------------------------------------------------------

    /** The plain conditions, all of which must hold. */
    private static final class Conditions extends RecipeFilter {

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

        @Nullable
        private final Pattern group;

        private Conditions(@Nullable Pattern id, @Nullable String mod,
                           @Nullable List<String> types, @Nullable List<String> outputs,
                           @Nullable List<String> inputs, @Nullable Pattern group) {
            this.id = id;
            this.mod = mod;
            this.types = types;
            this.outputs = outputs;
            this.inputs = inputs;
            this.group = group;
        }

        @Override
        public boolean test(ResourceLocation recipeId, JsonElement json) {
            if (id != null && !id.matcher(recipeId.toString()).find()) {
                return false;
            }

            if (mod != null && !recipeId.getNamespace().equals(mod)) {
                return false;
            }

            if (!(json instanceof JsonObject object)) {
                return types == null && outputs == null && inputs == null && group == null;
            }

            if (types != null) {
                var type = object.has("type") ? object.get("type").getAsString() : "";

                if (!types.contains(normalise(type))) {
                    return false;
                }
            }

            if (group != null) {
                var name = object.has("group") && object.get("group").isJsonPrimitive()
                    ? object.get("group").getAsString() : "";

                if (!group.matcher(name).find()) {
                    return false;
                }
            }

            if (outputs != null && !matchesAny(outputs, collectResults(object))) {
                return false;
            }

            return inputs == null || matchesAny(inputs, collectIngredients(object));
        }

        @Override
        public boolean test(net.minecraft.world.item.crafting.Recipe<?> recipe) {
            var recipeId = recipe.getId();

            if (id != null && !id.matcher(recipeId.toString()).find()) {
                return false;
            }

            if (mod != null && !recipeId.getNamespace().equals(mod)) {
                return false;
            }

            if (types != null && !matchesLoadedType(recipe)) {
                return false;
            }

            if (group != null && !group.matcher(recipe.getGroup()).find()) {
                return false;
            }

            if (outputs != null && !matchesAny(outputs,
                itemIds(recipe.getResultItem(), namesTag(outputs)))) {
                return false;
            }

            if (inputs == null) {
                return true;
            }

            var found = new ArrayList<String>();
            var wantsTags = namesTag(inputs);

            for (var ingredient : recipe.getIngredients()) {
                for (var stack : ingredient.getItems()) {
                    found.addAll(itemIds(stack, wantsTags));
                }
            }

            return matchesAny(inputs, found);
        }

        /**
         * Whether a loaded recipe is one of the wanted types.
         *
         * <p>Two ids are tried, because {@code type} means one thing in a recipe file and another in
         * a loaded recipe. What the file spells {@code minecraft:crafting_shaped} is a serialiser;
         * the loaded recipe's {@code getType()} is {@code minecraft:crafting}, shared by every shape
         * of crafting recipe there is. A filter written once has to mean the same thing on both
         * sides, so the serialiser is checked first and the recipe type after it.
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

        @Override
        public String toString() {
            var parts = new ArrayList<String>();

            if (id != null) {
                parts.add("id=" + id);
            }

            if (mod != null) {
                parts.add("mod=" + mod);
            }

            if (types != null) {
                parts.add("type=" + types);
            }

            if (outputs != null) {
                parts.add("output=" + outputs);
            }

            if (inputs != null) {
                parts.add("input=" + inputs);
            }

            if (group != null) {
                parts.add("group=" + group);
            }

            return String.join(", ", parts);
        }
    }

    /** The opposite of another filter. */
    private static final class Not extends RecipeFilter {

        private final RecipeFilter filter;

        private Not(RecipeFilter filter) {
            this.filter = filter;
        }

        @Override
        public boolean test(ResourceLocation recipeId, JsonElement json) {
            return !filter.test(recipeId, json);
        }

        @Override
        public boolean test(net.minecraft.world.item.crafting.Recipe<?> recipe) {
            return !filter.test(recipe);
        }

        @Override
        public String toString() {
            return "not(" + filter + ")";
        }
    }

    /** Several filters, either all of which or any of which must match. */
    private static final class Group extends RecipeFilter {

        private final List<RecipeFilter> filters;

        private final boolean all;

        private Group(List<RecipeFilter> filters, boolean all) {
            this.filters = filters;
            this.all = all;
        }

        @Override
        public boolean test(ResourceLocation recipeId, JsonElement json) {
            for (var filter : filters) {
                if (filter.test(recipeId, json) != all) {
                    return !all;
                }
            }

            return all;
        }

        @Override
        public boolean test(net.minecraft.world.item.crafting.Recipe<?> recipe) {
            for (var filter : filters) {
                if (filter.test(recipe) != all) {
                    return !all;
                }
            }

            return all;
        }

        @Override
        public String toString() {
            return (all ? "and" : "or") + filters;
        }
    }

    // --- reading the pieces --------------------------------------------------------------------

    /**
     * Reads a pattern from a wildcard string or a regular expression.
     *
     * @param value the value a key held
     * @return the pattern, or {@code null} if there was nothing there
     */
    @Nullable
    private static Pattern patternOf(@Nullable Object value) {
        if (value instanceof Pattern pattern) {
            return pattern;
        }

        if (value instanceof Value v) {
            var regex = regexOf(v);

            if (regex != null) {
                return regex;
            }
        }

        var text = ValueUtils.asString(value);
        return text == null ? null : toPattern(text);
    }

    /**
     * Reads a guest regular expression, if that is what this is.
     *
     * <p>Recognised by its members rather than by its class: a script's {@code /x/i} is a guest
     * object, and the only thing that reliably distinguishes one is that it answers {@code source},
     * {@code flags} and {@code exec}. A pack written for KubeJS passes these, so they have to work.
     *
     * @param value a guest value
     * @return the compiled pattern, or {@code null} if the value is not a regular expression
     */
    @Nullable
    private static Pattern regexOf(Value value) {
        if (value.isString() || !value.hasMembers()
            || !value.hasMember("source") || !value.hasMember("exec")) {
            return null;
        }

        var source = value.getMember("source");

        if (source == null || !source.isString()) {
            return null;
        }

        var flags = value.hasMember("flags") && value.getMember("flags").isString()
            ? value.getMember("flags").asString() : "";

        return compile(source.asString(), flags);
    }

    /**
     * Reads a regular expression written as a string — {@code '/^minecraft:.*_slab$/i'}.
     *
     * <p>Needed because that is how a filter travels through JSON and through a variable a pack
     * assembled by hand, and KubeJS accepts it everywhere it accepts a real {@code /x/}. Without
     * this the text is taken literally, which matches no recipe at all and says nothing about
     * why — the worst kind of incompatibility to debug.
     *
     * @param text the value a script passed
     * @return the pattern, or {@code null} if the text is not in that form
     */
    @Nullable
    private static Pattern parseRegex(String text) {
        if (text.length() < 3 || text.charAt(0) != '/') {
            return null;
        }

        var end = text.lastIndexOf('/');

        if (end < 2) {
            return null;
        }

        var flags = text.substring(end + 1);

        // A trailing segment that is not flags means this was never a regular expression -- an id
        // with slashes in it, most likely -- and it has to keep being read literally.
        for (var i = 0; i < flags.length(); i++) {
            if ("igmsuyd".indexOf(flags.charAt(i)) == -1) {
                return null;
            }
        }

        return compile(text.substring(1, end), flags);
    }

    /**
     * Compiles a JavaScript pattern, keeping the flags that mean anything here.
     *
     * <p>{@code g}, {@code y} and their friends are about where a search resumes, and nothing here
     * searches the same string twice.
     *
     * @return the pattern, or {@code null} if Java cannot read it
     */
    @Nullable
    private static Pattern compile(String source, String flags) {
        var bits = (flags.indexOf('i') == -1 ? 0 : Pattern.CASE_INSENSITIVE)
            | (flags.indexOf('s') == -1 ? 0 : Pattern.DOTALL)
            | (flags.indexOf('m') == -1 ? 0 : Pattern.MULTILINE);

        try {
            return Pattern.compile(source, bits);
        } catch (java.util.regex.PatternSyntaxException ex) {
            ConsoleJS.SERVER.error("Not a pattern Java understands: /" + source + "/"
                + flags + " (" + ex.getDescription() + ")");
            return null;
        }
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

    /** Whether any wanted id is a tag, and so whether the items have to be asked for theirs. */
    private static boolean namesTag(List<String> wanted) {
        for (var w : wanted) {
            if (w.startsWith("#")) {
                return true;
            }
        }

        return false;
    }

    /**
     * The ids one stack's item answers to, or nothing at all when the stack is empty.
     *
     * @param includeTags whether to add the tags the item is in, each written {@code #id}
     */
    private static List<String> itemIds(net.minecraft.world.item.ItemStack stack,
                                        boolean includeTags) {
        if (stack.isEmpty()) {
            return List.of();
        }

        var id = String.valueOf(
            net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem()));

        if (!includeTags) {
            return List.of(id);
        }

        // A loaded recipe holds items, not the text a file spelled them with, so a filter naming a
        // tag has nothing to compare against unless the item is asked what it is in. Only when the
        // filter does name one: this runs for every item of every ingredient of every recipe being
        // tested, and asking each of them for its tags is not free.
        var ids = new ArrayList<String>();
        ids.add(id);
        stack.getTags().forEach(tag -> ids.add("#" + tag.location()));
        return ids;
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

        // The top level first, and usually only. Nearly every recipe there is names its result
        // right here, and this path costs one lookup per key rather than a walk of the whole
        // recipe -- which matters because `event.remove({ output: ... })` is the most common thing
        // a pack writes, and every one of those calls tests every recipe in the game.
        for (var key : RecipeJson.RESULT_KEYS) {
            if (recipe.has(key)) {
                collectResultsInto(recipe.get(key), true, ids);
            }
        }

        if (ids.isEmpty()) {
            // Nothing at the top: a modded type that nests its whole operation under a key of its
            // own, or a recipe this mod has wrapped, which keeps the original one level down.
            collectResultsInto(recipe, false, ids);
        }

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

    /**
     * Turns a {@code *} wildcard pattern into a regex, escaping everything else.
     *
     * <p>Anchored, so {@code 'minecraft:stick'} means that recipe and not every id containing it.
     * A regular expression a script writes is not anchored, because that is how one behaves
     * everywhere else in JavaScript.
     */
    private static Pattern toPattern(String text) {
        var regex = parseRegex(text);

        if (regex != null) {
            return regex;
        }

        var builder = new StringBuilder();

        for (var part : text.split("\\*", -1)) {
            if (builder.length() > 0) {
                builder.append(".*");
            }

            builder.append(Pattern.quote(part));
        }

        return Pattern.compile("^" + (text.indexOf(':') == -1
            ? "(minecraft:)?" + builder : builder.toString()) + "$");
    }
}
