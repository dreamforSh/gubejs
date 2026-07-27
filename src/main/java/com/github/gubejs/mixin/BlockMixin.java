/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/BlockKJS.java
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
package com.github.gubejs.mixin;

import com.github.gubejs.core.BlockKJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The half of the block callbacks that {@code Block} declares rather than {@code BlockBehaviour}.
 *
 * <p>Split from {@link BlockBehaviourMixin} only because of where the game put these two methods.
 * The field they read is the one that mixin installs, reached through {@link BlockKJS} — which
 * every block already implements by the time this runs, since {@code Block extends BlockBehaviour}.
 */
@Mixin(Block.class)
public abstract class BlockMixin implements BlockKJS {

    @Inject(method = "stepOn", at = @At("HEAD"))
    private void gubejs$stepOn(Level level, BlockPos pos, BlockState state, Entity entity,
                               CallbackInfo callback) {
        var callbacks = gjs$getCallbacks();

        if (callbacks != null) {
            callbacks.onSteppedOn(level, pos, state, entity);
        }
    }

    @Inject(method = "fallOn", at = @At("HEAD"))
    private void gubejs$fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                               float fallDistance, CallbackInfo callback) {
        var callbacks = gjs$getCallbacks();

        if (callbacks != null) {
            callbacks.onFallenOn(level, pos, state, entity, fallDistance);
        }
    }
}
