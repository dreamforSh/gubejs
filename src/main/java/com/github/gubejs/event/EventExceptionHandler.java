package com.github.gubejs.event;

import org.jetbrains.annotations.Nullable;

/**
 * Decides what to do about a listener that threw.
 *
 * <p>Given to {@link EventHandler#post} by callers that can do something better than log — a
 * recipe event, say, which can name the recipe being built.
 */
@FunctionalInterface
public interface EventExceptionHandler {

    /**
     * Handles a failure from one listener.
     *
     * @param event the event being posted
     * @param container the listener that threw
     * @param error what it threw
     * @return the error to report, or {@code null} to swallow it and carry on with the next
     *     listener
     */
    @Nullable
    Throwable handle(EventJS event, EventHandlerContainer container, Throwable error);
}
