/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/BlockStateBaseMixin.java
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

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Opens the per-state properties {@code BlockEvents.modification} changes.
 *
 * <p>Hardness lives on the block state, not on the block: a block whose properties change how hard
 * it is to break — an ore that is harder when lit, a door that is not — computes one value per
 * state when the state definition is built, and nothing reads the block's own copy afterwards.
 * Changing the block would therefore change nothing.
 *
 * <p>{@link Mutable} is what allows writing a field the game declared final. That is safe here
 * because nothing has read these yet: the modification event fires while the game is still
 * loading, before any world exists to have cached a destroy speed.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public interface BlockStateBaseAccessor {

    /**
     * Sets how long the block takes to break.
     *
     * @param value the destroy speed, where {@code -1} is unbreakable
     */
    @Mutable
    @Accessor("destroySpeed")
    void gubejs$setDestroySpeed(float value);

    /**
     * Sets whether the right tool is needed for the block to drop anything.
     *
     * @param value {@code true} to require the tool
     */
    @Mutable
    @Accessor("requiresCorrectToolForDrops")
    void gubejs$setRequiresCorrectToolForDrops(boolean value);
}
