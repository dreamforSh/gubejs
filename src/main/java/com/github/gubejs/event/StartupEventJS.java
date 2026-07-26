package com.github.gubejs.event;

/**
 * The event handed to {@code StartupEvents.init} and {@code StartupEvents.postInit}.
 *
 * <p>Carries nothing: startup scripts work through the globals, and the event only exists to mark
 * the point in loading that has been reached.
 */
public final class StartupEventJS extends EventJS {
}
