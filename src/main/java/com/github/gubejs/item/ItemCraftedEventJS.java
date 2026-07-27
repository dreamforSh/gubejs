/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/ItemCraftedEventJS.java
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
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * An item a player has just taken out of a crafting grid.
 *
 * <p>Fires after the craft, so the result cannot be refused — but it can be changed, since the
 * stack handed here is the one going into the player's hand.
 */
public final class ItemCraftedEventJS extends PlayerEventJS {

    private final ItemStack item;

    private final Container grid;

    public ItemCraftedEventJS(Player player, ItemStack item, Container grid) {
        super(player);
        this.item = item;
        this.grid = grid;
    }

    /**
     * Returns what was crafted.
     *
     * @return the result stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the grid it was crafted in, for reading what went into it.
     *
     * @return the crafting container
     */
    public Container getGrid() {
        return grid;
    }

    /**
     * Returns the grid it was crafted in.
     *
     * @return the crafting container
     * @deprecated the KubeJS spelling; {@link #getGrid()} says what it is
     */
    @Deprecated
    public Container getInventory() {
        return grid;
    }
}
