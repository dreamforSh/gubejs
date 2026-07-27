/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/BlockBehaviourMixin.java
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
 * Opens the whole-block properties {@code BlockEvents.modification} changes.
 *
 * <p>Blast resistance is one value for the block rather than one per state, which is why it is
 * here and hardness is on {@link BlockStateBaseAccessor}.
 */
@Mixin(BlockBehaviour.class)
public interface BlockBehaviourAccessor {

    /**
     * Sets how well the block resists explosions.
     *
     * @param value the resistance; obsidian is 1200, stone is 6
     */
    @Mutable
    @Accessor("explosionResistance")
    void gubejs$setExplosionResistance(float value);

    /**
     * Sets whether the block gets random ticks.
     *
     * <p>Read through the block state, but stored here — which is why a block given a
     * {@code randomTick} callback after it was registered can be turned on at all. The properties
     * object that would normally decide this is gone by then.
     *
     * @param value {@code true} to tick the block
     */
    @Mutable
    @Accessor("isRandomlyTicking")
    void gubejs$setRandomlyTicking(boolean value);
}
