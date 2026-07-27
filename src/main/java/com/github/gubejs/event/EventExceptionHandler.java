/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/event/EventExceptionHandler.java
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

import org.jetbrains.annotations.Nullable;

/**
 * Decides what to do about a listener that threw.
 *
 * <p>Given to {@link EventHandler#post} by callers that can do something better than log — a
 * recipe event, say, which can name the recipe being built.
 */
@FunctionalInterface
public interface EventExceptionHandler {

    /**
     * Handles a failure from one listener.
     *
     * @param event the event being posted
     * @param container the listener that threw
     * @param error what it threw
     * @return the error to report, or {@code null} to swallow it and carry on with the next
     *     listener
     */
    @Nullable
    Throwable handle(EventJS event, EventHandlerContainer container, Throwable error);
}
