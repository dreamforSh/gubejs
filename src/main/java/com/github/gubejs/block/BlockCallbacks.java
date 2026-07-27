/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/block/KubeJSBlockProperties.java
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
package com.github.gubejs.block;

import com.github.gubejs.event.EventExit;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.function.Consumer;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The behaviour a script gave a block, beyond what its properties can say.
 *
 * <pre>{@code
 * event.create('cursed_stone')
 *     .randomTick(event => event.block.up.set('minecraft:fire'))
 *     .steppedOn(event => event.entity.setSecondsOnFire(4))
 * }</pre>
 *
 * <p>Held by the block itself, through the mixin that gives every block somewhere to keep one, so
 * the same callbacks can be attached to a block a script created and to one that already existed —
 * {@code BlockEvents.modification} sets them the same way.
 *
 * <p>Each is called from the game's own method on the block, which means the usual caveat about
 * subclasses: a block whose class overrides {@code randomTick} without calling {@code super}
 * decides for itself, and nothing here is reached. That is every crop and sapling in the game, and
 * no block a script creates unless it asked for the {@code crop} type.
 *
 * <p>A callback that throws is reported once and then dropped. These run inside the game's own
 * block methods — a random tick is thousands of calls a second across a loaded world — and a
 * listener failing every time would fill the log faster than anyone could read it.
 */
public final class BlockCallbacks {

    /** Called on a random tick, if the block is randomly ticking. */
    @Nullable
    public Consumer<BlockCallbackEventJS> randomTick;

    /** Called when an entity walks over the block. */
    @Nullable
    public Consumer<BlockCallbackEventJS> steppedOn;

    /** Called when an entity lands on the block. */
    @Nullable
    public Consumer<BlockCallbackEventJS> fallenOn;

    /** Asked whether the block can be built over; answers with a boolean. */
    @Nullable
    public Function<BlockCallbackEventJS, Object> canBeReplaced;

    /**
     * Which script type wrote these, so calling one enters the right context.
     *
     * <p>Necessary rather than tidy. A callback is a JavaScript function, and GraalJS refuses to
     * let two threads be inside one context at once — these run on the server thread while a client
     * script may be running on the render thread, both in the startup context that registered them.
     * Going through the manager takes the lock that turns that into a wait.
     */
    @Nullable
    private com.github.gubejs.script.ScriptType owner;

    // --- what a script sets ----------------------------------------------------------------------

    /**
     * Runs a callback on every random tick.
     *
     * <p>Random ticking is not free and is off for most blocks, so setting this also turns it on —
     * for a block a script created, through its properties; for one that already existed, by
     * changing the flag on each of its states.
     *
     * <p>How often it fires is the game's business: three blocks per section per tick, chosen at
     * random, which is roughly once every 47 seconds for any given block at the default rate.
     *
     * @param callback what to run
     */
    public void setRandomTick(@Nullable Consumer<BlockCallbackEventJS> callback) {
        randomTick = callback;
        claim();
    }

    /**
     * Runs a callback when an entity walks over the block.
     *
     * <p>Every tick the entity is on it, not once on arrival — magma blocks burn this way.
     *
     * @param callback what to run
     */
    public void setSteppedOn(@Nullable Consumer<BlockCallbackEventJS> callback) {
        steppedOn = callback;
        claim();
    }

    /**
     * Runs a callback when an entity lands on the block.
     *
     * <p>{@code event.fallDistance} is how far it fell. Fall damage is dealt by the game's own
     * method afterwards, so a callback wanting to prevent it sets the entity's fall distance to
     * zero.
     *
     * @param callback what to run
     */
    public void setFallenOn(@Nullable Consumer<BlockCallbackEventJS> callback) {
        fallenOn = callback;
        claim();
    }

    /**
     * Decides whether the block can be built over, the way grass and water can.
     *
     * <p>The callback returns {@code true} or {@code false}; returning nothing leaves the block's
     * own answer, which comes from its material.
     *
     * @param callback what to ask
     */
    public void setCanBeReplaced(@Nullable Function<BlockCallbackEventJS, Object> callback) {
        canBeReplaced = callback;
        claim();
    }

