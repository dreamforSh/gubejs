/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/custom/BasicBlockJS.java
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
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A block a script created, occupying less than the whole cube.
 *
 * <p>A class rather than something hung off the block afterwards, because the game bakes each
 * state's shapes while the block's constructor runs. A shape handed over any later than this is
 * never asked for.
 */
public class GubejsBasicBlock extends Block {

    @Nullable
    private final VoxelShape shape;

    public GubejsBasicBlock(Properties properties, @Nullable VoxelShape shape) {
        super(properties);
        this.shape = shape;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                        CollisionContext context) {
        return shape == null ? super.getShape(state, level, pos, context) : shape;
    }
}
