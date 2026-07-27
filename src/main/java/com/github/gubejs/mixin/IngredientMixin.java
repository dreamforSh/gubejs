/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/IngredientMixin.java
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

import com.github.gubejs.core.IngredientKJS;
import net.minecraft.world.item.crafting.Ingredient;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every ingredient answer the questions a script asks one about itself.
 *
 * <p>No body, like the other interface mixins here: everything is a default method, so nothing in
 * {@link Ingredient} is replaced or renamed. Forge's own ingredient kinds extend this class, so
 * a compound or NBT ingredient answers the same methods a plain one does.
 */
@Mixin(Ingredient.class)
public abstract class IngredientMixin implements IngredientKJS {
}
