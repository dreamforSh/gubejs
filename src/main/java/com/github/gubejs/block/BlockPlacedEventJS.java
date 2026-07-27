/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/BlockPlacedEventJS.java
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * A block being placed.
 *
 * <p>{@code event.cancel()} stops the placement.
 */
public final class BlockPlacedEventJS extends BlockEventJS {

    @Nullable
    private final Entity placer;

    public BlockPlacedEventJS(Level level, BlockPos pos, BlockState state, @Nullable Entity placer) {
        super(level, pos, state, placer instanceof Player player ? player : null);
        this.placer = placer;
    }

    /**
     * Returns what placed the block, which is not always a player.
     *
     * @return the placing entity, or {@code null} when a dispenser or a script did it
     */
    @Nullable
    public Entity getPlacer() {
        return placer;
    }
}
