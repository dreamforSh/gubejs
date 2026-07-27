/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/custom/ShapedBlockBuilder.java
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

import com.github.gubejs.registry.RegistryInfo;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;

/**
 * Builds the block shapes a pack makes out of a material it just invented — stairs, a slab, a
 * fence, a wall.
 *
 * <p>These are one builder rather than eight because they differ only in two places: which vanilla
 * class to construct, and which model files to generate. Everything a script sets — hardness,
 * sound, texture — means the same thing for all of them.
 *
 * <p>The models are generated from the one texture, which is what makes a full set of shapes
 * possible without an author writing eight blockstate files by hand. A file the pack already
 * provides always wins, so a hand-written model is never overwritten.
 */
public class ShapedBlockBuilder extends BlockBuilder {

    /** The shapes a script can create. */
    public enum Shape {
        STAIRS,
        SLAB,
        FENCE,
        FENCE_GATE,
        WALL,
        CARPET,
        WOODEN_PRESSURE_PLATE,
        STONE_PRESSURE_PLATE,
        WOODEN_BUTTON,
        STONE_BUTTON,
        DOOR,
        TRAPDOOR
    }

    private final Shape shape;

    public ShapedBlockBuilder(ResourceLocation id, Shape shape) {
        super(id);
        this.shape = shape;
    }

    @Override
    public Block createObject() {
        var properties = createProperties();

        block = switch (shape) {
            // Stairs need a state to copy their behaviour from -- what they sound like when
            // walked on and how they render. Vanilla's own use the block they are made of; this
            // has no such block, so stone stands in and every property that matters is then
            // overridden by the properties above.
            case STAIRS -> new StairBlock(Blocks.STONE::defaultBlockState, properties) { };
            case SLAB -> new SlabBlock(properties);
            case FENCE -> new FenceBlock(properties);
            case FENCE_GATE -> new FenceGateBlock(properties);
            case WALL -> new WallBlock(properties);
            case CARPET -> new net.minecraft.world.level.block.CarpetBlock(properties);
            // A wooden plate answers to anything that stands on it, including a dropped item; a
            // stone one only to something alive. That difference is the whole reason vanilla has
            // two, and it is not something a texture can imply.
            case WOODEN_PRESSURE_PLATE -> new PressurePlateBlock(
                PressurePlateBlock.Sensitivity.EVERYTHING, properties);
            case STONE_PRESSURE_PLATE -> new PressurePlateBlock(
                PressurePlateBlock.Sensitivity.MOBS, properties);
            // The two buttons differ in how long they stay pressed, which vanilla holds in the
            // subclass rather than in a property.
            case WOODEN_BUTTON ->
                new net.minecraft.world.level.block.WoodButtonBlock(properties);
            case STONE_BUTTON ->
                new net.minecraft.world.level.block.StoneButtonBlock(properties);
            // The remaining two have protected constructors -- vanilla builds them from its own
            // subclasses -- so an empty subclass is what reaches them.
            case DOOR -> new DoorBlock(properties) { };
            case TRAPDOOR -> new TrapDoorBlock(properties) { };
        };

        return block;
    }

