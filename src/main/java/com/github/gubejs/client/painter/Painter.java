/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/painter/Painter.java
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
package com.github.gubejs.client.painter;

import com.github.gubejs.util.NbtHelper;
import com.github.gubejs.util.ValueUtils;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * What this client is drawing over the game, by name.
 *
 * <p>Named rather than a list, because a pack updates one element at a time: a health bar redrawn
 * every tick would otherwise mean sending, and clearing, everything else on the screen with it.
 * Sending {@code { hp: { ... } }} replaces just that one, and sending {@code { hp: null }} takes
 * it away.
 *
 * <p>Client-side state with no server counterpart. A server script's {@code player.paint} sends a
 * description; this is where it lands, and the server does not keep a copy — which is what makes a
 * player disconnecting and rejoining start from an empty screen rather than a stale one.
 */
public final class Painter {

    /** The one this client draws from. */
    public static final Painter INSTANCE = new Painter();

    private final Map<String, PaintObject> objects = new LinkedHashMap<>();

    private Painter() {
    }

    /**
     * Adds or replaces objects, and removes the ones given as null.
     *
     * @param value an object whose keys are names and whose values are descriptions
     */
    public void paint(@Nullable Object value) {
        var tag = NbtHelper.compound(ValueUtils.unwrap(value));

        if (tag == null) {
            return;
        }

        synchronized (this) {
            for (var name : tag.getAllKeys()) {
                var description = tag.get(name);

                // An empty compound is how a script says "remove this" without reaching for null,
                // which is awkward to write inside an object literal.
                if (description == null || !(description instanceof CompoundTag compound)
                    || compound.isEmpty()) {
                    objects.remove(name);
                } else {
                    objects.put(name, new PaintObject(compound));
                }
            }
        }

        announce();
    }

    /**
     * Removes one object.
     *
     * @param name what it was added under
     */
    public void remove(String name) {
        synchronized (this) {
            objects.remove(name);
        }

        announce();
    }

    /** Removes everything. */
    public void clear() {
        synchronized (this) {
            objects.clear();
        }

        announce();
    }

    /**
     * Tells the client scripts that what is on screen has changed.
     *
     * <p>Outside the lock, deliberately: a listener is free to call straight back into
     * {@link #paint}, and holding the monitor across a call into script would be a deadlock
     * waiting for the day someone does.
     */
    private void announce() {
        var handler = com.github.gubejs.bindings.event.ClientEvents.PAINTER_UPDATED;

        if (handler.hasListeners()) {
            handler.post(com.github.gubejs.script.ScriptType.CLIENT,
                new com.github.gubejs.client.ClientEventJS());
        }
    }

    /**
     * Returns the names of everything currently being drawn.
     *
     * @return the names, in the order they were added
     */
    public synchronized List<String> getNames() {
        return new ArrayList<>(objects.keySet());
    }

    /**
     * Reports whether anything is being drawn.
     *
     * @return {@code true} if the screen is clear
     */
    public synchronized boolean isEmpty() {
        return objects.isEmpty();
    }

    /**
     * Draws everything.
     *
     * <p>Over a copy of the values, because a client script may add or remove an object from a
     * tick listener while this is running — the render thread and the client tick are the same
     * thread, but a scheduled callback is not obliged to be.
     *
     * @param pose the transform stack the overlay is being drawn with
     * @param screenWidth the width of the screen in GUI pixels
     * @param screenHeight the height of the screen in GUI pixels
     */
    public void draw(PoseStack pose, int screenWidth, int screenHeight) {
        List<PaintObject> snapshot;

        synchronized (this) {
            if (objects.isEmpty()) {
                return;
            }

            snapshot = new ArrayList<>(objects.values());
        }

        for (var object : snapshot) {
            if (object.isVisible()) {
                object.draw(pose, screenWidth, screenHeight);
            }
        }
    }

    /**
     * Applies a description that arrived from the server.
     *
     * @param data the payload of the internal paint message
     */
    public void receive(CompoundTag data) {
        if (data.getBoolean("clear")) {
            clear();
        }

        if (data.contains("objects", Tag.TAG_COMPOUND)) {
            paint(data.getCompound("objects"));
        }
    }
}
