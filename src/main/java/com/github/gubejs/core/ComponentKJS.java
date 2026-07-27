/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/ComponentKJS.java
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
package com.github.gubejs.core;

import com.github.gubejs.bindings.ColorWrapper;
import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.util.ValueUtils;
import java.util.Map;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Styling a component by chaining, which is how a pack writes text —
 * {@code Text.gold('Home').clickRunCommand('/home').hover('Go home')}.
 *
 * <p>Vanilla can express all of this, but only through {@code withStyle} and a {@link net.minecraft.network.chat.Style}
 * built up by hand, and every step needs the enum or the event class named. These read as the pack
 * means them, and each returns the component so the next call continues the sentence.
 *
 * <p>Every method mutates and returns the same component rather than copying. That matches
 * {@code withStyle}, and it matters for the common shape {@code const t = Text.of('x'); t.bold()} —
 * a copying version would leave {@code t} unstyled and the pack author looking for the reason.
 */
public interface ComponentKJS extends Component {

    default MutableComponent gjs$self() {
        return (MutableComponent) this;
    }

    /**
     * Whether this component carries any styling of its own.
     *
     * @return {@code false} if it would render exactly as its parent styles it
     */
    default boolean hasStyle() {
        return getStyle() != null && !getStyle().isEmpty();
    }

    /**
     * Whether anything is appended to this component.
     *
     * @return whether it has siblings
     */
    default boolean hasSiblings() {
        return !getSiblings().isEmpty();
    }

    /**
     * Whether this component would render as nothing at all.
     *
     * <p>Not the same as an empty string: a component holding only empty siblings is empty too,
     * and that is the shape {@code Text.join([])} and a cancelled tooltip line leave behind.
     *
     * @return whether it renders no characters
     */
    default boolean isEmptyComponent() {
        if (getContents() != ComponentContents.EMPTY && !getString().isEmpty()) {
            return false;
        }

        for (var sibling : getSiblings()) {
            if (!sibling.getString().isEmpty()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Renders this component as the JSON a datapack would contain.
     *
     * @return the JSON text
     */
    default String toJson() {
        return Component.Serializer.toJson(gjs$self());
    }

    // --- style ---------------------------------------------------------------------------------

    /**
     * Sets the colour.
     *
     * @param color anything {@code Color.of} accepts — {@code '#ff8800'}, {@code 'red'},
     *     {@code 0xFF8800}, or {@code {r: 255, g: 136, b: 0}}
     * @return this component
     */
    default MutableComponent color(@Nullable Object color) {
        var unwrapped = ValueUtils.unwrap(color);

        if (unwrapped == null) {
            return noColor();
        }

        // The alpha a colour carries has nowhere to go: chat text has no transparency, and keeping
        // the byte would turn every opaque colour into a negative number that TextColor rejects.
        var rgb = ColorWrapper.of(unwrapped) & 0xFFFFFF;
        return gjs$self().setStyle(getStyle().withColor(TextColor.fromRgb(rgb)));
    }

    /**
     * Clears the colour, so this component renders in whatever colour it is placed in.
     *
     * @return this component
     */
    default MutableComponent noColor() {
        return gjs$self().setStyle(getStyle().withColor((TextColor) null));
    }

    /**
     * Turns bold on or off.
     *
     * @param value {@code true}, {@code false}, or {@code null} to inherit
     * @return this component
     */
    default MutableComponent bold(@Nullable Boolean value) {
        return gjs$self().setStyle(getStyle().withBold(value));
    }

    /** Turns bold on. @return this component */
    default MutableComponent bold() {
        return bold(Boolean.TRUE);
    }

    /**
     * Turns italics on or off.
     *
     * @param value {@code true}, {@code false}, or {@code null} to inherit
     * @return this component
     */
    default MutableComponent italic(@Nullable Boolean value) {
        return gjs$self().setStyle(getStyle().withItalic(value));
    }

    /** Turns italics on. @return this component */
    default MutableComponent italic() {
        return italic(Boolean.TRUE);
    }

    /**
     * Turns underlining on or off.
     *
     * @param value {@code true}, {@code false}, or {@code null} to inherit
     * @return this component
     */
    default MutableComponent underlined(@Nullable Boolean value) {
        return gjs$self().setStyle(getStyle().withUnderlined(value));
    }

    /** Turns underlining on. @return this component */
    default MutableComponent underlined() {
        return underlined(Boolean.TRUE);
    }

    /**
     * Turns the strikethrough on or off.
     *
     * @param value {@code true}, {@code false}, or {@code null} to inherit
     * @return this component
     */
    default MutableComponent strikethrough(@Nullable Boolean value) {
        return gjs$self().setStyle(getStyle().withStrikethrough(value));
    }

    /** Turns the strikethrough on. @return this component */
    default MutableComponent strikethrough() {
        return strikethrough(Boolean.TRUE);
    }

    /**
     * Turns the scrambled rendering on or off.
     *
     * @param value {@code true}, {@code false}, or {@code null} to inherit
     * @return this component
     */
    default MutableComponent obfuscated(@Nullable Boolean value) {
        return gjs$self().setStyle(getStyle().withObfuscated(value));
    }

    /** Turns the scrambled rendering on. @return this component */
    default MutableComponent obfuscated() {
        return obfuscated(Boolean.TRUE);
    }

    /**
     * Sets what shift-clicking this text inserts into the chat box.
     *
     * @param text the text to insert, or {@code null} for none
     * @return this component
     */
    default MutableComponent insertion(@Nullable String text) {
        return gjs$self().setStyle(getStyle().withInsertion(text));
    }

    /**
     * Sets the font.
     *
     * @param font the font id, e.g. {@code 'minecraft:uniform'}, or {@code null} for the default
     * @return this component
     */
    default MutableComponent font(@Nullable Object font) {
        var text = ValueUtils.asString(font);
        var id = text == null ? null : ResourceLocation.tryParse(text);
        return gjs$self().setStyle(getStyle().withFont(id));
    }

    // --- click ---------------------------------------------------------------------------------

    /**
     * Sets what clicking this text does.
     *
     * <p>Takes a {@link ClickEvent}, or the shorthand a pack actually writes: a string starting
     * with {@code /} runs that command, one starting with {@code http://} or {@code https://} opens
     * that address, and {@code {action: 'copy_to_clipboard', value: 'x'}} names any action
     * explicitly. Anything else is a suggested command, which is what vanilla does with a bare
     * word in a book.
     *
     * @param click the click behaviour, or {@code null} to remove it
     * @return this component
     */
    default MutableComponent click(@Nullable Object click) {
        var unwrapped = ValueUtils.unwrap(click);

        if (unwrapped == null) {
            return gjs$self().setStyle(getStyle().withClickEvent(null));
        } else if (unwrapped instanceof ClickEvent event) {
            return gjs$self().setStyle(getStyle().withClickEvent(event));
        } else if (unwrapped instanceof Map<?, ?> map) {
            var action = ClickEvent.Action.getByName(ValueUtils.asString(map.get("action")));
            var value = ValueUtils.asString(map.get("value"));

            if (action == null || value == null) {
                return gjs$self();
            }

            return gjs$self().setStyle(getStyle().withClickEvent(new ClickEvent(action, value)));
        }

        var text = String.valueOf(unwrapped);

        if (text.startsWith("/")) {
            return clickRunCommand(text);
        } else if (text.startsWith("http://") || text.startsWith("https://")) {
            return clickOpenUrl(text);
        }

        return clickSuggestCommand(text);
    }

    /**
     * Runs a command when clicked, as the player who clicked.
     *
     * @param command the command, with its leading slash
     * @return this component
     */
    default MutableComponent clickRunCommand(String command) {
        return click(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command));
    }

    /**
     * Puts a command in the chat box when clicked, without sending it.
     *
     * @param command the command
     * @return this component
     */
    default MutableComponent clickSuggestCommand(String command) {
        return click(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, command));
    }

    /**
     * Copies text to the clipboard when clicked.
     *
     * @param text what to copy
     * @return this component
     */
    default MutableComponent clickCopy(String text) {
        return click(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text));
    }

