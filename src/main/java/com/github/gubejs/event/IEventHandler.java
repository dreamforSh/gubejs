/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/event/IEventHandler.java
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

/**
 * One registered listener.
 *
 * <p>Usually a JavaScript function, wrapped so that calling it goes through the context lock; but
 * a Java plugin can register one directly, and both look the same from here.
 */
@FunctionalInterface
public interface IEventHandler {

    /**
     * Runs this listener.
     *
     * @param event the event being posted
     * @throws EventExit if the listener called {@code cancel}, {@code success} or {@code exit}
     */
    void onEvent(EventJS event);
}
