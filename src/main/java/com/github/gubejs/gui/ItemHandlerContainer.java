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
package com.github.gubejs.gui;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandlerModifiable;

/**
 * Shows a Forge item handler as a {@link Container}, so a vanilla menu can be built over it.
 *
 * <p>Forge went the other way — {@code InvWrapper} makes a container look like a handler — because
 * that is the direction machines need. This is the direction a screen needs: {@link
 * net.minecraft.world.inventory.ChestMenu} and every other vanilla menu is written against
 * {@code Container}, and a block entity a script created holds an {@code ItemStackHandler}.
 *
 * <p>A view rather than a copy. Every read and write goes to the handler, so a hopper pulling from
 * the block while a player has it open is seen by both.
 */
public class ItemHandlerContainer implements Container {

    private final IItemHandlerModifiable handler;

    /**
     * How many slots the menu sees, which may be more than the handler has.
     *
     * <p>A chest screen is always a multiple of nine. A handler with ten slots is shown as
     * eighteen, and the eight that do not exist answer empty and refuse everything — which is what
     * the screen draws as a locked slot rather than as a hole.
     */
    private final int size;

    public ItemHandlerContainer(IItemHandlerModifiable handler, int size) {
        this.handler = handler;
        this.size = size;
    }

    @Override
    public int getContainerSize() {
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (var slot = 0; slot < handler.getSlots(); slot++) {
            if (!handler.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return slot < handler.getSlots() ? handler.getStackInSlot(slot) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        if (slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }

        var stack = handler.getStackInSlot(slot);

        if (stack.isEmpty() || count <= 0) {
            return ItemStack.EMPTY;
        }

        var taken = stack.split(count);
        handler.setStackInSlot(slot, stack.isEmpty() ? ItemStack.EMPTY : stack);
        return taken;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (slot >= handler.getSlots()) {
            return ItemStack.EMPTY;
        }

        var stack = handler.getStackInSlot(slot);
        handler.setStackInSlot(slot, ItemStack.EMPTY);
        return stack;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (slot < handler.getSlots()) {
            handler.setStackInSlot(slot, stack);
        }
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot < handler.getSlots() && handler.isItemValid(slot, stack);
    }

    @Override
    public int getMaxStackSize() {
        return handler.getSlots() == 0 ? 64 : handler.getSlotLimit(0);
    }

    @Override
    public void setChanged() {
    }

    /**
     * Never invalidates the open screen.
     *
     * <p>The block being broken while the screen is open is handled by the block entity going away,
     * at which point the handler this holds is simply a detached one — the player is putting items
     * into nothing, which is the same outcome as a chest broken from underneath them.
     */
    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        for (var slot = 0; slot < handler.getSlots(); slot++) {
            handler.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }
}
