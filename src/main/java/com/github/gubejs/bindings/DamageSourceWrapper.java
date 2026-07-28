/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/bindings/DamageSourceWrapper.java
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

import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code DamageSource} global, and the conversion that lets a string stand in for one.
 *
 * <pre>{@code
 * entity.attack('cactus', 2)
 * entity.attack(DamageSource.playerAttack(event.player), 6)
 * }</pre>
 *
 * <p>Every kind of damage the game deals without an attacker is a static constant, which is not
 * something a pack has any reason to import — so the name is what a script passes, and this is what
 * turns it into the source. The names are the constants' own, lower-cased:
 * {@code 'in_fire'}, {@code 'lava'}, {@code 'out_of_world'}, {@code 'magic'} and the rest, read off
 * the class rather than listed here so a name the game has is a name this accepts.
 */
public final class DamageSourceWrapper {

    /** The constants by name, built once from {@link DamageSource}'s own fields. */
    @Nullable
    private static Map<String, DamageSource> constants;

    private DamageSourceWrapper() {
    }

    /**
     * Reads a damage source from a constant or from its name.
     *
     * @param value a {@link DamageSource}, its name, or an entity — an entity means "hurt by this",
     *     the way a mob attack does
     * @return the source, {@link DamageSource#GENERIC} when the name is not one the game has
     */
    public static DamageSource of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof DamageSource source) {
            return source;
        } else if (unwrapped instanceof Player player) {
            return DamageSource.playerAttack(player);
        } else if (unwrapped instanceof LivingEntity entity) {
            return DamageSource.mobAttack(entity);
        } else if (unwrapped instanceof Entity entity) {
            // Not living, so nothing is swinging it: an arrow, a snowball, a falling anvil. Thrown
            // damage is the game's own answer for all of those.
            return DamageSource.thrown(entity, null);
        }

        var name = ValueUtils.asString(unwrapped);

        if (name == null || name.isBlank()) {
            return DamageSource.GENERIC;
        }

        var source = constants().get(normalise(name));

        if (source != null) {
            return source;
        }

        ConsoleJS.getCurrent(ConsoleJS.SERVER).warn("There is no damage source called '" + name
            + "'; using generic. The names are " + constants().keySet());
        return DamageSource.GENERIC;
    }

    /**
     * Reports whether a string names a damage source, quietly.
     *
     * <p>Asked by the engine while it works out which overload a call meant, so it says no rather
     * than warning: an id that is not a damage source is usually an argument for another method.
     *
     * @param name the text to test
     * @return {@code true} if {@link #of} would find a source
     */
    public static boolean looksLikeDamageSource(String name) {
        return constants().containsKey(normalise(name));
    }

    /**
     * Returns the damage a player deals by hitting something.
     *
     * @param player who is hitting
     * @return the source
     */
    public static DamageSource playerAttack(Player player) {
        return DamageSource.playerAttack(player);
    }

    /**
     * Returns the damage a mob deals by hitting something.
     *
     * @param entity who is hitting
     * @return the source
     */
    public static DamageSource mobAttack(LivingEntity entity) {
        return DamageSource.mobAttack(entity);
    }

    /**
     * Returns the damage a thrown thing deals.
     *
     * @param projectile the arrow, snowball or other projectile
     * @param thrower who threw it, or {@code null} for a dispenser
     * @return the source
     */
    public static DamageSource thrown(Entity projectile, @Nullable Entity thrower) {
        return DamageSource.thrown(projectile, thrower);
    }

    /**
     * Returns an explosion's damage.
     *
     * @param entity who set it off, or {@code null} for one nobody is credited with
     * @return the source
     */
    public static DamageSource explosion(@Nullable LivingEntity entity) {
        return DamageSource.explosion(entity);
    }

    /**
     * Returns every name {@link #of} accepts.
     *
     * @return the names
     */
    public static java.util.Set<String> getNames() {
        return constants().keySet();
    }

    private static synchronized Map<String, DamageSource> constants() {
        if (constants == null) {
            var found = new LinkedHashMap<String, DamageSource>();

            for (var field : DamageSource.class.getFields()) {
                if (field.getType() == DamageSource.class
                    && java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    try {
                        found.put(normalise(field.getName()), (DamageSource) field.get(null));
                    } catch (IllegalAccessException ignored) {
                        // A constant that cannot be read is one no script can name; the lookup
                        // reports the name as unknown, which is the truth.
                    }
                }
            }

            constants = found;
        }

        return constants;
    }

    /** Strips the punctuation the spellings of one name differ by. */
    private static String normalise(String name) {
        var builder = new StringBuilder(name.length());

        for (var c : name.toCharArray()) {
            if (c >= 'A' && c <= 'Z') {
                builder.append(Character.toLowerCase(c));
            } else if (c >= 'a' && c <= 'z' || c >= '0' && c <= '9') {
                builder.append(c);
            }
        }

        return builder.toString();
    }
}
