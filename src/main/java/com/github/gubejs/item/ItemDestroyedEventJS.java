/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * forge/src/main/java/dev/latvian/mods/kubejs/item/forge/ItemDestroyedEventJS.java
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
import org.jetbrains.annotations.Nullable;

/**
 * A tool that has just broken in a player's hands.
 *
 * <pre>{@code
 * ItemEvents.destroyed('minecraft:diamond_pickaxe', event => {
 *     event.player.give('minecraft:diamond')
 *     event.player.tell('Your pickaxe broke')
 * })
 * }</pre>
 *
 * <p>What a pack uses to hand part of a broken tool back, or to warn the player. The item is already
 * gone by the time this fires — the game removes it and then says so, and there is no earlier point
 * that distinguishes "broke" from "was damaged" — so the stack here is a copy of what it was, and
 * changing it changes nothing.
 */
public final class ItemDestroyedEventJS extends PlayerEventJS {

    private final ItemStack item;

    @Nullable
    private final InteractionHand hand;

    public ItemDestroyedEventJS(Player player, ItemStack item, @Nullable InteractionHand hand) {
        super(player);
        this.item = item;
        this.hand = hand;
    }

    /**
     * Returns the item as it was before it broke, with its NBT and enchantments.
     *
     * @return the stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns which hand it was in.
     *
     * @return {@code 'main_hand'} or {@code 'off_hand'}, or {@code null} for a piece of armour or
     *     anything else that broke outside a hand
     */
    @Nullable
    public String getHand() {
        return hand == null ? null : hand.name().toLowerCase(java.util.Locale.ROOT);
    }
}