    /** Remembers which script type is registering, while it is still running. */
    private void claim() {
        var current = com.github.gubejs.script.ScriptType.getCurrent();

        if (current != null) {
            owner = current;
        }
    }

    /** Whether anything at all was set, so a block with none can skip the work of attaching them. */
    public boolean isEmpty() {
        return randomTick == null && steppedOn == null && fallenOn == null && canBeReplaced == null;
    }

    /** Whether this block wants random ticks, which decides a property rather than a callback. */
    public boolean wantsRandomTicks() {
        return randomTick != null;
    }

    // --- what the game calls ---------------------------------------------------------------------

    /**
     * Runs the random-tick callback.
     *
     * @param level the level the block is in
     * @param pos where the block is
     * @param state the block's state
     */
    public void onRandomTick(Level level, BlockPos pos, BlockState state) {
        if (randomTick != null) {
            randomTick = run(randomTick, new BlockCallbackEventJS(level, pos, state), "randomTick");
        }
    }

    /**
     * Runs the stepped-on callback.
     *
     * @param level the level the block is in
     * @param pos where the block is
     * @param state the block's state
     * @param entity what walked over it
     */
    public void onSteppedOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (steppedOn != null) {
            steppedOn = run(steppedOn,
                new BlockCallbackEventJS(level, pos, state, entity, 0F), "steppedOn");
        }
    }

    /**
     * Runs the fallen-on callback.
     *
     * @param level the level the block is in
     * @param pos where the block is
     * @param state the block's state
     * @param entity what landed on it
     * @param fallDistance how far it fell
     */
    public void onFallenOn(Level level, BlockPos pos, BlockState state, Entity entity,
                           float fallDistance) {
        if (fallenOn != null) {
            fallenOn = run(fallenOn,
                new BlockCallbackEventJS(level, pos, state, entity, fallDistance), "fallenOn");
        }
    }

    /**
     * Asks the can-be-replaced callback.
     *
     * @param level the level the block is in
     * @param pos where the block is
     * @param state the block's state
     * @param entity who is placing, or {@code null}
     * @param fallback the block's own answer
     * @return what the callback said, or {@code fallback} if it said nothing
     */
    public boolean onCanBeReplaced(Level level, BlockPos pos, BlockState state,
                                   @Nullable Entity entity, boolean fallback) {
        if (canBeReplaced == null) {
            return fallback;
        }

        var event = new BlockCallbackEventJS(level, pos, state, entity, 0F);
        Object answer;

        try {
            answer = ValueUtils.unwrap(enter(() -> canBeReplaced.apply(event)));
        } catch (Throwable ex) {
            canBeReplaced = null;
            report(ex, "canBeReplaced");
            return fallback;
        }

        return answer instanceof Boolean decided ? decided : fallback;
    }

    /**
     * Calls one callback, returning it if it survived and {@code null} if it is to be dropped.
     *
     * <p>Returning the callback rather than mutating a field is what lets the caller assign the
     * result straight back — the field being cleared is the whole of "dropped".
     */
    @Nullable
    private Consumer<BlockCallbackEventJS> run(Consumer<BlockCallbackEventJS> callback,
                                               BlockCallbackEventJS event, String name) {
        try {
            enter(() -> {
                callback.accept(event);
                return null;
            });
            return callback;
        } catch (Throwable ex) {
            // event.cancel() means "stop here", which has already happened; only a real failure is
            // worth reporting, and only a real failure costs the callback its place.
            if (EventExit.unwrap(ex) != null) {
                return callback;
            }

            report(ex, name);
            return null;
        }
    }

    /**
     * Runs {@code body} inside the context the callback belongs to.
     *
     * <p>Straight through when the script type is unknown, which only happens for a callback a Java
     * plugin set — one that is an ordinary Java lambda and has no context to enter.
     */
    @Nullable
    private <T> T enter(java.util.function.Supplier<T> body) {
        var manager = owner == null ? null : owner.getManager();
        return manager == null ? body.get() : manager.inContext(body);
    }

    private static void report(Throwable ex, String name) {
        ConsoleJS.getCurrent(ConsoleJS.SERVER).handleError(ex,
            "Error in a block's " + name + " callback; it has been removed");
    }
}
