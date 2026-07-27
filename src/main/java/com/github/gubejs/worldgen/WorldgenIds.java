/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
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
package com.github.gubejs.worldgen;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Numbers the generated files that have nothing to be named after.
 *
 * <p>An ore is named after its block; adding a feature that already exists to a few more biomes
 * has no such name, and two of those in one pack must not overwrite each other's file.
 *
 * <p>Reset on every startup, so the same scripts produce the same filenames — a generated file
 * whose name changed between launches would leave the previous one behind.
 */
final class WorldgenIds {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private WorldgenIds() {
    }

    static int next() {
        return COUNTER.getAndIncrement();
    }

    static void reset() {
        COUNTER.set(0);
    }
}
