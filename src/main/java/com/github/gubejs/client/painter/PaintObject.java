/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/painter/PainterObject.java
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
package com.github.gubejs.client.painter;

import com.github.gubejs.bindings.ColorWrapper;
import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.ConsoleJS;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * One thing drawn on the screen, described entirely by a tag.
 *
 * <p>A tag rather than an object graph because this has to cross the network: a server script says
 * what a player's screen should show, and the description is the only thing that can be sent. It
 * is also what lets a client script build one the same way — there is one shape to learn, not two.
 *
 * <pre>{@code
 * player.paint({
 *     hp: { type: 'text', text: 'HP', x: 10, y: 10, color: '#FF5555', shadow: true },
 *     bar: { type: 'rectangle', x: 10, y: 22, w: 100, h: 6, color: '#8800FF00' }
 * })
 * }</pre>
 *
 * <p>Any of {@code x}, {@code y}, {@code w} and {@code h} may be written as a string instead of a
 * number — {@code '$SW/2-50'}, {@code '$SH-30'}, {@code '50%'} — see {@link PaintExpression}. The
 * alignments take {@code 'left'}/{@code 'center'}/{@code 'right'} and
 * {@code 'top'}/{@code 'center'}/{@code 'bottom'} as readily as they take {@code 0}/{@code 1}/
 * {@code 2}.
 */
public class PaintObject extends GuiComponent {

    /** How a coordinate is measured: from the left/top, from the middle, or from the right/bottom. */
    private static final int ALIGN_START = 0;

    private static final int ALIGN_CENTER = 1;

    private static final int ALIGN_END = 2;

    private final CompoundTag tag;

    /** Parsed expressions, by property, so a description drawn every frame is parsed once. */
    @Nullable
    private Map<String, PaintExpression> expressions;

    /** Alignment names already complained about, so a typo is one line and not one a frame. */
    @Nullable
    private Set<String> reportedAlignments;

    public PaintObject(CompoundTag tag) {
        this.tag = tag;
    }

    /**
     * Returns whether this object should be drawn at all.
     *
     * @return {@code true} unless the description says {@code visible: false}
     */
    public boolean isVisible() {
        return !tag.contains("visible") || tag.getBoolean("visible");
    }

    /**
     * Draws the object.
     *
     * @param pose the transform stack the overlay is being drawn with
     * @param screenWidth the width of the screen in GUI pixels
     * @param screenHeight the height of the screen in GUI pixels
     */
    public void draw(PoseStack pose, int screenWidth, int screenHeight) {
        // Two passes, because '$W' is only an answerable question once the width is a number.
        var unmeasured = new PaintExpression.Frame(
            screenWidth, screenHeight, PaintExpression.UNKNOWN, PaintExpression.UNKNOWN);
        var width = intOf("w", 0, unmeasured, screenWidth);
        var height = intOf("h", 0, unmeasured, screenHeight);
        var frame = new PaintExpression.Frame(screenWidth, screenHeight, width, height);
        var x = position("x", "alignX", screenWidth, width, frame);
        var y = position("y", "alignY", screenHeight, height, frame);

        switch (tag.getString("type")) {
            case "rectangle", "rect" -> fill(pose, x, y, x + width, y + height, color("color", 0xFFFFFFFF));
            case "gradient" -> fillGradient(pose, x, y, x + width, y + height,
                color("color", 0xFFFFFFFF), color("colorEnd", color("color", 0xFFFFFFFF)));
            case "text" -> drawText(pose, x, y);
            case "item" -> drawItem(x, y);
            case "texture" -> drawTexture(pose, x, y, width, height);
            default -> {
                // Nothing: an unknown type is a typo in a script, and refusing to draw is quieter
                // than a message logged sixty times a second.
            }
        }
    }

    private void drawText(PoseStack pose, int x, int y) {
        var font = Minecraft.getInstance().font;
        var text = component();
        var color = color("color", 0xFFFFFFFF);

        // The width is not given for text, so centring has to happen after it is measured.
        var aligned = switch (alignmentOf("alignX")) {
            case ALIGN_CENTER -> x - font.width(text) / 2;
            case ALIGN_END -> x - font.width(text);
            default -> x;
        };

        if (tag.getBoolean("shadow")) {
            font.drawShadow(pose, text, aligned, y, color);
        } else {
            font.draw(pose, text, aligned, y, color);
        }
    }

    private void drawItem(int x, int y) {
        var stack = item();

        if (stack.isEmpty()) {
            return;
        }

        var renderer = Minecraft.getInstance().getItemRenderer();
        renderer.renderAndDecorateItem(stack, x, y);

        if (tag.getBoolean("count")) {
            renderer.renderGuiItemDecorations(Minecraft.getInstance().font, stack, x, y);
        }
    }

