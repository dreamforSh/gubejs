/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/ItemMixin.java
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

import com.github.gubejs.core.ItemKJS;
import com.github.gubejs.item.ItemModifications;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lets {@code ItemEvents.modification} change the properties of an item that already existed.
 *
 * <p>Each injection returns early only when the script actually set that property, so an item no
 * script touched behaves exactly as its own mod wrote it — the cost is one null check on a method
 * the game was already calling virtually.
 */
@Mixin(Item.class)
public abstract class ItemMixin implements ItemKJS {

    @Unique
    @Nullable
    private ItemModifications gubejs$modifications;

    @Override
    @Nullable
    public ItemModifications gjs$getModifications() {
        return gubejs$modifications;
    }

    @Override
    public ItemModifications gjs$getOrCreateModifications() {
        if (gubejs$modifications == null) {
            gubejs$modifications = new ItemModifications((Item) (Object) this);
        }

        return gubejs$modifications;
    }

    @Inject(method = "getMaxStackSize", at = @At("HEAD"), cancellable = true)
    private void gubejs$getMaxStackSize(CallbackInfoReturnable<Integer> callback) {
        if (gubejs$modifications != null && gubejs$modifications.maxStackSize != null) {
            callback.setReturnValue(gubejs$modifications.maxStackSize);
        }
    }

    @Inject(method = "getMaxDamage", at = @At("HEAD"), cancellable = true)
    private void gubejs$getMaxDamage(CallbackInfoReturnable<Integer> callback) {
        if (gubejs$modifications != null && gubejs$modifications.maxDamage != null) {
            callback.setReturnValue(gubejs$modifications.maxDamage);
        }
    }

    @Inject(method = "getRarity", at = @At("HEAD"), cancellable = true)
    private void gubejs$getRarity(ItemStack stack, CallbackInfoReturnable<Rarity> callback) {
        if (gubejs$modifications != null && gubejs$modifications.rarity != null) {
            callback.setReturnValue(gubejs$modifications.rarity);
        }
    }

