/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/special/ShapedKubeJSRecipe.java
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

import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;

/**
 * A shaped recipe that can refuse to be mirrored, and can keep the blank rows around its pattern.
 *
 * <p>Neither is expressible in a recipe file, and both come from the same place: vanilla's shaped
 * recipe decides on its own that a pattern means "this shape, anywhere in the grid, either way
 * round". Usually that is what a pack wants. When it is not — a recipe where left and right are
 * different, or one that has to be crafted in the middle of the grid — there is no way to say so.
 *
 * <ul>
 *   <li><strong>No mirror.</strong> Vanilla tries the pattern both ways round; this tries it once.
 *   <li><strong>No shrink.</strong> Vanilla trims blank rows and columns off a pattern when it
 *       reads it, so {@code ['   ', ' A ', '   ']} becomes a one-cell recipe that matches anywhere.
 *       Keeping the blanks makes the position part of the recipe.
 * </ul>
 */
public class GubejsShapedRecipe extends ShapedRecipe {

    private final boolean mirror;

    public GubejsShapedRecipe(ResourceLocation id, String group, int width, int height,
                              NonNullList<Ingredient> ingredients,
                              net.minecraft.world.item.ItemStack result, boolean mirror) {
        super(id, group, width, height, ingredients, result);
        this.mirror = mirror;
    }

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        if (mirror) {
            return super.matches(container, level);
        }

        for (var x = 0; x <= container.getWidth() - getWidth(); x++) {
            for (var y = 0; y <= container.getHeight() - getHeight(); y++) {
                if (matchesAt(container, x, y)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Tries the pattern with its top-left corner at one position, the right way round.
     *
     * <p>Written out rather than calling vanilla's, whose one-position check is private and
     * always takes a mirrored flag. Every slot in the grid is tested, including the ones outside
     * the pattern — those have to be empty, or a larger arrangement would match a smaller recipe.
     */
    private boolean matchesAt(CraftingContainer container, int originX, int originY) {
        for (var x = 0; x < container.getWidth(); x++) {
            for (var y = 0; y < container.getHeight(); y++) {
                var patternX = x - originX;
                var patternY = y - originY;
                var ingredient = Ingredient.EMPTY;

                if (patternX >= 0 && patternY >= 0
                    && patternX < getWidth() && patternY < getHeight()) {
                    ingredient = getIngredients().get(patternX + patternY * getWidth());
                }

                if (!ingredient.test(container.getItem(x + y * container.getWidth()))) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeSerializer<?> getSerializer() {
        // The wrapper is what wrote this recipe and what has to write it back; vanilla's shaped
        // serialiser would drop both flags.
        return GubejsRecipes.MODIFIED.get();
    }

    /**
     * Reads a shaped recipe without trimming its pattern.
     *
     * @param id the recipe id
     * @param json the recipe, in the vanilla shaped format
     * @param mirror whether the pattern may be flipped when matching
     * @return the recipe
     */
    public static GubejsShapedRecipe fromJson(ResourceLocation id, JsonObject json, boolean mirror) {
        var key = new HashMap<String, Ingredient>();

        for (var entry : GsonHelper.getAsJsonObject(json, "key").entrySet()) {
            if (entry.getKey().length() != 1) {
                throw new JsonSyntaxException("Pattern key '" + entry.getKey()
                    + "' is not one character");
            }

            key.put(entry.getKey(), Ingredient.fromJson(entry.getValue()));
        }

        key.put(" ", Ingredient.EMPTY);

        var rows = GsonHelper.getAsJsonArray(json, "pattern");

        if (rows.isEmpty()) {
            throw new JsonSyntaxException("The pattern is empty");
        }

        var pattern = new String[rows.size()];

        for (var i = 0; i < pattern.length; i++) {
            pattern[i] = GsonHelper.convertToString(rows.get(i), "pattern[" + i + "]");

            if (pattern[i].length() != pattern[0].length()) {
                throw new JsonSyntaxException("Every row of the pattern must be the same length");
            }
        }

        var width = pattern[0].length();
        var height = pattern.length;
        var ingredients = NonNullList.withSize(width * height, Ingredient.EMPTY);

        for (var y = 0; y < height; y++) {
            for (var x = 0; x < width; x++) {
                var symbol = String.valueOf(pattern[y].charAt(x));
                var ingredient = key.get(symbol);

                if (ingredient == null) {
                    throw new JsonSyntaxException("The pattern uses '" + symbol
                        + "', which the key does not define");
                }

                ingredients.set(x + y * width, ingredient);
            }
        }

        return new GubejsShapedRecipe(id, GsonHelper.getAsString(json, "group", ""),
            width, height, ingredients,
            ShapedRecipe.itemStackFromJson(GsonHelper.getAsJsonObject(json, "result")), mirror);
    }

    /**
     * Returns whether the pattern may be flipped, for the serialiser writing it to the network.
     *
     * @return {@code true} if mirroring is allowed
     */
    public boolean canMirror() {
        return mirror;
    }
}
