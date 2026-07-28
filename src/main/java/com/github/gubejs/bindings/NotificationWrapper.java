/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/util/NotificationBuilder.java
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

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Notification} global: the description of a toast, before anyone is shown it.
 *
 * <pre>{@code
 * const done = Notification.make({ title: 'Quest complete', icon: 'minecraft:diamond' })
 * event.server.players.forEach(player => player.notify(done))
 * }</pre>
 *
 * <p>Makes nothing appear by itself. A toast belongs to one client, so raising one is
 * {@code player.notify(...)}, and what this class returns is the tag that crosses the network to
 * get there — see {@link com.github.gubejs.client.painter.ScriptToast} for the other end. That is
 * also why a description built once can be handed to every player in turn.
 *
 * <p>Every field is normalised on the way in, so a colour given as {@code 'red'} or as a number
 * and an icon given as an item stack all reach the client in the one form the toast reads. Styled
 * text is the exception: the toast is described by plain strings, so a component is flattened to
 * its text and its colours are lost.
 */
public final class NotificationWrapper {

    private NotificationWrapper() {
    }

    /**
     * Builds a notification from a description.
     *
     * <p>Accepts either the whole object — {@code title}, {@code subtitle}, {@code icon},
     * {@code color} and {@code duration} in milliseconds — or a single piece of text, which is
     * taken as the title, since that is the notification a script usually wants.
     *
     * @param description the object, or the title on its own
     * @return the notification, ready for {@code player.notify}
     */
    public static CompoundTag make(@Nullable Object description) {
        var unwrapped = ValueUtils.unwrap(description);

        if (unwrapped instanceof CompoundTag tag) {
            return tag.copy();
        } else if (unwrapped instanceof Map<?, ?> map) {
            return fromMap(map);
        }

        return make(unwrapped, null, null);
    }

    /**
     * Builds a notification with a title and a second line.
     *
     * @param title the first line
     * @param subtitle the second line, which may be {@code null} for none
     * @return the notification
     */
    public static CompoundTag make(@Nullable Object title, @Nullable Object subtitle) {
        return make(title, subtitle, null);
    }

    /**
     * Builds a notification with a title, a second line and an item beside them.
     *
     * @param title the first line
     * @param subtitle the second line, which may be {@code null} for none
     * @param icon what to draw beside the text, which may be {@code null} for nothing
     * @return the notification
     */
    public static CompoundTag make(@Nullable Object title, @Nullable Object subtitle,
                                   @Nullable Object icon) {
        var tag = new CompoundTag();
        tag.putString("title", flatten(title));
        tag.putString("subtitle", flatten(subtitle));

        var item = iconId(icon);

        // Absent rather than empty, because the toast lays the text out differently when there is
        // no icon and it decides that on the key being there at all.
        if (!item.isEmpty()) {
            tag.putString("icon", item);
        }

        return tag;
    }

    private static CompoundTag fromMap(Map<?, ?> map) {
        var tag = make(pick(map, "title", "text"), pick(map, "subtitle", "subtext"),
            pick(map, "icon", "item"));

        var color = map.get("color");

        if (color != null) {
            // As hex text, since that is the only form the toast reads a colour in and it accepts
            // rather more spellings than it writes.
            tag.putString("color", ColorWrapper.toHex(ColorWrapper.of(color)));
        }

        if (ValueUtils.unwrap(map.get("duration")) instanceof Number duration) {
            tag.putLong("duration", duration.longValue());
        }

        return tag;
    }

    @Nullable
    private static Object pick(Map<?, ?> map, String key, String alias) {
        return map.containsKey(key) ? map.get(key) : map.get(alias);
    }

    private static String flatten(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);
        return unwrapped == null ? "" : TextWrapper.of(unwrapped).getString();
    }

    private static String iconId(@Nullable Object icon) {
        var unwrapped = ValueUtils.unwrap(icon);

        if (unwrapped == null) {
            return "";
        } else if (unwrapped instanceof CharSequence text) {
            return text.toString();
        }

        var stack = ItemStackJS.of(unwrapped);

        if (stack.isEmpty()) {
            return "";
        }

        var id = ForgeRegistries.ITEMS.getKey(stack.getItem());
        return id == null ? "" : id.toString();
    }
}
