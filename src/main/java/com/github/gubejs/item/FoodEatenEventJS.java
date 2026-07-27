/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/FoodEatenEventJS.java
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
package com.github.gubejs.item;

import com.github.gubejs.player.PlayerEventJS;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A player finishing eating or drinking something.
 *
 * <p>Fires after the food's own effects have been applied, so this is where a pack adds an extra
 * one. {@code event.cancel()} replaces the leftover stack with the untouched item, which is as
 * close to undoing the meal as the game allows.
 */
public final class FoodEatenEventJS extends PlayerEventJS {

    private final ItemStack item;

    private ItemStack resultItem;

    public FoodEatenEventJS(Player player, ItemStack item, ItemStack resultItem) {
        super(player);
        this.item = item;
        this.resultItem = resultItem;
    }

    /**
     * Returns what was eaten, as it was before being consumed.
     *
     * @return the eaten stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns what is left in the hand afterwards — an empty bottle, or nothing.
     *
     * @return the leftover stack
     */
    public ItemStack getResultItem() {
        return resultItem;
    }

    /**
     * Replaces what is left in the hand afterwards.
     *
     * @param value an item, an id, or {@code null} for nothing
     */
    public void setResultItem(@Nullable Object value) {
        resultItem = value == null ? ItemStack.EMPTY : ItemStackJS.of(value);
    }
}
