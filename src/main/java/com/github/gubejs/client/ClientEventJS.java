/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/ClientEventJS.java
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
package com.github.gubejs.client;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for events that only happen on a client.
 *
 * <p>Everything here can be {@code null}: a client script's context exists from the moment the
 * resource packs load, which is long before there is a world or a player.
 */
public class ClientEventJS extends EventJS implements ScriptTypeHolder {

    /**
     * Returns the game instance.
     *
     * @return the client
     */
    public Minecraft getClient() {
        return Minecraft.getInstance();
    }

    /**
     * Returns the player at the keyboard.
     *
     * @return the player, or {@code null} outside a world
     */
    @Nullable
    public LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * Returns the level the player is in.
     *
     * @return the level, or {@code null} outside a world
     */
    @Nullable
    public ClientLevel getLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.CLIENT;
    }
}
