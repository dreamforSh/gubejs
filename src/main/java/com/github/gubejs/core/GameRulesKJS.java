/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/core/GameRulesKJS.java
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
package com.github.gubejs.core;

import com.github.gubejs.util.ConsoleJS;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.level.GameRules;
import org.jetbrains.annotations.Nullable;

/**
 * Reading and writing game rules by name — {@code level.gameRules.getBoolean('keepInventory')}.
 *
 * <p>Vanilla keys a rule by a {@link GameRules.Key} constant, which is a field on {@code GameRules}
 * with a name of its own ({@code RULE_KEEPINVENTORY}) that a script has no good way to reach. The
 * name in that constant is the one everybody knows — it is what {@code /gamerule} takes and what a
 * wiki page calls it — so that is what these take.
 *
 * <p>An unknown rule name is reported and answers a default rather than throwing: a pack asking
 * about a rule another mod adds should degrade to "not set", not stop.
 */
public interface GameRulesKJS {

    /** Rule name to its key, built once from the same list {@code /gamerule} completes from. */
    Map<String, GameRules.Key<?>> KEYS = new LinkedHashMap<>();

    /**
     * Returns the rule keys, indexed by the name {@code /gamerule} uses.
     *
     * @return the keys by name
     */
    static Map<String, GameRules.Key<?>> keys() {
        if (KEYS.isEmpty()) {
            GameRules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
                @Override
                public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> key,
                                                                 GameRules.Type<T> type) {
                    KEYS.put(key.getId(), key);
                }
            });
        }

        return KEYS;
    }

    /**
     * Returns every rule name.
     *
     * @return the names, in the order the game declares them
     */
    static List<String> getNames() {
        return List.copyOf(keys().keySet());
    }

    default GameRules gjs$self() {
        return (GameRules) this;
    }

    /**
     * Returns a rule's value as the game stores it.
     *
     * @param rule the rule name, e.g. {@code keepInventory}
     * @return the value, or {@code null} if no rule goes by that name
     */
    @Nullable
    default GameRules.Value<?> get(String rule) {
        var key = keys().get(rule);

        if (key == null) {
            ConsoleJS.SERVER.warn("There is no game rule called '" + rule + "'");
            return null;
        }

        return gjs$self().getRule(cast(key));
    }

    /**
     * Returns a rule's value as the string {@code /gamerule} would print.
     *
     * @param rule the rule name
     * @return the value, or an empty string for an unknown rule
     */
    default String getString(String rule) {
        var value = get(rule);
        return value == null ? "" : value.serialize();
    }

    /**
     * Returns a true/false rule.
     *
     * @param rule the rule name, e.g. {@code keepInventory}
     * @return whether it is on, or {@code false} for an unknown rule or one that is not a flag
     */
    default boolean getBoolean(String rule) {
        return get(rule) instanceof GameRules.BooleanValue value && value.get();
    }

    /**
     * Returns a numeric rule.
     *
     * @param rule the rule name, e.g. {@code randomTickSpeed}
     * @return the number, or {@code 0} for an unknown rule or one that is not a number
     */
    default int getInt(String rule) {
        return get(rule) instanceof GameRules.IntegerValue value ? value.get() : 0;
    }

    /**
     * Sets a rule.
     *
     * <p>Takes a boolean or a number, whichever the rule holds. Writing the wrong kind is reported
     * rather than coerced, because a flag silently becoming {@code false} because a script passed
     * {@code 0} is a bug that takes a long evening to find.
     *
     * @param rule the rule name
     * @param value the new value
     */
    default void set(String rule, Object value) {
        var current = get(rule);
        var unwrapped = com.github.gubejs.util.ValueUtils.unwrap(value);

        if (current instanceof GameRules.BooleanValue flag) {
            if (unwrapped instanceof Boolean bool) {
                flag.set(bool, null);
            } else {
                ConsoleJS.SERVER.error("Game rule '" + rule + "' is true or false, not '"
                    + unwrapped + "'");
            }
        } else if (current instanceof GameRules.IntegerValue number) {
            if (unwrapped instanceof Number n) {
                number.set(n.intValue(), null);
            } else {
                ConsoleJS.SERVER.error("Game rule '" + rule + "' is a number, not '"
                    + unwrapped + "'");
            }
        } else if (current != null) {
            ConsoleJS.SERVER.error("Game rule '" + rule + "' is of a kind this cannot set");
        }
    }

    /**
     * Narrows a key to the value type {@link GameRules#getRule} wants.
     *
     * <p>The map above holds keys of every value type at once, which no signature can express —
     * a {@code Key<BooleanValue>} and a {@code Key<IntegerValue>} have nothing in common but the
     * wildcard, and the wildcard is not what the lookup accepts.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static GameRules.Key cast(GameRules.Key<?> key) {
        return (GameRules.Key) key;
    }
}
