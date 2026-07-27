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
import com.github.gubejs.event.Extra;
import com.github.gubejs.event.ForgeEventBridge;
import com.github.gubejs.event.ForgeEventJS;
import com.github.gubejs.script.ScriptTypePredicate;

/**
 * The {@code ForgeEvents} global: any event Forge fires, named by its class.
 *
 * <pre>{@code
 * ForgeEvents.onEvent('net.minecraftforge.event.entity.living.LivingExperienceDropEvent', event => {
 *     event.setDroppedExperience(event.getDroppedExperience() * 2)
 * })
 *
 * ForgeEvents.onEvent('net.minecraftforge.event.entity.player.PlayerInteractEvent$RightClickBlock',
 *     event => {
 *         if (event.getEntity().isCrouching()) {
 *             event.setCanceled(true)
 *         }
 *     })
 * }</pre>
 *
 * <p>The escape hatch, for the several hundred events this mod does not wrap. The listener is
 * handed Forge's own event object, so everything Forge's documentation says about that event is
 * true of what the script has — including {@code setCanceled}, which is how one of these is
 * cancelled. {@code event.cancel()} belongs to this mod's own events and is not what these use.
 *
 * <p>Listening to a superclass listens to everything under it, because that is how Forge's own
 * dispatch works: {@code LivingEvent} reaches every event about a living entity.
 *
 * <p>Unlike the KubeJS equivalent, these reload. There, the script's function is itself the bus
 * listener and cannot be taken off again, so a changed listener needs the game restarted; here the
 * subscription belongs to this mod and only the script's half is replaced. See
 * {@link ForgeEventBridge}.
 *
 * <p>Which script type a listener runs in is decided by the thread the event arrives on — the
 * server thread posts to server scripts, the render thread to client scripts. A startup script can
 * listen too, and its listeners run for both, which is the way to catch something that fires before
 * any world has loaded.
 *
 * <p>Forge's generic events — {@code AttachCapabilitiesEvent} and its kind — are listened to the
 * same way, and filtered inside the listener with {@code event.getGenericType()}.
 */
public interface ForgeEvents {

    EventGroup GROUP = EventGroup.of("ForgeEvents");

    /**
     * The id: a Forge event class, named in full.
     *
     * <p>Identity keys, because two names for one class resolve to the same object and a
     * {@code Class} has no cheaper comparison than the one it inherits.
     */
    Extra EVENT_CLASS = new Extra()
        .transformer(ForgeEventBridge::resolve)
        .identity()
        .required()
        .validator(id -> id instanceof Class)
        .display(id -> id instanceof Class<?> type ? type.getName() : String.valueOf(id));

    /** Any event on Forge's game bus. */
    EventHandler ON_EVENT = GROUP.add("onEvent", ScriptTypePredicate.ALL, () -> ForgeEventJS.class)
        .extra(EVENT_CLASS)
        .onListen(ForgeEventBridge::bridgeGameEvent);
}
