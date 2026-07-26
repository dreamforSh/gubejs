package com.github.gubejs.block;

import com.github.gubejs.level.LevelEventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for every event about one block in the world.
 */
public class BlockEventJS extends LevelEventJS {

    private final BlockPos pos;

    private final BlockState state;

    @Nullable
    private final Player player;

    public BlockEventJS(Level level, BlockPos pos, BlockState state, @Nullable Player player) {
        super(level);
        this.pos = pos;
        this.state = state;
        this.player = player;
    }

    /**
     * Returns where the block is.
     *
     * @return the position
     */
    public BlockPos getPos() {
        return pos;
    }

    /**
     * Returns the block this happened to, as something a script can read and change.
     *
     * <p>{@code event.block.id}, {@code event.block.down}, {@code event.block.set(...)} — the
     * shape a KubeJS pack is written against, and the one that lets a listener walk to a
     * neighbouring block without building a position first.
     *
     * @return the block
     */
    public BlockContainerJS getBlock() {
        return new BlockContainerJS(getLevel(), pos);
    }

    /**
     * Returns the block state as it was when the event fired.
     *
     * <p>Read from the event rather than from the level, which matters on the events that fire
     * around a change: during {@code broken}, the level may already hold air.
     *
     * @return the state
     */
    public BlockState getBlockState() {
        return state;
    }

    /**
     * Returns the block's id, e.g. {@code minecraft:stone}.
     *
     * @return the id
     */
    public String getId() {
        return String.valueOf(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    /**
     * Returns the block entity, for blocks that have one.
     *
     * <p>Not {@code getEntity()}: on the events where something walked into or landed on the
     * block, {@code event.entity} has to mean that entity, the way it does everywhere else.
     *
     * @return the block entity, or {@code null}
     */
    @Nullable
    public BlockEntity getBlockEntity() {
        return getLevel().getBlockEntity(pos);
    }

    /**
     * Returns the player involved.
     *
     * @return the player, or {@code null} when nothing did this on a player's behalf
     */
    @Nullable
    public Player getPlayer() {
        return player;
    }
}