    @Override
    public Map<String, String> getGeneratedAssets() {
        var assets = new LinkedHashMap<String, String>();
        var namespace = id.getNamespace();
        var path = id.getPath();
        var face = texture != null ? texture.toString() : namespace + ":block/" + path;

        // Only the shapes whose models can be derived from one texture. A door and a trapdoor
        // need their own artwork -- a door is two half-height textures and nothing can invent
        // them -- so those are left for the pack to supply.
        switch (shape) {
            case STAIRS -> {
                assets.put(blockstate(namespace, path), stairsBlockstate(namespace, path));
                assets.put(model(namespace, path), template("minecraft:block/stairs", face));
                assets.put(model(namespace, path + "_inner"),
                    template("minecraft:block/inner_stairs", face));
                assets.put(model(namespace, path + "_outer"),
                    template("minecraft:block/outer_stairs", face));
                assets.put(itemModel(namespace, path), parent(namespace + ":block/" + path));
            }
            case SLAB -> {
                assets.put(blockstate(namespace, path), slabBlockstate(namespace, path));
                assets.put(model(namespace, path), template("minecraft:block/slab", face));
                assets.put(model(namespace, path + "_top"),
                    template("minecraft:block/slab_top", face));
                // A double slab is a full block, and the blockstate above names this model for it.
                assets.put(model(namespace, path + "_double"),
                    template("minecraft:block/cube_all", face));
                assets.put(itemModel(namespace, path), parent(namespace + ":block/" + path));
            }
            case FENCE_GATE -> {
                assets.put(blockstate(namespace, path), fenceGateBlockstate(namespace, path));

                for (var variant : new String[] {"", "_open", "_wall", "_wall_open"}) {
                    assets.put(model(namespace, path + variant),
                        template("minecraft:block/template_fence_gate" + variant, face));
                }

                assets.put(itemModel(namespace, path), parent(namespace + ":block/" + path));
            }
            case FENCE -> {
                assets.put(blockstate(namespace, path), fenceBlockstate(namespace, path));
                assets.put(model(namespace, path + "_post"),
                    template("minecraft:block/fence_post", face));
                assets.put(model(namespace, path + "_side"),
                    template("minecraft:block/fence_side", face));
                assets.put(model(namespace, path + "_inventory"),
                    template("minecraft:block/fence_inventory", face));
                assets.put(itemModel(namespace, path),
                    parent(namespace + ":block/" + path + "_inventory"));
            }
            case WALL -> {
                assets.put(blockstate(namespace, path), wallBlockstate(namespace, path));
                assets.put(model(namespace, path + "_post"),
                    template("minecraft:block/template_wall_post", face));
                assets.put(model(namespace, path + "_side"),
                    template("minecraft:block/template_wall_side", face));
                assets.put(model(namespace, path + "_side_tall"),
                    template("minecraft:block/template_wall_side_tall", face));
                assets.put(model(namespace, path + "_inventory"),
                    template("minecraft:block/wall_inventory", face));
                assets.put(itemModel(namespace, path),
                    parent(namespace + ":block/" + path + "_inventory"));
            }
            case CARPET -> {
                assets.put(blockstate(namespace, path), simpleBlockstate(namespace, path));
                assets.put(model(namespace, path), carpetModel(face));
                assets.put(itemModel(namespace, path), parent(namespace + ":block/" + path));
            }
            case WOODEN_PRESSURE_PLATE, STONE_PRESSURE_PLATE -> {
                assets.put(blockstate(namespace, path), pressurePlateBlockstate(namespace, path));
                assets.put(model(namespace, path),
                    template("minecraft:block/pressure_plate_up", face));
                assets.put(model(namespace, path + "_down"),
                    template("minecraft:block/pressure_plate_down", face));
                assets.put(itemModel(namespace, path), parent(namespace + ":block/" + path));
            }
            case WOODEN_BUTTON, STONE_BUTTON -> {
                assets.put(blockstate(namespace, path), buttonBlockstate(namespace, path));
                assets.put(model(namespace, path), template("minecraft:block/button", face));
                assets.put(model(namespace, path + "_pressed"),
                    template("minecraft:block/button_pressed", face));
                assets.put(model(namespace, path + "_inventory"),
                    template("minecraft:block/button_inventory", face));
                assets.put(itemModel(namespace, path),
                    parent(namespace + ":block/" + path + "_inventory"));
            }
            default -> {
                // Unreachable: the shapes without model generation are not registered as types.
            }
        }

        return assets;
    }

    // --- file paths ----------------------------------------------------------------------------

    private static String blockstate(String namespace, String path) {
        return "assets/" + namespace + "/blockstates/" + path + ".json";
    }

    private static String model(String namespace, String path) {
        return "assets/" + namespace + "/models/block/" + path + ".json";
    }

    private static String itemModel(String namespace, String path) {
        return "assets/" + namespace + "/models/item/" + path + ".json";
    }

    // --- model bodies --------------------------------------------------------------------------

    /** A model that is one of vanilla's templates with every face set to the same texture. */
    private static String template(String parent, String face) {
        return """
            {
              "parent": "%s",
              "textures": {
                "texture": "%s",
                "bottom": "%s",
                "top": "%s",
                "side": "%s",
                "wall": "%s"
              }
            }""".formatted(parent, face, face, face, face, face);
    }

    private static String parent(String model) {
        return "{\n  \"parent\": \"%s\"\n}".formatted(model);
    }

    /** A carpet, whose template names its one texture {@code wool} rather than {@code texture}. */
    private static String carpetModel(String face) {
        return """
            {
              "parent": "minecraft:block/carpet",
              "textures": {
                "wool": "%s"
              }
            }""".formatted(face);
    }

