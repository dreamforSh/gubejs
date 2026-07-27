package com.github.gubejs.server;

import com.github.gubejs.event.EventExit;
import com.github.gubejs.event.IEventHandler;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.ConsoleJS;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.Nullable;

/**
 * The timers {@code server.scheduleInTicks} creates, and the tick that runs them.
 *
 * <h2>Why ticks and not a thread</h2>
 *
 * <p>A callback scheduled from a script has to run on the server thread: it will touch the world,
 * and the world is not thread-safe. So the queue is drained from the end of the server tick rather
 * than from a timer thread, which also makes the delay mean what a pack author expects — game
 * ticks, stopping while the game is paused.
 *
 * <h2>Ordering</h2>
 *
 * <p>A priority queue keyed on the tick each callback comes due, so a tick with nothing scheduled
 * costs one comparison against the head. Ties run in the order they were scheduled, which is what
 * a pack registering several timers in one script expects.
 */
public final class ScheduledEvents {

    /** One scheduled callback. */
    public static final class Entry implements Comparable<Entry> {

        final long delay;

        final IEventHandler callback;

        @Nullable
        final Object data;

        @Nullable
        final ServerLevel level;

        /**
         * Which script type scheduled this, or {@code null} if a Java plugin did.
         *
         * <p>What decides whether a reload drops it. A callback belongs to the context that was
         * running when it was scheduled, and only that context's reload can take it away — a
         * startup script's timer outlives every server reload, because the startup context is
         * never closed and the script that would re-create the timer never runs again.
         */
        @Nullable
        final ScriptType owner;

        /** The tick this comes due on, filled in when it enters the queue. */
        long dueAt;

        /** Breaks ties between callbacks due on the same tick. */
        long sequence;

        Entry(long delay, IEventHandler callback, @Nullable Object data,
              @Nullable ServerLevel level) {
            this(delay, callback, data, level, ScriptType.getCurrent());
        }

        private Entry(long delay, IEventHandler callback, @Nullable Object data,
                      @Nullable ServerLevel level, @Nullable ScriptType owner) {
            this.delay = Math.max(1L, delay);
            this.callback = callback;
            this.data = data;
            this.level = level;
            this.owner = owner;
        }

        Entry withDelay(long newDelay) {
            // The owner is carried over rather than read again: a callback that reschedules itself
            // belongs to whoever scheduled the first one, whatever is on the stack now.
            return new Entry(newDelay, callback, data, level, owner);
        }

        /** Whether a server script reload is what this callback would be lost to. */
        boolean belongsToServerScripts() {
            return owner == ScriptType.SERVER;
        }

        @Override
        public int compareTo(Entry other) {
            var byTime = Long.compare(dueAt, other.dueAt);
            return byTime != 0 ? byTime : Long.compare(sequence, other.sequence);
        }
    }

    private static final PriorityQueue<Entry> QUEUE = new PriorityQueue<>();

    /**
     * Callbacks scheduled from somewhere other than the server tick.
     *
     * <p>Scripts load on a reload worker and a script may schedule something as it loads, so the
     * queue itself is only ever touched from the server thread and everything else goes through
     * here first.
     */
    private static final ConcurrentLinkedQueue<Entry> PENDING = new ConcurrentLinkedQueue<>();

    /**
     * Set when a reload has replaced the callbacks, so the queue is dropped on the next tick.
     *
     * <p>Not cleared on the spot, because the queue belongs to the server thread and a reload runs
     * on a worker. Everything already in it is dead either way: a reload closes the script context,
     * so a callback held from before it can no longer be called at all.
     */
    private static final AtomicBoolean CLEAR_REQUESTED = new AtomicBoolean();

    private static long currentTick;

    private static long sequence;

    private ScheduledEvents() {
    }

    /**
     * Queues a callback.
     *
     * @param entry what to run and when
     */
    public static void schedule(Entry entry) {
        PENDING.add(entry);
    }

    /**
     * Queues a callback, built from what a script passed.
     *
     * @param ticks how many ticks to wait, at least one
     * @param callback what to run
     * @param data anything to hand back to the callback, or {@code null}
     * @param level the level it was scheduled from, or {@code null}
     */
    public static void schedule(long ticks, IEventHandler callback, @Nullable Object data,
                                @Nullable ServerLevel level) {
        schedule(new Entry(ticks, callback, data, level));
    }

    /** Forgets every timer, which a server shutdown must do or the next world inherits them. */
    public static void clear() {
        QUEUE.clear();
        PENDING.clear();
        CLEAR_REQUESTED.set(false);
        currentTick = 0L;
    }

    /**
     * Forgets the timers a previous server script load scheduled.
     *
     * <p>Called as server scripts reload, and it has to be: such a timer holds a JavaScript
     * function from the context the reload is about to close, and calling one after that throws.
     * Dropping them is also what a pack author means by reloading — the scripts are about to run
     * again and schedule whatever they schedule.
     *
     * <p>Only those, though. A timer a startup script scheduled belongs to a context nothing ever
     * closes, and no reload re-runs the script that would put it back — so clearing one would lose
     * it for the rest of the session, for no reason.
     *
     * <p>Safe from a reload worker. What is pending is dropped now, since that queue is concurrent;
     * what is already timed is dropped by the server thread on its next tick.
     */
    public static void clearForReload() {
        PENDING.removeIf(Entry::belongsToServerScripts);
        CLEAR_REQUESTED.set(true);
    }

    /**
     * Runs whatever came due, from the end of the server tick.
     *
     * @param server the running server
     */
    public static void tick(MinecraftServer server) {
        currentTick++;

        // Before the pending queue is drained, so that a timer scheduled by the load that asked
        // for the clear survives it -- which is the whole of what a reload is supposed to leave.
        if (CLEAR_REQUESTED.compareAndSet(true, false)) {
            QUEUE.removeIf(Entry::belongsToServerScripts);
        }

        for (var entry = PENDING.poll(); entry != null; entry = PENDING.poll()) {
            entry.dueAt = currentTick + entry.delay;
            entry.sequence = sequence++;
            QUEUE.add(entry);
        }

        if (QUEUE.isEmpty() || QUEUE.peek().dueAt > currentTick) {
            return;
        }

        // Collected before running: a callback that reschedules itself must not be run again in
        // the same tick, and draining into a list is the simplest way to guarantee that.
        List<Entry> due = new ArrayList<>();

        while (!QUEUE.isEmpty() && QUEUE.peek().dueAt <= currentTick) {
            due.add(QUEUE.poll());
        }

        for (var entry : due) {
            try {
                entry.callback.onEvent(new ScheduledEventJS(entry, server));
            } catch (Throwable ex) {
                // A callback calling event.cancel() means "stop here", which has already happened
                // by the time the exit reaches us. Only a real failure is worth reporting.
                if (EventExit.unwrap(ex) == null) {
                    ConsoleJS.SERVER.handleError(ex, "Error in a scheduled callback");
                }
            }
        }

        ScriptType.SERVER.console.flush();
    }
}
