/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/PoiTypeBuilder.java
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
package com.github.gubejs.misc;

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.village.poi.PoiType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Builds a point of interest — {@code event.create('forge').block('mypack:forge')}.
 *
 * <p>A point of interest is how the game finds a block again: villagers walk to their job site,
 * bees to their hive, and a nether portal is one so a piglin can be led through it. Registering one
 * is what makes {@link VillagerProfessionBuilder a profession} possible, and it is also the only way
 * to have a block that mobs seek out at all.
 *
 * <p>{@code maxTickets} is how many mobs can claim the same block. A job site is one; a meeting
 * point is thirty-two, because a whole village gathers at the bell.
 */
public class PoiTypeBuilder extends BuilderBase<PoiType> {

    /** Every block state that counts as this point of interest. */
    protected final Set<BlockState> blockStates = new LinkedHashSet<>();

    /** How many mobs can claim one at a time. */
    protected int maxTickets = 1;

    /** How close a mob has to get before it counts as arrived. */
    protected int validRange = 1;

    public PoiTypeBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Adds every state of a block.
     *
     * <p>All of them, because a block's states are how it records being open, powered or facing
     * somewhere, and a point of interest that stopped applying when a door was opened would be a
     * strange thing.
     *
     * @param block the block id
     * @return this builder
     */
    public PoiTypeBuilder block(Object block) {
        var unwrapped = ValueUtils.unwrap(block);

        if (unwrapped instanceof net.minecraft.world.level.block.Block found) {
            blockStates.addAll(found.getStateDefinition().getPossibleStates());
            return this;
        }

        var id = ResourceLocation.tryParse(String.valueOf(unwrapped));
        var found = id == null ? null : Registry.BLOCK.get(id);

        if (found == null || found == net.minecraft.world.level.block.Blocks.AIR) {
            ConsoleJS.STARTUP.error("'" + unwrapped + "' is not a registered block");
        } else {
            blockStates.addAll(found.getStateDefinition().getPossibleStates());
        }

        return this;
    }

    /**
     * Adds one particular block state.
     *
     * @param state the state
     * @return this builder
     */
    public PoiTypeBuilder blockState(BlockState state) {
        blockStates.add(state);
        return this;
    }

    /**
     * Sets how many mobs can claim one block at a time.
     *
     * @param maxTickets a job site is 1, a village meeting point is 32
     * @return this builder
     */
    public PoiTypeBuilder maxTickets(int maxTickets) {
        this.maxTickets = maxTickets;
        return this;
    }

    /**
     * Sets how close a mob has to get before it counts as having arrived.
     *
     * @param validRange the distance in blocks
     * @return this builder
     */
    public PoiTypeBuilder validRange(int validRange) {
        this.validRange = validRange;
        return this;
    }

    @Override
    public PoiType createObject() {
        if (blockStates.isEmpty()) {
            ConsoleJS.STARTUP.warn("Point of interest " + id
                + " has no blocks, so nothing will ever be one");
        }

        return new PoiType(Set.copyOf(blockStates), maxTickets, validRange);
    }

    /** Registers the point of interest types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.POINT_OF_INTEREST_TYPE.addType("basic", PoiTypeBuilder::new)
            .defaultType("basic");
    }
}
