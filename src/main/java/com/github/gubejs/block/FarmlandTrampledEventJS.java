/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/FarmlandTrampledEventJS.java
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

/**
 * Something landing on farmland hard enough to turn it back into dirt.
 *
 * <p>{@code event.cancel()} saves the crop, which is the one-line answer to "stop mobs ruining my
 * farm".
 */
public final class FarmlandTrampledEventJS extends BlockEventJS {

    private final Entity entity;

    private final float distance;

    public FarmlandTrampledEventJS(Level level, BlockPos pos, BlockState state, Entity entity,
                                   float distance) {
        super(level, pos, state, entity instanceof Player player ? player : null);
        this.entity = entity;
        this.distance = distance;
    }

    /**
     * Returns what landed on the farmland.
     *
     * @return the entity, which is often not a player
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Returns how far it fell.
     *
     * @return the fall distance in blocks
     */
    public float getDistance() {
        return distance;
    }
}