    // --- blockstate bodies ---------------------------------------------------------------------

    private static String stairsBlockstate(String namespace, String path) {
        var model = namespace + ":block/" + path;
        var body = new StringBuilder("{\n  \"variants\": {\n");

        // Written out rather than generated from a template, because a stair's state map is the
        // one place where the rotations do not follow a rule a loop could express.
        for (var half : new String[] {"bottom", "top"}) {
            for (var shape : new String[] {"straight", "inner_left", "inner_right",
                "outer_left", "outer_right"}) {
                for (var facing : new String[] {"north", "east", "south", "west"}) {
                    body.append("    \"facing=").append(facing)
                        .append(",half=").append(half)
                        .append(",shape=").append(shape).append("\": ")
                        .append(stairVariant(model, half, shape, facing)).append(",\n");
                }
            }
        }

        body.setLength(body.length() - 2);
        return body.append("\n  }\n}").toString();
    }

    private static String stairVariant(String model, String half, String shape, String facing) {
        var suffix = shape.startsWith("inner") ? "_inner" : shape.startsWith("outer") ? "_outer" : "";
        var y = switch (facing) {
            case "east" -> 0;
            case "south" -> 90;
            case "west" -> 180;
            default -> 270;
        };

        // The corner shapes are the same model turned a quarter further, and the left variants a
        // quarter back from the right ones.
        if (shape.endsWith("_left")) {
            y = (y + 270) % 360;
        }

        var uvlock = y != 0 || half.equals("top");
        var parts = new StringBuilder("{ \"model\": \"").append(model).append(suffix).append("\"");

        if (half.equals("top")) {
            parts.append(", \"x\": 180");
        }

        if (y != 0) {
            parts.append(", \"y\": ").append(y);
        }

        if (uvlock) {
            parts.append(", \"uvlock\": true");
        }

        return parts.append(" }").toString();
    }

    private static String slabBlockstate(String namespace, String path) {
        return """
            {
              "variants": {
                "type=bottom": { "model": "%1$s:block/%2$s" },
                "type=top": { "model": "%1$s:block/%2$s_top" },
                "type=double": { "model": "%1$s:block/%2$s_double" }
              }
            }""".formatted(namespace, path);
    }

    private static String fenceGateBlockstate(String namespace, String path) {
        var body = new StringBuilder("{\n  \"variants\": {\n");

        // Four facings times open times in-wall. The models differ only by suffix, and the
        // rotation follows the facing, so this one is a loop where the stairs are not.
        for (var inWall : new boolean[] {false, true}) {
            for (var open : new boolean[] {false, true}) {
                for (var facing : new String[] {"north", "east", "south", "west"}) {
                    var y = switch (facing) {
                        case "east" -> 90;
                        case "south" -> 180;
                        case "west" -> 270;
                        default -> 0;
                    };

                    body.append("    \"facing=").append(facing)
                        .append(",in_wall=").append(inWall)
                        .append(",open=").append(open).append("\": { \"model\": \"")
                        .append(namespace).append(":block/").append(path)
                        .append(inWall ? "_wall" : "").append(open ? "_open" : "")
                        .append('"');

                    if (y != 0) {
                        body.append(", \"y\": ").append(y);
                    }

                    body.append(", \"uvlock\": true },\n");
                }
            }
        }

        body.setLength(body.length() - 2);
        return body.append("\n  }\n}").toString();
    }

    private static String fenceBlockstate(String namespace, String path) {
        return """
            {
              "multipart": [
                { "apply": { "model": "%1$s:block/%2$s_post" } },
                { "when": { "north": "true" },
                  "apply": { "model": "%1$s:block/%2$s_side", "uvlock": true } },
                { "when": { "east": "true" },
                  "apply": { "model": "%1$s:block/%2$s_side", "y": 90, "uvlock": true } },
                { "when": { "south": "true" },
                  "apply": { "model": "%1$s:block/%2$s_side", "y": 180, "uvlock": true } },
                { "when": { "west": "true" },
                  "apply": { "model": "%1$s:block/%2$s_side", "y": 270, "uvlock": true } }
              ]
            }""".formatted(namespace, path);
    }

