package com.github.gubejs.block.entity;

import com.github.gubejs.event.IEventHandler;
import org.jetbrains.annotations.Nullable;

/**
 * What a script asked a block to remember, and to do on its own.
 *
 * <p>A block state can hold a handful of enumerated properties and nothing else. Anything a block
 * needs to keep — items, a counter, a name — needs a block entity, and a block entity is the one
 * piece of block behaviour that cannot be expressed as data.
 *
 * <pre>{@code
 * event.create('smelter').blockEntity(be => {
 *     be.inventorySize = 9
 *     be.serverTick(20, 0, entity => {
 *         entity.data.putInt('ticks', entity.data.getInt('ticks') + 1)
 *     })
 * })
 * }</pre>
 */
public class BlockEntityBuilder {

    /** How many item slots the block has, or {@code 0} for none. */
    public int inventorySize;

    /** How often the tick callback runs, in ticks. */
    public int tickInterval = 20;

    /** Which tick within the interval it runs on, so several blocks can be spread out. */
    public int tickOffset;

    /** What to run on the server every {@link #tickInterval} ticks. */
    @Nullable
    public IEventHandler serverTick;

    /** Whether the data should be sent to clients as it changes. */
    public boolean synced;

    /**
     * Gives the block an inventory.
     *
     * <p>Exposed as a Forge item handler, so hoppers, pipes and anything else that moves items
     * can reach it. There is no screen — opening one needs a menu and a client-side screen, and
     * neither is something a script can currently ask for.
     *
     * @param size how many slots
     */
    public void setInventorySize(int size) {
        inventorySize = Math.max(0, size);
    }

    /**
     * Sets how often the tick callback runs.
     *
     * @param interval the number of ticks between runs
     */
    public void setTickInterval(int interval) {
        tickInterval = Math.max(1, interval);
    }

    /**
     * Runs a function on the server, on an interval.
     *
     * <p>Every block entity of this type ticks, so this runs once per placed block per interval.
     * The offset is what keeps a hundred of them from all running on the same tick.
     *
     * @param interval how many ticks between runs
     * @param offset which tick within the interval to run on
     * @param callback what to run
     */
    public void serverTick(int interval, int offset, IEventHandler callback) {
        setTickInterval(interval);
        this.tickOffset = offset;
        this.serverTick = callback;
    }

    /**
     * Sends the block's data to clients whenever it changes.
     *
     * <p>Off by default: a block entity ticking twenty times a second and syncing each time is a
     * packet per tick per block, which is how a pack makes a server unplayable without noticing.
     *
     * @param synced whether to sync
     */
    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    /**
     * Reports whether this block entity needs a ticker at all.
     *
     * @return {@code true} if a script gave it something to do
     */
    public boolean ticks() {
        return serverTick != null;
    }
}
