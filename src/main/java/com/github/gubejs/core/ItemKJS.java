/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/ItemKJS.java
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

import com.github.gubejs.item.ItemModifications;
import org.jetbrains.annotations.Nullable;

/**
 * Where an item keeps the properties a script changed on it.
 *
 * <p>A field on the item rather than a map keyed by item: the mixin that reads these runs inside
 * {@code getMaxStackSize}, which the game calls for every stack in every inventory slot it draws,
 * and a hash lookup there would be felt.
 */
public interface ItemKJS {

    /**
     * Returns what a script changed on this item.
     *
     * @return the modifications, or {@code null} if nothing was changed
     */
    @Nullable
    ItemModifications gjs$getModifications();

    /**
     * Returns what a script changed on this item, creating the record if there is none.
     *
     * @return the modifications
     */
    ItemModifications gjs$getOrCreateModifications();
}
