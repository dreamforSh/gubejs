/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/KubeJSBlockProperties.java
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

import java.util.function.Function;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Material;
import net.minecraft.world.level.material.MaterialColor;

/**
 * A block's properties, carrying the builder that made them.
 *
 * <p>The only way a block can learn what a script asked for while it is still being constructed.
 * The game asks a block for its state properties from inside the {@code Block} constructor — before
 * any field of a subclass has been assigned — so a subclass cannot answer from a field of its own.
 * The properties object is the one thing that already exists at that point, because
 * {@code BlockBehaviour} assigns it first, so the builder travels on this.
 */
public class GubejsBlockProperties extends BlockBehaviour.Properties {

    /** The builder these properties were made by. */
    public final BlockBuilder builder;

    public GubejsBlockProperties(Material material, Function<BlockState, MaterialColor> mapColor,
                                 BlockBuilder builder) {
        super(material, mapColor);
        this.builder = builder;
    }
}
