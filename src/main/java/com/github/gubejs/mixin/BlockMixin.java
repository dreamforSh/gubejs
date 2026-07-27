package com.github.gubejs.mixin;

import com.github.gubejs.core.BlockKJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The half of the block callbacks that {@code Block} declares rather than {@code BlockBehaviour}.
 *
 * <p>Split from {@link BlockBehaviourMixin} only because of where the game put these two methods.
 * The field they read is the one that mixin installs, reached through {@link BlockKJS} — which
 * every block already implements by the time this runs, since {@code Block extends BlockBehaviour}.
 */
@Mixin(Block.class)
public abstract class BlockMixin implements BlockKJS {

    @Inject(method = "stepOn", at = @At("HEAD"))
    private void gubejs$stepOn(Level level, BlockPos pos, BlockState state, Entity entity,
                               CallbackInfo callback) {
        var callbacks = gjs$getCallbacks();

        if (callbacks != null) {
            callbacks.onSteppedOn(level, pos, state, entity);
        }
    }

    @Inject(method = "fallOn", at = @At("HEAD"))
    private void gubejs$fallOn(Level level, BlockState state, BlockPos pos, Entity entity,
                               float fallDistance, CallbackInfo callback) {
        var callbacks = gjs$getCallbacks();

        if (callbacks != null) {
            callbacks.onFallenOn(level, pos, state, entity, fallDistance);
        }
    }
}
