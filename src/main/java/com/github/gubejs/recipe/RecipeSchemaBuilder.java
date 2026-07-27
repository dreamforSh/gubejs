/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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
import org.jetbrains.annotations.Nullable;

/**
 * A recipe type's argument shape, written out by a script.
 *
 * <p>Each of {@link #result}, {@link #ingredient} and {@link #value} claims the next argument a
 * script will pass and says which JSON key it becomes, so the order they are called in is the order
 * the arguments go in:
 *
 * <pre>{@code
 * StartupEvents.recipeSchemaRegistry(event => {
 *     event.register('mymod:grinding', schema => {
 *         schema.result('output')            // first argument
 *         schema.ingredient('input')         // second
 *         schema.value('processingTime', 200) // third, 200 when the script leaves it out
 *     })
 * })
 *
 * // and then, from a server script:
 * event.recipes.mymod.grinding('4x minecraft:gravel', 'minecraft:cobblestone')
 * }</pre>
 *
 * <p>{@code result} and {@code ingredient} differ from {@code value} in how they read what is
 * passed: both accept the shorthands a pack expects — {@code '4x minecraft:gravel'},
 * {@code '#minecraft:planks'}, a stack, a list of either — and write the JSON the vanilla codecs
 * read. Passing a list to one of them writes an array; passing one thing writes an object.
 *
 * <p>Most modded recipe types never need any of this. A type with even one recipe in a datapack
 * already has its shape worked out from that recipe, which is what
 * {@link RecipeSchema the schema lookup} does before falling back. This is for the type whose
 * recipes are all added in code, so there is no example anywhere to learn from.
 */
public final class RecipeSchemaBuilder implements RecipeSchema {

    /** How one argument is converted on its way into the JSON. */
    private enum Role {
        RESULT,
        INGREDIENT,
        VALUE
    }

    private record Argument(String key, Role role, @Nullable Object fallback) {
    }

    private final List<Argument> arguments = new ArrayList<>();

    private final Map<String, JsonElement> constants = new LinkedHashMap<>();

    /**
     * Claims the next argument as a recipe result.
     *
     * @param key the JSON key it becomes, e.g. {@code result} or {@code output}
     * @return this builder
     */
    public RecipeSchemaBuilder result(String key) {
        arguments.add(new Argument(key, Role.RESULT, null));
        return this;
    }

    /**
     * Claims the next argument as an ingredient.
     *
     * @param key the JSON key it becomes, e.g. {@code ingredient} or {@code input}
     * @return this builder
     */
    public RecipeSchemaBuilder ingredient(String key) {
        arguments.add(new Argument(key, Role.INGREDIENT, null));
        return this;
    }

    /**
     * Claims the next argument as a plain value, left out of the JSON when the script omits it.
     *
     * @param key the JSON key it becomes
     * @return this builder
     */
    public RecipeSchemaBuilder value(String key) {
        arguments.add(new Argument(key, Role.VALUE, null));
        return this;
    }

    /**
     * Claims the next argument as a plain value, with what to write when the script omits it.
     *
     * @param key the JSON key it becomes
     * @param fallback what to write when the script passes fewer arguments than this
     * @return this builder
     */
    public RecipeSchemaBuilder value(String key, Object fallback) {
        arguments.add(new Argument(key, Role.VALUE, ValueUtils.unwrap(fallback)));
        return this;
    }

    /**
     * Adds a key that is the same in every recipe of this type and claims no argument.
     *
     * <p>For the key a serialiser insists on and a script would only ever write one value for —
     * a mode flag, a version number.
     *
     * @param key the JSON key
     * @param value what to write
     * @return this builder
     */
    public RecipeSchemaBuilder constant(String key, Object value) {
        constants.put(key, JsonUtils.of(value));
        return this;
    }

    @Override
    public JsonObject build(ResourceLocation type, List<Object> args) {
        // One object argument is the raw form -- event.recipes.mymod.grinding({ ... }) -- which
        // stays available whatever the schema says, because a schema can only describe the calls
        // its author thought of and the raw form describes every other one.
        if (args.size() == 1 && ValueUtils.unwrap(args.get(0)) instanceof Map<?, ?>) {
            var json = JsonUtils.objectOf(args.get(0));

            // Filled in, not written over: a script using the raw form is saying what it wants
            // every key to be, and a constant overruling one of them would be this schema
            // silently disagreeing with the call in front of it.
            constants.forEach((key, value) -> {
                if (!json.has(key)) {
                    json.add(key, value);
                }
            });

            return json;
        }

        var json = new JsonObject();

        for (var i = 0; i < arguments.size(); i++) {
            var argument = arguments.get(i);
            // Unwrapped before the null check, because a script writing an explicit undefined in
            // the middle of a call arrives as a guest value that is only null once asked.
            var passed = i < args.size() ? ValueUtils.unwrap(args.get(i)) : null;

            if (passed == null) {
                if (argument.fallback() != null) {
                    json.add(argument.key(), JsonUtils.of(argument.fallback()));
                }

                continue;
            }

            json.add(argument.key(), convert(argument.role(), passed));
        }

        constants.forEach(json::add);
        return json;
    }

    /** Turns one argument into JSON, spreading a list into an array of the same kind. */
    private static JsonElement convert(Role role, Object passed) {
        if (role == Role.VALUE) {
            return JsonUtils.of(passed);
        }

        if (ValueUtils.unwrap(passed) instanceof List<?> list) {
            var array = new JsonArray();

            for (var element : list) {
                array.add(role == Role.RESULT
                    ? RecipeJson.result(element) : RecipeJson.ingredient(element));
            }

            return array;
        }

        return role == Role.RESULT ? RecipeJson.result(passed) : RecipeJson.ingredient(passed);
    }

    @Override
    public String toString() {
        var names = new ArrayList<String>(arguments.size());
        arguments.forEach(argument -> names.add(argument.key()));
        return "schema(" + String.join(", ", names) + ")";
    }
}
