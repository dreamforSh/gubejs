/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/custom/FallingBlockBuilder.java
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

import com.github.gubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.FallingBlock;

/**
 * Builds a block that falls — {@code event.create('ash', 'falling')}.
 *
 * <p>A plain block in every respect but one: nothing holds it up. Its model and blockstate are the
 * basic ones, since falling is behaviour rather than shape.
 *
 * <p>The dust a falling block gives off while it falls is coloured from its texture's average, so
 * a new falling block gets that for free.
 */
public class FallingBlockBuilder extends BlockBuilder {

    public FallingBlockBuilder(ResourceLocation id) {
        super(id);
        this.material = net.minecraft.world.level.material.Material.SAND;
        this.soundType = net.minecraft.world.level.block.SoundType.SAND;
        this.hardness = 0.5F;
        this.resistance = 0.5F;
    }

    @Override
    public Block createObject() {
        block = new FallingBlock(createProperties()) { };
        return block;
    }

    /** Registers the falling block type scripts can create. */
    public static void registerTypes() {
        RegistryInfo.BLOCK.addType("falling", FallingBlockBuilder::new);
    }
}
