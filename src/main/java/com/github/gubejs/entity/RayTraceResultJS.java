package com.github.gubejs.entity;

import com.github.gubejs.block.BlockContainerJS;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * What {@code entity.rayTrace(...)} found — the block or entity being looked at.
 *
 * <pre>{@code
 * const hit = event.player.rayTrace(20)
 * if (hit?.block) {
 *     event.player.tell(`You are looking at ${hit.block.id}`)
 * } else if (hit?.entity) {
 *     hit.entity.setSecondsOnFire(4)
 * }
 * }</pre>
 *
 * <p>Only one of {@link #getBlock()} and {@link #getEntity()} is ever set, which is what makes the
 * pair worth checking rather than a type field: a script asks for the one it cares about and gets
 * {@code null} when the trace found the other.
 */
public class RayTraceResultJS {

    /** Who was looking. */
    private final Entity source;

    /** What the game's own trace returned. */
    private final HitResult hit;

    @Nullable
    private final BlockContainerJS block;

    @Nullable
    private final Entity entity;

    public RayTraceResultJS(Entity source, BlockHitResult hit) {
        this.source = source;
        this.hit = hit;
        this.block = new BlockContainerJS(source.level, hit.getBlockPos());
        this.entity = null;
    }

    public RayTraceResultJS(Entity source, EntityHitResult hit) {
        this.source = source;
        this.hit = hit;
        this.block = null;
        this.entity = hit.getEntity();
    }

    /**
     * Returns the entity that was looking.
     *
     * @return the entity the trace started from
     */
    public Entity getSource() {
        return source;
    }

    /**
     * Returns the block that was hit.
     *
     * @return the block, or {@code null} if an entity was hit instead
     */
    @Nullable
    public BlockContainerJS getBlock() {
        return block;
    }

    /**
     * Returns the entity that was hit.
     *
     * @return the entity, or {@code null} if a block was hit instead
     */
    @Nullable
    public Entity getEntity() {
        return entity;
    }

    /**
     * Returns exactly where the trace landed.
     *
     * @return the point, which for a block is on its surface rather than at its corner
     */
    public Vec3 getHitVec() {
        return hit.getLocation();
    }

    /**
     * Returns which face of the block was hit.
     *
     * @return the face, or {@code null} when an entity was hit
     */
    @Nullable
    public Direction getFacing() {
        return hit instanceof BlockHitResult blockHit ? blockHit.getDirection() : null;
    }

    /**
     * Returns how far away the hit was.
     *
     * @return the distance in blocks, measured from the eyes
     */
    public double getDistance() {
        return source.getEyePosition(1F).distanceTo(hit.getLocation());
    }

    /**
     * Returns the game's own result, for anything not covered here.
     *
     * @return the hit result
     */
    public HitResult getHit() {
        return hit;
    }
}
