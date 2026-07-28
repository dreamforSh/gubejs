/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/mixin/common/BlockBehaviourMixin.java
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

import com.github.gubejs.block.BlockCallbacks;
import com.github.gubejs.core.BlockKJS;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives every block somewhere to keep the callbacks a script gave it, and runs them.
 *
 * <p>On {@code BlockBehaviour} rather than on a block class of this mod's own, which is what makes
 * the same callbacks work on a block a script created and on one that shipped with the game:
 * {@code BlockEvents.modification} can hand {@code minecraft:stone} a random tick.
 *
 * <p>The catch is inheritance. These are the game's own methods and a subclass may override them
 * without calling {@code super}, in which case nothing here runs — every crop and sapling overrides
 * {@code randomTick}, and slime overrides {@code fallOn}. Nothing a script creates does, unless it
 * asked for the {@code crop} type.
 *
 * <p>Each injection is a null comparison against a field that stays null for all but a handful of
 * blocks, which is what keeps {@code stepOn} — called for every entity on every block it walks over
 * — unaffected for the rest of the game.
 */
@Mixin(BlockBehaviour.class)
public abstract class BlockBehaviourMixin implements BlockKJS {

    @Unique
    @Nullable
    private BlockCallbacks gubejs$callbacks;

    @Override
    @Nullable
    public BlockCallbacks gjs$getCallbacks() {
        return gubejs$callbacks;
    }

    @Override
    public void gjs$setCallbacks(@Nullable BlockCallbacks callbacks) {
        gubejs$callbacks = callbacks;
    }

    @Override
    public BlockCallbacks gjs$getOrCreateCallbacks() {
        if (gubejs$callbacks == null) {
            gubejs$callbacks = new BlockCallbacks();
        }

        return gubejs$callbacks;
    }

    @Inject(method = "randomTick", at = @At("HEAD"))
    private void gubejs$randomTick(BlockState state, ServerLevel level, BlockPos pos,
                                   RandomSource random, CallbackInfo callback) {
        if (gubejs$callbacks != null) {
            gubejs$callbacks.onRandomTick(level, pos, state);
        }
    }

    /**
     * The full descriptor, because {@code canBeReplaced} is also declared for fluids and the two
     * would otherwise both match.
     */
    @Inject(method = "canBeReplaced(Lnet/minecraft/world/level/block/state/BlockState;"
        + "Lnet/minecraft/world/item/context/BlockPlaceContext;)Z",
        at = @At("HEAD"), cancellable = true)
    private void gubejs$canBeReplaced(BlockState state, BlockPlaceContext context,
                                      CallbackInfoReturnable<Boolean> callback) {
        if (gubejs$callbacks == null || gubejs$callbacks.canBeReplaced == null) {
            return;
        }

        // The block's own answer is worked out first so the callback can decline to decide and
        // leave it alone, which is what returning nothing from the callback means.
        var fallback = state.getMaterial().isReplaceable()
            && (context.getItemInHand().isEmpty()
            || !context.getItemInHand().is(state.getBlock().asItem()));

        callback.setReturnValue(gubejs$callbacks.onCanBeReplaced(context.getLevel(),
            context.getClickedPos(), state, context.getPlayer(), fallback));
    }

    /**
     * Lets a script's {@code rightClick} callback answer a click on the block.
     *
     * <p>{@code CONSUME} rather than {@code SUCCESS}, because the arm swing belongs to the item and
     * this click was the block's: a block that opened something does not make the player wave.
     */
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void gubejs$use(BlockState state, net.minecraft.world.level.Level level, BlockPos pos,
                            net.minecraft.world.entity.player.Player player,
                            net.minecraft.world.InteractionHand hand,
                            net.minecraft.world.phys.BlockHitResult hit,
                            CallbackInfoReturnable<net.minecraft.world.InteractionResult> callback) {
        if (gubejs$callbacks == null || gubejs$callbacks.rightClicked == null) {
            return;
        }

        if (gubejs$callbacks.onRightClicked(level, pos, state, player)) {
            callback.setReturnValue(net.minecraft.world.InteractionResult.CONSUME);
        }
    }
}