    /**
     * Turns to a page when clicked, in a written book.
     *
     * @param page the page number
     * @return this component
     */
    default MutableComponent clickChangePage(Object page) {
        return click(new ClickEvent(ClickEvent.Action.CHANGE_PAGE, String.valueOf(
            ValueUtils.unwrap(page))));
    }

    /**
     * Opens an address in the browser when clicked, after the player confirms.
     *
     * @param url the address
     * @return this component
     */
    default MutableComponent clickOpenUrl(String url) {
        return click(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
    }

    /**
     * Opens a local file when clicked.
     *
     * <p>Only the client itself may write this one — vanilla ignores it on anything that arrived
     * over the network, so a server sending it achieves nothing.
     *
     * @param path the file path
     * @return this component
     */
    default MutableComponent clickOpenFile(String path) {
        return click(new ClickEvent(ClickEvent.Action.OPEN_FILE, path));
    }

    // --- hover ---------------------------------------------------------------------------------

    /**
     * Sets what hovering over this text shows.
     *
     * @param text anything {@code Text.of} accepts, or {@code null} to remove the tooltip
     * @return this component
     */
    default MutableComponent hover(@Nullable Object text) {
        var unwrapped = ValueUtils.unwrap(text);

        if (unwrapped == null) {
            return gjs$self().setStyle(getStyle().withHoverEvent(null));
        }

        var hover = new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextWrapper.of(unwrapped));
        return gjs$self().setStyle(getStyle().withHoverEvent(hover));
    }
}
