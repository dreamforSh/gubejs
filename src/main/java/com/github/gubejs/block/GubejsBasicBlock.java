/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/custom/BasicBlockJS.java
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
package com.github.gubejs.block;

import com.github.gubejs.util.ConsoleJS;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A block a script created: whatever shape, state properties and water behaviour it asked for.
 *
 * <p>A class rather than something hung off the block afterwards, because the game settles both the
 * shapes and the set of block states while the block's constructor runs. Anything handed over later
 * than that is never asked for.
 */
public class GubejsBasicBlock extends Block implements SimpleWaterloggedBlock {

    @Nullable
    private final VoxelShape shape;

    public GubejsBasicBlock(Properties properties, @Nullable VoxelShape shape) {
        super(properties);
        this.shape = shape;
        applyDefaultState();
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // The properties object, because it is the only thing that exists this early -- this runs
        // from the Block constructor, before any field above has been assigned.
        if (properties instanceof GubejsBlockProperties gubejs) {
            for (var property : gubejs.builder.getBlockStateProperties()) {
                builder.add(property);
            }
        }
    }

    /**
     * Applies what {@code defaultState(...)} asked for.
     *
     * <p>After the super constructor, which has already registered the state the game picked, so
     * this replaces it rather than competing with it. Waterlogging defaults to dry: a block placed
     * by a command or by another mod should not arrive full of water it was never in.
     */
    private void applyDefaultState() {
        if (!(properties instanceof GubejsBlockProperties gubejs)) {
            return;
        }

        var builder = gubejs.builder;
        var state = defaultBlockState();

        if (builder.canBeWaterlogged()) {
            state = state.setValue(BlockStateProperties.WATERLOGGED, false);
        }

        for (var entry : builder.getDefaultStateValues().entrySet()) {
            state = withValue(state, entry.getKey(), entry.getValue(), builder);
        }

        registerDefaultState(state);
    }

    /**
     * Sets one property of a state from what a script wrote for it.
     *
     * <p>Parsed from the text form rather than matched against the property's value type, because a
     * property does not expose its values as anything a script's {@code 'north'} or {@code true}
     * could be compared to — but every one of them can read its own serialised form back.
     *
     * @return the new state, or the old one if the value was not one the property accepts
     */
    private static BlockState withValue(BlockState state, String name, @Nullable Object value,
                                        BlockBuilder builder) {
        for (var property : builder.getBlockStateProperties()) {
            if (property.getName().equals(name)) {
                return withParsed(state, property, String.valueOf(value), builder);
            }
        }

        ConsoleJS.STARTUP.error(builder.id + " has no state property called '" + name
            + "'; defaultState can only name the properties the block was given");
        return state;
    }

    private static <T extends Comparable<T>> BlockState withParsed(BlockState state,
                                                                   Property<T> property,
                                                                   String value,
                                                                   BlockBuilder builder) {
        var parsed = property.getValue(value);

        if (parsed.isEmpty()) {
            ConsoleJS.STARTUP.error(builder.id + " cannot have '" + property.getName() + "' set to '"
                + value + "'; the property accepts " + property.getPossibleValues());
            return state;
        }

        return state.setValue(property, parsed.get());
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                               CollisionContext context) {
        return shape == null ? super.getShape(state, level, pos, context) : shape;
    }

    // --- water ---------------------------------------------------------------------------------

    /**
     * Places the block waterlogged when it is placed in water.
     *
     * <p>Worked out from where it is placed rather than left to a script, because there is nothing
     * else it could sensibly be: a slab put into a water column is wet, and one put in the air is
     * not. A block without the property gets its default state, as before.
     */
    @Override
    @Nullable
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = defaultBlockState();

        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return state;
        }

        var fluid = context.getLevel().getFluidState(context.getClickedPos());
        return state.setValue(BlockStateProperties.WATERLOGGED, fluid.getType() == Fluids.WATER);
    }

    @Override
    @SuppressWarnings("deprecation")
    public FluidState getFluidState(BlockState state) {
        return state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)
            ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    /**
     * Whether light passes down through the block.
     *
     * <p>Water in the block stops it, which is why this is not left to the properties alone: a
     * waterlogged block that still let full skylight through would light the column below it as if
     * the water were not there.
     */
    @Override
    public boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        if (state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)) {
            return false;
        }

        return super.propagatesSkylightDown(state, level, pos);
    }

    /**
     * Lets water out into a neighbouring space that just opened up.
     *
     * <p>Every waterlogged block in the game does this; without it the water inside a block sits
     * there when the block beside it is mined, instead of flowing out.
     */
    @Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        if (state.getOptionalValue(BlockStateProperties.WATERLOGGED).orElse(false)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /*
     * The three methods below exist because SimpleWaterloggedBlock is implemented at class level
     * while WATERLOGGED is only present on the blocks that asked for it: every one of the
     * interface's defaults reads the property unguarded, and StateHolder.getValue throws on a
     * property the state does not have. A bucket click on any script block without waterlogged()
     * reaches them, so each one answers for itself before delegating.
     */

    @Override
    public boolean canPlaceLiquid(BlockGetter level, BlockPos pos, BlockState state, Fluid fluid) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return false;
        }

        return SimpleWaterloggedBlock.super.canPlaceLiquid(level, pos, state, fluid);
    }

    @Override
    public boolean placeLiquid(LevelAccessor level, BlockPos pos, BlockState state,
                               FluidState fluidState) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return false;
        }

        return SimpleWaterloggedBlock.super.placeLiquid(level, pos, state, fluidState);
    }

    @Override
    public ItemStack pickupBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!state.hasProperty(BlockStateProperties.WATERLOGGED)) {
            return ItemStack.EMPTY;
        }

        return SimpleWaterloggedBlock.super.pickupBlock(level, pos, state);
    }
}
