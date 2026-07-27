/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/ItemEntityInteractedEventJS.java
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * A player right-clicking an entity while holding an item.
 *
 * <p>{@code event.cancel()} stops the interaction, so neither the item nor the entity reacts.
 */
public final class ItemEntityInteractedEventJS extends PlayerEventJS {

    private final ItemStack item;

    private final Entity target;

    private final InteractionHand hand;

    public ItemEntityInteractedEventJS(Player player, ItemStack item, Entity target,
                                       InteractionHand hand) {
        super(player);
        this.item = item;
        this.target = target;
        this.hand = hand;
    }

    /**
     * Returns the item the player was holding.
     *
     * @return the held stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the entity that was clicked.
     *
     * @return the target
     */
    public Entity getTarget() {
        return target;
    }

    /**
     * Returns which hand was used.
     *
     * @return the hand
     */
    public InteractionHand getHand() {
        return hand;
    }
}
