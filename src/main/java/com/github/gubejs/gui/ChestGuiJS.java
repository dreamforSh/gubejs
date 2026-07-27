/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/gui/KubeJSGUI.java
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

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.script.ScriptType;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A screen a script puts together — {@code player.openChestGUI('Shop', 3, gui => ...)}.
 *
 * <pre>{@code
 * PlayerEvents.loggedIn(event => {
 *     event.player.openChestGUI('Choose a path', 3, gui => {
 *         gui.fill('minecraft:gray_stained_glass_pane', ' ')
 *
 *         gui.button(11, Item.of('minecraft:iron_pickaxe').withName('The miner'), click => {
 *             click.player.stages.add('miner')
 *             click.close()
 *         })
 *
 *         gui.button(15, Item.of('minecraft:iron_sword').withName('The fighter'), click => {
 *             click.player.stages.add('fighter')
 *             click.close()
 *         })
 *     })
 * })
 * }</pre>
 *
 * <p>It is a chest, which is the point. The client draws it with the screen it already has for a
 * chest, so nothing has to be installed there and a vanilla client sees exactly what a modded one
 * does — a script can therefore build a menu for a server whose players have no mods at all.
 *
 * <p>Slots are locked by default: clicking one runs the callback and moves nothing, which is what
 * makes an item a button rather than an item. {@link #unlocked()} turns that off for a screen that
 * really is storage.
 */
public class ChestGuiJS {

    /** How many slots a row of a chest has, which is not something a script may change. */
    public static final int COLUMNS = 9;

    private final Component title;

    private final int rows;

    private final SimpleContainer container;

    /** An inventory the screen shows instead of its own, or {@code null} when it has its own. */
    @Nullable
    private Container backing;

    private final Map<Integer, Consumer<ChestGuiClickEventJS>> slotCallbacks = new HashMap<>();

    @Nullable
    private Consumer<ChestGuiClickEventJS> anyCallback;

    private boolean locked = true;

    /** Which script type built this, so a callback enters the right context. See the click event. */
    @Nullable
    final ScriptType owner = ScriptType.getCurrent();

    public ChestGuiJS(Component title, int rows) {
        this.title = title;
        this.rows = Math.max(1, Math.min(6, rows));
        this.container = new SimpleContainer(this.rows * COLUMNS);
    }

    // --- what the menu needs ---------------------------------------------------------------------

    /** The title shown at the top of the screen. */
    public Component getTitle() {
        return title;
    }

    /** How many rows of nine the screen has, between one and six. */
    public int getRows() {
        return rows;
    }

    /** The items on screen. */
    public Container getContainer() {
        return backing == null ? container : backing;
    }

    /** Whether clicking a slot moves anything. */
    public boolean isLocked() {
        return locked;
    }

    // --- contents ---------------------------------------------------------------------------------

    /**
     * Puts an item in a slot.
     *
     * @param slot the slot, counted from the top-left across each row
     * @param item what to put there
     * @return this
     */
    public ChestGuiJS set(int slot, @Nullable Object item) {
        var target = getContainer();

        if (slot >= 0 && slot < target.getContainerSize()) {
            target.setItem(slot, ItemStackJS.of(item).copy());
        }

        return this;
    }

    /**
     * Puts an item in a slot, by column and row.
     *
     * @param x the column, 0 to 8
     * @param y the row, counted from the top
     * @param item what to put there
     * @return this
     */
    public ChestGuiJS setAt(int x, int y, @Nullable Object item) {
        return set(y * COLUMNS + x, item);
    }

    /**
     * Returns what is in a slot.
     *
     * @param slot the slot
     * @return the stack, empty if the slot is
     */
    public ItemStack get(int slot) {
        var target = getContainer();
        return slot >= 0 && slot < target.getContainerSize()
            ? target.getItem(slot) : ItemStack.EMPTY;
    }

    /**
     * Fills every empty slot with the same thing.
     *
     * <p>What a menu uses for its background: a pane with a blank name in every slot nothing else
     * claimed, so the screen reads as a panel rather than as an inventory with gaps.
     *
     * @param item what to fill with
     * @return this
     */
    public ChestGuiJS fill(@Nullable Object item) {
        var stack = ItemStackJS.of(item);
        var target = getContainer();

        for (var slot = 0; slot < target.getContainerSize(); slot++) {
            if (target.getItem(slot).isEmpty()) {
                target.setItem(slot, stack.copy());
            }
        }

        return this;
    }

    /** Empties every slot. */
    public ChestGuiJS clear() {
        getContainer().clearContent();
        return this;
    }

    // --- clicks ------------------------------------------------------------------------------------

    /**
     * Runs a callback when one slot is clicked.
     *
     * @param slot the slot to listen to
     * @param callback what to run
     * @return this
     */
    public ChestGuiJS onClick(int slot, Consumer<ChestGuiClickEventJS> callback) {
        slotCallbacks.put(slot, callback);
        return this;
    }

    /**
     * Runs a callback when any slot is clicked.
     *
     * <p>After the slot's own callback, if it has one.
     *
     * @param callback what to run
     * @return this
     */
    public ChestGuiJS onClick(Consumer<ChestGuiClickEventJS> callback) {
        anyCallback = callback;
        return this;
    }

    /**
     * Puts an item in a slot and gives it a callback, which is what a button is.
     *
     * @param slot the slot
     * @param item what it looks like
     * @param callback what it does
     * @return this
     */
    public ChestGuiJS button(int slot, @Nullable Object item,
                             Consumer<ChestGuiClickEventJS> callback) {
        return set(slot, item).onClick(slot, callback);
    }

    /**
     * Lets items be taken out and put in.
     *
     * <p>For a screen that is storage rather than a menu. The callbacks still run.
     *
     * @return this
     */
    public ChestGuiJS unlocked() {
        this.locked = false;
        return this;
    }

    /**
     * Runs whatever is listening for a click on a slot.
     *
     * @param event the click
     */
    void handleClick(ChestGuiClickEventJS event) {
        var callback = slotCallbacks.get(event.getSlot());

        if (callback != null) {
            event.run(callback);
        }

        if (anyCallback != null) {
            event.run(anyCallback);
        }
    }

    /**
     * Whether anything at all listens for clicks, so a screen with none can skip building an event.
     */
    boolean hasCallbacks() {
        return anyCallback != null || !slotCallbacks.isEmpty();
    }

    /**
     * Builds a screen over an inventory that already exists, rather than over slots of its own.
     *
     * <p>What {@code player.openBlockInventory(block)} uses: the slots are the block entity's, so
     * an item a player takes out is taken out of the block, and a hopper emptying it while the
     * screen is open is seen.
     *
     * @param title what to call it
     * @param backing the inventory to show
     * @return a screen showing it, unlocked, with as many rows as it takes
     */
    public static ChestGuiJS over(Component title, Container backing) {
        var rows = Math.max(1, Math.min(6, (backing.getContainerSize() + COLUMNS - 1) / COLUMNS));
        var gui = new ChestGuiJS(title, rows);
        gui.backing = backing;
        gui.locked = false;
        return gui;
    }

    /**
     * Reports whether a player may still have this open.
     *
     * @param player the player
     * @return {@code true}, always — a scripted screen has nothing that could go away underneath it
     */
    public boolean stillValid(Player player) {
        return true;
    }
}
