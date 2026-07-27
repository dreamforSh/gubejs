/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/entity/BlockEntityJSTicker.java
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

import com.github.gubejs.block.BlockContainerJS;
import com.github.gubejs.event.EventJS;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What a block entity's tick callback is handed.
 *
 * <pre>{@code
 * be.serverTick(20, 0, event => {
 *     event.data.putInt('age', event.data.getInt('age') + 1)
 *     event.sync()
 * })
 * }</pre>
 */
public class BlockEntityTickEventJS extends EventJS {

    private final GubejsBlockEntity entity;

    BlockEntityTickEventJS(GubejsBlockEntity entity) {
        this.entity = entity;
    }

    /**
     * Returns the block entity itself.
     *
     * @return the block entity
     */
    public GubejsBlockEntity getEntity() {
        return entity;
    }

    /**
     * Returns the block, as a script sees any other block.
     *
     * @return the block
     */
    public BlockContainerJS getBlock() {
        return entity.getBlock();
    }

    /**
     * Returns the tag this block stores things in.
     *
     * <p>Changes are saved with the world. {@link #sync()} is what tells clients about them.
     *
     * @return the tag
     */
    public CompoundTag getData() {
        return entity.data;
    }

    /**
     * Returns the block's items.
     *
     * @return the handler, or {@code null} if the block has no inventory
     */
    @Nullable
    public ItemStackHandler getInventory() {
        return entity.getInventory();
    }

    /**
     * Returns the level the block is in.
     *
     * @return the level
     */
    public Level getLevel() {
        return entity.getLevel();
    }

    /**
     * Marks the block as changed, and tells nearby clients if the block entity is synced.
     */
    public void sync() {
        entity.sync();
    }
}
