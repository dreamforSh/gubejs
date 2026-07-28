/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/InventoryKJS.java
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
package com.github.gubejs.core;

import com.github.gubejs.item.IngredientJS;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Searching and changing a container by what is in it, rather than by slot number.
 *
 * <pre>{@code
 * const slot = player.inventory.find('#forge:ingots/iron')
 * if (slot !== -1) {
 *     player.inventory.extractItem(slot, 1)
 * }
 *
 * if (player.inventory.count('minecraft:diamond') >= 4) {
 *     player.inventory.clear('minecraft:diamond')
 * }
 * }</pre>
 *
 * <p>On {@link Container} rather than on the player's inventory alone, so the same methods answer
 * for a chest a script reached through {@code block.inventory} — the question "does this hold four
 * diamonds" has one answer wherever it is asked.
 *
 * <p>Every ingredient argument goes through {@code Ingredient.of}, so an item id, a {@code #tag} or
 * a list all work in the same place.
 */
public interface InventoryKJS {

    default Container gjs$self() {
        return (Container) this;
    }

    /**
     * Returns every stack in the container, empty slots left out.
     *
     * @return the stacks, in slot order
     */
    default List<ItemStack> getAllItems() {
        var self = gjs$self();
        var items = new ArrayList<ItemStack>();

        for (var slot = 0; slot < self.getContainerSize(); slot++) {
            var stack = self.getItem(slot);

            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        return items;
    }

    /**
     * Finds the first slot holding something an ingredient matches.
     *
     * @param ingredient an item id, a {@code #tag}, a list, or nothing for anything at all
     * @return the slot number, or -1 if there is none
     */
    default int find(@Nullable Object ingredient) {
        var self = gjs$self();
        var parsed = gjs$matcher(ingredient);

        for (var slot = 0; slot < self.getContainerSize(); slot++) {
            if (parsed.test(self.getItem(slot))) {
                return slot;
            }
        }

        return -1;
    }

    /**
     * Counts the items an ingredient matches.
     *
     * @param ingredient an item id, a {@code #tag}, a list, or nothing for everything in there
     * @return how many items, counting stack sizes rather than slots
     */
    default int count(@Nullable Object ingredient) {
        var self = gjs$self();
        var parsed = gjs$matcher(ingredient);
        var found = 0;

        for (var slot = 0; slot < self.getContainerSize(); slot++) {
            var stack = self.getItem(slot);

            if (parsed.test(stack)) {
                found += stack.getCount();
            }
        }

        return found;
    }

    /** @return how many items are in the container, of whatever kind */
    default int count() {
        return count(null);
    }

    /**
     * Removes everything an ingredient matches.
     *
     * @param ingredient an item id, a {@code #tag}, a list, or nothing to empty the container
     * @return how many items were removed
     */
    default int clear(@Nullable Object ingredient) {
        var self = gjs$self();
        var parsed = gjs$matcher(ingredient);
        var removed = 0;

        for (var slot = 0; slot < self.getContainerSize(); slot++) {
            var stack = self.getItem(slot);

            if (parsed.test(stack)) {
                removed += stack.getCount();
                self.setItem(slot, ItemStack.EMPTY);
            }
        }

        if (removed > 0) {
            self.setChanged();
        }

        return removed;
    }

    /** @return how many items were removed, having emptied the container */
    default int clear() {
        return clear(null);
    }

    /**
     * Turns an ingredient argument into the test the methods above run per slot.
     *
     * <p>"Anything at all" is answered by a test of its own rather than by an ingredient built from
     * every item in the game: the answer is the same and the work is not — a container with a
     * thousand-value ingredient is a thousand comparisons per slot, for a question that is really
     * "is this slot empty".
     *
     * @param ingredient what the script named, or nothing
     * @return the test
     */
    private static java.util.function.Predicate<ItemStack> gjs$matcher(@Nullable Object ingredient) {
        if (IngredientJS.namesEverything(ingredient)) {
            return stack -> !stack.isEmpty();
        }

        var parsed = IngredientJS.of(ingredient);
        return parsed::test;
    }

    /**
     * Takes items out of a slot.
     *
     * @param slot which slot
     * @param amount how many to take
     * @return what was taken, empty if the slot was empty or out of range
     */
    default ItemStack extractItem(int slot, int amount) {
        var self = gjs$self();

        if (slot < 0 || slot >= self.getContainerSize() || amount <= 0) {
            return ItemStack.EMPTY;
        }

        // removeItem rather than a count subtraction, because a container decides for itself what
        // happens when a slot empties -- a furnace stops smelting, a shulker box updates its
        // tooltip -- and it only finds out through this method.
        var taken = self.removeItem(slot, amount);

        if (!taken.isEmpty()) {
            self.setChanged();
        }

        return taken;
    }

    /**
     * Puts an item in a slot, on top of what is there if the two stack.
     *
     * @param slot which slot
     * @param item the item, as anything {@code Item.of} accepts
     * @return what would not fit, empty if all of it did
     */
    default ItemStack insertItem(int slot, Object item) {
        var self = gjs$self();
        var stack = com.github.gubejs.item.ItemStackJS.of(item);

        if (slot < 0 || slot >= self.getContainerSize() || stack.isEmpty()) {
            return stack;
        }

        var existing = self.getItem(slot);

        if (existing.isEmpty()) {
            var limit = Math.min(self.getMaxStackSize(), stack.getMaxStackSize());
            var moved = stack.copy();
            moved.setCount(Math.min(stack.getCount(), limit));
            self.setItem(slot, moved);
            self.setChanged();

            var left = stack.copy();
            left.shrink(moved.getCount());
            return left;
        }

        if (!ItemStack.isSameItemSameTags(existing, stack)) {
            return stack;
        }

        var limit = Math.min(self.getMaxStackSize(), existing.getMaxStackSize());
        var space = limit - existing.getCount();

        if (space <= 0) {
            return stack;
        }

        var moved = Math.min(space, stack.getCount());
        existing.grow(moved);
        self.setItem(slot, existing);
        self.setChanged();

        var left = stack.copy();
        left.shrink(moved);
        return left;
    }
}
