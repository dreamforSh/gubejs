/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/ItemDroppedEventJS.java
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
 * A player throwing an item on the ground.
 *
 * <p>{@code event.cancel()} keeps it in the inventory — the usual way a pack stops a quest item
 * being lost.
 */
public final class ItemDroppedEventJS extends PlayerEventJS {

    private final ItemEntity itemEntity;

    public ItemDroppedEventJS(Player player, ItemEntity itemEntity) {
        super(player);
        this.itemEntity = itemEntity;
    }

    /**
     * Returns the item that was dropped.
     *
     * @return the stack the dropped entity carries
     */
    public ItemStack getItem() {
        return itemEntity.getItem();
    }

    /**
     * Returns the entity that was about to appear in the world.
     *
     * @return the item entity
     */
    public ItemEntity getItemEntity() {
        return itemEntity;
    }
}
