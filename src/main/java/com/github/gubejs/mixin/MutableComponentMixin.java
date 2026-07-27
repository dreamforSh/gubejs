/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/components/MutableComponentMixin.java
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

import com.github.gubejs.core.ComponentKJS;
import net.minecraft.network.chat.MutableComponent;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Gives every component the chained styling calls a pack writes.
 *
 * <p>Onto {@link MutableComponent} rather than {@code Component}: the calls set a style and hand
 * the component back, and only the mutable form has a style to set. Anything a script builds is
 * mutable — {@code Text.of}, {@code copy()} and the vanilla factories all return this type — so the
 * distinction does not show up in a script.
 */
@Mixin(MutableComponent.class)
public abstract class MutableComponentMixin implements ComponentKJS {
}
