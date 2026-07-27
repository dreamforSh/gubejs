/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/BlockLeftClickedEventJS.java
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
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * A player left-clicking a block, before it starts to break.
 *
 * <p>{@code event.cancel()} stops the punch, which is how a pack makes a block unbreakable by hand
 * without making it unbreakable outright.
 */
public final class BlockLeftClickedEventJS extends BlockEventJS {

    private final Direction face;

    public BlockLeftClickedEventJS(Level level, BlockPos pos, Player player, Direction face) {
        super(level, pos, level.getBlockState(pos), player);
        this.face = face;
    }

    /**
     * Returns which side of the block was hit.
     *
     * @return the face
     */
    public Direction getFacing() {
        return face;
    }

    /**
     * Returns what the player was holding.
     *
     * @return the main-hand stack
     */
    public ItemStack getItem() {
        return getPlayer() == null ? ItemStack.EMPTY : getPlayer().getItemInHand(InteractionHand.MAIN_HAND);
    }
}
