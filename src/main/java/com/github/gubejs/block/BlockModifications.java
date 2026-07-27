/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/KubeJSBlockProperties.java
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

import org.jetbrains.annotations.Nullable;

/**
 * The properties a script changed on a block that already existed.
 *
 * <p>Every field starts as {@code null}, meaning "the script said nothing about this" — the same
 * arrangement as {@link com.github.gubejs.item.ItemModifications}, and for the same reason:
 * {@code 0} is a real hardness and {@code false} is a real answer to whether a tool is needed.
 */
public final class BlockModifications {

    /** How long the block takes to break, or {@code null} to leave it. */
    @Nullable
    public Float hardness;

    /** How well it resists explosions, or {@code null} to leave it. */
    @Nullable
    public Float resistance;

    /** Whether the right tool is needed for it to drop anything, or {@code null} to leave it. */
    @Nullable
    public Boolean requiresTool;

    /** The behaviour a script gave the block, or {@code null} if it gave it none. */
    @Nullable
    public BlockCallbacks callbacks;

    /**
     * Returns the callbacks, creating a set on first use.
     *
     * @return the callbacks
     */
    private BlockCallbacks callbacks() {
        if (callbacks == null) {
            callbacks = new BlockCallbacks();
        }

        return callbacks;
    }

    /**
     * Runs a callback on every random tick, turning random ticking on for the block.
     *
     * <pre>{@code
     * BlockEvents.modification(event => {
     *     event.modify('minecraft:cobblestone', block => {
     *         block.randomTick(e => e.block.set('minecraft:mossy_cobblestone'))
     *     })
     * })
     * }</pre>
     *
     * <p>Whether this reaches the block depends on the block: one whose class decides its own
     * random ticking — every crop and sapling — never asks. See {@link BlockCallbacks}.
     *
     * @param callback what to run
     */
    public void randomTick(java.util.function.Consumer<BlockCallbackEventJS> callback) {
        callbacks().setRandomTick(callback);
    }

    /**
     * Runs a callback every tick an entity is standing on the block.
     *
     * @param callback what to run, with {@code event.entity}
     */
    public void steppedOn(java.util.function.Consumer<BlockCallbackEventJS> callback) {
        callbacks().setSteppedOn(callback);
    }

    /**
     * Runs a callback when an entity lands on the block.
     *
     * @param callback what to run, with {@code event.entity} and {@code event.fallDistance}
     */
    public void fallenOn(java.util.function.Consumer<BlockCallbackEventJS> callback) {
        callbacks().setFallenOn(callback);
    }

    /**
     * Decides whether the block can be built over.
     *
     * @param callback returns {@code true} or {@code false}, or nothing to leave the block's own
     *     answer
     */
    public void canBeReplaced(
        java.util.function.Function<BlockCallbackEventJS, Object> callback) {
        callbacks().setCanBeReplaced(callback);
    }

    /**
     * Sets how long the block takes to break.
     *
     * @param value the hardness; stone is 1.5, obsidian is 50, {@code -1} is unbreakable
     */
    public void setHardness(float value) {
        hardness = value;
    }

    /**
     * Sets how well the block resists explosions.
     *
     * @param value the resistance; stone is 6, obsidian is 1200
     */
    public void setResistance(float value) {
        resistance = value;
    }

    /**
     * Sets whether the right tool is needed for the block to drop anything.
     *
     * @param value {@code true} to require the tool
     */
    public void setRequiresTool(boolean value) {
        requiresTool = value;
    }
}
