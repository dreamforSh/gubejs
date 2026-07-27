/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * forge/src/main/java/dev/latvian/mods/kubejs/forge/ForgeEventWrapper.java
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
package com.github.gubejs.event;

import net.minecraftforge.eventbus.api.Event;

/**
 * Carries a Forge event to the listeners a script registered for its class.
 *
 * <p>The only {@link EventJS} that does not hand itself to the listener. A script listening to
 * {@code LivingDeathEvent} wants Forge's {@code LivingDeathEvent} — with {@code getEntity()},
 * {@code getSource()}, {@code setCanceled(...)} and everything else Forge documents — and there is
 * no version of this mod re-declaring those that would be as useful as the real thing.
 *
 * <p>So this exists only to satisfy {@link EventHandler#post}, which needs an {@code EventJS}, and
 * {@link #gjs$scriptValue()} unwraps it again on the way to the listener. Which also means the
 * three interruption methods on {@link EventJS} are not what a listener uses here: a Forge event is
 * cancelled by calling {@code event.setCanceled(true)} on it, the way every Forge mod does.
 */
public final class ForgeEventJS extends EventJS {

    /** The event Forge fired. */
    public final Event event;

    public ForgeEventJS(Event event) {
        this.event = event;
    }

    @Override
    public Object gjs$scriptValue() {
        return event;
    }

    @Override
    public String toString() {
        return event.getClass().getName();
    }
}
