/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/player/InventoryChangedEventJS.java
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
package com.github.gubejs.player;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A slot in a player's own inventory being written.
 *
 * <p>Fires on the write, not on a stack merely growing: picking up a second stick when one is
 * already held changes a count in place, and nothing calls into the container to say so. Listen
 * for the item that matters rather than trying to keep a running total.
 */
public final class InventoryChangedEventJS extends PlayerEventJS {

    private final ItemStack item;

    private final int slot;

    public InventoryChangedEventJS(Player player, ItemStack item, int slot) {
        super(player);
        this.item = item;
        this.slot = slot;
    }

    /**
     * Returns what is now in the slot.
     *
     * @return the stack, empty when the slot was cleared
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns which slot changed.
     *
     * @return the slot index, 0-8 being the hotbar
     */
    public int getSlot() {
        return slot;
    }
}
