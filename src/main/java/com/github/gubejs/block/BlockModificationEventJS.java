/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/BlockModificationEventJS.java
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

import com.github.gubejs.core.BlockKJS;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.mixin.BlockBehaviourAccessor;
import com.github.gubejs.mixin.BlockStateBaseAccessor;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code BlockEvents.modification}: changing blocks that already exist.
 *
 * <pre>{@code
 * BlockEvents.modification(event => {
 *     event.modify('minecraft:obsidian', block => {
 *         block.hardness = 5
 *         block.resistance = 20
 *         block.requiresTool = false
 *     })
 * })
 * }</pre>
 *
 * <p>Fires once, after every mod has registered its blocks. Unlike item modifications, these are
 * written straight into the block rather than read back through a hook — a destroy speed is asked
 * for several times per tick while a player is mining, and a branch there would be felt.
 */
public class BlockModificationEventJS extends EventJS {

    /**
     * Changes every block matching a filter.
     *
     * @param filter a block id, a {@code #tag}, an array of either, {@code '*'} for everything, or
     *     an object with a {@code mod} key
     * @param action what to change
     * @return how many blocks were changed
     */
    public int modify(@Nullable Object filter, Consumer<BlockModifications> action) {
        var matches = matcher(filter);
        var count = 0;

        for (var block : ForgeRegistries.BLOCKS) {
            if (!matches.test(block)) {
                continue;
            }

            var modifications = new BlockModifications();
            action.accept(modifications);
            apply(block, modifications);
            count++;
        }

        if (count == 0) {
            ConsoleJS.STARTUP.warn("No blocks matched " + ValueUtils.unwrap(filter));
        }

        return count;
    }

    /** Writes what the script set, leaving everything it did not alone. */
    private static void apply(Block block, BlockModifications modifications) {
        if (modifications.resistance != null) {
            ((BlockBehaviourAccessor) block).gubejs$setExplosionResistance(modifications.resistance);
        }

        if (modifications.callbacks != null && !modifications.callbacks.isEmpty()) {
            ((BlockKJS) block).gjs$setCallbacks(modifications.callbacks);

            // The block rather than its states: whether a block ticks randomly is one flag on the
            // block, and every state reads it back through the block it belongs to.
            if (modifications.callbacks.wantsRandomTicks()) {
                ((BlockBehaviourAccessor) block).gubejs$setRandomlyTicking(true);
            }
        }

        if (modifications.hardness == null && modifications.requiresTool == null) {
            return;
        }

        // Every state, because hardness and the tool requirement are computed per state when the
        // state definition is built and the block's own copy is never read again.
        for (var state : block.getStateDefinition().getPossibleStates()) {
            if (modifications.hardness != null) {
                ((BlockStateBaseAccessor) state).gubejs$setDestroySpeed(modifications.hardness);
            }

            if (modifications.requiresTool != null) {
                ((BlockStateBaseAccessor) state)
                    .gubejs$setRequiresCorrectToolForDrops(modifications.requiresTool);
            }
        }
    }

    private static Predicate<Block> matcher(@Nullable Object filter) {
        var unwrapped = ValueUtils.unwrap(filter);

        if (unwrapped == null || unwrapped.equals("*")) {
            return block -> true;
        }

        if (unwrapped instanceof Map<?, ?> map && map.containsKey("mod")) {
            var mod = String.valueOf(map.get("mod"));
            return block -> {
                var id = ForgeRegistries.BLOCKS.getKey(block);
                return id != null && id.getNamespace().equals(mod);
            };
        }

        var wanted = ValueUtils.listOf(unwrapped);
        return block -> {
            for (var value : wanted) {
                if (matchesOne(block, String.valueOf(value).trim())) {
                    return true;
                }
            }

            return false;
        };
    }

    private static boolean matchesOne(Block block, String text) {
        if (text.startsWith("#")) {
            var id = ResourceLocation.tryParse(text.substring(1));
            return id != null && block.defaultBlockState()
                .is(TagKey.create(net.minecraft.core.Registry.BLOCK_REGISTRY, id));
        }

        var id = ForgeRegistries.BLOCKS.getKey(block);
        return id != null
            && id.toString().equals(text.indexOf(':') == -1 ? "minecraft:" + text : text);
    }
}
