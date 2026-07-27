package com.github.gubejs.bindings.event;

import com.github.gubejs.block.BlockBrokenEventJS;
import com.github.gubejs.block.BlockLeftClickedEventJS;
import com.github.gubejs.block.BlockPlacedEventJS;
import com.github.gubejs.block.BlockRightClickedEventJS;
import com.github.gubejs.block.FarmlandTrampledEventJS;
import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code BlockEvents} global.
 *
 * <p>Every event here takes an optional block, so a listener for one block does not run for every
 * other: {@code BlockEvents.rightClicked('minecraft:furnace', event => ...)}.
 */
public interface BlockEvents {

    EventGroup GROUP = EventGroup.of("BlockEvents");

    /**
     * A block id, keyed by the {@link Block} itself.
     *
     * <p>Blocks are singletons, so the lookup is a reference comparison. A script may pass a block
     * state, a block item, or an id string in the same position.
     */
    Extra SUPPORTS_BLOCK = new Extra()
        .transformer(BlockEvents::transformBlock)
        .display(o -> String.valueOf(ForgeRegistries.BLOCKS.getKey((Block) o)))
        .identity();

    @Nullable
    private static Object transformBlock(Object o) {
        if (o instanceof Block block) {
            return block == Blocks.AIR ? null : block;
        } else if (o instanceof BlockState state) {
            return state.getBlock();
        } else if (o instanceof BlockItem item) {
            return item.getBlock();
        }

        var id = ResourceLocation.tryParse(String.valueOf(o));
        var block = id == null ? null : ForgeRegistries.BLOCKS.getValue(id);
        return block == null || block == Blocks.AIR ? null : block;
    }

    /** A player right-clicking a block. {@code event.cancel()} stops the block responding. */
    /**
     * Changes the properties of blocks that already exist — hardness, blast resistance.
     *
     * <p>Fires once while the game loads, after every mod has registered its blocks.
     */
    EventHandler MODIFICATION = GROUP.startup("modification",
        () -> com.github.gubejs.block.BlockModificationEventJS.class);

    EventHandler RIGHT_CLICKED = GROUP.common("rightClicked", () -> BlockRightClickedEventJS.class)
        .extra(SUPPORTS_BLOCK).hasResult();

    /** A player left-clicking a block, before it starts breaking. */
    EventHandler LEFT_CLICKED = GROUP.common("leftClicked", () -> BlockLeftClickedEventJS.class)
        .extra(SUPPORTS_BLOCK).hasResult();

    /** A block being broken. {@code event.cancel()} leaves it in place. */
    EventHandler BROKEN = GROUP.common("broken", () -> BlockBrokenEventJS.class)
        .extra(SUPPORTS_BLOCK).hasResult();

    /** A block being placed. {@code event.cancel()} stops the placement. */
    EventHandler PLACED = GROUP.common("placed", () -> BlockPlacedEventJS.class)
        .extra(SUPPORTS_BLOCK).hasResult();

    /**
     * Something falling onto farmland hard enough to turn it back into dirt.
     *
     * <p>{@code event.cancel()} saves the crop. The id is the block landed on, which in vanilla is
     * always {@code minecraft:farmland}.
     */
    EventHandler FARMLAND_TRAMPLED = GROUP.common("farmlandTrampled",
        () -> FarmlandTrampledEventJS.class).extra(SUPPORTS_BLOCK).hasResult();

    // --- detectors ---------------------------------------------------------------------------

    /**
     * The redstone signal reaching a detector block changing —
     * {@code BlockEvents.detectorChanged('alarm', event => ...)}.
     *
     * <p>Takes the detector's name rather than a block id, since that is what a script named when
     * it created the block. Fires alongside one of {@link #DETECTOR_POWERED} and
     * {@link #DETECTOR_UNPOWERED}, never on its own.
     */
    EventHandler DETECTOR_CHANGED = GROUP.common("detectorChanged",
        () -> com.github.gubejs.block.DetectorBlockEventJS.class).extra(Extra.STRING);

    /** A detector block going from unpowered to powered. */
    EventHandler DETECTOR_POWERED = GROUP.common("detectorPowered",
        () -> com.github.gubejs.block.DetectorBlockEventJS.class).extra(Extra.STRING);

    /** A detector block losing its signal. */
    EventHandler DETECTOR_UNPOWERED = GROUP.common("detectorUnpowered",
        () -> com.github.gubejs.block.DetectorBlockEventJS.class).extra(Extra.STRING);
}
