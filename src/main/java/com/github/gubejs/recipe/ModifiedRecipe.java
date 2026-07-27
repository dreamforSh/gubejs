/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/recipe/special/KubeJSCraftingRecipe.java
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

import java.util.List;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A crafting recipe with the things a script asked for that no recipe file can say.
 *
 * <p>Wraps another recipe rather than replacing it. Matching, the ingredient list, the result and
 * the recipe book entry all still come from the recipe underneath, so a recipe viewer shows what
 * it always did and only the two moments a modifier is about behave differently:
 *
 * <ul>
 *   <li>what is left in the grid — {@code keepIngredient}, {@code damageIngredient},
 *       {@code replaceIngredient};
 *   <li>what comes out — {@code modifyResult}.
 * </ul>
 *
 * <p>{@code getType()} deliberately reports the wrapped recipe's type rather than one of its own.
 * That is what keeps the crafting table finding it: recipes are looked up by type, and a type
 * nothing looks up is a recipe nothing can craft.
 */
public class ModifiedRecipe implements CraftingRecipe {

    private final ResourceLocation id;

    private final CraftingRecipe inner;

    private final List<IngredientAction> actions;

    /** The {@code modifyResult} callback's number, or {@code -1} for no callback. */
    private final int resultCallback;

    public ModifiedRecipe(ResourceLocation id, CraftingRecipe inner,
                          List<IngredientAction> actions, int resultCallback) {
        this.id = id;
        this.inner = inner;
        this.actions = actions;
        this.resultCallback = resultCallback;
    }

    /**
     * Returns the recipe underneath, for the serialiser that has to write it back out.
     *
     * @return the wrapped recipe
     */
    public CraftingRecipe getInner() {
        return inner;
    }

    /**
     * Returns the modifiers.
     *
     * @return the actions, in the order the script added them
     */
    public List<IngredientAction> getActions() {
        return actions;
    }

    /**
     * Returns the {@code modifyResult} callback's number.
     *
     * @return the number, or {@code -1}
     */
    public int getResultCallback() {
        return resultCallback;
    }

    // --- what changes --------------------------------------------------------------------------

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer container) {
        var remaining = inner.getRemainingItems(container);

        if (actions.isEmpty()) {
            return remaining;
        }

        for (var slot = 0; slot < container.getContainerSize(); slot++) {
            var stack = container.getItem(slot);

            if (stack.isEmpty()) {
                continue;
            }

            for (var action : actions) {
                if (action.matches(stack)) {
                    // First match wins: two actions naming the same item is a script contradicting
                    // itself, and picking the earlier one at least makes the order meaningful.
                    remaining.set(slot, action.apply(stack, remaining.get(slot)));
                    break;
                }
            }
        }

        return remaining;
    }

    @Override
    public ItemStack assemble(CraftingContainer container) {
        var result = inner.assemble(container);
        return resultCallback < 0 ? result
            : RecipeCallbacks.apply(resultCallback, result, container);
    }

    // --- what does not -------------------------------------------------------------------------

    @Override
    public boolean matches(CraftingContainer container, Level level) {
        return inner.matches(container, level);
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return inner.canCraftInDimensions(width, height);
    }

    @Override
    public ItemStack getResultItem() {
        return inner.getResultItem();
    }

    @Override
    public NonNullList<Ingredient> getIngredients() {
        return inner.getIngredients();
    }

    @Override
    public boolean isSpecial() {
        return inner.isSpecial();
    }

    @Override
    public String getGroup() {
        return inner.getGroup();
    }

    @Override
    public ItemStack getToastSymbol() {
        return inner.getToastSymbol();
    }

    @Override
    public boolean isIncomplete() {
        return inner.isIncomplete();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return GubejsRecipes.MODIFIED.get();
    }

    @Override
    public net.minecraft.world.item.crafting.RecipeType<?> getType() {
        return inner.getType();
    }
}
