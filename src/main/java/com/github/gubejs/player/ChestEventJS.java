/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/player/ChestEventJS.java
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
package com.github.gubejs.player;

import com.github.gubejs.block.BlockContainerJS;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * A player opening or closing a chest.
 *
 * <p>{@code PlayerEvents.inventoryOpened} narrowed to the one menu a pack usually means. What it
 * buys over filtering by menu type is {@link #getInventory} and {@link #getBlock}: the chest itself,
 * rather than the screen showing it, so a listener can read what is inside and where it is.
 *
 * <pre>{@code
 * PlayerEvents.chestOpened(event => {
 *     if (event.block?.id == 'minecraft:trapped_chest') {
 *         event.player.tell('Careful.')
 *     }
 * })
 * }</pre>
 *
 * <p>A double chest is one menu over two block entities, and {@link #getBlock} then names the half
 * the player clicked. Barrels, shulker boxes and hoppers are not chest menus and do not fire this —
 * they are {@code inventoryOpened} with a menu type.
 */
public class ChestEventJS extends InventoryEventJS {

    public ChestEventJS(Player player, AbstractContainerMenu menu) {
        super(player, menu);
    }

    /**
     * Returns what the screen is showing.
     *
     * @return the chest's own inventory
     */
    public Container getInventory() {
        return ((ChestMenu) getMenu()).getContainer();
    }

    /**
     * Returns the chest in the world.
     *
     * @return the block, or {@code null} when the inventory is not one — an ender chest, or a
     *     screen a mod opened over a container that is not placed anywhere
     */
    @Nullable
    public BlockContainerJS getBlock() {
        if (getInventory() instanceof BlockEntity entity && entity.getLevel() != null) {
            return new BlockContainerJS(entity.getLevel(), entity.getBlockPos());
        }

        return null;
    }
}
