/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/IngredientWrapper.java
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
package com.github.gubejs.bindings;

import com.github.gubejs.item.IngredientJS;
import net.minecraft.world.item.crafting.Ingredient;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Ingredient} global, for recipe inputs.
 *
 * <pre>{@code
 * Ingredient.of('#forge:ingots/iron')
 * Ingredient.of(['minecraft:stick', 'minecraft:bone'])
 * Ingredient.all
 * }</pre>
 */
public final class IngredientWrapper {

    private IngredientWrapper() {
    }

    /**
     * Builds an ingredient.
     *
     * @param value anything that names one or more items
     * @return the ingredient, empty if the value names nothing
     */
    public static Ingredient of(@Nullable Object value) {
        return IngredientJS.of(value);
    }

    /**
     * Returns an ingredient matching nothing.
     *
     * @return the empty ingredient
     */
    public static Ingredient getNone() {
        return Ingredient.EMPTY;
    }

    /**
     * Returns an ingredient matching every item there is.
     *
     * @return the ingredient
     */
    public static Ingredient getAll() {
        return IngredientJS.all();
    }

    /**
     * Returns an ingredient matching every item from one mod.
     *
     * @param modId the mod id
     * @return the ingredient
     */
    public static Ingredient ofMod(String modId) {
        return IngredientJS.ofMod(modId);
    }

    /**
     * Returns an ingredient matching every item in a tag.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @return the ingredient
     */
    public static Ingredient ofTag(String tag) {
        return IngredientJS.parse(tag.startsWith("#") ? tag : "#" + tag);
    }

    /**
     * Returns an ingredient matching what the first one does, minus what the second matches.
     *
     * <p>The same as {@code Ingredient.of(a).subtract(b)}, for a script that reads better with the
     * operation in front.
     *
     * @param from what to start from
     * @param without what to leave out
     * @return the narrowed ingredient
     */
    public static Ingredient subtract(@Nullable Object from, @Nullable Object without) {
        return ((com.github.gubejs.core.IngredientKJS) IngredientJS.of(from)).subtract(without);
    }
}