    private void drawTexture(PoseStack pose, int x, int y, int width, int height) {
        var texture = ResourceLocation.tryParse(tag.getString("texture"));

        if (texture == null) {
            return;
        }

        var color = color("color", 0xFFFFFFFF);
        RenderSystem.setShader(net.minecraft.client.renderer.GameRenderer::getPositionTexShader);
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShaderColor(
            ColorWrapper.getRed(color) / 255F, ColorWrapper.getGreen(color) / 255F,
            ColorWrapper.getBlue(color) / 255F, ColorWrapper.getAlpha(color) / 255F);
        RenderSystem.enableBlend();

        // The whole texture, since a script giving pixel offsets into an atlas would need to know
        // the atlas layout -- and a script's own texture is a file of exactly the right size.
        blit(pose, x, y, 0, 0F, 0F, width, height, width, height);

        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
        RenderSystem.disableBlend();
    }

    /**
     * Works out where one axis starts.
     *
     * <p>The alignment is what makes a description survive a resized window: a coordinate measured
     * from the right stays the same distance from the right whatever the screen is.
     */
    private int position(String key, String alignKey, int screenSize, int objectSize,
            PaintExpression.Frame frame) {
        var value = intOf(key, 0, frame, screenSize);

        return switch (alignmentOf(alignKey)) {
            case ALIGN_CENTER -> screenSize / 2 + value - objectSize / 2;
            case ALIGN_END -> screenSize - value - objectSize;
            default -> value;
        };
    }

    private Component component() {
        if (tag.contains("text", Tag.TAG_COMPOUND) || tag.contains("text", Tag.TAG_LIST)) {
            return TextWrapper.fromJson(com.github.gubejs.util.NbtHelper.toJson(tag.get("text")));
        }

        return TextWrapper.of(tag.getString("text"));
    }

    private ItemStack item() {
        return tag.contains("item", Tag.TAG_COMPOUND)
            ? ItemStack.of(tag.getCompound("item"))
            : ItemStackJS.of(tag.getString("item"));
    }

    private int color(String key, int fallback) {
        return tag.contains(key) ? ColorWrapper.of(readColor(key)) : fallback;
    }

    @Nullable
    private Object readColor(String key) {
        var value = tag.get(key);

        if (value == null) {
            return null;
        }

        // Both spellings: a script writes '#ff0000' as often as it writes a number, and NBT keeps
        // whichever one it was given.
        return value.getId() == Tag.TAG_STRING ? value.getAsString()
            : com.github.gubejs.util.NbtHelper.toObject(value);
    }

    /**
     * Reads a measurement, which a description is free to have written as a sum.
     *
     * <p>{@code percentOf} is what a trailing {@code %} is a percentage of, which is the screen
     * size along the axis the property belongs to and so cannot be worked out from the key alone.
     */
    private int intOf(String key, int fallback, PaintExpression.Frame frame, int percentOf) {
        var value = tag.get(key);

        if (value == null) {
            return fallback;
        }

        if (value.getId() == Tag.TAG_STRING) {
            return expression(key, value.getAsString()).get(frame, percentOf);
        }

        return tag.getInt(key);
    }

    private PaintExpression expression(String key, String source) {
        if (expressions == null) {
            expressions = new HashMap<>(4);
        }

        return expressions.computeIfAbsent(key, ignored -> PaintExpression.of(source));
    }

    /**
     * Reads an alignment, by name or by number.
     *
     * <p>Both, because a description is as likely to have been written by hand as generated: the
     * names are what a person reaches for, and the numbers are what an older pack already says.
     */
    private int alignmentOf(String key) {
        var value = tag.get(key);

        if (value == null) {
            return ALIGN_START;
        }

        if (value.getId() != Tag.TAG_STRING) {
            return tag.getInt(key);
        }

        var name = value.getAsString();

        return switch (name.toLowerCase(Locale.ROOT)) {
            case "left", "top", "start" -> ALIGN_START;
            case "center", "centre", "middle" -> ALIGN_CENTER;
            case "right", "bottom", "end" -> ALIGN_END;
            default -> {
                reportAlignment(key, name);
                yield ALIGN_START;
            }
        };
    }

    private void reportAlignment(String key, String name) {
        if (reportedAlignments == null) {
            reportedAlignments = new HashSet<>(2);
        }

        if (reportedAlignments.add(name)) {
            ConsoleJS.CLIENT.warn("'" + name + "' is not an alignment -- " + key
                + " takes left, center, right, top, bottom, or 0, 1 and 2");
        }
    }
}