    @Inject(method = "isFireResistant", at = @At("HEAD"), cancellable = true)
    private void gubejs$isFireResistant(CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications != null && gubejs$modifications.fireResistant != null) {
            callback.setReturnValue(gubejs$modifications.fireResistant);
        }
    }

    @Inject(method = "getCraftingRemainingItem", at = @At("HEAD"), cancellable = true)
    private void gubejs$getCraftingRemainingItem(CallbackInfoReturnable<Item> callback) {
        if (gubejs$modifications != null && gubejs$modifications.craftingRemainder != null) {
            callback.setReturnValue(gubejs$modifications.craftingRemainder);
        }
    }

    @Inject(method = "hasCraftingRemainingItem", at = @At("HEAD"), cancellable = true)
    private void gubejs$hasCraftingRemainingItem(CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications != null && gubejs$modifications.craftingRemainder != null) {
            callback.setReturnValue(true);
        }
    }

    @Inject(method = "getFoodProperties", at = @At("HEAD"), cancellable = true)
    private void gubejs$getFoodProperties(CallbackInfoReturnable<FoodProperties> callback) {
        if (gubejs$modifications == null) {
            return;
        }

        if (gubejs$modifications.foodRemoved) {
            callback.setReturnValue(null);
        } else if (gubejs$modifications.food != null) {
            callback.setReturnValue(gubejs$modifications.food);
        }
    }

    /**
     * Kept in step with {@link #gubejs$getFoodProperties}, because the game asks this rather than
     * comparing the properties to null — and an item whose food was added by a script would
     * otherwise carry properties nothing ever consults.
     */
    @Inject(method = "isEdible", at = @At("HEAD"), cancellable = true)
    private void gubejs$isEdible(CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications == null) {
            return;
        }

        if (gubejs$modifications.foodRemoved) {
            callback.setReturnValue(false);
        } else if (gubejs$modifications.food != null) {
            callback.setReturnValue(true);
        }
    }

    // --- appearance ----------------------------------------------------------------------------

    /**
     * Adds the lines a script asked for under the item's name.
     *
     * <p>At the tail, so they come after whatever the item says for itself rather than above its
     * own description.
     */
    @Inject(method = "appendHoverText", at = @At("TAIL"))
    private void gubejs$appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level,
                                       java.util.List<net.minecraft.network.chat.Component> lines,
                                       net.minecraft.world.item.TooltipFlag flag, CallbackInfo callback) {
        if (gubejs$modifications != null && gubejs$modifications.tooltip != null) {
            lines.addAll(gubejs$modifications.tooltip);
        }
    }

    @Inject(method = "isFoil", at = @At("HEAD"), cancellable = true)
    private void gubejs$isFoil(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications != null && gubejs$modifications.glow != null) {
            callback.setReturnValue(gubejs$modifications.glow);
        }
    }

    @Inject(method = "getBarColor", at = @At("HEAD"), cancellable = true)
    private void gubejs$getBarColor(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (gubejs$modifications != null && gubejs$modifications.barColor != null) {
            callback.setReturnValue(gubejs$modifications.barColor);
        }
    }

    /**
     * Answers the bar's length in the thirteen pixels the game draws it in.
     *
     * <p>Rounded rather than truncated, so a bar a script set to 1 is full: thirteen pixels is the
     * whole width, and {@code 12.999} would come out one pixel short of it.
     */
    @Inject(method = "getBarWidth", at = @At("HEAD"), cancellable = true)
    private void gubejs$getBarWidth(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (gubejs$modifications != null && gubejs$modifications.barWidth != null) {
            var clamped = Math.max(0D, Math.min(1D, gubejs$modifications.barWidth));
            callback.setReturnValue((int) Math.round(clamped * 13D));
        }
    }

    /** Kept in step with {@link #gubejs$getBarWidth}: a bar nothing shows is a bar nobody sees. */
    @Inject(method = "isBarVisible", at = @At("HEAD"), cancellable = true)
    private void gubejs$isBarVisible(ItemStack stack, CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$modifications != null && gubejs$modifications.barWidth != null) {
            callback.setReturnValue(true);
        }
    }

    // --- behaviour -----------------------------------------------------------------------------

    @Inject(method = "getUseDuration", at = @At("HEAD"), cancellable = true)
    private void gubejs$getUseDuration(ItemStack stack, CallbackInfoReturnable<Integer> callback) {
        if (gubejs$modifications != null && gubejs$modifications.useDuration != null) {
            callback.setReturnValue(gubejs$modifications.useDuration);
        }
    }

    @Inject(method = "getUseAnimation", at = @At("HEAD"), cancellable = true)
    private void gubejs$getUseAnimation(ItemStack stack,
                                        CallbackInfoReturnable<net.minecraft.world.item.UseAnim> callback) {
        if (gubejs$modifications != null && gubejs$modifications.useAnimation != null) {
            callback.setReturnValue(gubejs$modifications.useAnimation);
        }
    }

    /**
     * Lets a script's {@code use} callback answer a right click.
     *
     * <p>Only when the callback says it did something. A callback that answers nothing leaves the
     * item's own behaviour intact, which is what keeps {@code use} usable on a food.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void gubejs$use(net.minecraft.world.level.Level level,
                            net.minecraft.world.entity.player.Player player,
                            net.minecraft.world.InteractionHand hand,
                            CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<ItemStack>> callback) {
        var callbacks = gubejs$modifications == null ? null : gubejs$modifications.callbacks;

        if (callbacks == null || callbacks.use == null) {
            return;
        }

        var stack = player.getItemInHand(hand);

        if (callbacks.onUse(stack, level, player, hand)) {
            callback.setReturnValue(
                net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide()));
        }
    }

    @Inject(method = "finishUsingItem", at = @At("HEAD"), cancellable = true)
    private void gubejs$finishUsingItem(ItemStack stack, net.minecraft.world.level.Level level,
                                        net.minecraft.world.entity.LivingEntity entity,
                                        CallbackInfoReturnable<ItemStack> callback) {
        var callbacks = gubejs$modifications == null ? null : gubejs$modifications.callbacks;

        if (callbacks == null || callbacks.finishUsing == null) {
            return;
        }

        var result = callbacks.onFinishUsing(stack, level, entity);

        if (result != null) {
            callback.setReturnValue(result);
        }
    }

    @Inject(method = "releaseUsing", at = @At("HEAD"), cancellable = true)
    private void gubejs$releaseUsing(ItemStack stack, net.minecraft.world.level.Level level,
                                     net.minecraft.world.entity.LivingEntity entity, int timeLeft,
                                     CallbackInfo callback) {
        var callbacks = gubejs$modifications == null ? null : gubejs$modifications.callbacks;

        if (callbacks != null && callbacks.releaseUsing != null
            && callbacks.onReleaseUsing(stack, level, entity, timeLeft)) {
            callback.cancel();
        }
    }

    /**
     * Lets a script's {@code hurtEnemy} callback run when the item lands a hit.
     *
     * <p>The return value is the item's answer to "did I do something", which is what decides
     * whether it loses durability — so a callback that returns {@code false} leaves the weapon
     * undamaged.
     */
    @Inject(method = "hurtEnemy", at = @At("HEAD"), cancellable = true)
    private void gubejs$hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target,
                                  net.minecraft.world.entity.LivingEntity attacker,
                                  CallbackInfoReturnable<Boolean> callback) {
        var callbacks = gubejs$modifications == null ? null : gubejs$modifications.callbacks;

        if (callbacks == null || callbacks.hurtEnemy == null) {
            return;
        }

        var answer = callbacks.onHurtEnemy(stack, target, attacker);

        if (answer != null) {
            callback.setReturnValue(answer);
        }
    }
}
