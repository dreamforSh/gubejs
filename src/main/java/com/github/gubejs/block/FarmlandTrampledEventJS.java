package com.github.gubejs.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Something landing on farmland hard enough to turn it back into dirt.
 *
 * <p>{@code event.cancel()} saves the crop, which is the one-line answer to "stop mobs ruining my
 * farm".
 */
public final class FarmlandTrampledEventJS extends BlockEventJS {

    private final Entity entity;

    private final float distance;

    public FarmlandTrampledEventJS(Level level, BlockPos pos, BlockState state, Entity entity,
                                   float distance) {
        super(level, pos, state, entity instanceof Player player ? player : null);
        this.entity = entity;
        this.distance = distance;
    }

    /**
     * Returns what landed on the farmland.
     *
     * @return the entity, which is often not a player
     */
    public Entity getEntity() {
        return entity;
    }

    /**
     * Returns how far it fell.
     *
     * @return the fall distance in blocks
     */
    public float getDistance() {
        return distance;
    }
}
