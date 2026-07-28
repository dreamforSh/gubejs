/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/player/KubeJSInventoryListener.java
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

import com.github.gubejs.bindings.event.PlayerEvents;
import com.github.gubejs.player.InventoryChangedEventJS;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Posts {@code PlayerEvents.inventoryChanged} when a slot is written.
 *
 * <p>{@code setItem} is the one funnel every container write goes through — a slot dragged in a
 * menu, a hopper insertion, a pickup that lands in an empty slot. What it does not catch is a
 * stack merely growing: picking up a second stick when one is already held adds to a count in
 * place, and nothing calls back in to say so. That is a vanilla shape, not something this hook
 * chose, and it is documented on the event.
 */
@Mixin(Inventory.class)
public abstract class InventoryMixin {

    @Shadow
    @Final
    public Player player;

    @Inject(method = "setItem", at = @At("RETURN"))
    private void gubejs$setItem(int slot, ItemStack stack, CallbackInfo ci) {
        if (PlayerEvents.INVENTORY_CHANGED.hasListeners() && !stack.isEmpty()) {
            PlayerEvents.INVENTORY_CHANGED.post(
                new InventoryChangedEventJS(player, stack, slot), stack.getItem());
        }
    }
}
