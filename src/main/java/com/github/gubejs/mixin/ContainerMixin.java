/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/ContainerMixin.java
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
package com.github.gubejs.mixin;

import com.github.gubejs.core.InventoryKJS;
import net.minecraft.world.Container;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives every container the searching methods a script uses.
 *
 * <p>An interface mixin onto an interface, which is what makes one set of methods answer for a
 * player's inventory, a chest, a hopper and a block entity a mod wrote: they all implement
 * {@link Container}, and none of them has to know about this.
 */
@Mixin(Container.class)
public interface ContainerMixin extends InventoryKJS {
}
