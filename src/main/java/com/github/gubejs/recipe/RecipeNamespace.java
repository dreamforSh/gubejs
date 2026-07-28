/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/schema/RecipeNamespace.java
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * One mod's recipe types — the {@code minecraft} in {@code event.recipes.minecraft.smelting}.
 *
 * <p>Members are answered for any name that could be a recipe type rather than only for the ones
 * that exist. A script naming a type from a mod that is not installed then fails where the recipe
 * is created, with the type in the message, instead of at the property access with
 * {@code undefined is not a function}.
 */
public final class RecipeNamespace implements ProxyObject {

    /**
     * Names JavaScript asks about that are never recipe types.
     *
     * <p>{@code then} is the one that matters: any object with a {@code then} member is a thenable,
     * so returning a function for it would make {@code await} on anything holding one of these
     * hang forever. The rest are the property probes an engine or a debugger makes.
     */
    private static final Set<String> RESERVED = Set.of("then", "name", "length", "constructor",
        "prototype", "call", "apply", "bind", "valueOf", "toString", "toJSON", "inspect", "iterator");

    private final RecipesEventJS event;

    private final String namespace;

    private final Map<String, RecipeTypeFunction> functions = new ConcurrentHashMap<>();

    RecipeNamespace(RecipesEventJS event, String namespace) {
        this.event = event;
        this.namespace = namespace;
    }

    @Override
    public Object getMember(String key) {
        return functions.computeIfAbsent(key,
            path -> new RecipeTypeFunction(event, new ResourceLocation(namespace, snakeCase(path))));
    }

    @Override
    public boolean hasMember(String key) {
        return isRecipeTypeName(key);
    }

    @Override
    public Object getMemberKeys() {
        var keys = new ArrayList<String>();

        for (var id : ForgeRegistries.RECIPE_SERIALIZERS.getKeys()) {
            if (id.getNamespace().equals(namespace)) {
                keys.add(id.getPath());
            }
        }

        return keys;
    }

    @Override
    public void putMember(String key, org.graalvm.polyglot.Value value) {
        throw new UnsupportedOperationException("Recipe types cannot be assigned to");
    }

    /**
     * Reports whether a name could be a recipe type at all.
     *
     * <p>Capitals are accepted so that {@code event.recipes.minecraft.craftingShaped} reaches the
     * same type as {@code crafting_shaped} — KubeJS mounts every recipe type under both spellings,
     * and a pack copied from one uses whichever its author preferred. Everything else is the
     * character set {@link ResourceLocation} accepts, which rules out the punctuated probes an
     * engine makes without needing to list them.
     *
     * @param key the member name being looked up
     * @return {@code true} if a recipe type could be called that
     */
    static boolean isRecipeTypeName(String key) {
        if (key.isEmpty() || RESERVED.contains(key)) {
            return false;
        }

        for (var i = 0; i < key.length(); i++) {
            var c = key.charAt(i);

            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9'
                || c == '_' || c == '.' || c == '-' || c == '/')) {
                return false;
            }
        }

        return true;
    }

    /**
     * Turns {@code craftingShaped} into {@code crafting_shaped}.
     *
     * <p>A no-op for a name that is already lower case, which is the common case, so the camelCase
     * spelling costs nothing to support.
     *
     * @param key the member name a script used
     * @return the resource location path it names
     */
    static String snakeCase(String key) {
        var builder = new StringBuilder(key.length() + 4);

        for (var i = 0; i < key.length(); i++) {
            var c = key.charAt(i);

            if (c >= 'A' && c <= 'Z') {
                if (i > 0) {
                    builder.append('_');
                }

                builder.append(Character.toLowerCase(c));
            } else {
                builder.append(c);
            }
        }

        return builder.toString();
    }

    /** The namespaces {@link #getMemberKeys} on the parent object reports. */
    static List<String> knownNamespaces() {
        var keys = new ArrayList<String>();

        for (var id : ForgeRegistries.RECIPE_SERIALIZERS.getKeys()) {
            if (!keys.contains(id.getNamespace())) {
                keys.add(id.getNamespace());
            }
        }

        return keys;
    }
}
