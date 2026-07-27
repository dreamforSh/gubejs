/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/entity/BlockEntityJS.java
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
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The block entity behind a block a script gave memory to.
 *
 * <p>One class for every such block rather than one per block type: what differs between them is
 * held by the {@link BlockEntityBuilder} this was created from, and generating a class per block
 * would buy nothing.
 */
public class GubejsBlockEntity extends BlockEntity {

    private final BlockEntityBuilder builder;

    /** Whatever a script chose to store here. Saved with the block and, if asked, synced. */
    public final CompoundTag data = new CompoundTag();

    @Nullable
    private final ItemStackHandler inventory;

    @Nullable
    private LazyOptional<?> inventoryHolder;

    public GubejsBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state,
                             BlockEntityBuilder builder) {
        super(type, pos, state);
        this.builder = builder;

        if (builder.inventorySize > 0) {
            this.inventory = new ItemStackHandler(builder.inventorySize) {
                @Override
                protected void onContentsChanged(int slot) {
                    // Without this the world never learns the block changed, and the items are
                    // gone the next time the chunk is loaded.
                    setChanged();
                    sync();
                }
            };
            this.inventoryHolder = LazyOptional.of(() -> inventory);
        } else {
            this.inventory = null;
        }
    }

    /**
     * Returns the block's items, for a script that wants to read or fill them.
     *
     * @return the handler, or {@code null} if the block has no inventory
     */
    @Nullable
    public ItemStackHandler getInventory() {
        return inventory;
    }

    /**
     * Returns the block this entity belongs to, as a script sees any other block.
     *
     * @return the block
     */
    public BlockContainerJS getBlock() {
        return new BlockContainerJS(level, worldPosition);
    }

    /**
     * Returns the scratch tag a script stores things in.
     *
     * @return the tag
     */
    public CompoundTag getData() {
        return data;
    }

    /**
     * Saves and, if the builder asked for it, tells nearby clients.
     *
     * <p>What a script calls after changing {@link #data}, since nothing else can see that it did.
     */
    public void sync() {
        setChanged();

        if (builder.synced && level != null && !level.isClientSide()) {
            var state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, 3);
        }
    }

    // --- saving --------------------------------------------------------------------------------

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.put("gubejs:data", data);

        if (inventory != null) {
            tag.put("gubejs:inventory", inventory.serializeNBT());
        }
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        data.getAllKeys().clear();

        if (tag.contains("gubejs:data")) {
            // Merged into the existing tag rather than replacing it, because a script may already
            // hold a reference to this one.
            data.merge(tag.getCompound("gubejs:data"));
        }

        if (inventory != null && tag.contains("gubejs:inventory")) {
            inventory.deserializeNBT(tag.getCompound("gubejs:inventory"));
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        var tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    @Nullable
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return builder.synced ? ClientboundBlockEntityDataPacket.create(this) : null;
    }

    // --- capabilities --------------------------------------------------------------------------

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable
        net.minecraft.core.Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && inventoryHolder != null) {
            return inventoryHolder.cast();
        }

        return super.getCapability(capability, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();

        if (inventoryHolder != null) {
            inventoryHolder.invalidate();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();

        if (inventoryHolder != null) {
            inventoryHolder.invalidate();
            inventoryHolder = null;
        }
    }
}
