/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/fluid/FluidBuilder.java
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
package com.github.gubejs.fluid;

import com.github.gubejs.Gubejs;
import com.github.gubejs.bindings.ColorWrapper;
import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Material;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a fluid — {@code event.create('molten_iron').thickTexture(0xFFAA00)}.
 *
 * <p>One call in a script becomes five registry entries, because that is what a working fluid is
 * made of in this version:
 *
 * <ul>
 *   <li>the <strong>still</strong> fluid, which is what this builder registers;
 *   <li>the <strong>flowing</strong> fluid, a second object the game switches to as it spreads;
 *   <li>the <strong>fluid type</strong>, holding the physics and the textures both fluids share;
 *   <li>the <strong>block</strong> the fluid forms in the world;
 *   <li>the <strong>bucket</strong> that carries it.
 * </ul>
 *
 * <p>A pack should not have to know that. The four others are created from this one in
 * {@link #expand}, which runs after the startup scripts and before any registry is filled, and
 * every reference between them is a supplier — so it does not matter which registry Forge fills
 * first.
 *
 * <p>Textures default to water's, tinted. Producing a genuinely new fluid texture means shipping
 * two animated files and their {@code .mcmeta}; tinting one the game already has produces a fluid
 * that looks right immediately, which is what nearly every pack wants.
 */
public class FluidBuilder extends BuilderBase<Fluid> {

    /** Water's textures, which take a tint well because they are nearly greyscale. */
    private static final ResourceLocation WATER_STILL =
        new ResourceLocation("minecraft", "block/water_still");

    private static final ResourceLocation WATER_FLOW =
        new ResourceLocation("minecraft", "block/water_flow");

    /** Lava's textures, for anything that should look molten. */
    private static final ResourceLocation LAVA_STILL =
        new ResourceLocation("minecraft", "block/lava_still");

    private static final ResourceLocation LAVA_FLOW =
        new ResourceLocation("minecraft", "block/lava_flow");

    /** What the screen is filled with when a player's head is under the fluid. */
    private static final ResourceLocation WATER_OVERLAY =
        new ResourceLocation("minecraft", "block/water_overlay");

    protected ResourceLocation stillTexture = WATER_STILL;

    protected ResourceLocation flowingTexture = WATER_FLOW;

    protected ResourceLocation overlayTexture = WATER_OVERLAY;

    protected int tintColor = 0xFFFFFFFF;

    protected int bucketColor = 0xFFFFFFFF;

    protected int luminosity = 0;

    protected int density = 1000;

    protected int temperature = 300;

    protected int viscosity = 1000;

    protected boolean gaseous;

    protected boolean hasBucket = true;

    protected boolean hasBlock = true;

    @Nullable
    protected CreativeModeTab tab = CreativeModeTab.TAB_MISC;

    /** Built once and shared, so the still and flowing fluids agree about each other. */
    @Nullable
    private ForgeFlowingFluid.Properties properties;

    private final FluidTypeBuilder typeBuilder = new FluidTypeBuilder(this);

    private final FlowingBuilder flowingBuilder = new FlowingBuilder(this);

    private final BlockBuilder blockBuilder = new BlockBuilder(this);

    private final BucketBuilder bucketBuilder = new BucketBuilder(this);

    public FluidBuilder(ResourceLocation id) {
        super(id);
    }

    // --- appearance ----------------------------------------------------------------------------

    /**
     * Draws the fluid like water, in a colour.
     *
     * @param color the tint, as {@code 0xRRGGBB} or a name
     * @return this builder
     */
    public FluidBuilder thinTexture(Object color) {
        stillTexture = WATER_STILL;
        flowingTexture = WATER_FLOW;
        return color(color);
    }

    /**
     * Draws the fluid like lava, in a colour.
     *
     * @param color the tint, as {@code 0xRRGGBB} or a name
     * @return this builder
     */
    public FluidBuilder thickTexture(Object color) {
        stillTexture = LAVA_STILL;
        flowingTexture = LAVA_FLOW;
        return color(color);
    }

    /**
     * Sets the tint applied to both the fluid and its bucket.
     *
     * @param color the colour
     * @return this builder
     */
    public FluidBuilder color(Object color) {
        tintColor = ColorWrapper.of(color);
        bucketColor = tintColor;
        return this;
    }

    /**
     * Points the fluid at textures of your own.
     *
     * @param still the id of the still texture, e.g. {@code mypack:block/oil_still}
     * @param flowing the id of the flowing texture
     * @return this builder
     */
    public FluidBuilder texture(Object still, Object flowing) {
        stillTexture = id(still, stillTexture);
        flowingTexture = id(flowing, flowingTexture);
        return this;
    }

    /**
     * Sets the texture drawn over the screen when a player is inside the fluid.
     *
     * @param overlay the texture id
     * @return this builder
     */
    public FluidBuilder overlayTexture(Object overlay) {
        overlayTexture = id(overlay, overlayTexture);
        return this;
    }

    /**
     * Tints the bucket separately from the fluid.
     *
     * @param color the colour
     * @return this builder
     */
    public FluidBuilder bucketColor(Object color) {
        bucketColor = ColorWrapper.of(color);
        return this;
    }

    // --- physics -------------------------------------------------------------------------------

    /**
     * Makes the fluid glow.
     *
     * @param luminosity 0 to 15, where lava is 15
     * @return this builder
     */
    public FluidBuilder luminosity(int luminosity) {
        this.luminosity = luminosity;
        return this;
    }

    /**
     * Sets how heavy the fluid is.
     *
     * <p>Water is 1000. A negative density makes the fluid rise, which is how a gas is spelled.
     *
     * @param density the density
     * @return this builder
     */
    public FluidBuilder density(int density) {
        this.density = density;
        return this;
    }

    /**
     * Sets how hot the fluid is, in kelvin.
     *
     * <p>Water is 300, lava is 1300. Mods that care about heat read this.
     *
     * @param temperature the temperature
     * @return this builder
     */
    public FluidBuilder temperature(int temperature) {
        this.temperature = temperature;
        return this;
    }

    /**
     * Sets how slowly the fluid spreads and how much it slows anything inside it.
     *
     * @param viscosity water is 1000, lava is 6000
     * @return this builder
     */
    public FluidBuilder viscosity(int viscosity) {
        this.viscosity = viscosity;
        return this;
    }

    /**
     * Makes the fluid a gas, which fills a space from the top down.
     *
     * @return this builder
     */
    public FluidBuilder gaseous() {
        gaseous = true;
        return this;
    }

    // --- what comes with it --------------------------------------------------------------------

    /**
     * Stops the bucket being created.
     *
     * <p>For a fluid that only ever exists inside a machine, where a bucket in the creative menu
     * would be noise.
     *
     * @return this builder
     */
    public FluidBuilder noBucket() {
        hasBucket = false;
        return this;
    }

    /**
     * Stops the block being created, so the fluid cannot be placed in the world.
     *
     * @return this builder
     */
    public FluidBuilder noBlock() {
        hasBlock = false;
        return this;
    }

    /**
     * Sets which creative tab the bucket appears in.
     *
     * @param tab the tab, or {@code null} to hide it
     * @return this builder
     */
    public FluidBuilder creativeTab(@Nullable CreativeModeTab tab) {
        this.tab = tab;
        return this;
    }

    // --- building ------------------------------------------------------------------------------

    @Override
    public Fluid createObject() {
        return new ForgeFlowingFluid.Source(fluidProperties());
    }

    /**
     * Queues the four registry entries that go with this fluid.
     *
     * <p>Called once, after the startup scripts have run and before any registry is filled — the
     * only window in which {@link #hasBucket} and {@link #hasBlock} are both known and it is still
     * possible to add to a registry.
     */
    public void expand() {
        RegistryInfo.FLUID_TYPE.getBuilders().add(typeBuilder);
        RegistryInfo.FLUID.getBuilders().add(flowingBuilder);

        if (hasBlock) {
            RegistryInfo.BLOCK.getBuilders().add(blockBuilder);
        }

        if (hasBucket) {
            RegistryInfo.ITEM.getBuilders().add(bucketBuilder);
        }
    }

    /**
     * Assembles the properties both fluids share.
     *
     * <p>Every reference in here is a supplier, which is what makes the registration order between
     * the five entries irrelevant: nothing is resolved until the game asks a built fluid what its
     * type or its bucket is, and by then all five exist.
     */
    private ForgeFlowingFluid.Properties fluidProperties() {
        if (properties != null) {
            return properties;
        }

        properties = new ForgeFlowingFluid.Properties(
            typeBuilder::get, this::get, flowingBuilder::get);

        if (hasBlock) {
            properties.block(() -> (LiquidBlock) blockBuilder.get());
        }

        if (hasBucket) {
            properties.bucket(bucketBuilder::get);
        }

        return properties;
    }

    @Override
    public Map<String, String> getTranslations() {
        var translations = new LinkedHashMap<String, String>();
        translations.put(descriptionId(), getDisplayName());

        if (hasBucket) {
            translations.put("item." + id.getNamespace() + "." + id.getPath().replace('/', '.')
                + "_bucket", getDisplayName() + " Bucket");
        }

        return translations;
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();

        if (hasBlock) {
            // A fluid block is drawn by the fluid renderer rather than from its model, so the
            // model exists only to name the particle texture -- which is what a player sees
            // splashing when they walk into it.
            assets.put("assets/" + id.getNamespace() + "/blockstates/" + id.getPath() + ".json",
                """
                {
                  "variants": {
                    "": { "model": "%s:block/%s" }
                  }
                }""".formatted(id.getNamespace(), id.getPath()));

            assets.put("assets/" + id.getNamespace() + "/models/block/" + id.getPath() + ".json",
                """
                {
                  "textures": {
                    "particle": "%s"
                  }
                }""".formatted(stillTexture));
        }

        if (hasBucket) {
            // Forge's fluid container model draws the fluid inside a bucket from the fluid's own
            // still texture and tint, so a new fluid gets a matching bucket with no art at all.
            // Named "forge:fluid_container" rather than the older "forge:bucket", which still
            // works on 1.19.2 but logs a deprecation warning per model.
            assets.put("assets/" + id.getNamespace() + "/models/item/"
                + id.getPath() + "_bucket.json",
                """
                {
                  "parent": "forge:item/bucket",
                  "loader": "forge:fluid_container",
                  "fluid": "%s"
                }""".formatted(id));
        }

        return assets;
    }

    /** The translation key both fluids and the block share. */
    String descriptionId() {
        return "fluid_type." + id.getNamespace() + "." + id.getPath().replace('/', '.');
    }

    private static ResourceLocation id(Object value, ResourceLocation fallback) {
        var parsed = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(value)));
        return parsed == null ? fallback : parsed;
    }

    /** Registers the fluid types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.FLUID
            .addType("basic", FluidBuilder::new)
            .defaultType("basic");
        Gubejs.LOGGER.debug("Registered fluid builder types");
    }

    // --- the four entries that come with a fluid -----------------------------------------------

    /** The physics and textures both fluids share. */
    private static final class FluidTypeBuilder extends BuilderBase<FluidType> {

        private final FluidBuilder fluid;

        private FluidTypeBuilder(FluidBuilder fluid) {
            super(fluid.id);
            this.fluid = fluid;
        }

        @Override
        public FluidType createObject() {
            var properties = FluidType.Properties.create()
                .descriptionId(fluid.descriptionId())
                .density(fluid.density)
                .temperature(fluid.temperature)
                .viscosity(fluid.viscosity)
                .lightLevel(fluid.luminosity)
                .canSwim(!fluid.gaseous)
                .canDrown(!fluid.gaseous)
                .supportsBoating(!fluid.gaseous);

            if (fluid.gaseous) {
                // A gas fills upwards and does not slow anything down, which is the whole of what
                // separates one from a liquid as far as the game is concerned.
                properties.motionScale(0.0023333333333333335D).fallDistanceModifier(0F);
            }

            return new GubejsFluidType(properties, fluid.stillTexture, fluid.flowingTexture,
                fluid.overlayTexture, fluid.tintColor);
        }

        @Override
        public Map<String, String> getTranslations() {
            // The fluid's own builder already writes the shared key.
            return Map.of();
        }
    }

    /** The second fluid object, which the game uses while the fluid is spreading. */
    private static final class FlowingBuilder extends BuilderBase<Fluid> {

        private final FluidBuilder fluid;

        private FlowingBuilder(FluidBuilder fluid) {
            super(new ResourceLocation(fluid.id.getNamespace(), fluid.id.getPath() + "_flowing"));
            this.fluid = fluid;
        }

        @Override
        public Fluid createObject() {
            return new ForgeFlowingFluid.Flowing(fluid.fluidProperties());
        }

        @Override
        public Map<String, String> getTranslations() {
            return Map.of();
        }
    }

    /** The block the fluid forms in the world. */
    private static final class BlockBuilder extends BuilderBase<Block> {

        private final FluidBuilder fluid;

        private BlockBuilder(FluidBuilder fluid) {
            super(fluid.id);
            this.fluid = fluid;
        }

        @Override
        public Block createObject() {
            return new LiquidBlock(() -> (FlowingFluid) fluid.get(),
                BlockBehaviour.Properties.of(Material.WATER)
                    .noCollission()
                    .strength(100F)
                    .lightLevel(state -> fluid.luminosity)
                    .noLootTable());
        }

        @Override
        public Map<String, String> getTranslations() {
            return Map.of();
        }
    }

    /** The bucket that carries the fluid. */
    private static final class BucketBuilder extends BuilderBase<Item> {

        private final FluidBuilder fluid;

        private BucketBuilder(FluidBuilder fluid) {
            super(new ResourceLocation(fluid.id.getNamespace(), fluid.id.getPath() + "_bucket"));
            this.fluid = fluid;
        }

        @Override
        public Item createObject() {
            return new BucketItem(fluid::get, new Item.Properties()
                .craftRemainder(net.minecraft.world.item.Items.BUCKET)
                .stacksTo(1)
                .tab(fluid.tab));
        }

        @Override
        public Map<String, String> getTranslations() {
            return Map.of();
        }
    }
}
