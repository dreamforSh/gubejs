/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/RecipeFunction.java
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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceLocation;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * The {@code event.recipes} object: every recipe type in the game, addressed by mod and name.
 *
 * <pre>{@code
 * event.recipes.minecraft.crafting_shaped('minecraft:chest', ['SSS', 'S S', 'SSS'], {
 *     S: '#minecraft:planks'
 * })
 * event.recipes.create.mixing('minecraft:diamond', ['minecraft:coal', 'minecraft:coal'])
 * }</pre>
 *
 * <p>Two shorter spellings reach the same types, because existing packs use all three:
 *
 * <pre>{@code
 * event.recipes.shaped('minecraft:chest', ['SSS', 'S S', 'SSS'], { S: '#minecraft:planks' })
 * event.recipes['create:mixing']('minecraft:diamond', ['minecraft:coal'])
 * }</pre>
 *
 * <p>This is the shape KubeJS scripts are written against, so it is the shape a pack copied from
 * one gets here. Nothing is enumerated up front — a namespace is created the first time a script
 * names it, which is what lets a recipe type be addressed before its mod's registries are read.
 */
public final class RecipeFunctions implements ProxyObject {

    /**
     * The vanilla types a script can name without a namespace, and what each stands for.
     *
     * <p>Only a spelling: {@code event.recipes.shaped(...)} produces exactly what
     * {@code event.recipes.minecraft.crafting_shaped(...)} does, through the same schema.
     */
    private static final Map<String, String> SHORTCUTS = Map.ofEntries(
        Map.entry("shaped", "crafting_shaped"),
        Map.entry("shapeless", "crafting_shapeless"),
        Map.entry("smelting", "smelting"),
        Map.entry("blasting", "blasting"),
        Map.entry("smoking", "smoking"),
        Map.entry("campfireCooking", "campfire_cooking"),
        Map.entry("campfire_cooking", "campfire_cooking"),
        Map.entry("stonecutting", "stonecutting"),
        Map.entry("smithing", "smithing"));

    private final RecipesEventJS event;

    private final Map<String, RecipeNamespace> namespaces = new ConcurrentHashMap<>();

    private final Map<String, RecipeTypeFunction> types = new ConcurrentHashMap<>();

    RecipeFunctions(RecipesEventJS event) {
        this.event = event;
    }

    @Override
    public Object getMember(String key) {
        var shortcut = SHORTCUTS.get(key);

        if (shortcut != null) {
            return typeFunction(new ResourceLocation("minecraft", shortcut));
        }

        // A namespaced id, which is how a script reaches a type whose name is not a legal
        // identifier -- event.recipes['create:mixing'](...).
        if (key.indexOf(':') != -1) {
            var id = ResourceLocation.tryParse(RecipeNamespace.snakeCase(key));

            if (id != null) {
                return typeFunction(id);
            }
        }

        return namespaces.computeIfAbsent(key, namespace -> new RecipeNamespace(event, namespace));
    }

    @Override
    public boolean hasMember(String key) {
        if (SHORTCUTS.containsKey(key)) {
            return true;
        }

        var colon = key.indexOf(':');

        if (colon == -1) {
            return RecipeNamespace.isRecipeTypeName(key);
        }

        return RecipeNamespace.isRecipeTypeName(key.substring(0, colon))
            && RecipeNamespace.isRecipeTypeName(key.substring(colon + 1));
    }

    private RecipeTypeFunction typeFunction(ResourceLocation type) {
        return types.computeIfAbsent(type.toString(), ignored -> new RecipeTypeFunction(event, type));
    }

    @Override
    public Object getMemberKeys() {
        return RecipeNamespace.knownNamespaces();
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException("Recipe namespaces cannot be assigned to");
    }
}
