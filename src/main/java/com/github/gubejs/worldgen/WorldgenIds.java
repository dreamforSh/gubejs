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