    private static String wallBlockstate(String namespace, String path) {
        return """
            {
              "multipart": [
                { "when": { "up": "true" }, "apply": { "model": "%1$s:block/%2$s_post" } },
                { "when": { "north": "low" },
                  "apply": { "model": "%1$s:block/%2$s_side", "uvlock": true } },
                { "when": { "east": "low" },
                  "apply": { "model": "%1$s:block/%2$s_side", "y": 90, "uvlock": true } },
                { "when": { "south": "low" },
                  "apply": { "model": "%1$s:block/%2$s_side", "y": 180, "uvlock": true } },
                { "when": { "west": "low" },
                  "apply": { "model": "%1$s:block/%2$s_side", "y": 270, "uvlock": true } },
                { "when": { "north": "tall" },
                  "apply": { "model": "%1$s:block/%2$s_side_tall", "uvlock": true } },
                { "when": { "east": "tall" },
                  "apply": { "model": "%1$s:block/%2$s_side_tall", "y": 90, "uvlock": true } },
                { "when": { "south": "tall" },
                  "apply": { "model": "%1$s:block/%2$s_side_tall", "y": 180, "uvlock": true } },
                { "when": { "west": "tall" },
                  "apply": { "model": "%1$s:block/%2$s_side_tall", "y": 270, "uvlock": true } }
              ]
            }""".formatted(namespace, path);
    }

    /** One model for every state, for a shape that has only one. */
    private static String simpleBlockstate(String namespace, String path) {
        return """
            {
              "variants": {
                "": { "model": "%s:block/%s" }
              }
            }""".formatted(namespace, path);
    }

    private static String pressurePlateBlockstate(String namespace, String path) {
        return """
            {
              "variants": {
                "powered=false": { "model": "%1$s:block/%2$s" },
                "powered=true": { "model": "%1$s:block/%2$s_down" }
              }
            }""".formatted(namespace, path);
    }

    private static String buttonBlockstate(String namespace, String path) {
        var model = namespace + ":block/" + path;
        var body = new StringBuilder("{\n  \"variants\": {\n");

        for (var face : new String[] {"floor", "wall", "ceiling"}) {
            for (var facing : new String[] {"north", "east", "south", "west"}) {
                body.append("    \"face=").append(face)
                    .append(",facing=").append(facing)
                    .append(",powered=false\": ")
                    .append(buttonVariant(model, face, facing)).append(",\n");
                body.append("    \"face=").append(face)
                    .append(",facing=").append(facing)
                    .append(",powered=true\": ")
                    .append(buttonVariant(model + "_pressed", face, facing)).append(",\n");
            }
        }

        body.setLength(body.length() - 2);
        return body.append("\n  }\n}").toString();
    }

    /**
     * One button variant.
     *
     * <p>A button on a ceiling is the floor model turned upside down, and turning it that way
     * reverses which direction {@code facing} points at — which is why the ceiling rotations are
     * the floor ones plus half a turn rather than the same numbers.
     */
    private static String buttonVariant(String model, String face, String facing) {
        var y = switch (facing) {
            case "east" -> 90;
            case "south" -> 180;
            case "west" -> 270;
            default -> 0;
        };

        var parts = new StringBuilder("{ \"model\": \"").append(model).append('"');

        switch (face) {
            case "wall" -> parts.append(", \"uvlock\": true, \"x\": 90");
            case "ceiling" -> {
                parts.append(", \"x\": 180");
                y = (y + 180) % 360;
            }
            default -> {
                // A button on the floor needs no tilt.
            }
        }

        if (y != 0) {
            parts.append(", \"y\": ").append(y);
        }

        return parts.append(" }").toString();
    }

    /**
     * Registers the block shapes scripts can create.
     *
     * <p>Only the ones whose models this can generate from a single texture. A door and a trapdoor
     * need artwork that cannot be derived — a door is two half-height textures, and nothing here
     * can invent them — and registering a block whose model is missing produces a purple cube and
     * a wall of errors in the log. Better not to offer the type than to offer one that cannot work.
     */
    public static void registerTypes() {
        for (var shape : new Shape[] {Shape.STAIRS, Shape.SLAB, Shape.FENCE, Shape.FENCE_GATE,
            Shape.WALL, Shape.CARPET, Shape.WOODEN_PRESSURE_PLATE, Shape.STONE_PRESSURE_PLATE,
            Shape.WOODEN_BUTTON, Shape.STONE_BUTTON}) {
            var name = shape.name().toLowerCase(Locale.ROOT);
            RegistryInfo.BLOCK.addType(name, id -> new ShapedBlockBuilder(id, shape));
        }
    }
}
