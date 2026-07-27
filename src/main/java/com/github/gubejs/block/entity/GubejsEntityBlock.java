package com.github.gubejs.block.entity;

import com.github.gubejs.block.BlockBuilder;
import com.github.gubejs.event.EventExit;
import com.github.gubejs.util.ConsoleJS;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * A block that has a block entity, because a script asked it to remember something.
 *
 * <p>Separate from the plain block rather than a flag on it: {@link EntityBlock} is an interface,
 * and whether the game ever asks a block for an entity is decided by whether the block implements
 * it. A flag checked at runtime would be asked too late.
 */
public class GubejsEntityBlock extends Block implements EntityBlock {

    private final BlockBuilder builder;

    public GubejsEntityBlock(Properties properties, BlockBuilder builder) {
        super(properties);
        this.builder = builder;
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        var type = builder.getBlockEntityType();
        var entityBuilder = builder.getBlockEntityBuilder();

        return type == null || entityBuilder == null ? null
            : new GubejsBlockEntity(type, pos, state, entityBuilder);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state,
                                                                 BlockEntityType<T> type) {
        var entityBuilder = builder.getBlockEntityBuilder();

        // No ticker at all when the script gave it nothing to do. A ticker that returns
        // immediately still costs a virtual call per block per tick, and a pack placing a few
        // thousand of these would feel it.
        if (entityBuilder == null || !entityBuilder.ticks() || level.isClientSide()) {
            return null;
        }

        return (tickLevel, pos, tickState, entity) -> {
            if (!(entity instanceof GubejsBlockEntity gubejs)) {
                return;
            }

            if (tickLevel.getGameTime() % entityBuilder.tickInterval != entityBuilder.tickOffset) {
                return;
            }

            try {
                entityBuilder.serverTick.onEvent(new BlockEntityTickEventJS(gubejs));
            } catch (Throwable ex) {
                // event.cancel() means "stop here", which has already happened by the time the
                // exit reaches us. Only a real failure is worth reporting -- and reporting it
                // once a tick would fill the log, so the callback is dropped after it throws.
                if (EventExit.unwrap(ex) == null) {
                    ConsoleJS.SERVER.handleError(ex,
                        "Error ticking " + builder.id + "; the callback has been removed");
                    entityBuilder.serverTick = null;
                }
            }
        };
    }

    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState,
                         boolean moved) {
        // Before the super call, which is what actually removes the block entity.
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos)
            instanceof GubejsBlockEntity entity) {
            var inventory = entity.getInventory();

            if (inventory != null) {
                for (var slot = 0; slot < inventory.getSlots(); slot++) {
                    var stack = inventory.getStackInSlot(slot);

                    if (!stack.isEmpty()) {
                        // Dropped rather than deleted: a player breaking a machine expects what
                        // was inside it back, and nothing else in the game would return it.
                        net.minecraft.world.Containers.dropItemStack(level,
                            pos.getX(), pos.getY(), pos.getZ(), stack);
                    }
                }
            }
        }

        super.onRemove(state, level, pos, newState, moved);
    }
}
