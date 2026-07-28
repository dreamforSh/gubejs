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
package com.github.gubejs.item;

import com.github.gubejs.event.EventExit;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.function.Function;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * The behaviour a script gave an item, beyond what its properties can say.
 *
 * <pre>{@code
 * event.create('wand')
 *     .glow(true)
 *     .use(event => {
 *         if (event.server) {
 *             event.player.tell('zap')
 *         }
 *         return true
 *     })
 *     .hurtEnemy(event => event.target.setSecondsOnFire(4))
 * }</pre>
 *
 * <p>Held on {@link ItemModifications}, which every item already has somewhere to keep — so the same
 * callbacks work on an item a script created and on one that already existed, and neither needs an
 * {@code Item} subclass of its own. That is what makes them available on a tool, a piece of armour
 * and a music disc as well: those build their own item classes, and a mechanism that lived in one
 * class would have stopped at the plain type.
 *
 * <p>The caveat that comes with it is the same one the block callbacks carry: each is called from
 * the game's own method on {@code Item}, so an item class that overrides that method without
 * calling {@code super} decides for itself. Every vanilla tool does exactly that for
 * {@code hurtEnemy}, which is why an item that wants to hurt on hit should be created as a plain
 * item with an attack attribute rather than as a {@code sword}.
 *
 * <p>A callback that throws is reported once and then dropped: these run inside the game's own item
 * methods, and one failing on every swing would fill the log faster than anyone could read it.
 */
public final class ItemCallbacks {

    /** Asked when the item is right-clicked in the air; answers whether it did something. */
    @Nullable
    public Function<ItemCallbackEventJS, Object> use;

    /** Asked when a hold finishes; answers with the stack to leave in the hand. */
    @Nullable
    public Function<ItemCallbackEventJS, Object> finishUsing;

    /** Called when a hold is let go early. */
    @Nullable
    public Function<ItemCallbackEventJS, Object> releaseUsing;

    /** Called when the item hits an entity; answers whether to damage it as usual. */
    @Nullable
    public Function<ItemCallbackEventJS, Object> hurtEnemy;

    /**
     * Which script type wrote these, so calling one enters the right context.
     *
     * <p>GraalJS refuses to let two threads be inside one context at once, and these run on
     * whichever thread the game is using — the server thread for a swing, the render thread for a
     * client-side prediction. Going through the manager takes the lock that turns that into a wait.
     */
    @Nullable
    private ScriptType owner;

    /** Whether nothing at all was set, so an item with none can skip the lookups. */
    public boolean isEmpty() {
        return use == null && finishUsing == null && releaseUsing == null && hurtEnemy == null;
    }

    // --- what a script sets ----------------------------------------------------------------------

    /**
     * Runs a callback when the item is right-clicked with nothing under the cursor.
     *
     * <p>Returning {@code true} tells the game the item did something, which is what plays the swing
     * animation and stops the click passing on. Returning nothing leaves the item's own behaviour,
     * so a food is still eaten.
     *
     * <p>Fires on both sides. {@code event.server} is how a callback that gives an item or spawns
     * something tells them apart; doing it on both leaves the client showing something the server
     * never agreed to.
     *
     * @param callback what to run
     */
    public void setUse(@Nullable Function<ItemCallbackEventJS, Object> callback) {
        use = callback;
        claim();
    }

    /**
     * Runs a callback when a hold finishes — the moment a bow is loosed or a potion is drunk.
     *
     * <p>Only reached by an item that has a use duration and a use animation; a plain item is never
     * held. Returning an item replaces what stays in the hand, which is how a bottle becomes an
     * empty one.
     *
     * @param callback what to run
     */
    public void setFinishUsing(@Nullable Function<ItemCallbackEventJS, Object> callback) {
        finishUsing = callback;
        claim();
    }

    /**
     * Runs a callback when a hold is let go before it finished.
     *
     * <p>{@code event.timeLeft} is what a bow's draw strength is worked out from.
     *
     * @param callback what to run
     */
    public void setReleaseUsing(@Nullable Function<ItemCallbackEventJS, Object> callback) {
        releaseUsing = callback;
        claim();
    }

    /**
     * Runs a callback when the item is used to hit something.
     *
     * <p>{@code event.target} is what was hit and {@code event.entity} is who swung. Returning
     * {@code false} stops the item taking the durability damage a weapon normally takes.
     *
     * @param callback what to run
     */
    public void setHurtEnemy(@Nullable Function<ItemCallbackEventJS, Object> callback) {
        hurtEnemy = callback;
        claim();
    }

