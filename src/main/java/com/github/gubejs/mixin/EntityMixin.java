/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/EntityMixin.java
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

import com.github.gubejs.core.EntityKJS;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every entity answer the methods a KubeJS script calls on one.
 *
 * <p>No body: the interface's methods are all default methods, and the only thing this mixin does
 * is add the interface to the class. That is deliberate — nothing in {@link Entity} is replaced,
 * injected into or renamed, so this cannot conflict with another mod's mixins on the same class.
 */
@Mixin(Entity.class)
public abstract class EntityMixin implements EntityKJS {
}
