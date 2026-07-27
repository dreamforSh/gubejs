/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/ItemPickedUpEventJS.java
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
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A player picking an item up off the ground.
 *
 * <p>{@code event.cancel()} leaves it lying there.
 */
public final class ItemPickedUpEventJS extends PlayerEventJS {

    private final ItemEntity itemEntity;

    private final ItemStack item;

    public ItemPickedUpEventJS(Player player, ItemEntity itemEntity, ItemStack item) {
        super(player);
        this.itemEntity = itemEntity;
        this.item = item;
    }

    /**
     * Returns the item that was picked up.
     *
     * @return the stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the entity it was picked up from.
     *
     * @return the item entity
     */
    public ItemEntity getItemEntity() {
        return itemEntity;
    }
}
