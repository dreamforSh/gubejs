package com.github.gubejs.event;

/**
 * Thrown by {@code event.cancel()} and friends to unwind out of a listener immediately.
 *
 * <p>An exception rather than a return value because a script author writes
 * {@code if (bad) event.cancel()} and expects the rest of the function not to run. Nothing catches
 * it except {@link EventHandler#post}, and it carries no stack trace: it is control flow, and
 * filling one in on every cancelled interaction would cost more than the event itself.
 */
public final class EventExit extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Digs an exit out of however Graal wrapped it.
     *
     * <p>{@code event.cancel()} throws on the host side, but it is called from inside a script, so
     * by the time it comes back out of {@code Value.execute} it is a
     * {@link org.graalvm.polyglot.PolyglotException} wrapping the original. Unwrapping is what
     * makes cancelling work from a script at all.
     *
     * @param error what came out of the listener
     * @return the exit, or {@code null} if this failure was a real one
     */
    @org.jetbrains.annotations.Nullable
    public static EventExit unwrap(Throwable error) {
        for (var t = error; t != null; t = t.getCause()) {
            if (t instanceof EventExit exit) {
                return exit;
            }

            if (t instanceof org.graalvm.polyglot.PolyglotException polyglot
                && polyglot.isHostException()
                && polyglot.asHostException() instanceof EventExit exit) {
                return exit;
            }

            if (t == t.getCause()) {
                break;
            }
        }

        return null;
    }

    /** The outcome being carried out of the listener. */
    public final transient EventResult result;

    EventExit(EventResult result) {
        super(null, null, false, false);
        this.result = result;
    }

    @Override
    public String getMessage() {
        return "Event exited with " + result;
    }
}
