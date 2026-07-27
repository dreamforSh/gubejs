/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/DetectorBlockEventJS.java
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

import com.github.gubejs.level.LevelEventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A detector block seeing the redstone signal around it change.
 *
 * <pre>{@code
 * BlockEvents.detectorPowered('alarm', event => {
 *     event.block.up.set('minecraft:redstone_lamp', { lit: 'true' })
 * })
 * }</pre>
 */
public class DetectorBlockEventJS extends LevelEventJS {

    private final String detectorId;

    private final BlockPos pos;

    private final boolean powered;

    public DetectorBlockEventJS(String detectorId, Level level, BlockPos pos, boolean powered) {
        super(level);
        this.detectorId = detectorId;
        this.pos = pos;
        this.powered = powered;
    }

    /**
     * Returns which detector this is, which is what the listener filtered on.
     *
     * @return the detector id
     */
    public String getDetectorId() {
        return detectorId;
    }

    /**
     * Returns whether the detector is now powered.
     *
     * <p>Always {@code true} in {@code detectorPowered} and {@code false} in
     * {@code detectorUnpowered}; the flag is only worth reading in {@code detectorChanged}.
     *
     * @return whether it is powered
     */
    public boolean isPowered() {
        return powered;
    }

    /**
     * Returns the detector block itself.
     *
     * @return the block in the world
     */
    public BlockContainerJS getBlock() {
        return new BlockContainerJS(getLevel(), pos);
    }
}
