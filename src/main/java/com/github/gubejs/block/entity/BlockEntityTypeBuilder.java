/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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
package com.github.gubejs.block.entity;

import com.github.gubejs.block.BlockBuilder;
import com.github.gubejs.registry.BuilderBase;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Registers the block entity type that goes with a block a script gave memory to.
 *
 * <p>A separate registry entry because that is what the game wants: a block entity type is what
 * ties a block to the class that holds its data, and it lives in its own registry alongside the
 * block. Created from the block's builder in the same pass that gives a fluid its bucket.
 */
public class BlockEntityTypeBuilder extends BuilderBase<BlockEntityType<?>> {

    private final BlockBuilder block;

    public BlockEntityTypeBuilder(BlockBuilder block) {
        super(block.id);
        this.block = block;
    }

    @Override
    @SuppressWarnings("ConstantConditions")
    public BlockEntityType<?> createObject() {
        var entityBuilder = block.getBlockEntityBuilder();

        // build(null) is how every mod builds one: the data fixer type is for Mojang's own
        // migrations and nothing outside the game has one to pass.
        var type = BlockEntityType.Builder
            .of((pos, state) -> new GubejsBlockEntity(block.getBlockEntityType(), pos, state,
                entityBuilder), block.getBlock())
            .build(null);

        // Handed back to the block before this returns. The factory above asks for it again when
        // an entity is actually created, which is always later than this -- but the block itself
        // needs it from newBlockEntity, and nothing else would ever give it one.
        block.setBlockEntityType(type);
        return type;
    }

    @Override
    public Map<String, String> getTranslations() {
        // The block's own builder already names it; a block entity type is never shown.
        return Map.of();
    }
}
