/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/BlockRightClickedEventJS.java
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
package com.github.gubejs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A player right-clicking a block.
 *
 * <p>{@code event.cancel()} stops the block from responding, which is how a pack disables a
 * workstation or a door.
 */
public final class BlockRightClickedEventJS extends BlockEventJS {

    private final InteractionHand hand;

    public BlockRightClickedEventJS(Level level, BlockPos pos, Player player, InteractionHand hand) {
        super(level, pos, level.getBlockState(pos), player);
        this.hand = hand;
    }

    /**
     * Returns which hand was used.
     *
     * @return the hand
     */
    public InteractionHand getHand() {
        return hand;
    }

    /**
     * Returns what the player was holding.
     *
     * @return the held stack
     */
    public ItemStack getItem() {
        return getPlayer() == null ? ItemStack.EMPTY : getPlayer().getItemInHand(hand);
    }
}
