/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/player/PlayerEventJS.java
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

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * Base class for every event that happens to a player.
 *
 * <p>Which script type it goes to follows the player: a client-side player means a client script,
 * a server-side one means a server script. Getting that wrong is the classic scripting bug —
 * a listener that fires twice in single-player, once per side — so it is decided here rather
 * than left to each event.
 */
public class PlayerEventJS extends EventJS implements ScriptTypeHolder {

    private final Player player;

    public PlayerEventJS(Player player) {
        this.player = player;
    }

    /**
     * Returns the player this happened to.
     *
     * @return the player
     */
    public Player getPlayer() {
        return player;
    }

    /**
     * Returns the player as a server player, for the things only the server side can do.
     *
     * @return the server player, or {@code null} if this is the client's copy
     */
    public ServerPlayer getServerPlayer() {
        return player instanceof ServerPlayer serverPlayer ? serverPlayer : null;
    }

    /**
     * Returns the level the player is in.
     *
     * @return the level
     */
    public Level getLevel() {
        return player.level;
    }

    /**
     * Returns the player's name as plain text.
     *
     * @return the name
     */
    public String getUsername() {
        return player.getGameProfile().getName();
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return player.level.isClientSide() ? ScriptType.CLIENT : ScriptType.SERVER;
    }
}
