package com.github.gubejs.block;

import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a crop — {@code event.create('rice', 'crop').seed('mypack:rice_seeds')}.
 *
 * <p>A crop grows through a fixed number of stages, needs farmland under it and light above it, and
 * is planted from a seed item. All of that comes from vanilla's own crop; what this adds is the
 * stage count, since vanilla's is fixed at eight and a script may want four.
 *
 * <p>This is the one block type whose models are not derived from a single texture — a crop is a
 * texture per stage and nothing can invent the ones in between. The paths are conventional, so a
 * pack supplying {@code textures/block/<path>_stage0.png} up to the last stage gets the blockstate
 * and every model generated. A missing stage texture shows as the missing-texture checkerboard for
 * that stage alone.
 *
 * <p>The seed is what the crop drops and what plants it. Without one the crop can only be placed by
 * command, which is occasionally what a pack wants and usually a mistake, so it is logged.
 */
public class CropBlockBuilder extends BlockBuilder {

    /** How many growth stages, counting the ripe one. */
    protected int age = 8;

    /** What plants the crop, as an item id. */
    @Nullable
    protected Object seed;

    public CropBlockBuilder(ResourceLocation id) {
        super(id);
        this.material = net.minecraft.world.level.material.Material.PLANT;
        this.soundType = net.minecraft.world.level.block.SoundType.CROP;
        this.hardness = 0F;
        this.resistance = 0F;
        // A crop is not a block anyone puts in a hotbar: the seed is the item, and a block item
        // for the crop itself would sit in creative next to the seed doing the same thing worse.
        this.createItem = false;
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
     * Sets what plants the crop and what it drops.
     *
     * @param seed the item id
     * @return this builder
     */
    public CropBlockBuilder seed(Object seed) {
        this.seed = ValueUtils.unwrap(seed);
        return this;
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

            if (unwrapped instanceof ItemLike item) {
                return item;
            }

            var seedId = unwrapped == null
                ? null : ResourceLocation.tryParse(String.valueOf(unwrapped));
            var item = seedId == null ? null : Registry.ITEM.get(seedId);

            if (item == null || item == net.minecraft.world.item.Items.AIR) {
                ConsoleJS.STARTUP.warn("Crop " + builder.id
                    + " has no seed item, so nothing can plant it");
                return net.minecraft.world.item.Items.WHEAT_SEEDS;
            }

            return item;
        }

        @Override
        @SuppressWarnings("deprecation")
        public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos,
                                   CollisionContext context) {
            // Vanilla's shapes are an array indexed by age with eight entries in it, so a crop
            // with fewer stages would read past the end. Scaled instead: as tall as it has grown.
            var height = 2D + 14D * state.getValue(getAgeProperty()) / Math.max(1, getMaxAge());
            return box(0D, 0D, 0D, 16D, height, 16D);
        }
    }
}
