/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/event/ClientEvents.java
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
package com.github.gubejs.bindings.event;

import com.github.gubejs.client.ClientEventJS;
import com.github.gubejs.client.ClientInitEventJS;
import com.github.gubejs.client.DebugInfoEventJS;
import com.github.gubejs.client.LangEventJS;
import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;

/**
 * The {@code ClientEvents} global: things only the machine with a screen knows about.
 *
 * <p>Nothing here exists on a dedicated server, and a listener registered from a server script
 * would never fire — hence {@code client}, which refuses the registration outright rather than
 * leaving a pack author wondering.
 */
public interface ClientEvents {

    EventGroup GROUP = EventGroup.of("ClientEvents");

    /** Fires once the client has finished setting up. Listened to from a startup script. */
    EventHandler INIT = GROUP.startup("init", () -> ClientInitEventJS.class);

    /**
     * Adds files to a resource pack that sits above every other —
     * {@code ClientEvents.highPriorityAssets(event => ...)}.
     *
     * <p>Where a pack writes the models it would otherwise have to write by hand, one per file, for
     * things it made in a loop. Fires as the pack is opened on every resource reload.
     */
    EventHandler HIGH_ASSETS = GROUP.client("highPriorityAssets",
        () -> com.github.gubejs.client.GenerateClientAssetsEventJS.class);

    /** Fires when this client joins a world, single-player or otherwise. */
    EventHandler LOGGED_IN = GROUP.client("loggedIn", () -> ClientEventJS.class);

    /** Fires when this client leaves a world. */
    EventHandler LOGGED_OUT = GROUP.client("loggedOut", () -> ClientEventJS.class);

    /**
     * Fires every client tick, twenty times a second.
     *
     * <p>Keeps ticking in menus and while the game is paused in single-player, so check
     * {@code event.player} before using it.
     */
    EventHandler TICK = GROUP.client("tick", () -> ClientEventJS.class);

    /**
     * The screen being drawn — {@code ClientEvents.paintScreen(event => ...)}.
     *
     * <p>Fires once per frame, after the game's own interface — so in a world, not on the title
     * screen or behind a menu. What it draws lasts one frame, which is the difference from
     * {@code Client.paint(...)}: that keeps what it was given until something changes it, and
     * costs nothing in between.
     */
    EventHandler PAINT_SCREEN = GROUP.client("paintScreen",
        () -> com.github.gubejs.client.painter.PaintScreenEventJS.class);

    /**
     * Fires when what the named painter is drawing changes.
     *
     * <p>Including when the change came from the server, which is what makes it useful: a client
     * script can notice that a server script sent something and react to it, rather than polling.
     */
    EventHandler PAINTER_UPDATED = GROUP.client("painterUpdated", () -> ClientEventJS.class);

    /** The left-hand column of the F3 screen being assembled. Fires every frame it is open. */
    EventHandler DEBUG_LEFT = GROUP.client("leftDebugInfo", () -> DebugInfoEventJS.class);

    /** The right-hand column of the F3 screen being assembled. */
    EventHandler DEBUG_RIGHT = GROUP.client("rightDebugInfo", () -> DebugInfoEventJS.class);

    /**
     * The translation table being built — {@code ClientEvents.lang('en_us', event => ...)}.
     *
     * <p>Requires the language code, because the entries a pack adds are per-language and a
     * listener that ran for all of them would have no way to tell which one it was writing.
     */
    EventHandler LANG = GROUP.client("lang", () -> LangEventJS.class).extra(Extra.REQUIRES_STRING);

    /**
     * Adds a texture to a stitched atlas —
     * {@code ClientEvents.atlasSpriteRegistry('minecraft:blocks', event => ...)}.
     *
     * <p>Requires the atlas, because there are several and a listener that ran for all of them
     * would add every sprite to every one.
     */
    EventHandler ATLAS_SPRITE_REGISTRY = GROUP.client("atlasSpriteRegistry",
        () -> com.github.gubejs.client.AtlasSpriteRegistryEventJS.class).extra(Extra.REQUIRES_ID);
}
