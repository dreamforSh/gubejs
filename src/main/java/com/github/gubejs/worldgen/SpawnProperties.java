/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/level/gen/properties/AddSpawnProperties.java
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
package com.github.gubejs.worldgen;

import com.github.gubejs.util.ValueUtils;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * One mob a script asked to spawn naturally.
 *
 * <p>The weight is relative to everything else that spawns in the same biome and category, so a
 * number only means something next to the ones already there — a zombie is 95 in most overworld
 * biomes, an enderman 10.
 */
public class SpawnProperties {

    /** What spawns. */
    @Nullable
    public ResourceLocation entity;

    /** How likely it is, relative to everything else in the biome. */
    public int weight = 100;

    /** The smallest group. */
    public int minCount = 1;

    /** The largest group. */
    public int maxCount = 4;

    /** Which biomes it spawns in. */
    @Nullable
    public Object biomes;

    /**
     * Sets what spawns.
     *
     * @param entity the entity type id, e.g. {@code minecraft:zombie}
     */
    public void setEntity(Object entity) {
        var text = String.valueOf(ValueUtils.unwrap(entity)).trim();
        this.entity = ResourceLocation.tryParse(text.indexOf(':') == -1 ? "minecraft:" + text : text);
    }

    /**
     * Sets how likely the spawn is.
     *
     * @param weight the weight, relative to the biome's other spawns
     */
    public void setWeight(int weight) {
        this.weight = weight;
    }

    /**
     * Sets the smallest group that spawns at once.
     *
     * @param minCount the count
     */
    public void setMinCount(int minCount) {
        this.minCount = minCount;
    }

    /**
     * Sets the largest group that spawns at once.
     *
     * @param maxCount the count
     */
    public void setMaxCount(int maxCount) {
        this.maxCount = maxCount;
    }

    /**
     * Sets which biomes it spawns in.
     *
     * @param biomes a biome id, a {@code #tag}, or a list of ids
     */
    public void setBiomes(@Nullable Object biomes) {
        this.biomes = biomes;
    }
}
