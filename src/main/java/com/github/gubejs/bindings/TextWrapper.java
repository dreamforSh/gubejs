/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/TextWrapper.java
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
package com.github.gubejs.bindings;

import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Text} global, for building chat components.
 *
 * <pre>{@code
 * Text.of('plain text')
 * Text.gold('Warning!').bold(true)
 * Text.translate('block.minecraft.stone')
 * Text.join([Text.red('a'), ' and ', Text.blue('b')])
 * }</pre>
 *
 * <p>The colour helpers are generated from {@link ChatFormatting} rather than written out, so a
 * script gets every vanilla colour and formatting code by name without this file listing them.
 */
public final class TextWrapper {

    private TextWrapper() {
    }

    /**
     * Turns anything into a component.
     *
     * <p>A string becomes literal text, a list becomes those parts joined, and an object is read
     * as the JSON form a datapack would use — which is what makes a tooltip written as
     * {@code {text: 'hi', color: 'red'}} work.
     *
     * @param value what to convert
     * @return the component, empty for {@code null}
     */
    public static MutableComponent of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return Component.empty();
        } else if (unwrapped instanceof MutableComponent component) {
            return component;
        } else if (unwrapped instanceof Component component) {
            return component.copy();
        } else if (unwrapped instanceof CharSequence text) {
            return Component.literal(text.toString());
        } else if (unwrapped instanceof List<?> parts) {
            return join(parts);
        } else if (unwrapped instanceof Map<?, ?> || unwrapped instanceof com.google.gson.JsonElement) {
            var component = Component.Serializer.fromJson(JsonUtils.of(unwrapped));
            return component == null ? Component.empty() : component.copy();
        }

        return Component.literal(String.valueOf(unwrapped));
    }

    /**
     * Builds literal text, without the JSON interpretation {@link #of} applies to objects.
     *
     * @param value the text
     * @return the component
     */
    public static MutableComponent literal(@Nullable Object value) {
        return Component.literal(String.valueOf(ValueUtils.unwrap(value)));
    }

    /**
     * Builds a translated component.
     *
     * @param key the translation key
     * @param args values for the placeholders in the translation
     * @return the component
     */
    public static MutableComponent translate(String key, Object... args) {
        var converted = new Object[args.length];

        for (var i = 0; i < args.length; i++) {
            converted[i] = ValueUtils.unwrap(args[i]);
        }

        return Component.translatable(key, converted);
    }

    /**
     * Joins parts into one component.
     *
     * @param parts the parts, each converted with {@link #of}
     * @return the joined component
     */
    public static MutableComponent join(Iterable<?> parts) {
        var result = Component.empty();

        for (var part : parts) {
            result.append(of(part));
        }

        return result;
    }

    /**
     * Reads a component from its datapack JSON form.
     *
     * @param json the JSON, as text or as an object
     * @return the component
     */
    public static MutableComponent fromJson(@Nullable Object json) {
        var unwrapped = ValueUtils.unwrap(json);
        var element = unwrapped instanceof CharSequence text
            ? JsonUtils.parse(text.toString()) : JsonUtils.of(unwrapped);
        var component = Component.Serializer.fromJson(element);
        return component == null ? Component.empty() : component.copy();
    }

    /**
     * Renders a component as the JSON a datapack would contain.
     *
     * @param component the component to render
     * @return the JSON text
     */
    public static String toJson(Component component) {
        return Component.Serializer.toJson(component);
    }

    /**
     * Builds text in a colour given as a hex number, which vanilla's named colours cannot express.
     *
     * @param value the text
     * @param color the RGB colour, e.g. {@code 0xFF8800}
     * @return the component
     */
    public static MutableComponent colored(@Nullable Object value, int color) {
        return of(value).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(color)));
    }

    // --- the vanilla colours, by name --------------------------------------------------------

    /**
     * Builds text with one of the vanilla formatting codes applied.
     *
     * @param formatting the code to apply
     * @param value the text
     * @return the component
     */
    public static MutableComponent withFormatting(ChatFormatting formatting, @Nullable Object value) {
        return of(value).withStyle(formatting);
    }

    public static MutableComponent black(@Nullable Object value) {
        return withFormatting(ChatFormatting.BLACK, value);
    }

    public static MutableComponent darkBlue(@Nullable Object value) {
        return withFormatting(ChatFormatting.DARK_BLUE, value);
    }

    public static MutableComponent darkGreen(@Nullable Object value) {
        return withFormatting(ChatFormatting.DARK_GREEN, value);
    }

    public static MutableComponent darkAqua(@Nullable Object value) {
        return withFormatting(ChatFormatting.DARK_AQUA, value);
    }

    public static MutableComponent darkRed(@Nullable Object value) {
        return withFormatting(ChatFormatting.DARK_RED, value);
    }

    public static MutableComponent darkPurple(@Nullable Object value) {
        return withFormatting(ChatFormatting.DARK_PURPLE, value);
    }

    public static MutableComponent gold(@Nullable Object value) {
        return withFormatting(ChatFormatting.GOLD, value);
    }

    public static MutableComponent gray(@Nullable Object value) {
        return withFormatting(ChatFormatting.GRAY, value);
    }

    public static MutableComponent darkGray(@Nullable Object value) {
        return withFormatting(ChatFormatting.DARK_GRAY, value);
    }

    public static MutableComponent blue(@Nullable Object value) {
        return withFormatting(ChatFormatting.BLUE, value);
    }

    public static MutableComponent green(@Nullable Object value) {
        return withFormatting(ChatFormatting.GREEN, value);
    }

    public static MutableComponent aqua(@Nullable Object value) {
        return withFormatting(ChatFormatting.AQUA, value);
    }

    public static MutableComponent red(@Nullable Object value) {
        return withFormatting(ChatFormatting.RED, value);
    }

    public static MutableComponent lightPurple(@Nullable Object value) {
        return withFormatting(ChatFormatting.LIGHT_PURPLE, value);
    }

    public static MutableComponent yellow(@Nullable Object value) {
        return withFormatting(ChatFormatting.YELLOW, value);
    }

    public static MutableComponent white(@Nullable Object value) {
        return withFormatting(ChatFormatting.WHITE, value);
    }
}
