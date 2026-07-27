/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/item/ItemClickedEventJS.java
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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * A player right-clicking with an item in hand.
 *
 * <p>{@code event.cancel()} stops the item doing whatever it normally would.
 */
public final class ItemClickedEventJS extends PlayerEventJS {

    private final ItemStack item;

    private final InteractionHand hand;

    public ItemClickedEventJS(Player player, ItemStack item, InteractionHand hand) {
        super(player);
        this.item = item;
        this.hand = hand;
    }

    /**
     * Returns the item that was used.
     *
     * @return the held stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the item's id, e.g. {@code minecraft:stick}.
     *
     * @return the id
     */
    public String getId() {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(item.getItem()));
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
