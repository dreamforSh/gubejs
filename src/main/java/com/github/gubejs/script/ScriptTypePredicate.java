/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/script/ScriptTypePredicate.java
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
package com.github.gubejs.script;

import java.util.List;

/**
 * Decides which script types an event may be listened to from.
 *
 * <p>Every event handler carries one. {@link ScriptType} implements it as "only me", and the
 * constants here cover the combinations events actually use.
 */
public interface ScriptTypePredicate {

    /** Server and client, but not startup. */
    ScriptTypePredicate COMMON = of(ScriptType.SERVER, ScriptType.CLIENT);

    /** Startup and server. */
    ScriptTypePredicate STARTUP_OR_SERVER = of(ScriptType.STARTUP, ScriptType.SERVER);

    /** Startup and client. */
    ScriptTypePredicate STARTUP_OR_CLIENT = of(ScriptType.STARTUP, ScriptType.CLIENT);

    /** Every script type. */
    ScriptTypePredicate ALL = of(ScriptType.VALUES);

    /**
     * Reports whether {@code type} may listen.
     *
     * @param type the script type asking
     * @return {@code true} if listening is allowed
     */
    boolean test(ScriptType type);

    /**
     * Returns the types this predicate accepts, for error messages.
     *
     * @return the accepted types
     */
    List<ScriptType> getValidTypes();

    /**
     * Returns a predicate accepting everything this one rejects.
     *
     * @return the complement of this predicate
     */
    ScriptTypePredicate negate();

    /**
     * Builds a predicate accepting exactly the given types.
     *
     * @param types the types to accept
     * @return a predicate over them
     */
    static ScriptTypePredicate of(ScriptType... types) {
        var list = List.of(types);

        return new ScriptTypePredicate() {
            @Override
            public boolean test(ScriptType type) {
                return list.contains(type);
            }

            @Override
            public List<ScriptType> getValidTypes() {
                return list;
            }

            @Override
            public ScriptTypePredicate negate() {
                var rest = new java.util.ArrayList<ScriptType>(ScriptType.VALUES.length);

                for (var type : ScriptType.VALUES) {
                    if (!list.contains(type)) {
                        rest.add(type);
                    }
                }

                return of(rest.toArray(new ScriptType[0]));
            }

            @Override
            public String toString() {
                return list.toString();
            }
        };
    }
}
