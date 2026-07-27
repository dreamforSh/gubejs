/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/BlockKJS.java
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
package com.github.gubejs.core;

import com.github.gubejs.block.BlockCallbacks;
import org.jetbrains.annotations.Nullable;

/**
 * Where a block keeps the behaviour a script gave it.
 *
 * <p>The same arrangement as {@link ItemKJS}, and for the same reason: the mixin reading these sits
 * inside {@code randomTick} and {@code stepOn}, which run for every entity on every block it walks
 * over, and a map keyed by block would be a hash lookup on that path.
 *
 * <p>Installed on {@code BlockBehaviour}, so every block in the game has the field — a block no
 * script mentioned holds {@code null} and costs one comparison.
 */
public interface BlockKJS {

    /**
     * Returns the callbacks a script gave this block.
     *
     * @return the callbacks, or {@code null} if there are none
     */
    @Nullable
    BlockCallbacks gjs$getCallbacks();

    /**
     * Gives this block a set of callbacks.
     *
     * @param callbacks what to run, or {@code null} to take them away
     */
    void gjs$setCallbacks(@Nullable BlockCallbacks callbacks);

    /**
     * Returns the callbacks a script gave this block, creating a set if there are none.
     *
     * @return the callbacks
     */
    BlockCallbacks gjs$getOrCreateCallbacks();
}
