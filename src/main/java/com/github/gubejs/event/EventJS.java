package com.github.gubejs.event;

import org.jetbrains.annotations.Nullable;

/**
 * Base class for everything handed to a script listener as {@code event}.
 *
 * <p>Subclasses add whatever the event is about. What lives here is the way out: three methods
 * that stop the listener where it stands and tell the game what to do instead.
 */
public abstract class EventJS {

    /**
     * Returns what a listener is handed as its {@code event} argument.
     *
     * <p>This, for everything that describes an event of this mod's own. The one case that differs
     * is {@link ForgeEventJS}, which stands in for an event the game already had a class for: a
     * script listening to a Forge event wants that event, with the methods Forge documents, not a
     * wrapper around it that would have to re-expose every one of them.
     *
     * @return the object script listeners see
     */
    public Object gjs$scriptValue() {
        return this;
    }

    /**
     * The value {@link #cancel()} and friends carry out when the script names none.
     *
     * @return the default, or {@code null}
     */
    @Nullable
    protected Object defaultExitValue() {
        return null;
    }

    /**
     * Converts whatever the script passed to {@code cancel}/{@code success}/{@code exit} into the
     * type the game expects at the other end.
     *
     * @param value what the script passed
     * @return what the caller of {@link EventHandler#post} should see
     */
    @Nullable
    protected Object mapExitValue(@Nullable Object value) {
        return value;
    }

    /**
     * Stops the event with a "no" outcome. Nothing after this call in the listener runs.
     *
     * @return never; the return type only exists so {@code return event.cancel()} reads naturally
     */
    public Object cancel() {
        return cancel(defaultExitValue());
    }

    /**
     * Stops the event with a "yes" outcome. Nothing after this call in the listener runs.
     *
     * @return never
     */
    public Object success() {
        return success(defaultExitValue());
    }

    /**
     * Stops the event and leaves the outcome to the game. Nothing after this call runs.
     *
     * @return never
     */
    public Object exit() {
        return exit(defaultExitValue());
    }

    /**
     * Stops the event with a "no" outcome and a value.
     *
     * @param value what to hand back
     * @return never
     */
    public Object cancel(@Nullable Object value) {
        throw EventResult.Type.INTERRUPT_FALSE.exit(mapExitValue(value));
    }

    /**
     * Stops the event with a "yes" outcome and a value.
     *
     * @param value what to hand back
     * @return never
     */
    public Object success(@Nullable Object value) {
        throw EventResult.Type.INTERRUPT_TRUE.exit(mapExitValue(value));
    }

    /**
     * Stops the event with a value, leaving the outcome to the game.
     *
     * @param value what to hand back
     * @return never
     */
    public Object exit(@Nullable Object value) {
        throw EventResult.Type.INTERRUPT_DEFAULT.exit(mapExitValue(value));
    }

    /**
     * Called once every listener has run, whatever the outcome.
     *
     * @param result what the event decided
     */
    protected void afterPosted(EventResult result) {
    }
}
