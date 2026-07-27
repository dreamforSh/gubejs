/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/player/PlayerChatReceivedEventJS.java
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
package com.github.gubejs.player;

import com.github.gubejs.bindings.TextWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * A chat message a player sent, before anyone has seen it.
 *
 * <p>{@code event.cancel()} drops the message. Assigning to {@code event.message} rewrites it.
 */
public final class PlayerChatEventJS extends PlayerEventJS {

    private final String message;

    @Nullable
    private Component component;

    public PlayerChatEventJS(Player player, String message) {
        super(player);
        this.message = message;
    }

    /**
     * Returns what the player typed.
     *
     * @return the raw message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the message as it will be sent, including any rewrite.
     *
     * @return the component, or {@code null} if the message was not rewritten
     */
    @Nullable
    public Component getComponent() {
        return component;
    }

    /**
     * Replaces the message.
     *
     * @param value the new message, as text or a component
     */
    public void setComponent(@Nullable Object value) {
        component = value == null ? null : TextWrapper.of(value);
    }
}
