package com.github.gubejs.block;

import com.github.gubejs.util.NbtHelper;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * One block in the world: a level and a position, and everything a script wants to do with the
 * pair.
 *
 * <pre>{@code
 * BlockEvents.rightClicked(event => {
 *     const below = event.block.down
 *     if (below.id === 'minecraft:diamond_block') {
 *         below.set('minecraft:emerald_block')
 *     }
 * })
 * }</pre>
 *
 * <p>This is what {@code event.block} is, and what {@code level.getBlock(x, y, z)} returns. A
 * position on its own could not answer {@code .id} or accept {@code .set(...)}, and a block state
 * on its own does not know where it is — a pack needs both together, and holding them apart is
 * what makes scripting blocks tedious.
 *
 * <p>Nothing is cached. The state is read from the level each time it is asked for, so a script
 * that changes a block and reads it back sees the change.
 */
public class BlockContainerJS {

    /** The level this block is in. */
    protected final Level level;

    /** Where the block is. */
    protected final BlockPos pos;

    public BlockContainerJS(Level level, BlockPos pos) {
        this.level = level;
        this.pos = pos;
    }

    // --- where ---------------------------------------------------------------------------------

    /**
     * Returns the level this block is in.
     *
     * @return the level
     */
    public Level getLevel() {
        return level;
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
     * Returns the x coordinate.
     *
     * @return the x
     */
    public int getX() {
        return pos.getX();
    }

    /**
     * Returns the y coordinate.
     *
     * @return the y
     */
    public int getY() {
        return pos.getY();
    }

    /**
     * Returns the z coordinate.
     *
     * @return the z
     */
    public int getZ() {
        return pos.getZ();
    }

    /**
     * Returns the dimension id, e.g. {@code minecraft:overworld}.
     *
     * @return the id
     */
    public String getDimension() {
        return level.dimension().location().toString();
    }

    // --- what ----------------------------------------------------------------------------------

    /**
     * Returns the block state.
     *
     * @return the state, read fresh from the level
     */
    public BlockState getBlockState() {
        return level.getBlockState(pos);
    }

    /**
     * Returns the block.
     *
     * @return the block
     */
    public Block getBlock() {
        return getBlockState().getBlock();
    }

    /**
     * Returns the block's id, e.g. {@code minecraft:stone}.
     *
     * @return the id
     */
    public String getId() {
        return String.valueOf(ForgeRegistries.BLOCKS.getKey(getBlock()));
    }

    /**
     * Returns the block state's properties, as strings.
     *
     * <p>{@code event.block.properties.facing === 'north'} — the values are the names the state
     * is written with in a blockstate file and in a {@code /setblock} command, not the enum
     * constants, so a script compares them to what it sees in game.
     *
     * @return the properties
     */
    public Map<String, String> getProperties() {
        var map = new LinkedHashMap<String, String>();
        var state = getBlockState();

        for (var property : state.getProperties()) {
            map.put(property.getName(), nameOf(state, property));
        }

        return map;
    }

    /**
     * Returns whether the block is in a tag.
     *
     * @param tag the tag id, with or without the leading {@code #}
     * @return {@code true} if it is
     */
    public boolean hasTag(String tag) {
        var id = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        return id != null && getBlockState().is(
            net.minecraft.tags.TagKey.create(net.minecraft.core.Registry.BLOCK_REGISTRY, id));
    }

    /**
     * Returns whether there is nothing here.
     *
     * @return {@code true} for air
     */
    public boolean isAir() {
        return getBlockState().isAir();
    }

    /**
     * Returns the fluid in this block, which is water for a waterlogged one.
     *
     * @return the fluid
     */
    public Fluid getFluid() {
        return level.getFluidState(pos).getType();
    }

    /**
     * Returns how bright it is here.
     *
     * @return the light level, 0-15
     */
    public int getLight() {
        return level.getMaxLocalRawBrightness(pos);
    }

    /**
     * Returns the biome id at this position.
     *
     * @return the id, e.g. {@code minecraft:plains}
     */
    public String getBiomeId() {
        return level.getBiome(pos).unwrapKey()
            .map(key -> key.location().toString()).orElse("minecraft:plains");
    }

    /**
     * Returns what this block drops when broken, as it would drop right now.
     *
     * @return the drops, empty for a block that drops nothing
     */
    public List<ItemStack> getDrops() {
        return level instanceof ServerLevel serverLevel
            ? Block.getDrops(getBlockState(), serverLevel, pos, getEntity())
            : List.of();
    }

    // --- changing ------------------------------------------------------------------------------

    /**
     * Replaces the block.
     *
     * @param value a block id, a block state string like {@code 'minecraft:furnace[facing=north]'},
     *     or a {@link BlockState}
     */
    public void set(@Nullable Object value) {
        set(value, null, 3);
    }

    /**
     * Replaces the block, setting state properties at the same time.
     *
     * @param value what to put here
     * @param properties the block state properties to set, as strings
     */
    public void set(@Nullable Object value, @Nullable Object properties) {
        set(value, properties, 3);
    }

    /**
     * Replaces the block, with control over what the change notifies.
     *
     * @param value what to put here
     * @param properties the block state properties to set, or {@code null}
     * @param flags the vanilla block update flags — {@code 3} is the normal "update and notify",
     *     {@code 2} skips the neighbour update, {@code 0} tells nobody
     */
    public void set(@Nullable Object value, @Nullable Object properties, int flags) {
        var state = BlockStateJS.of(value);

        if (properties != null && ValueUtils.unwrap(properties) instanceof Map<?, ?> map) {
            state = BlockStateJS.withProperties(state, map);
        }

        level.setBlock(pos, state, flags);
    }

    /**
     * Removes the block, leaving air.
     */
    public void destroy() {
        level.destroyBlock(pos, false);
    }

    /**
     * Removes the block and drops what it would have dropped.
     *
     * @param tool the tool to break it with, which decides the drops for anything needing one
     */
    public void destroy(@Nullable Object tool) {
        if (level instanceof ServerLevel serverLevel) {
            Block.dropResources(getBlockState(), serverLevel, pos, getEntity(), null,
                com.github.gubejs.item.ItemStackJS.of(tool));
        }

        level.removeBlock(pos, false);
    }

    /**
     * Sets one block state property, keeping the rest.
     *
     * @param key the property name, as it is spelled in a blockstate file
     * @param value the value, as a string
     */
    public void setProperty(String key, Object value) {
        level.setBlock(pos,
            BlockStateJS.withProperties(getBlockState(), Map.of(key, value)), 3);
    }

    // --- block entity --------------------------------------------------------------------------

    /**
     * Returns the block entity, for blocks that have one.
     *
     * @return the block entity, or {@code null}
     */
    @Nullable
    public BlockEntity getEntity() {
        return level.getBlockEntity(pos);
    }

    /**
     * Returns the block entity's data.
     *
     * <p>Reading a chest's {@code Items} or a sign's text is what this is for. The tag is a copy;
     * {@link #setEntityData} writes one back.
     *
     * @return the data, or {@code null} for a block with no block entity
     */
    @Nullable
    public CompoundTag getEntityData() {
        var entity = getEntity();
        return entity == null ? null : entity.saveWithFullMetadata();
    }

    /**
     * Writes data back into the block entity.
     *
     * @param value the tag, or an object to convert into one
     */
    public void setEntityData(@Nullable Object value) {
        var entity = getEntity();

        if (entity == null) {
            return;
        }

        entity.load(NbtHelper.compound(value));
        entity.setChanged();

        // Without this the client keeps showing what the block entity held before, since a
        // block entity only syncs when the game decides something changed.
        if (level instanceof ServerLevel) {
            var state = getBlockState();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    /**
     * Returns the inventory of a block that has one — a chest, a furnace, a hopper.
     *
     * @return the item handler, or {@code null} if this block holds no items
     */
    @Nullable
    public net.minecraftforge.items.IItemHandler getInventory() {
        var entity = getEntity();
        return entity == null ? null
            : entity.getCapability(net.minecraftforge.common.capabilities.ForgeCapabilities.ITEM_HANDLER)
            .orElse(null);
    }

    // --- neighbours ----------------------------------------------------------------------------

    /**
     * Returns the block one step in a direction.
     *
     * @param direction which way
     * @return the neighbour
     */
    public BlockContainerJS offset(Direction direction) {
        return new BlockContainerJS(level, pos.relative(direction));
    }

    /**
     * Returns the block a number of steps in a direction.
     *
     * @param direction which way
     * @param distance how far
     * @return the neighbour
     */
    public BlockContainerJS offset(Direction direction, int distance) {
        return new BlockContainerJS(level, pos.relative(direction, distance));
    }

    /**
     * Returns the block at an offset.
     *
     * @param x how far along x
     * @param y how far along y
     * @param z how far along z
     * @return the block there
     */
    public BlockContainerJS offset(int x, int y, int z) {
        return new BlockContainerJS(level, pos.offset(x, y, z));
    }

    /** @return the block below */
    public BlockContainerJS getDown() {
        return offset(Direction.DOWN);
    }

    /** @return the block above */
    public BlockContainerJS getUp() {
        return offset(Direction.UP);
    }

    /** @return the block to the north */
    public BlockContainerJS getNorth() {
        return offset(Direction.NORTH);
    }

    /** @return the block to the south */
    public BlockContainerJS getSouth() {
        return offset(Direction.SOUTH);
    }

    /** @return the block to the west */
    public BlockContainerJS getWest() {
        return offset(Direction.WEST);
    }

    /** @return the block to the east */
    public BlockContainerJS getEast() {
        return offset(Direction.EAST);
    }

    /**
     * Returns whether this is the same block, in the same place, in the same level.
     *
     * @param other what to compare with
     * @return {@code true} if they are the same position in the same level
     */
    @Override
    public boolean equals(Object other) {
        return other instanceof BlockContainerJS block
            && block.level == level && block.pos.equals(pos);
    }

    @Override
    public int hashCode() {
        return pos.hashCode();
    }

    @Override
    public String toString() {
        return getId() + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
    }

    /** Reads a property's value as the string a blockstate file would spell it with. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String nameOf(BlockState state,
                                 net.minecraft.world.level.block.state.properties.Property<?> property) {
        return ((net.minecraft.world.level.block.state.properties.Property) property)
            .getName(state.getValue(property));
    }
}
