/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/event/EventResult.java
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
 * What a posted event decided, and the value it decided it with.
 *
 * <p>Three outcomes beyond "nothing happened": a listener said no ({@link Type#INTERRUPT_FALSE},
 * which is what {@code event.cancel()} produces), said yes ({@link Type#INTERRUPT_TRUE}, from
 * {@code event.success()}), or said "stop here but let the game decide"
 * ({@link Type#INTERRUPT_DEFAULT}, from {@code event.exit()}).
 */
public final class EventResult {

    /** The outcomes a listener can produce. */
    public enum Type {

        /** A listener threw. The event is abandoned and the failure logged. */
        ERROR,

        /** No listener had an opinion. */
        PASS,

        /** Stop, and let vanilla behaviour stand. */
        INTERRUPT_DEFAULT,

        /** Stop, and treat the outcome as a refusal. */
        INTERRUPT_FALSE,

        /** Stop, and treat the outcome as an approval. */
        INTERRUPT_TRUE;

        private final EventResult defaultResult;

        private final EventExit defaultExit;

        Type() {
            this.defaultResult = new EventResult(this, null);
            this.defaultExit = new EventExit(this.defaultResult);
        }

        /**
         * Returns the exception that unwinds a listener with this outcome.
         *
         * @param value the value carried out, or {@code null}
         * @return a shared exit for {@code null}, a fresh one otherwise
         */
        public EventExit exit(@Nullable Object value) {
            return value == null ? defaultExit : new EventExit(new EventResult(this, value));
        }

        /**
         * Returns this outcome with no value attached.
         *
         * @return the shared valueless result
         */
        public EventResult result() {
            return defaultResult;
        }
    }

    /** The result of an event nobody interrupted. */
    public static final EventResult PASS = Type.PASS.result();

    private final Type type;

    private final Object value;

    private EventResult(Type type, @Nullable Object value) {
        this.type = type;
        this.value = value;
    }

    public Type type() {
        return type;
    }

    /**
     * Returns the value the listener passed to {@code cancel}, {@code success} or {@code exit}.
     *
     * @return the value, or {@code null} if there was none
     */
    @Nullable
    public Object value() {
        return value;
    }

    /** Whether any listener interrupted, whatever it decided. */
    public boolean override() {
        return type != Type.PASS;
    }

    public boolean pass() {
        return type == Type.PASS;
    }

    public boolean error() {
        return type == Type.ERROR;
    }

    public boolean interruptDefault() {
        return type == Type.INTERRUPT_DEFAULT;
    }

    /** Whether the event was cancelled, i.e. {@code event.cancel()} ran. */
    public boolean interruptFalse() {
        return type == Type.INTERRUPT_FALSE;
    }

    /** Whether the event was approved, i.e. {@code event.success()} ran. */
    public boolean interruptTrue() {
        return type == Type.INTERRUPT_TRUE;
    }

    /**
     * Maps this result onto Forge's cancel/allow/default triple.
     *
     * @return the matching Forge event result
     */
    public net.minecraftforge.eventbus.api.Event.Result forge() {
        return switch (type) {
            case INTERRUPT_FALSE -> net.minecraftforge.eventbus.api.Event.Result.DENY;
            case INTERRUPT_TRUE -> net.minecraftforge.eventbus.api.Event.Result.ALLOW;
            default -> net.minecraftforge.eventbus.api.Event.Result.DEFAULT;
        };
    }

    @Override
    public String toString() {
        return value == null ? type.name() : type.name() + "(" + value + ")";
    }
}
