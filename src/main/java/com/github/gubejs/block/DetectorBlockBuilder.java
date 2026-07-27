/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/DetectorBlock.java
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

import com.github.gubejs.bindings.event.BlockEvents;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Builds a detector — {@code event.create('alarm', 'detector')}.
 *
 * <p>A detector is a block that does nothing except notice redstone. When the signal reaching it
 * goes from off to on or back, it fires {@code BlockEvents.detectorChanged} along with one of
 * {@code detectorPowered} and {@code detectorUnpowered}, and a script decides what that means.
 *
 * <pre>{@code
 * StartupEvents.registry('block', event => {
 *     event.create('alarm', 'detector')
 * })
 *
 * BlockEvents.detectorPowered('alarm', event => {
 *     event.block.level.players.forEach(p => p.tell(Text.red('Intruder')))
 * })
 * }</pre>
 *
 * <p>The name a listener filters on is the block's path with any {@code detector_} prefix or
 * {@code _detector} suffix removed, so {@code mypack:alarm_detector} is listened to as
 * {@code 'alarm'}. {@link #detectorId} overrides that when the two should differ.
 *
 * <p>The block looks like a redstone lamp by default — off when unpowered, lit when powered —
 * because this mod ships no textures of its own and a block with an invented texture path would be
 * a purple cube. {@link #texture} and {@link #poweredTexture} replace either half.
 */
public class DetectorBlockBuilder extends BlockBuilder {

    private static final String OFF_TEXTURE = "minecraft:block/redstone_lamp";

    private static final String ON_TEXTURE = "minecraft:block/redstone_lamp_on";

    /** What listeners filter on. */
    protected String detectorId;

    /** The face shown while powered, or {@code null} for the lit redstone lamp. */
    @org.jetbrains.annotations.Nullable
    protected String poweredTexture;

    public DetectorBlockBuilder(ResourceLocation id) {
        super(id);
        this.detectorId = defaultDetectorId(id);
        // Bedrock-like, because a detector is part of a mechanism rather than a building block and
        // one that a stray explosion can remove is a mechanism that silently stops working.
        this.hardness = 5F;
        this.resistance = 1200F;
    }

    /**
     * Sets the name listeners filter on.
     *
     * @param detectorId the name, as it will be written in {@code BlockEvents.detectorChanged}
     * @return this builder
     */
    public DetectorBlockBuilder detectorId(Object detectorId) {
        this.detectorId = String.valueOf(ValueUtils.unwrap(detectorId));
        return this;
    }

    /**
     * Sets the face shown while the detector is powered.
     *
     * @param texture the texture id
     * @return this builder
     */
    public DetectorBlockBuilder poweredTexture(Object texture) {
        this.poweredTexture = String.valueOf(ValueUtils.unwrap(texture));
        return this;
    }

    /**
     * Returns the name listeners filter on.
     *
     * @return the detector id
     */
    public String getDetectorId() {
        return detectorId;
    }

    @Override
    public Block createObject() {
        block = new DetectorBlock(createProperties(), this);
        return block;
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var namespace = id.getNamespace();
        var path = id.getPath();
        var off = texture != null ? texture.toString() : OFF_TEXTURE;
        var on = poweredTexture != null ? poweredTexture : ON_TEXTURE;

        assets.put("assets/" + namespace + "/blockstates/" + path + ".json",
            """
            {
              "variants": {
                "powered=false": { "model": "%1$s:block/%2$s" },
                "powered=true": { "model": "%1$s:block/%2$s_on" }
              }
            }""".formatted(namespace, path));

        assets.put("assets/" + namespace + "/models/block/" + path + ".json", cubeAll(off));
        assets.put("assets/" + namespace + "/models/block/" + path + "_on.json", cubeAll(on));

        if (hasItem()) {
            assets.put("assets/" + namespace + "/models/item/" + path + ".json",
                "{\n  \"parent\": \"%s:block/%s\"\n}".formatted(namespace, path));
        }

        return assets;
    }

    private static String cubeAll(String face) {
        return """
            {
              "parent": "minecraft:block/cube_all",
              "textures": {
                "all": "%s"
              }
            }""".formatted(face);
    }

    /** Strips the {@code detector} the block's name almost always already says. */
    private static String defaultDetectorId(ResourceLocation id) {
        var name = id.getPath().replace('/', '.');

        if (name.endsWith("_detector")) {
            name = name.substring(0, name.length() - "_detector".length());
        }

        if (name.startsWith("detector_")) {
            name = name.substring("detector_".length());
        }

        return name;
    }

    /** Registers the detector type scripts can create. */
    public static void registerTypes() {
        RegistryInfo.BLOCK.addType("detector", DetectorBlockBuilder::new);
    }

    /**
     * The block itself.
     *
     * <p>Its whole behaviour is one override: notice that the signal changed, write the new state,
     * and tell the scripts. The state is written before the events fire, so a listener reading
     * {@code event.block} sees the block as it now is.
     */
    public static final class DetectorBlock extends Block {

        private final DetectorBlockBuilder builder;

        private DetectorBlock(Properties properties, DetectorBlockBuilder builder) {
            super(properties);
            this.builder = builder;
            registerDefaultState(
                stateDefinition.any().setValue(BlockStateProperties.POWERED, false));
        }

        @Override
        protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> definition) {
            definition.add(BlockStateProperties.POWERED);
        }

        @Override
        @SuppressWarnings("deprecation")
        public void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighbour,
                                    BlockPos neighbourPos, boolean moving) {
            // Server only, the way the redstone lamp is: the client is told about the state change
            // by the block update that follows, and writing it here as well would fight that.
            if (level.isClientSide()) {
                return;
            }

            var powered = level.hasNeighborSignal(pos);

            if (powered == state.getValue(BlockStateProperties.POWERED)) {
                return;
            }

            level.setBlock(pos, state.setValue(BlockStateProperties.POWERED, powered), 2);

            var id = builder.detectorId;
            var toggled = powered ? BlockEvents.DETECTOR_POWERED : BlockEvents.DETECTOR_UNPOWERED;

            if (!BlockEvents.DETECTOR_CHANGED.hasListeners(id) && !toggled.hasListeners(id)) {
                return;
            }

            var event = new DetectorBlockEventJS(id, level, pos, powered);
            BlockEvents.DETECTOR_CHANGED.post(event, id);
            toggled.post(event, id);
        }
    }
}
