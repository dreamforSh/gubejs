package com.github.gubejs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * What a block's own callback is handed — {@code event.create('x').steppedOn(event => ...)}.
 *
 * <p>The same shape as every other block event, so a listener written against
 * {@code BlockEvents.rightClicked} reads the same here: {@code event.block}, {@code event.level},
 * {@code event.pos}. What it adds is the entity the callback is about, for the several callbacks
 * that are about one.
 */
public class BlockCallbackEventJS extends BlockEventJS {

    @Nullable
    private final Entity entity;

    private final float fallDistance;

    public BlockCallbackEventJS(Level level, BlockPos pos, BlockState state,
                                @Nullable Entity entity, float fallDistance) {
        super(level, pos, state, entity instanceof Player player ? player : null);
        this.entity = entity;
        this.fallDistance = fallDistance;
    }

    public BlockCallbackEventJS(Level level, BlockPos pos, BlockState state) {
        this(level, pos, state, null, 0F);
    }

    /**
     * Returns the entity this is about.
     *
     * @return the entity that stepped on or landed on the block, or {@code null} for the callbacks
     *     that are not about one
     */
    @Nullable
    public Entity getEntity() {
        return entity;
    }

    /**
     * Returns how far the entity fell before landing here.
     *
     * @return the distance in blocks, {@code 0} outside {@code fallenOn}
     */
    public float getFallDistance() {
        return fallDistance;
    }
}
