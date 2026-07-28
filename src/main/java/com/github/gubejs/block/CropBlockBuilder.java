/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/custom/CropBlockBuilder.java
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

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a crop — {@code event.create('rice', 'crop').age(4).crop('mypack:rice', 2)}.
 *
 * <p>A crop grows through a fixed number of stages, needs farmland under it and light above it, and
 * is planted from a seed item. All of that comes from vanilla's own crop; what this adds is the
 * stage count, since vanilla's is fixed at eight and a script may want four, and what the ripe crop
 * gives back.
 *
 * <p>This is the one block type whose models are not derived from a single texture — a crop is a
 * texture per stage and nothing can invent the ones in between. The paths are conventional, so a
 * pack supplying {@code textures/block/<path>_stage0.png} up to the last stage gets the blockstate
 * and every model generated. A missing stage texture shows as the missing-texture checkerboard for
 * that stage alone.
 *
 * <p>The seed is what plants the crop and what it drops before it is ripe, so one is created for it
 * — an item at the crop's own id, planted the way wheat seeds are, drawn from
 * {@code textures/item/<path>.png}. A crop that should grow from an item a pack already has says so
 * with {@link #seed}, and then no item of its own is created.
 *
 * <p>What a ripe crop drops does not go through a loot table. A loot table is a datapack file and a
 * builder has nowhere to put one that survives a reload, so the block answers for its own drops
 * instead — which is why {@link #crop} takes a chance rather than the fortune curve a datapack would
 * write.
 */
public class CropBlockBuilder extends BlockBuilder {

    /** How many growth stages, counting the ripe one. */
    protected int age = 8;

    /** What plants the crop, as an item id, or {@code null} for the seed created with it. */
    @Nullable
    protected Object seed;

    /** What a ripe crop gives, or {@code null} for nothing but its seed back. */
    @Nullable
    protected Object crop;

    /** How much of it, on average; see {@link #crop(Object, double)}. */
    protected double cropChance = 1D;

    /** Whether breaking the crop returns the seed that planted it. */
    protected boolean dropSeed = true;

    /** Whether bone meal grows it. */
    protected boolean bonemeal = true;

    /** What the crop may be planted on, as block ids and tags; empty for farmland. */
    protected final List<String> surviveOn = new ArrayList<>();

    /** The shape of one stage, for a crop that is not simply taller as it grows. */
    protected final Map<Integer, VoxelShape> ageShapes = new LinkedHashMap<>();

    /** {@link #surviveOn} once the block registry can answer for it; see {@link #canPlantOn}. */
    @Nullable
    private List<Predicate<BlockState>> plantableOn;

    /** {@link #crop} once the item registry can answer for it. */
    @Nullable
    private ItemStack cropStack;

    public CropBlockBuilder(ResourceLocation id) {
        super(id);
        this.material = net.minecraft.world.level.material.Material.PLANT;
        this.soundType = net.minecraft.world.level.block.SoundType.CROP;
        this.hardness = 0F;
        this.resistance = 0F;
        this.noCollision = true;
        // A crop's model is a pair of crossed planes with transparency around them, which the solid
        // pass would draw as black. Every crop in the game is drawn in the cutout pass.
        this.renderType = "cutout";
        // The item this creates is the seed, not a building block.
        this.tab = net.minecraft.world.item.CreativeModeTab.TAB_MISC;
    }

    /**
     * Sets how many growth stages the crop has.
     *
     * @param age the number of stages, counting the ripe one; vanilla wheat has 8
     * @return this builder
     */
    public CropBlockBuilder age(int age) {
        this.age = Math.max(2, age);
        return this;
    }

    /**
     * Plants the crop from an item that already exists, instead of creating one.
     *
     * <p>Also stops the seed item being created: two items that both plant the same crop, sitting
     * next to each other in the creative menu, is never what a pack meant.
     *
     * @param seed the item id
     * @return this builder
     */
    public CropBlockBuilder seed(Object seed) {
        this.seed = ValueUtils.unwrap(seed);
        this.createItem = false;
        return this;
    }

    /**
     * Sets what a ripe crop gives when broken.
     *
     * @param output the item, as an id or a stack
     * @return this builder
     */
    public CropBlockBuilder crop(Object output) {
        return crop(output, 1D);
    }

    /**
     * Sets what a ripe crop gives, and how much.
     *
     * <pre>{@code
     * event.create('rice', 'crop').crop('mypack:rice', 2.5)
     * }</pre>
     *
     * <p>Below 1 the chance is the odds of getting the drop at all; above 1 it is how many are
     * given on average, so {@code 2.5} is two every time and a third half the time. Fortune does not
     * enter into it — the fortune curves a crop's drops usually have live in a loot table, and this
     * crop has none.
     *
     * @param output the item, as an id or a stack
     * @param chance how likely, or how many
     * @return this builder
     */
    public CropBlockBuilder crop(Object output, double chance) {
        this.crop = ValueUtils.unwrap(output);
        this.cropChance = Math.max(0D, chance);
        return this;
    }

    /**
     * Sets whether breaking the crop gives its seed back.
     *
     * <p>On by default, and for an unripe crop the seed is all there is to get: a crop that drops
     * nothing until it is grown is a crop a player cannot move.
     *
     * @param dropSeed whether the seed drops
     * @return this builder
     */
    public CropBlockBuilder dropSeed(boolean dropSeed) {
        this.dropSeed = dropSeed;
        return this;
    }

    /**
     * Sets whether bone meal grows the crop.
     *
     * @param bonemeal {@code false} for a crop that can only be waited on
     * @return this builder
     */
    public CropBlockBuilder bonemeal(boolean bonemeal) {
        this.bonemeal = bonemeal;
        return this;
    }

    /**
     * Sets what the crop may be planted on, and so what it stays planted on.
     *
     * <pre>{@code
     * event.create('kelp_pod', 'crop').survive('minecraft:sand', '#minecraft:dirt')
     * }</pre>
     *
     * <p>Farmland only when nothing is named. The light the crop needs is separate and vanilla's:
     * a crop in the dark breaks whatever it is standing on.
     *
     * @param blocks one or more block ids, or tags with a leading {@code #}
     * @return this builder
     */
    public CropBlockBuilder survive(Object... blocks) {
        for (var block : blocks) {
            for (var value : ValueUtils.listOf(block)) {
                var text = String.valueOf(ValueUtils.unwrap(value)).trim();

                if (!text.isEmpty()) {
                    surviveOn.add(text);
                }
            }
        }

        plantableOn = null;
        return this;
    }

    /**
     * Runs a callback instead of growing the crop, on every tick the game would have grown it.
     *
     * <pre>{@code
     * event.create('mushroom_ring', 'crop').growTick(event => {
     *     if (event.block.up.id === 'minecraft:air') {
     *         event.block.set(event.block.id, { age: 1 })
     *     }
     * })
     * }</pre>
     *
     * <p>Instead of, not as well as: the callback owns growth once it is set, so a crop that should
     * still ripen has to say so by setting its age. Which is the only arrangement that lets a
     * callback decide <em>whether</em> to grow, and the reason vanilla's light and crowding rules no
     * longer apply once one is set.
     *
     * <p>The same slot as {@link #randomTick}, since a growth tick is a random tick — how often it
     * fires is the game's business, roughly once every 47 seconds per crop.
     *
     * @param callback what to run
     * @return this builder
     */
    public CropBlockBuilder growTick(Consumer<BlockCallbackEventJS> callback) {
        callbacks().setRandomTick(callback);
        return this;
    }

    /**
     * Sets the shape of one growth stage, in the sixteenths a model is measured in.
     *
     * <p>Only the outline and what an arrow flies through; a crop has no collision either way. Any
     * stage left unset is as tall as it has grown, which is what a crop wants often enough that
     * most never set one.
     *
     * @param age which stage, 0 for freshly planted
     * @param x0 first corner, 0-16
     * @param y0 first corner, 0-16
     * @param z0 first corner, 0-16
     * @param x1 opposite corner, 0-16
     * @param y1 opposite corner, 0-16
     * @param z1 opposite corner, 0-16
     * @return this builder
     */
    public CropBlockBuilder ageBox(int age, double x0, double y0, double z0,
                                   double x1, double y1, double z1) {
        ageShapes.put(age, Block.box(x0, y0, z0, x1, y1, z1));
        return this;
    }

    /**
     * Whether the crop may be planted on the block below it.
     *
     * <p>Resolved on first use rather than as the script runs, because neither the block registry
     * nor the block tags are readable while a startup script is still queuing builders.
     *
     * @param state the state below the crop
     * @return whether it will do
     */
    boolean canPlantOn(BlockState state) {
        if (plantableOn == null) {
            var built = new ArrayList<Predicate<BlockState>>(surviveOn.size());

            for (var entry : surviveOn) {
                var tag = entry.startsWith("#");
                var id = ResourceLocation.tryParse(tag ? entry.substring(1) : entry);

                if (id == null) {
                    ConsoleJS.STARTUP.error("Crop " + this.id + " cannot be planted on '" + entry
                        + "': that is not a block id or a tag");
                } else if (tag) {
                    var key = TagKey.create(Registry.BLOCK_REGISTRY, id);
                    built.add(below -> below.is(key));
                } else {
                    var block = Registry.BLOCK.get(id);

                    if (block == net.minecraft.world.level.block.Blocks.AIR) {
                        ConsoleJS.STARTUP.error("Crop " + this.id + " cannot be planted on '"
                            + entry + "': there is no such block");
                    } else {
                        built.add(below -> below.is(block));
                    }
                }
            }

            plantableOn = built;
        }

        for (var predicate : plantableOn) {
            if (predicate.test(state)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns what a ripe crop gives, as one of them.
     *
     * <p>Resolved on first use, for the same reason {@link #canPlantOn} is: the item may be one
     * another builder is still queued to create.
     *
     * @return the stack, empty when the script named nothing
     */
    ItemStack cropDrop() {
        if (cropStack == null) {
            cropStack = crop == null ? ItemStack.EMPTY : ItemStackJS.of(crop);

            if (crop != null && cropStack.isEmpty()) {
                ConsoleJS.STARTUP.error("Crop " + id + " drops '" + crop
                    + "', which is not an item");
            }
        }

        return cropStack;
    }

    @Override
    protected BlockBehaviour.Properties createProperties() {
        // Every crop in the game is constructed with this on, and it is the flag rather than the
        // block's own isRandomlyTicking that decides whether the game ticks the block at all -- a
        // crop without it stays at stage zero for ever.
        return super.createProperties().randomTicks();
    }

    @Override
    @Nullable
    public Item createBlockItem() {
        if (!createItem) {
            return null;
        }

        // What wheat seeds are: an item that plants a block and takes its name from itself rather
        // than from the block, so 'Rice Seeds' and 'Rice' can be two different names.
        return new ItemNameBlockItem(get(), new Item.Properties().tab(tab));
    }

    @Override
    public Map<String, String> getTranslations() {
        var translations = new LinkedHashMap<>(super.getTranslations());

        if (createItem) {
            translations.put("item." + id.getNamespace() + "."
                + id.getPath().replace('/', '.'), getDisplayName() + " Seeds");
        }

        return translations;
    }

    @Override
    public Block createObject() {
        block = ScriptCropBlock.create(createProperties(), this);
        return block;
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var namespace = id.getNamespace();
        var path = id.getPath();
        var body = new StringBuilder("{\n  \"variants\": {\n");

        for (var stage = 0; stage < age; stage++) {
            body.append("    \"age=").append(stage).append("\": { \"model\": \"")
                .append(namespace).append(":block/").append(path)
                .append("_stage").append(stage).append("\" },\n");

            assets.put("assets/" + namespace + "/models/block/" + path + "_stage" + stage + ".json",
                """
                {
                  "parent": "minecraft:block/crop",
                  "textures": {
                    "crop": "%s:block/%s_stage%d"
                  }
                }""".formatted(namespace, path, stage));
        }

        body.setLength(body.length() - 2);
        assets.put("assets/" + namespace + "/blockstates/" + path + ".json",
            body.append("\n  }\n}").toString());

        if (createItem) {
            // A flat sprite, not the block's model: the seed is an item in its own right and a crop
            // stage held in the hand is a pair of planes seen edge-on.
            assets.put("assets/" + namespace + "/models/item/" + path + ".json",
                """
                {
                  "parent": "minecraft:item/generated",
                  "textures": {
                    "layer0": "%s:item/%s"
                  }
                }""".formatted(namespace, path));
        }

        return assets;
    }

    /** Registers the crop type scripts can create. */
    public static void registerTypes() {
        RegistryInfo.BLOCK.addType("crop", CropBlockBuilder::new);
    }

    /**
     * The crop itself.
     *
     * <p>The age property is built once per stage count and shared by every crop with that many
     * stages, because a block state property is compared by identity — two properties both called
     * {@code age} over the same range are still two different properties, and a state read from a
     * blockstate file naming one would not match a block defined with the other.
     */
    private static final class ScriptCropBlock extends CropBlock {

        private static final Map<Integer, IntegerProperty> AGE_PROPERTIES = new LinkedHashMap<>();

        /**
         * The stage count of the crop being constructed right now.
         *
         * <p>Vanilla's crop constructor calls {@code createBlockStateDefinition} and
         * {@code getAgeProperty} before this class's own constructor body has run, so neither can
         * read a field. The count is put here for the length of the call instead — which is why
         * construction goes through {@link #create} rather than through {@code new}.
         */
        private static final ThreadLocal<Integer> CONSTRUCTING =
            ThreadLocal.withInitial(() -> 8);

        private final CropBlockBuilder builder;

        private final IntegerProperty ageProperty;

        private ScriptCropBlock(Properties properties, CropBlockBuilder builder) {
            super(properties);
            this.builder = builder;
            this.ageProperty = ageProperty(builder.age);
        }

        private static ScriptCropBlock create(Properties properties, CropBlockBuilder builder) {
            CONSTRUCTING.set(builder.age);

            try {
                return new ScriptCropBlock(properties, builder);
            } finally {
                CONSTRUCTING.remove();
            }
        }

        private static IntegerProperty ageProperty(int age) {
            // Vanilla's own for the usual eight stages, so such a crop is interchangeable with
            // wheat everywhere a property is compared.
            return age == 8 ? BlockStateProperties.AGE_7 : AGE_PROPERTIES.computeIfAbsent(age,
                a -> IntegerProperty.create("age", 0, a - 1));
        }

        @Override
        protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> definition) {
            definition.add(ageProperty(CONSTRUCTING.get()));
        }

        @Override
        public IntegerProperty getAgeProperty() {
            return ageProperty == null ? ageProperty(CONSTRUCTING.get()) : ageProperty;
        }

        @Override
        public int getMaxAge() {
            return builder.age - 1;
        }

        @Override
        protected ItemLike getBaseSeedId() {
            var unwrapped = builder.seed;

            if (unwrapped == null) {
                // The item created alongside the crop. Found through the block rather than kept on
                // the builder because a block item registers itself against its block, and this is
                // that lookup -- so it answers however the item was registered.
                var own = asItem();

                if (own != Items.AIR) {
                    return own;
                }
            } else if (unwrapped instanceof ItemLike item) {
                return item;
            } else {
                var seedId = ResourceLocation.tryParse(String.valueOf(unwrapped));
                var item = seedId == null ? Items.AIR : Registry.ITEM.get(seedId);

                if (item != Items.AIR) {
                    return item;
                }
            }

            ConsoleJS.STARTUP.warn("Crop " + builder.id
                + " has no seed item, so nothing can plant it");
            return Items.WHEAT_SEEDS;
        }

        /**
         * What the crop drops, in place of the loot table it has not got.
         *
         * <p>The whole of the crop's drops, so nothing a loot table would have contributed applies:
         * no fortune curve and no silk touch. Both belong to a table, and a builder has nowhere to
         * write one that outlives a datapack reload.
         *
         * @param state the state being broken
         * @param context what the game knows about the breaking
         * @return the stacks to drop
         */
        @Override
        public List<ItemStack> getDrops(BlockState state, LootContext.Builder context) {
            // Mutable, and empty rather than List.of() when there is nothing: what the game does
            // with a loot table's own drops includes handing the list to the harvest event, which
            // mods add to.
            var drops = new ArrayList<ItemStack>(2);

            if (builder.noDrops) {
                return drops;
            }

            if (isMaxAge(state)) {
                var crop = builder.cropDrop();
                var count = rolls(builder.cropChance, context.getLevel().getRandom());

                if (!crop.isEmpty() && count > 0) {
                    var stack = crop.copy();
                    stack.setCount(crop.getCount() * count);
                    drops.add(stack);
                }
            }

            if (builder.dropSeed) {
                drops.add(new ItemStack(getBaseSeedId()));
            }

            return drops;
        }

        /**
         * Rolls how many of a drop to give.
         *
         * <p>A chance of 2.5 is two every time and a third half the time, which is what "how many on
         * average" has to mean for a whole number of items.
         */
        private static int rolls(double chance, RandomSource random) {
            var whole = (int) chance;
            return random.nextDouble() < chance - whole ? whole + 1 : whole;
        }

        @Override
        protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
            return builder.surviveOn.isEmpty()
                ? super.mayPlaceOn(state, level, pos) : builder.canPlantOn(state);
        }

        @Override
        public boolean isValidBonemealTarget(BlockGetter level, BlockPos pos, BlockState state,
                                             boolean client) {
            return builder.bonemeal && super.isValidBonemealTarget(level, pos, state, client);
        }

        /**
         * Grows the crop, or runs what the script asked for instead.
         *
         * <p>The callbacks are reached from here rather than from the mixin every other block goes
         * through: this class overrides the game's own random tick to grow the crop, and an override
         * that does not call {@code super} is one the mixin's injection never sees.
         *
         * @param state the crop's state
         * @param level the level it is in
         * @param pos where it is
         * @param random the level's randomness
         */
        @Override
        public void randomTick(BlockState state, ServerLevel level, BlockPos pos,
                               RandomSource random) {
            var callbacks = builder.callbacks;

            if (callbacks == null || !callbacks.wantsRandomTicks()) {
                super.randomTick(state, level, pos, random);
                return;
            }

            callbacks.onRandomTick(level, pos, state);
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            var age = state.getValue(getAgeProperty());
            var shape = builder.ageShapes.get(age);

            if (shape != null) {
                return shape;
            }

            // Vanilla's shapes are an array indexed by age with eight entries in it, so a crop
            // with fewer stages would read past the end. Scaled instead: as tall as it has grown.
            return box(0D, 0D, 0D, 16D, 2D + 14D * age / Math.max(1, getMaxAge()), 16D);
        }
    }
}
