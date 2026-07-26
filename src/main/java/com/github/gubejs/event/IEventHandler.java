package com.github.gubejs.event;

/**
 * One registered listener.
 *
 * <p>Usually a JavaScript function, wrapped so that calling it goes through the context lock; but
 * a Java plugin can register one directly, and both look the same from here.
 */
@FunctionalInterface
public interface IEventHandler {

    /**
     * Runs this listener.
     *
     * @param event the event being posted
     * @throws EventExit if the listener called {@code cancel}, {@code success} or {@code exit}
     */
    void onEvent(EventJS event);
}
