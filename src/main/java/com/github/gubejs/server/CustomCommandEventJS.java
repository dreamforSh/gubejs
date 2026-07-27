/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/server/CustomCommandEventJS.java
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
package com.github.gubejs.server;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A command a script invented, reached through {@code /gubejs custom_command <id>}.
 *
 * <pre>{@code
 * ServerEvents.customCommand('daily', event => {
 *     event.player.give('minecraft:diamond')
 * })
 * }</pre>
 *
 * <p>The simple alternative to {@code ServerEvents.commandRegistry}: no Brigadier, and the
 * listener can be changed by a reload without rebuilding the command tree — which matters because
 * a registered command's tree is built once, when the server starts.
 */
public final class CustomCommandEventJS extends EventJS implements ScriptTypeHolder {

    private final String id;

    private final CommandSourceStack source;

    public CustomCommandEventJS(String id, CommandSourceStack source) {
        this.id = id;
        this.source = source;
    }

    /**
     * Returns which custom command was run.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns who or what ran it.
     *
     * @return the command source
     */
    public CommandSourceStack getSource() {
        return source;
    }

    /**
     * Returns the player who ran it.
     *
     * @return the player, or {@code null} when it came from the console or a command block
     */
    @Nullable
    public ServerPlayer getPlayer() {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    /**
     * Returns the server.
     *
     * @return the server
     */
    public MinecraftServer getServer() {
        return source.getServer();
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.SERVER;
    }
}
