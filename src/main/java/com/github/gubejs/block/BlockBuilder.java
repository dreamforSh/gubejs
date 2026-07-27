/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/BlockBuilder.java
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

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Material;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a plain block — {@code event.create('steel_block')}.
 *
 * <p>A block item is created alongside unless {@link #noItem()} says otherwise, since a block with
 * no way to obtain it is almost never what a pack meant. The block state file, both models and the
 * translation are generated.
 */
public class BlockBuilder extends BuilderBase<Block> {

    protected Material material = Material.STONE;

    protected float hardness = 1.5F;

    protected float resistance = 3F;

    protected int lightLevel;

    protected boolean requiresTool;

    protected SoundType soundType = SoundType.STONE;

    protected boolean createItem = true;

    @Nullable
    protected CreativeModeTab tab = CreativeModeTab.TAB_BUILDING_BLOCKS;

    @Nullable
    protected ResourceLocation texture;

    /** Which of the game's render passes the block is drawn in; see {@link #renderType}. */
    protected String renderType = "solid";

    protected boolean noCollision;

    protected boolean transparent;

    protected boolean noDrops;

    protected boolean noValidSpawns;

    protected float friction = 0.6F;

    protected float speedFactor = 1F;

    protected float jumpFactor = 1F;

    @Nullable
    protected net.minecraft.world.level.material.MaterialColor mapColor;

    /** The shape the block occupies, or {@code null} for the whole cube. */
    @Nullable
    protected net.minecraft.world.phys.shapes.VoxelShape shape;

    /** Filled in once {@link #createObject()} has run, so the block item can point at the block. */
    /** The built block. Protected so a subclass building a different shape can store its own. */
    @Nullable
    protected Block block;

    public BlockBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets what the block is made of, which decides how it burns, pushes and sounds by default.
     *
     * @param material the material, by name — {@code stone}, {@code metal}, {@code wood}, ...
     * @return this builder
     */
    public BlockBuilder material(Material material) {
        this.material = material;
        return this;
    }

    /**
     * Sets how long the block takes to break.
     *
     * @param hardness the hardness, or -1 for unbreakable
     * @return this builder
     */
    public BlockBuilder hardness(double hardness) {
        this.hardness = (float) hardness;
        return this;
    }

    /**
     * Sets how well the block resists explosions.
     *
     * @param resistance the blast resistance
     * @return this builder
     */
    public BlockBuilder resistance(double resistance) {
        this.resistance = (float) resistance;
        return this;
    }

    /**
     * Makes the block emit light.
     *
     * @param lightLevel 0 to 15
     * @return this builder
     */
    public BlockBuilder lightLevel(int lightLevel) {
        this.lightLevel = lightLevel;
        return this;
    }

    /**
     * Makes the block drop nothing unless mined with the right tool.
     *
     * @param requiresTool whether a tool is needed
     * @return this builder
     */
    public BlockBuilder requiresTool(boolean requiresTool) {
        this.requiresTool = requiresTool;
        return this;
    }

    /**
     * Sets the sounds the block makes.
     *
     * @param soundType the sound type
     * @return this builder
     */
    public BlockBuilder soundType(SoundType soundType) {
        this.soundType = soundType;
        return this;
    }

    /**
     * Sets which creative tab the block item appears in.
     *
     * @param tab the tab, or {@code null} to hide it
     * @return this builder
     */
    public BlockBuilder creativeTab(@Nullable CreativeModeTab tab) {
        this.tab = tab;
        return this;
    }

    /**
     * Points the generated model at a texture other than the one named after the block.
     *
     * @param texture the texture id
     * @return this builder
     */
    public BlockBuilder texture(Object texture) {
        this.texture = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(texture)));
        return this;
    }

    /**
     * Leaves the block without an item, for something only obtainable by command.
     *
     * @return this builder
     */
    public BlockBuilder noItem() {
        this.createItem = false;
        return this;
    }

    // --- appearance ----------------------------------------------------------------------------

    /**
     * Sets which of the game's render passes draws the block.
     *
     * <p>The default, {@code solid}, ignores transparency in the texture entirely — a glass texture
     * drawn in it comes out as an opaque block with black where the holes should be. Which pass a
     * block belongs in is not something a model file can say in this version, so it has to be set
     * here.
     *
     * <p>Anything but {@code solid} also turns occlusion off. A block that lets light or a view
     * through has to be excluded from face culling as well, or the faces of its neighbours behind
     * it are never drawn and the hole shows the void.
     *
     * @param renderType one of {@code solid}, {@code cutout}, {@code cutout_mipped},
     *     {@code translucent} or {@code tripwire}
     * @return this builder
     */
    public BlockBuilder renderType(String renderType) {
        this.renderType = renderType;
        return this;
    }

    /**
     * Draws the block in the pass that keeps fully transparent pixels transparent, the way leaves
     * and iron bars are drawn.
     *
     * @return this builder
     */
    public BlockBuilder defaultCutout() {
        return renderType("cutout");
    }

    /**
     * Draws the block in the pass that keeps partly transparent pixels partly transparent, the way
     * stained glass and ice are drawn.
     *
     * @return this builder
     */
    public BlockBuilder defaultTranslucent() {
        return renderType("translucent");
    }

    /**
     * Returns the render pass this block was asked for.
     *
     * @return the render type name
     */
    public String getRenderType() {
        return renderType;
    }

    /**
     * Whether the block hides what is behind it, for lighting and face culling.
     *
     * @param opaque {@code false} for a block light and sight pass through
     * @return this builder
     */
    public BlockBuilder opaque(boolean opaque) {
        this.transparent = !opaque;
        return this;
    }

    /**
     * Stops the block hiding what is behind it, without changing its render pass.
     *
     * <p>What a block with a model smaller than a cube needs, so the faces around it keep being
     * drawn.
     *
     * @return this builder
     */
    public BlockBuilder notSolid() {
        return opaque(false);
    }

    /**
     * Sets the colour the block shows on a map.
     *
     * @param color a {@link net.minecraft.world.level.material.MaterialColor}, a dye colour name
     *     such as {@code 'red'}, or a colour id
     * @return this builder
     */
    public BlockBuilder mapColor(Object color) {
        this.mapColor = materialColorOf(color);
        return this;
    }

    // --- physics -------------------------------------------------------------------------------

    /**
     * Lets everything walk and fall through the block, the way a torch or tall grass is passed
     * through.
     *
     * @return this builder
     */
    public BlockBuilder noCollision() {
        this.noCollision = true;
        return this;
    }

    /**
     * Sets the shape the block occupies, in the sixteenths a model is measured in.
     *
     * <p>The same numbers as a model's {@code from} and {@code to}, so a script can copy them
     * across rather than convert them. This is the collision box and the outline both; the model is
     * a separate matter and a block whose shape and model disagree looks wrong rather than breaks.
     *
     * @param x0 first corner, 0-16
     * @param y0 first corner, 0-16
     * @param z0 first corner, 0-16
     * @param x1 opposite corner, 0-16
     * @param y1 opposite corner, 0-16
     * @param z1 opposite corner, 0-16
     * @return this builder
     */
    public BlockBuilder box(double x0, double y0, double z0, double x1, double y1, double z1) {
        this.shape = Block.box(x0, y0, z0, x1, y1, z1);
        return this;
    }

    /**
     * Returns the shape the block occupies.
     *
     * @return the shape, or {@code null} for the whole cube
     */
    @Nullable
    public net.minecraft.world.phys.shapes.VoxelShape getShape() {
        return shape;
    }

    /**
     * Sets how slippery the block is.
     *
     * @param friction 0.6 for most blocks, 0.98 for ice
     * @return this builder
     */
    public BlockBuilder slipperiness(double friction) {
        this.friction = (float) friction;
        return this;
    }

    /**
     * Sets how fast things move along the block.
     *
     * @param speedFactor 1 for most blocks, 0.4 for soul sand
     * @return this builder
     */
    public BlockBuilder speedFactor(double speedFactor) {
        this.speedFactor = (float) speedFactor;
        return this;
    }

    /**
     * Sets how high things can jump from the block.
     *
     * @param jumpFactor 1 for most blocks, 0.5 for honey
     * @return this builder
     */
    public BlockBuilder jumpFactor(double jumpFactor) {
        this.jumpFactor = (float) jumpFactor;
        return this;
    }

    /**
     * Makes the block drop nothing when broken, whatever its loot table says.
     *
     * @return this builder
     */
    public BlockBuilder noDrops() {
        this.noDrops = true;
        return this;
    }

    /**
     * Makes the block unbreakable, the way bedrock is.
     *
     * @return this builder
     */
    public BlockBuilder unbreakable() {
        this.hardness = -1F;
        this.resistance = 3600000F;
        return this;
    }

    /**
     * Stops anything spawning on the block.
     *
     * @return this builder
     */
    public BlockBuilder noValidSpawns() {
        this.noValidSpawns = true;
        return this;
    }

    // --- behaviour -----------------------------------------------------------------------------

    /** What the block should do beyond what its properties say, or {@code null} for nothing. */
    @Nullable
    protected BlockCallbacks callbacks;

    /**
     * Returns the callbacks, creating a set on first use.
     *
     * @return the callbacks
     */
    protected BlockCallbacks callbacks() {
        if (callbacks == null) {
            callbacks = new BlockCallbacks();
        }

        return callbacks;
    }

    /**
     * Runs a callback on every random tick, and turns random ticking on.
     *
     * <pre>{@code
     * event.create('creeping_moss').randomTick(event => {
     *     const target = event.block.offset(Math.floor(Math.random() * 3) - 1, 0, 0)
     *     if (target.id === 'minecraft:stone') {
     *         target.set('mypack:creeping_moss')
     *     }
     * })
     * }</pre>
     *
     * @param callback what to run
     * @return this builder
     */
    public BlockBuilder randomTick(java.util.function.Consumer<BlockCallbackEventJS> callback) {
        callbacks().setRandomTick(callback);
        return this;
    }

    /**
     * Runs a callback every tick an entity is standing on the block.
     *
     * @param callback what to run, with {@code event.entity}
     * @return this builder
     */
    public BlockBuilder steppedOn(java.util.function.Consumer<BlockCallbackEventJS> callback) {
        callbacks().setSteppedOn(callback);
        return this;
    }

    /**
     * Runs a callback when an entity lands on the block.
     *
     * @param callback what to run, with {@code event.entity} and {@code event.fallDistance}
     * @return this builder
     */
    public BlockBuilder fallenOn(java.util.function.Consumer<BlockCallbackEventJS> callback) {
        callbacks().setFallenOn(callback);
        return this;
    }

    /**
     * Decides whether the block can be built over, the way tall grass can.
     *
     * @param callback returns {@code true} or {@code false}, or nothing to leave the block's own
     *     answer
     * @return this builder
     */
    public BlockBuilder canBeReplaced(
        java.util.function.Function<BlockCallbackEventJS, Object> callback) {
        callbacks().setCanBeReplaced(callback);
        return this;
    }

    /** Whether a block item should be registered for this block. */
    public boolean hasItem() {
        return createItem;
    }

    /** What a script asked the block to remember, or {@code null} for a plain block. */
    @Nullable
    protected com.github.gubejs.block.entity.BlockEntityBuilder blockEntity;

    /** Filled in when the block entity type is registered, so the entity can name its own type. */
    @Nullable
    private net.minecraft.world.level.block.entity.BlockEntityType<?> blockEntityType;

    /**
     * Gives the block a block entity, so it can hold items or data of its own.
     *
     * <pre>{@code
     * event.create('smelter').blockEntity(be => {
     *     be.inventorySize = 9
     *     be.serverTick(20, 0, entity => { ... })
     * })
     * }</pre>
     *
     * @param action configures the block entity
     * @return this builder
     */
    public BlockBuilder blockEntity(
        java.util.function.Consumer<com.github.gubejs.block.entity.BlockEntityBuilder> action) {
        var builder = new com.github.gubejs.block.entity.BlockEntityBuilder();
        action.accept(builder);
        blockEntity = builder;
        return this;
    }

    /**
     * Returns what the script asked the block to remember.
     *
     * @return the block entity builder, or {@code null} for a plain block
     */
    @Nullable
    public com.github.gubejs.block.entity.BlockEntityBuilder getBlockEntityBuilder() {
        return blockEntity;
    }

    /**
     * Returns the registered block entity type.
     *
     * @return the type, or {@code null} before it is registered
     */
    @Nullable
    public net.minecraft.world.level.block.entity.BlockEntityType<?> getBlockEntityType() {
        return blockEntityType;
    }

    /**
     * Remembers the block entity type once the registry has built it.
     *
     * @param type the type
     */
    public void setBlockEntityType(net.minecraft.world.level.block.entity.BlockEntityType<?> type) {
        this.blockEntityType = type;
    }

    @Override
    public Block createObject() {
        // A block with memory has to be an EntityBlock, and that is a different class -- the
        // interface decides whether the game ever asks the block for an entity at all.
        block = blockEntity == null
            ? new GubejsBasicBlock(createProperties(), shape)
            : new com.github.gubejs.block.entity.GubejsEntityBlock(createProperties(), this);
        return block;
    }

    /**
     * Returns the block, attaching the script's callbacks the first time it is asked for.
     *
     * <p>Here rather than in {@link #createObject()} because the shaped types override that and
     * build a block of their own; every one of them still comes back through this.
     */
    @Override
    public Block get() {
        var created = super.get();

        if (callbacks != null && !callbacks.isEmpty()
            && created instanceof com.github.gubejs.core.BlockKJS holder
            && holder.gjs$getCallbacks() == null) {
            holder.gjs$setCallbacks(callbacks);
        }

        return created;
    }

    /**
     * Builds the block item that goes with this block.
     *
     * @return the item, or {@code null} if the script asked for none
     */
    @Nullable
    public Item createBlockItem() {
        if (!createItem) {
            return null;
        }

        return new BlockItem(get(), new Item.Properties().tab(tab));
    }

    /**
     * Assembles the vanilla properties object from everything the script set.
     *
     * @return the properties
     */
    protected BlockBehaviour.Properties createProperties() {
        var properties = (mapColor == null
            ? BlockBehaviour.Properties.of(material)
            : BlockBehaviour.Properties.of(material, mapColor))
            .strength(hardness, resistance)
            .sound(soundType)
            .friction(friction)
            .speedFactor(speedFactor)
            .jumpFactor(jumpFactor);

        if (requiresTool) {
            properties.requiresCorrectToolForDrops();
        }

        if (noCollision) {
            properties.noCollission();
        }

        // A render pass other than solid implies it: a block drawn with holes in it that still
        // occludes leaves its neighbours' faces unrendered, and the holes show the void rather than
        // what is behind the block. Asking a pack to remember notSolid() alongside every
        // defaultCutout() would only mean forgetting it.
        if (transparent || !"solid".equals(renderType)) {
            properties.noOcclusion();
        }

        if (noDrops) {
            properties.noLootTable();
        }

        if (noValidSpawns) {
            properties.isValidSpawn((state, level, pos, type) -> false);
        }

        if (lightLevel > 0) {
            properties.lightLevel(state -> lightLevel);
        }

        // The flag rather than the callback is what the game reads to decide whether to tick the
        // block at all, and it is baked into each state as the state definition is built -- so it
        // has to be set here, not when the callback runs.
        if (callbacks != null && callbacks.wantsRandomTicks()) {
            properties.randomTicks();
        }

        return properties;
    }

    @Override
    public Map<String, String> getTranslations() {
        return Map.of("block." + id.getNamespace() + "." + id.getPath().replace('/', '.'),
            getDisplayName());
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var namespace = id.getNamespace();
        var path = id.getPath();
        var model = namespace + ":block/" + path;
        var face = texture != null ? texture.toString() : namespace + ":block/" + path;

        assets.put("assets/" + namespace + "/blockstates/" + path + ".json",
            """
            {
              "variants": {
                "": { "model": "%s" }
              }
            }""".formatted(model));

        assets.put("assets/" + namespace + "/models/block/" + path + ".json",
            """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all": "%s"
              }
            }""".formatted(face));

        if (createItem) {
            assets.put("assets/" + namespace + "/models/item/" + path + ".json",
                """
                {
                  "parent": "%s"
                }""".formatted(model));
        }

        return assets;
    }

    /**
     * Reads a map colour from whatever a script named it by.
     *
     * <p>The game keeps these as constants with names of their own ({@code COLOR_RED}) and no
     * lookup by name, so a dye colour is the way in: those are the sixteen names a pack already
     * knows, and each one carries the matching map colour.
     *
     * @param color a material colour, a dye colour name, or a colour id
     * @return the colour, or {@code null} if nothing goes by that name
     */
    @Nullable
    protected static net.minecraft.world.level.material.MaterialColor materialColorOf(
        Object color) {
        var unwrapped = ValueUtils.unwrap(color);

        if (unwrapped instanceof net.minecraft.world.level.material.MaterialColor materialColor) {
            return materialColor;
        } else if (unwrapped instanceof Number number) {
            return net.minecraft.world.level.material.MaterialColor.byId(number.intValue());
        }

        var name = ValueUtils.asString(unwrapped);
        var dye = name == null ? null : net.minecraft.world.item.DyeColor.byName(name, null);

        if (dye == null) {
            com.github.gubejs.util.ConsoleJS.STARTUP.warn(
                "There is no map colour called '" + name + "'");
            return null;
        }

        return dye.getMaterialColor();
    }

    /** Registers the block types scripts can create. */
    public static void registerTypes() {
        com.github.gubejs.registry.RegistryInfo.BLOCK
            .addType("basic", BlockBuilder::new)
            .defaultType("basic");
    }

    /**
     * Returns the block once it has been built.
     *
     * @return the block, or {@code null} before registration
     */
    @Nullable
    public Block getBlock() {
        return block;
    }
}
