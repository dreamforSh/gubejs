/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/misc/VillagerTypeBuilder.java
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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerType;

/**
 * Builds a villager biome variant — {@code event.create('volcanic')}.
 *
 * <p>A villager type is what a villager wears, and nothing more: vanilla has seven of them, one per
 * group of biomes. The clothing comes from
 * {@code assets/minecraft/textures/entity/villager/type/<path>.png} — under {@code minecraft},
 * because the renderer builds that path from the type's name rather than its full id.
 *
 * <p>Which biomes produce which type is not decided here. It comes from the
 * {@code minecraft:villager_type} biome mapping, which in this version is code rather than data, so
 * a new type only appears on villagers a script or a command creates.
 */
public class VillagerTypeBuilder extends BuilderBase<VillagerType> {

    public VillagerTypeBuilder(ResourceLocation id) {
        super(id);
    }

    @Override
    public VillagerType createObject() {
        return new VillagerType(id.getPath());
    }

    /** Registers the villager types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.VILLAGER_TYPE.addType("basic", VillagerTypeBuilder::new).defaultType("basic");
    }
}
