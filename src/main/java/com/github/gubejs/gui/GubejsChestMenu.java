/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/gui/KubeJSMenu.java
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

import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;

/**
 * The menu behind a scripted screen.
 *
 * <p>A {@link ChestMenu}, and deliberately so. The menu type is one of the vanilla
 * {@code GENERIC_9xN}, which means the client already knows how to draw it: nothing is registered,
 * no screen class is shipped, and a player with no mods installed sees the same menu a modded one
 * does. The alternative — a menu type of this mod's own — would need a screen on the client and
 * would stop working the moment a vanilla client connected.
 *
 * <p>What is added is the interception in {@link #clicked}. A locked screen runs the callback and
 * then does nothing, so the item in the slot stays where it is; the client had already drawn the
 * item as picked up, and the correction the server sends afterwards puts it back. That correction
 * is not extra work this class arranges — the network handler compares the menu to what the client
 * predicted after every click and sends the difference regardless.
 */
public class GubejsChestMenu extends ChestMenu {

    private final ChestGuiJS gui;

    public GubejsChestMenu(int containerId, Inventory playerInventory, ChestGuiJS gui) {
        super(typeFor(gui.getRows()), containerId, playerInventory, gui.getContainer(),
            gui.getRows());
        this.gui = gui;
    }

    /**
     * Builds the thing {@code player.openMenu} wants.
     *
     * @param gui the screen to show
     * @return a provider that opens it
     */
    public static MenuProvider providerFor(ChestGuiJS gui) {
        return new SimpleMenuProvider((id, inventory, player) ->
            new GubejsChestMenu(id, inventory, gui), gui.getTitle());
    }

    /** The screen this menu is showing. */
    public ChestGuiJS getGui() {
        return gui;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        var size = gui.getRows() * ChestGuiJS.COLUMNS;
        var inScreen = slotId >= 0 && slotId < size;

        if (inScreen && gui.hasCallbacks()) {
            gui.handleClick(
                new ChestGuiClickEventJS(gui, this, player, slotId, button, clickType));

            // A callback is allowed to close the screen, and one that did has already replaced the
            // player's menu with their own inventory. Carrying on would move items in a menu that
            // is no longer open.
            if (player.containerMenu != this) {
                return;
            }
        }

        if (!gui.isLocked()) {
            super.clicked(slotId, button, clickType, player);
            return;
        }

        // Locked: nothing moves. Shift-clicking from the player's own inventory is stopped too --
        // it does not name a slot on the screen, but it would push an item into one.
        if (inScreen || clickType == ClickType.QUICK_MOVE) {
            return;
        }

        super.clicked(slotId, button, clickType, player);
    }

    /**
     * Refuses to move items into a locked screen.
     *
     * <p>{@link #clicked} already stops the shift-click that would reach here, but the game calls
     * this from more than one place and a menu that answers honestly is easier to reason about than
     * one that relies on every caller being intercepted.
     */
    @Override
    public net.minecraft.world.item.ItemStack quickMoveStack(Player player, int slot) {
        return gui.isLocked()
            ? net.minecraft.world.item.ItemStack.EMPTY : super.quickMoveStack(player, slot);
    }

    @Override
    public boolean stillValid(Player player) {
        return gui.stillValid(player);
    }

    /**
     * Picks the vanilla menu type for a number of rows.
     *
     * @param rows one to six
     * @return the matching {@code GENERIC_9xN}
     */
    private static MenuType<ChestMenu> typeFor(int rows) {
        return switch (rows) {
            case 1 -> MenuType.GENERIC_9x1;
            case 2 -> MenuType.GENERIC_9x2;
            case 4 -> MenuType.GENERIC_9x4;
            case 5 -> MenuType.GENERIC_9x5;
            case 6 -> MenuType.GENERIC_9x6;
            default -> MenuType.GENERIC_9x3;
        };
    }

    /**
     * Builds a title from what a script passed, falling back to the chest's own.
     *
     * @param title a string, a component, or {@code null}
     * @return the title
     */
    public static Component titleOf(Object title) {
        var component = com.github.gubejs.bindings.TextWrapper.of(title);
        return component.getString().isEmpty()
            ? Component.translatable("container.chest") : component;
    }
}
