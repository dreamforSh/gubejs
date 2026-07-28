/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/ItemStackMixin.java
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

import com.github.gubejs.core.ItemStackKJS;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Makes every item stack answer the methods a KubeJS script calls on one.
 */
@Mixin(ItemStack.class)
public abstract class ItemStackMixin implements ItemStackKJS {

    /**
     * How often a recipe produces this stack, for the machine types that roll their outputs.
     *
     * <p>A field rather than a key in the stack's NBT, deliberately: a chance is something a script
     * says while it writes a recipe, not something the item carries around the world. In NBT it
     * would end up on the item a player is holding, where it means nothing and shows up in every
     * comparison that asks whether two stacks are the same.
     *
     * <p>It follows that it does not survive {@code copy()} — which is exactly the lifetime it
     * needs. {@code withChance} returns the copy and the recipe is written from that same object.
     */
    @org.spongepowered.asm.mixin.Unique
    private double gubejs$chance = Double.NaN;

    @Override
    public double gjs$getChance() {
        return gubejs$chance;
    }

    @Override
    public void gjs$setChance(double chance) {
        this.gubejs$chance = chance;
    }
}
