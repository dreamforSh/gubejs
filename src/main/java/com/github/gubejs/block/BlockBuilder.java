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
import java.util.List;
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

    /** The block state properties a script added, in the order it added them. */
    protected final java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>>
        blockStateProperties = new java.util.LinkedHashSet<>();

    /** What the default state should say, by property name. */
    protected final Map<String, Object> defaultStateValues = new LinkedHashMap<>();

    /** Which model each face uses, by texture slot; empty for one texture on every face. */
    protected final Map<String, String> textures = new LinkedHashMap<>();

    /** A model to use instead of a generated one. */
    @Nullable
    protected ResourceLocation model;

    /** Tint per tint index, for a block whose colour is not in its texture. */
    protected final Map<Integer, Integer> tints = new LinkedHashMap<>();

    /** The tags the block's item should be in; the block's own are on the builder base. */
    protected final java.util.Set<ResourceLocation> itemTags = new java.util.LinkedHashSet<>();

    /**
     * Whether the block suffocates, blocks the view and conducts redstone.
     *
     * <p>Null until a script says, and left alone when it does not: the game works each of these out
     * from the material and the shape, and a flag defaulted to either value here would override that
     * reasoning for every block — a decorative half-height block would start suffocating whoever
     * stood in it.
     */
    @Nullable
    protected Boolean suffocating;

    @Nullable
    protected Boolean viewBlocking;

    @Nullable
    protected Boolean redstoneConductor;

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
     * <p>Both scales work, because packs are written in both: {@code 15} is full brightness and so
     * is {@code 1.0}, and anything up to and including 1 is read as a fraction of full brightness
     * the way KubeJS reads it. Which matters more than it looks — {@code .lightLevel(1)} in a KubeJS
     * pack means a glowstone-bright block, and reading it as "one light level" would leave the
     * block all but dark with nothing to say why.
     *
     * @param lightLevel 0 to 15, or a fraction of 1
     * @return this builder
     */
    public BlockBuilder lightLevel(double lightLevel) {
        // Truncated rather than rounded for the fraction, because that is what KubeJS does with it
        // and a block ported from a pack should light the room to the same number.
        this.lightLevel = lightLevel > 0D && lightLevel <= 1D
            ? (int) (lightLevel * 15D) : (int) Math.round(lightLevel);
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
     * @param soundType the sound type, or its name — {@code 'wood'}, {@code 'nether_bricks'},
     *     anything {@link SoundType} has a constant for
     * @return this builder
     */
    public BlockBuilder soundType(Object soundType) {
        this.soundType = soundTypeOf(soundType);
        return this;
    }

    /** @return this builder, with the sounds stone makes */
    public BlockBuilder stoneSoundType() {
        return soundType(SoundType.STONE);
    }

    /** @return this builder, with the sounds wood makes */
    public BlockBuilder woodSoundType() {
        return soundType(SoundType.WOOD);
    }

    /** @return this builder, with the sounds gravel makes */
    public BlockBuilder gravelSoundType() {
        return soundType(SoundType.GRAVEL);
    }

    /** @return this builder, with the sounds grass makes */
    public BlockBuilder grassSoundType() {
        return soundType(SoundType.GRASS);
    }

    /** @return this builder, with the sounds metal makes */
    public BlockBuilder metalSoundType() {
        return soundType(SoundType.METAL);
    }

    /** @return this builder, with the sounds glass makes */
    public BlockBuilder glassSoundType() {
        return soundType(SoundType.GLASS);
    }

    /** @return this builder, with the sounds wool makes */
    public BlockBuilder woolSoundType() {
        return soundType(SoundType.WOOL);
    }

    /** @return this builder, with the sounds sand makes */
    public BlockBuilder sandSoundType() {
        return soundType(SoundType.SAND);
    }

    /** @return this builder, with the sounds a plant makes */
    public BlockBuilder plantSoundType() {
        return soundType(SoundType.GRASS);
    }

    /** @return this builder, with the sounds snow makes */
    public BlockBuilder snowSoundType() {
        return soundType(SoundType.SNOW);
    }

    /** @return this builder, with the sounds a ladder makes */
    public BlockBuilder ladderSoundType() {
        return soundType(SoundType.LADDER);
    }

    /** @return this builder, with the sounds an anvil makes */
    public BlockBuilder anvilSoundType() {
        return soundType(SoundType.ANVIL);
    }

    /** @return this builder, with the sounds slime makes */
    public BlockBuilder slimeSoundType() {
        return soundType(SoundType.SLIME_BLOCK);
    }

    /**
     * The sound types by name, built once from the game's own constants.
     *
     * <p>Read off the class rather than listed here: there are forty-odd of them in 1.19.2 and a
     * hand-written list would be missing whichever one a pack turns out to want.
     */
    @Nullable
    private static Map<String, SoundType> soundTypes;

    /**
     * Reads a sound type from a constant or from its name.
     *
     * @param value a {@link SoundType} or a name in any of the spellings a pack uses
     * @return the sound type, {@link SoundType#STONE} when the name is not one the game has
     */
    private static SoundType soundTypeOf(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof SoundType type) {
            return type;
        }

        if (soundTypes == null) {
            var found = new LinkedHashMap<String, SoundType>();

            for (var field : SoundType.class.getFields()) {
                if (field.getType() == SoundType.class
                    && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        found.put(field.getName(), (SoundType) field.get(null));
                    } catch (IllegalAccessException ignored) {
                        // A constant that cannot be read is a constant a script cannot name; the
                        // lookup below reports the name as unknown, which is the truth.
                    }
                }
            }

            soundTypes = found;
        }

        var name = String.valueOf(unwrapped).trim();
        var colon = name.indexOf(':');
        var type = soundTypes.get(constantName(colon == -1 ? name : name.substring(colon + 1)));

        if (type != null) {
            return type;
        }

        com.github.gubejs.util.ConsoleJS.STARTUP.error("There is no block sound type called '"
            + name + "'; using stone. The names are " + soundTypes.keySet());
        return SoundType.STONE;
    }

    /** Turns {@code 'netherBricks'} and {@code 'nether bricks'} into {@code NETHER_BRICKS}. */
    private static String constantName(String name) {
        var builder = new StringBuilder(name.length() + 4);

        for (var i = 0; i < name.length(); i++) {
            var c = name.charAt(i);

            if (c >= 'A' && c <= 'Z') {
                if (i > 0 && builder.length() > 0 && builder.charAt(builder.length() - 1) != '_') {
                    builder.append('_');
                }

                builder.append(c);
            } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                builder.append(Character.toUpperCase(c));
            } else if (builder.length() > 0 && builder.charAt(builder.length() - 1) != '_') {
                builder.append('_');
            }
        }

        return builder.toString();
    }

    /**
     * Sets which creative tab the block item appears in.
     *
     * @param tab the tab, its name — {@code 'buildingBlocks'}, {@code 'misc'}, {@code 'kubejs'} —
     *     or {@code null} to hide it
     * @return this builder
     */
    public BlockBuilder creativeTab(@Nullable Object tab) {
        this.tab = com.github.gubejs.item.CreativeTabs.find(tab);
        return this;
    }

    /**
     * Sets which creative tab the block item appears in, under the name KubeJS packs use for it.
     *
     * @param tab the tab or its name
     * @return this builder
     */
    public BlockBuilder group(@Nullable Object tab) {
        return creativeTab(tab);
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

    // --- block states --------------------------------------------------------------------------

    /**
     * Gives the block a state property.
     *
     * <pre>{@code
     * event.create('valve')
     *     .property(BlockProperties.HORIZONTAL_FACING)
     *     .property('lit')
     *     .defaultState({ lit: false })
     * }</pre>
     *
     * <p>A vanilla property can be named as a string as well as passed as a constant, by the name
     * of the field on {@code BlockProperties} — {@code 'horizontal_facing'} rather than
     * {@code 'facing'}, because half a dozen vanilla properties are spelled {@code facing} in a
     * blockstate file and only the field name says which of them is meant.
     *
     * <p>Every combination of every property is a separate blockstate, so the generated blockstate
     * file maps all of them to one model. A property whose value should change what is drawn needs a
     * blockstate file of the pack's own under {@code kubejs/assets/}.
     *
     * @param property a {@code BlockProperties} constant or the name of one
     * @return this builder
     */
    public BlockBuilder property(Object property) {
        var parsed = propertyOf(property);

        if (parsed != null) {
            blockStateProperties.add(parsed);
        }

        return this;
    }

    /**
     * Gives the block a true/false property of its own.
     *
     * @param name the property name, as it appears in a blockstate file
     * @return this builder
     */
    public BlockBuilder booleanProperty(String name) {
        blockStateProperties.add(
            net.minecraft.world.level.block.state.properties.BooleanProperty.create(name));
        return this;
    }

    /**
     * Gives the block a numeric property of its own.
     *
     * @param name the property name
     * @param min the lowest value, 0 or more
     * @param max the highest value
     * @return this builder
     */
    public BlockBuilder intProperty(String name, int min, int max) {
        blockStateProperties.add(
            net.minecraft.world.level.block.state.properties.IntegerProperty.create(name, min, max));
        return this;
    }

    /**
     * Lets the block hold water, the way a slab or a fence does.
     *
     * @return this builder
     */
    public BlockBuilder waterlogged() {
        return property(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
    }

    /**
     * Whether this block can hold water.
     *
     * @return whether the waterlogged property was added
     */
    public boolean canBeWaterlogged() {
        return blockStateProperties.contains(
            net.minecraft.world.level.block.state.properties.BlockStateProperties.WATERLOGGED);
    }

    /**
     * Says what the block's default state should be.
     *
     * <p>The state a block is placed in when nothing decides otherwise, and the one a script gets
     * from {@code Block.getBlock(...).defaultState}. Facing and waterlogging are worked out from
     * where the block is placed instead — see {@link GubejsBasicBlock#getStateForPlacement}.
     *
     * @param values property name to value, e.g. {@code { lit: false, level: 3 }}
     * @return this builder
     */
    public BlockBuilder defaultState(Object values) {
        var unwrapped = ValueUtils.unwrap(values);

        if (unwrapped instanceof Map<?, ?> map) {
            map.forEach((key, value) -> defaultStateValues.put(String.valueOf(key),
                ValueUtils.unwrap(value)));
        } else {
            com.github.gubejs.util.ConsoleJS.STARTUP.error("defaultState takes an object of "
                + "property names to values, e.g. { lit: false }");
        }

        return this;
    }

    /**
     * Returns the state properties a script added.
     *
     * @return the properties, in the order they were added
     */
    public java.util.Set<net.minecraft.world.level.block.state.properties.Property<?>>
        getBlockStateProperties() {
        return blockStateProperties;
    }

    /**
     * Returns what the default state should say.
     *
     * @return property name to value
     */
    public Map<String, Object> getDefaultStateValues() {
        return defaultStateValues;
    }

    // --- appearance details --------------------------------------------------------------------

    /**
     * Puts the same texture on every face, which is what {@link #texture(Object)} already does.
     *
     * @param texture the texture id
     * @return this builder
     */
    public BlockBuilder textureAll(Object texture) {
        return texture(texture);
    }

    /**
     * Puts a texture on one face.
     *
     * <p>Any face left unset falls back to {@link #texture(Object)}, or to the texture named after
     * the block — so a block that only differs on top states only that.
     *
     * @param direction the face
     * @param texture the texture id
     * @return this builder
     */
    public BlockBuilder textureSide(net.minecraft.core.Direction direction, Object texture) {
        return texture(faceOf(direction), texture);
    }

    /**
     * Sets one texture slot of the generated model.
     *
     * @param slot the slot name — {@code up}, {@code down}, {@code north}, {@code south},
     *     {@code east}, {@code west}, or anything a model given to {@link #model} uses
     * @param texture the texture id
     * @return this builder
     */
    public BlockBuilder texture(String slot, Object texture) {
        var id = ValueUtils.asString(texture);

        if (id != null) {
            textures.put(slot, id);
        }

        return this;
    }

    /**
     * Points the blockstate at a model the pack ships, instead of generating one.
     *
     * <p>Nothing else about the block changes; the shape is still {@link #box} and the item model
     * still points at whatever this names. A block whose model and shape disagree looks wrong rather
     * than breaks.
     *
     * @param model the model id, e.g. {@code 'mypack:block/valve'}
     * @return this builder
     */
    public BlockBuilder model(Object model) {
        this.model = ResourceLocation.tryParse(String.valueOf(ValueUtils.unwrap(model)));
        return this;
    }

    /**
     * Tints the block, for a texture drawn in greyscale.
     *
     * <p>A constant colour, not a function. The game asks for a tint once per face per block per
     * frame, and a script there would be run tens of thousands of times a second on the render
     * thread — through a lock the script context holds during a reload. A colour that has to vary
     * belongs in the texture, or in several blocks.
     *
     * @param index the tint index the model's faces use, usually 0
     * @param color anything {@code Color.of} accepts
     * @return this builder
     */
    public BlockBuilder color(int index, Object color) {
        tints.put(index, com.github.gubejs.bindings.ColorWrapper.of(color) & 0xFFFFFF);
        return this;
    }

    /**
     * Tints the block's first tint index.
     *
     * @param color anything {@code Color.of} accepts
     * @return this builder
     */
    public BlockBuilder color(Object color) {
        return color(0, color);
    }

    /**
     * Puts the block's item in one or more tags.
     *
     * <p>Separate from {@link #tag}, which is the block's own: the two registries have separate tags
     * even where they share a name, and a recipe asking for {@code #forge:storage_blocks/steel} is
     * asking about the item while a tool asking about {@code #minecraft:mineable/pickaxe} is asking
     * about the block.
     *
     * @param tags one or more tag ids
     * @return this builder
     */
    public BlockBuilder tagItem(Object... tags) {
        for (var tag : tags) {
            for (var value : ValueUtils.listOf(tag)) {
                var text = String.valueOf(ValueUtils.unwrap(value)).trim();
                var id = ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text);

                if (id == null) {
                    com.github.gubejs.util.ConsoleJS.STARTUP.error("Not a tag id: '" + text + "'");
                } else {
                    itemTags.add(id);
                }
            }
        }

        return this;
    }

    /**
     * Puts both the block and its item in the same tags.
     *
     * @param tags one or more tag ids
     * @return this builder
     */
    public BlockBuilder tagBoth(Object... tags) {
        tag(tags);
        return tagItem(tags);
    }

    /**
     * Returns the tags the block's item should be in.
     *
     * @return the tag ids
     */
    public java.util.Set<ResourceLocation> getItemTags() {
        return itemTags;
    }

    /**
     * Whether the block stops a player suffocating inside it.
     *
     * @param suffocating whether being inside it does damage
     * @return this builder
     */
    public BlockBuilder suffocating(boolean suffocating) {
        this.suffocating = suffocating;
        return this;
    }

    /**
     * Whether the block blocks the view of a player inside it.
     *
     * @param viewBlocking whether the screen goes dark inside it
     * @return this builder
     */
    public BlockBuilder viewBlocking(boolean viewBlocking) {
        this.viewBlocking = viewBlocking;
        return this;
    }

    /**
     * Whether redstone travels through the block.
     *
     * @param conductor whether it conducts
     * @return this builder
     */
    public BlockBuilder redstoneConductor(boolean conductor) {
        this.redstoneConductor = conductor;
        return this;
    }

    /**
     * Returns the tints a script set.
     *
     * @return tint index to colour, empty when the block is drawn as its texture is
     */
    public Map<Integer, Integer> getTints() {
        return tints;
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

    /**
     * Runs a callback when a player right-clicks the block.
     *
     * <pre>{@code
     * event.create('bell_stone').rightClick(event => {
     *     if (!event.level.isClientSide) {
     *         event.player.tell('bong')
     *     }
     * })
     * }</pre>
     *
     * <p>The click is taken as handled unless the callback returns {@code false}, so the item in hand
     * does not also get to act on it. {@code BlockEvents.rightClicked} is the same thing as an event,
     * for a block this pack did not create.
     *
     * @param callback what to run
     * @return this builder
     */
    public BlockBuilder rightClick(
        java.util.function.Function<BlockCallbackEventJS, Object> callback) {
        callbacks().setRightClicked(callback);
        return this;
    }

    /**
     * Configures the block's own item — its tooltip, its rarity, how long it burns.
     *
     * <pre>{@code
     * event.create('steel_block').item(item => {
     *     item.tooltip('Heavier than it looks')
     *     item.rarity = 'uncommon'
     * })
     * }</pre>
     *
     * <p>A block item is not a separate registration a script can reach — it is created from the
     * block, after it — so this is the only place to say anything about it. What the callback is
     * handed is the same object {@code ItemEvents.modification} hands out, applied once the item
     * exists.
     *
     * @param callback describes the item
     * @return this builder
     */
    public BlockBuilder item(
        java.util.function.Consumer<com.github.gubejs.item.ItemModifications> callback) {
        itemChanges.add(callback);
        return this;
    }

    /** What {@link #item} asked for, applied when the block item is built. */
    private final List<java.util.function.Consumer<com.github.gubejs.item.ItemModifications>>
        itemChanges = new java.util.ArrayList<>();

    /**
     * Applies what {@link #item} asked for to the block item that was just built.
     *
     * @param item the block item
     */
    public void applyItemChanges(Item item) {
        if (itemChanges.isEmpty()) {
            return;
        }

        var modifications = ((com.github.gubejs.core.ItemKJS) item).gjs$getOrCreateModifications();
        itemChanges.forEach(change -> change.accept(modifications));
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

        // Said once, here, rather than when the property was added: a shaped type has state
        // properties of its own and builds its own block class, which never asks for these. Silence
        // would leave a pack author with a stairs block that ignores half its script.
        if (!blockStateProperties.isEmpty() && !(created instanceof GubejsBasicBlock)) {
            com.github.gubejs.util.ConsoleJS.STARTUP.warn(id + " is a '" + typeName()
                + "' block, which has state properties of its own, so the "
                + blockStateProperties.size() + " this script added were ignored. Only the 'basic' "
                + "type takes them.");
        }

        return created;
    }

    /** What a script called this block's type, for a message about it. */
    protected String typeName() {
        return getClass().getSimpleName();
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
        // This mod's own properties object rather than Properties.of, because the block asks it for
        // the state properties from inside its own constructor -- see GubejsBlockProperties.
        var colour = mapColor == null ? material.getColor() : mapColor;
        var properties = new GubejsBlockProperties(material, state -> colour, this)
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

        if (suffocating != null) {
            properties.isSuffocating((state, level, pos) -> suffocating);
        }

        if (viewBlocking != null) {
            properties.isViewBlocking((state, level, pos) -> viewBlocking);
        }

        if (redstoneConductor != null) {
            properties.isRedstoneConductor((state, level, pos) -> redstoneConductor);
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
        var generated = namespace + ":block/" + path;
        // One variant for every state. Extra properties multiply the states out, and a blockstate
        // file that named some of them and not others would leave the rest as a missing model --
        // the empty key matches whatever the others do not.
        var model = this.model != null ? this.model.toString() : generated;
        var face = texture != null ? texture.toString() : generated;

        assets.put("assets/" + namespace + "/blockstates/" + path + ".json",
            """
            {
              "variants": {
                "": { "model": "%s" }
              }
            }""".formatted(model));

        // Nothing generated when the pack named a model: it owns the file, and writing one at the
        // same path would either lose to it or replace it, both of them surprising.
        if (this.model == null) {
            assets.put("assets/" + namespace + "/models/block/" + path + ".json",
                textures.isEmpty() ? cubeAll(face) : cube(face));
        }

        if (createItem) {
            assets.put("assets/" + namespace + "/models/item/" + path + ".json",
                """
                {
                  "parent": "%s"
                }""".formatted(model));
        }

        return assets;
    }

    /** One texture on all six faces. */
    private static String cubeAll(String texture) {
        return """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all": "%s"
              }
            }""".formatted(texture);
    }

    /**
     * A model with a texture per face.
     *
     * <p>{@code particle} as well as the six, because that is the texture the game breaks and falls
     * on the block with, and a model without it logs a missing-texture warning for every block
     * placed.
     *
     * @param fallback the texture for any face the script did not set
     */
    private String cube(String fallback) {
        var slots = new LinkedHashMap<String, String>();

        for (var face : List.of("down", "up", "north", "south", "west", "east")) {
            slots.put(face, textures.getOrDefault(face, fallback));
        }

        slots.put("particle", textures.getOrDefault("particle",
            textures.getOrDefault("north", fallback)));

        // Anything else the script named, for a model of its own that this is the parent of.
        textures.forEach(slots::putIfAbsent);

        var entries = new java.util.ArrayList<String>(slots.size());
        slots.forEach((slot, texture) -> entries.add("    \"" + slot + "\": \"" + texture + "\""));

        return """
            {
              "parent": "minecraft:block/cube",
              "textures": {
            %s
              }
            }""".formatted(String.join(",\n", entries));
    }

    /**
     * Reads a state property from a constant or from the name of one.
     *
     * <p>By field name on {@code BlockStateProperties} rather than by the name the property writes
     * into a blockstate file, because those are not unique: {@code FACING},
     * {@code HORIZONTAL_FACING} and {@code FACING_HOPPER} all spell themselves {@code facing}, and
     * {@code AGE_1} through {@code AGE_25} are all {@code age}. Picking whichever came first in a
     * reflective scan would be a silent wrong answer.
     *
     * @param property a property, or the name of a field on {@code BlockProperties}
     * @return the property, or {@code null} if nothing goes by that name
     */
    @Nullable
    protected static net.minecraft.world.level.block.state.properties.Property<?> propertyOf(
        Object property) {
        var unwrapped = ValueUtils.unwrap(property);

        if (unwrapped instanceof net.minecraft.world.level.block.state.properties.Property<?> p) {
            return p;
        }

        var name = ValueUtils.asString(unwrapped);

        if (name != null) {
            try {
                var field = net.minecraft.world.level.block.state.properties.BlockStateProperties
                    .class.getField(name.toUpperCase(java.util.Locale.ROOT));

                if (net.minecraft.world.level.block.state.properties.Property.class
                    .isAssignableFrom(field.getType())) {
                    return (net.minecraft.world.level.block.state.properties.Property<?>)
                        field.get(null);
                }
            } catch (ReflectiveOperationException ignored) {
                // Reported below, with what a script can do about it.
            }
        }

        com.github.gubejs.util.ConsoleJS.STARTUP.error("There is no block state property called '"
            + name + "'. Use a BlockProperties constant, the name of one (e.g. "
            + "'horizontal_facing'), or booleanProperty/intProperty for one of your own.");
        return null;
    }

    /** The texture slot name a face uses in a {@code cube} model. */
    private static String faceOf(net.minecraft.core.Direction direction) {
        return switch (direction) {
            case UP -> "up";
            case DOWN -> "down";
            default -> direction.getSerializedName();
        };
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
