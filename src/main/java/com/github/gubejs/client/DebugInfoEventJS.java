/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/DebugInfoEventJS.java
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

import com.github.gubejs.util.ValueUtils;
import java.util.List;

/**
 * One of the two columns of F3 text, while it is being assembled.
 *
 * <pre>{@code
 * ClientEvents.rightDebugInfo(event => {
 *     event.add('Coins: ' + Client.player.persistentData.coins)
 * })
 * }</pre>
 *
 * <p>Fires every frame the debug screen is open, so build the string from something already
 * computed rather than computing it here.
 */
public final class DebugInfoEventJS extends ClientEventJS {

    private final List<String> lines;

    public DebugInfoEventJS(List<String> lines) {
        this.lines = lines;
    }

    /**
     * Returns the lines collected so far.
     *
     * @return the live list, so editing it edits the screen
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Adds a line at the bottom of the column.
     *
     * @param text what to show; {@code null} adds a blank line, which is how vanilla separates
     *     groups
     * @return this event
     */
    public DebugInfoEventJS add(Object text) {
        lines.add(text == null ? "" : String.valueOf(ValueUtils.unwrap(text)));
        return this;
    }
}
