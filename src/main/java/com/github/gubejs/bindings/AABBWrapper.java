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
package com.github.gubejs.bindings;

import com.github.gubejs.util.ValueUtils;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code AABB} global: building the boxes an area check needs.
 *
 * <p>The class itself is bound too, so {@code new AABB(...)} works; these are the spellings that
 * read better in a script and the ones KubeJS packs use.
 */
public final class AABBWrapper {

    private AABBWrapper() {
    }

    /**
     * Builds a box between two corners.
     *
     * @param x1 the first corner's x
     * @param y1 the first corner's y
     * @param z1 the first corner's z
     * @param x2 the second corner's x
     * @param y2 the second corner's y
     * @param z2 the second corner's z
     * @return the box
     */
    public static AABB of(double x1, double y1, double z1, double x2, double y2, double z2) {
        return new AABB(x1, y1, z1, x2, y2, z2);
    }

    /**
     * Builds a box from whatever a script passed.
     *
     * @param value a six-number array, a two-corner array, or an {@link AABB} already
     * @return the box, or a zero-sized one at the origin if the value is not a box
     */
    public static AABB of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof AABB box) {
            return box;
        } else if (unwrapped instanceof List<?> list && list.size() == 6) {
            return new AABB(number(list.get(0)), number(list.get(1)), number(list.get(2)),
                number(list.get(3)), number(list.get(4)), number(list.get(5)));
        }

        return new AABB(0, 0, 0, 0, 0, 0);
    }

    /**
     * Builds a box centred on a point.
     *
     * <p>{@code AABB.ofSize(pos, 8, 4, 8)} is the shape of an "everything within eight blocks"
     * check, which is what most area lookups in a script are.
     *
     * @param center where the middle is
     * @param x how far out along x
     * @param y how far out along y
     * @param z how far out along z
     * @return the box
     */
    public static AABB ofSize(Vec3 center, double x, double y, double z) {
        return new AABB(center.x - x, center.y - y, center.z - z,
            center.x + x, center.y + y, center.z + z);
    }

    /**
     * Builds the box one block fills.
     *
     * @param pos the block
     * @return the box
     */
    public static AABB ofBlock(BlockPos pos) {
        return new AABB(pos);
    }

    /**
     * Builds a cube centred on a point.
     *
     * @param center where the middle is
     * @param radius how far out in every direction
     * @return the box
     */
    public static AABB ofRadius(Vec3 center, double radius) {
        return ofSize(center, radius, radius, radius);
    }

    private static double number(@Nullable Object value) {
        return value instanceof Number n ? n.doubleValue() : 0;
    }
}
