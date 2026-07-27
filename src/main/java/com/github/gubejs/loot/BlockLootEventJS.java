/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/loot/BlockLootEventJS.java
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
package com.github.gubejs.loot;

import com.github.gubejs.block.BlockTargets;
import com.google.gson.JsonElement;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * What blocks drop when broken.
 *
 * <pre>{@code
 * ServerEvents.blockLootTables(event => {
 *     event.addBlock('minecraft:stone', loot => {
 *         loot.addPool(pool => {
 *             pool.addItem('minecraft:diamond')
 *             pool.survivesExplosion()
 *         })
 *     })
 *
 *     event.modifyBlock('#minecraft:logs', loot => {
 *         loot.addPool(pool => pool.addItem('minecraft:stick', 1, [0, 2]))
 *     })
 * })
 * }</pre>
 */
public final class BlockLootEventJS extends LootEventJS {

    public BlockLootEventJS(Map<ResourceLocation, JsonElement> tables) {
        super(tables);
    }

    @Override
    public String getType() {
        return "minecraft:block";
    }

    @Override
    public String getDirectory() {
        return "blocks";
    }

    /**
     * Replaces what one or more blocks drop.
     *
     * @param blocks a block id, a tag, {@code @mod}, {@code *}, or a list of them
     * @param callback builds the table, which every named block then shares
     */
    public void addBlock(Object blocks, Consumer<LootBuilder> callback) {
        var builder = build(null, callback);
        var json = builder.toJson();

        if (builder.customId != null) {
            addJson(builder.customId, json);
            return;
        }

        for (var id : BlockTargets.idsOf(blocks)) {
            addJson(id, json);
        }
    }

    /**
     * Makes one or more blocks drop themselves, and nothing else.
     *
     * <p>What a block registered by a script gets by default, and the shortest way to undo a
     * table another pack replaced.
     *
     * @param blocks a block id, a tag, or a list of them
     */
    public void addSimpleBlock(Object blocks) {
        addSimpleBlock(blocks, null);
    }

    /**
     * Makes one or more blocks drop a particular item.
     *
     * @param blocks a block id, a tag, or a list of them
     * @param item what to drop, or {@code null} for the block itself
     */
    public void addSimpleBlock(Object blocks, Object item) {
        for (var block : BlockTargets.blocksOf(blocks)) {
            var drop = item == null ? new ItemStack(block) : null;

            if (drop != null && drop.isEmpty()) {
                // A block with no item cannot drop itself; naming one explicitly still can.
                continue;
            }

            var id = ForgeRegistries.BLOCKS.getKey(block);
            var payload = item == null ? drop : item;

            add(id, loot -> loot.addPool(pool -> {
                pool.addItem(payload);
                pool.survivesExplosion();
            }));
        }
    }

    /**
     * Edits what one or more blocks drop, keeping whatever they dropped already.
     *
     * @param blocks a block id, a tag, or a list of them
     * @param callback edits each block's table
     */
    public void modifyBlock(Object blocks, Consumer<LootBuilder> callback) {
        for (var id : BlockTargets.idsOf(blocks)) {
            modify(id, callback);
        }
    }

    /**
     * Makes one or more blocks drop nothing.
     *
     * @param blocks a block id, a tag, or a list of them
     */
    public void removeBlock(Object blocks) {
        for (var id : BlockTargets.idsOf(blocks)) {
            remove(id);
        }
    }
}
