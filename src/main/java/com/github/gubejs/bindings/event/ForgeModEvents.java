/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * forge/src/main/java/dev/latvian/mods/kubejs/forge/ForgeKubeJSEvents.java
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

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.ForgeEventBridge;
import com.github.gubejs.event.ForgeEventJS;

/**
 * The {@code ForgeModEvents} global: the events fired while the game loads.
 *
 * <pre>{@code
 * ForgeModEvents.onEvent('net.minecraftforge.event.entity.EntityAttributeModificationEvent',
 *     event => {
 *         event.add(Java.loadClass('net.minecraft.world.entity.EntityType').PLAYER,
 *             Java.loadClass('net.minecraftforge.common.ForgeMod').SWIM_SPEED.get())
 *     })
 * }</pre>
 *
 * <p>Forge has two buses. One carries what happens in a running game and is {@link ForgeEvents};
 * this is the other, which carries the steps of loading — setup, registration, entity attributes,
 * renderers, model and texture loading. Each rejects the other's events outright, so the error
 * message for using the wrong one names the right one.
 *
 * <p>Startup scripts only, and not because of a policy: these events are fired once, while the game
 * loads, and server and client scripts do not exist yet. A listener registered from one would be
 * registered after the thing it wanted to hear about had already happened.
 *
 * <p>Startup scripts run inside this mod's constructor, so what is still ahead is everything after
 * mod construction. {@code FMLCommonSetupEvent}, {@code FMLClientSetupEvent},
 * {@code RegisterEvent}, {@code EntityRenderersEvent} and {@code RegisterColorHandlersEvent} are
 * all reachable; anything fired earlier is not.
 */
public interface ForgeModEvents {

    EventGroup GROUP = EventGroup.of("ForgeModEvents");

    /** Any event on this mod's own loading bus. */
    EventHandler ON_EVENT = GROUP.startup("onEvent", () -> ForgeEventJS.class)
        .extra(ForgeEvents.EVENT_CLASS)
        .onListen(ForgeEventBridge::bridgeModEvent);
}
