/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/BlockWrapper.java
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
package com.github.gubejs.bindings;

import com.github.gubejs.block.BlockStateJS;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Block} global.
 *
 * <pre>{@code
 * Block.of('minecraft:oak_log[axis=x]')
 * Block.getTypeList().filter(id => id.endsWith('_ore'))
 * }</pre>
 */
public final class BlockWrapper {

    private BlockWrapper() {
    }

    /**
     * Builds a block state.
     *
     * @param value anything that names a block
     * @return the state, air if the value names nothing
     */
    public static BlockState of(@Nullable Object value) {
        return BlockStateJS.of(value);
    }

    /**
     * Returns the default state of every registered block.
     *
     * @return the states, in registry order
     */
    public static List<BlockState> getList() {
        var list = new ArrayList<BlockState>();

        for (var block : ForgeRegistries.BLOCKS.getValues()) {
            list.add(block.defaultBlockState());
        }

        return list;
    }

    /**
     * Returns every registered block id.
     *
     * @return the ids, as strings
     */
    public static List<String> getTypeList() {
        var list = new ArrayList<String>();

        for (var key : ForgeRegistries.BLOCKS.getKeys()) {
            list.add(key.toString());
        }

        return list;
    }

    /**
     * Returns every block in a tag.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @return the blocks
     */
    public static List<Block> getBlocksInTag(String tag) {
        var id = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        var list = new ArrayList<Block>();

        if (id == null) {
            return list;
        }

        Registry.BLOCK.getTag(TagKey.create(Registry.BLOCK_REGISTRY, id)).ifPresent(holders ->
            holders.forEach(holder -> list.add(holder.value())));
        return list;
    }

    /**
     * Looks up a block by id.
     *
     * @param id the registry name
     * @return the block, or {@code null} if nothing is registered under it
     */
    @Nullable
    public static Block getBlock(String id) {
        var location = ResourceLocation.tryParse(id);
        return location != null && ForgeRegistries.BLOCKS.containsKey(location)
            ? ForgeRegistries.BLOCKS.getValue(location) : null;
    }

    /**
     * Reports whether an id names a registered block.
     *
     * @param id the registry name
     * @return {@code true} if the block exists
     */
    public static boolean exists(String id) {
        return getBlock(id) != null;
    }
}
