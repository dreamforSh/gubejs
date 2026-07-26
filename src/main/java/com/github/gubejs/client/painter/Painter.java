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
    public synchronized void paint(@Nullable Object value) {
        var tag = NbtHelper.compound(ValueUtils.unwrap(value));

        if (tag == null) {
            return;
        }

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

    /**
     * Removes one object.
     *
     * @param name what it was added under
     */
    public synchronized void remove(String name) {
        objects.remove(name);
    }

    /** Removes everything. */
    public synchronized void clear() {
        objects.clear();
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
