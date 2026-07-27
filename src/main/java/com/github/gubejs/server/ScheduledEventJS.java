/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/server/ScheduledEvent.java
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * A callback that came due — what {@code server.scheduleInTicks} hands its function.
 *
 * <pre>{@code
 * server.scheduleInTicks(100, event => {
 *     event.server.tell('a hundred ticks later')
 *     event.reschedule()   // and again in another hundred
 * })
 * }</pre>
 */
public final class ScheduledEventJS extends EventJS implements ScriptTypeHolder {

    private final ScheduledEvents.Entry entry;

    private final MinecraftServer server;

    ScheduledEventJS(ScheduledEvents.Entry entry, MinecraftServer server) {
        this.entry = entry;
        this.server = server;
    }

    /**
     * Returns the server the timer belongs to.
     *
     * @return the server
     */
    public MinecraftServer getServer() {
        return server;
    }

    /**
     * Returns the level the timer was scheduled from.
     *
     * @return the level, or the overworld when it was scheduled without one
     */
    public ServerLevel getLevel() {
        return entry.level == null ? server.overworld() : entry.level;
    }

    /**
     * Returns how long the timer was set for, in ticks.
     *
     * @return the delay
     */
    public long getTimer() {
        return entry.delay;
    }

    /**
     * Returns whatever was passed alongside the callback.
     *
     * @return the data, or {@code null} if none was given
     */
    @Nullable
    public Object getData() {
        return entry.data;
    }

    /**
     * Schedules the same callback again, for the same delay.
     *
     * <p>The way to write a repeating timer that can stop itself: call this at the end of the run
     * that should be followed by another, and simply do not call it on the last one.
     */
    public void reschedule() {
        reschedule(entry.delay);
    }

    /**
     * Schedules the same callback again, for a different delay.
     *
     * @param ticks how many ticks to wait this time
     */
    public void reschedule(long ticks) {
        ScheduledEvents.schedule(entry.withDelay(ticks));
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.SERVER;
    }
}