    /** Remembers which script type is registering, while it is still running. */
    private void claim() {
        var current = ScriptType.getCurrent();

        if (current != null) {
            owner = current;
        }
    }

    // --- what the game calls ---------------------------------------------------------------------

    /**
     * Asks the use callback.
     *
     * @param item the stack in hand
     * @param level the level
     * @param player who clicked
     * @param hand which hand
     * @return {@code true} if the callback said the item did something, {@code false} if it said
     *     nothing at all
     */
    public boolean onUse(ItemStack item, Level level, Player player, InteractionHand hand) {
        var answer = ask(use, new ItemCallbackEventJS(item, level, player, hand, null, 0), "use");

        if (answer == null) {
            return false;
        }

        return !(answer instanceof Boolean decided) || decided;
    }

    /**
     * Asks the finish-using callback.
     *
     * @param item the stack in hand
     * @param level the level
     * @param entity who was holding it
     * @return the stack to leave in the hand, or {@code null} to leave the item's own answer
     */
    @Nullable
    public ItemStack onFinishUsing(ItemStack item, Level level, LivingEntity entity) {
        var answer = ask(finishUsing,
            new ItemCallbackEventJS(item, level, entity, null, null, 0), "finishUsing");

        if (answer == null) {
            return null;
        }

        var stack = ItemStackJS.of(answer);
        return stack.isEmpty() ? ItemStack.EMPTY : stack;
    }

    /**
     * Calls the release-using callback.
     *
     * @param item the stack in hand
     * @param level the level
     * @param entity who was holding it
     * @param timeLeft how much of the hold was left
     * @return whether a callback ran, so the caller can skip the item's own behaviour
     */
    public boolean onReleaseUsing(ItemStack item, Level level, LivingEntity entity, int timeLeft) {
        return releaseUsing != null && ask(releaseUsing,
            new ItemCallbackEventJS(item, level, entity, null, null, timeLeft),
            "releaseUsing") != null;
    }

    /**
     * Asks the hurt-enemy callback.
     *
     * @param item the weapon
     * @param target what was hit
     * @param attacker who swung
     * @return {@code true} to take durability as usual, {@code false} if the callback refused, and
     *     {@code null} when there is no callback
     */
    @Nullable
    public Boolean onHurtEnemy(ItemStack item, LivingEntity target, LivingEntity attacker) {
        var answer = ask(hurtEnemy, new ItemCallbackEventJS(item, attacker.level, attacker, null,
            target, 0), "hurtEnemy");

        if (answer == null) {
            return null;
        }

        return !(answer instanceof Boolean decided) || decided;
    }

    /**
     * Calls one callback inside its own script context, dropping it if it fails.
     *
     * @return what the callback returned, unwrapped, or {@code null} when there was no callback or
     *     it failed
     */
    @Nullable
    private Object ask(@Nullable Function<ItemCallbackEventJS, Object> callback,
                       ItemCallbackEventJS event, String name) {
        if (callback == null) {
            return null;
        }

        try {
            var manager = owner == null ? null : owner.getManager();
            var answer = manager == null ? callback.apply(event)
                : manager.inContext(() -> callback.apply(event));

            // Unwrapped rather than handed on as a guest value: the callers compare it to a Boolean
            // and pass it to ItemStackJS, and neither knows anything about Graal.
            return ValueUtils.unwrap(answer);
        } catch (Throwable ex) {
            // event.cancel() is a script saying "stop here", which it already did; only a real
            // failure is worth reporting, and only a real failure costs the callback its place.
            if (EventExit.unwrap(ex) == null) {
                ConsoleJS.getCurrent(ConsoleJS.SERVER).handleError(ex,
                    "Error in an item's " + name + " callback; it has been removed");
                drop(callback);
            }

            return null;
        }
    }

    /** Forgets whichever callback just failed. */
    private void drop(Function<ItemCallbackEventJS, Object> callback) {
        if (use == callback) {
            use = null;
        }

        if (finishUsing == callback) {
            finishUsing = null;
        }

        if (releaseUsing == callback) {
            releaseUsing = null;
        }

        if (hurtEnemy == callback) {
            hurtEnemy = null;
        }
    }
}
