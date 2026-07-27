package com.github.gubejs.block;

import com.github.gubejs.level.LevelEventJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/**
 * A detector block seeing the redstone signal around it change.
 *
 * <pre>{@code
 * BlockEvents.detectorPowered('alarm', event => {
 *     event.block.up.set('minecraft:redstone_lamp', { lit: 'true' })
 * })
 * }</pre>
 */
public class DetectorBlockEventJS extends LevelEventJS {

    private final String detectorId;

    private final BlockPos pos;

    private final boolean powered;

    public DetectorBlockEventJS(String detectorId, Level level, BlockPos pos, boolean powered) {
        super(level);
        this.detectorId = detectorId;
        this.pos = pos;
        this.powered = powered;
    }

    /**
     * Returns which detector this is, which is what the listener filtered on.
     *
     * @return the detector id
     */
    public String getDetectorId() {
        return detectorId;
    }

    /**
     * Returns whether the detector is now powered.
     *
     * <p>Always {@code true} in {@code detectorPowered} and {@code false} in
     * {@code detectorUnpowered}; the flag is only worth reading in {@code detectorChanged}.
     *
     * @return whether it is powered
     */
    public boolean isPowered() {
        return powered;
    }

    /**
     * Returns the detector block itself.
     *
     * @return the block in the world
     */
    public BlockContainerJS getBlock() {
        return new BlockContainerJS(getLevel(), pos);
    }
}
