package com.github.gubejs.event;

import com.github.gubejs.util.ConsoleJS;
import org.jetbrains.annotations.Nullable;

/**
 * One listener, plus the next one after it.
 *
 * <p>A linked list rather than a {@code List} because the overwhelmingly common case is exactly
 * one listener per event per script type, and this way that case is one object with a null tail
 * instead of a list wrapping an array.
 */
public final class EventHandlerContainer {

    /**
     * Reports whether every slot in a per-script-type array is empty.
     *
     * @param array the array to check, possibly {@code null}
     * @return {@code true} if there is nothing left to run
     */
    public static boolean isEmpty(@Nullable EventHandlerContainer[] array) {
        if (array == null) {
            return true;
        }

        for (var c : array) {
            if (c != null) {
                return false;
            }
        }

        return true;
    }

    /** The id this listener was registered against, or {@code null} for a catch-all. */
    @Nullable
    public final Object extraId;

    /** The listener itself. */
    public final IEventHandler handler;

    /** Where it was registered, for error messages. */
    public final String source;

    EventHandlerContainer child;

    EventHandlerContainer(@Nullable Object extraId, IEventHandler handler, String source) {
        this.extraId = extraId;
        this.handler = handler;
        this.source = source;
    }

    /**
     * Runs this listener and everything chained after it.
     *
     * @param event the event to hand each listener
     * @param exceptionHandler consulted about failures, or {@code null} to report them as errors
     * @throws EventExit if a listener interrupted
     */
    void handle(EventJS event, @Nullable EventExceptionHandler exceptionHandler) {
        for (var itr = this; itr != null; itr = itr.child) {
            var restore = ConsoleJS.pushSource(itr.source);

            try {
                itr.handler.onEvent(event);
            } catch (Throwable ex) {
                var exit = EventExit.unwrap(ex);

                if (exit != null) {
                    throw exit;
                }

                if (exceptionHandler == null) {
                    throw EventResult.Type.ERROR.exit(ex);
                }

                var reported = exceptionHandler.handle(event, itr, ex);

                if (reported != null) {
                    throw EventResult.Type.ERROR.exit(reported);
                }
            } finally {
                ConsoleJS.pushSource(restore);
            }
        }
    }

    void add(@Nullable Object extraId, IEventHandler handler, String source) {
        var itr = this;

        while (itr.child != null) {
            itr = itr.child;
        }

        itr.child = new EventHandlerContainer(extraId, handler, source);
    }

    @Override
    public String toString() {
        return "Event Handler (" + source + ")";
    }
}
