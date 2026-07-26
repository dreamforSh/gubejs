package com.github.gubejs.event;

import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypePredicate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * A named set of events, bound into scripts as one global — {@code ServerEvents},
 * {@code PlayerEvents}, and whatever a plugin adds.
 *
 * <p>Declaring a group does not publish it; {@link #register()} does. That split exists so a
 * plugin can hold its group in a static field, which runs at class initialisation, and still
 * decide at load time whether the mod it integrates with is actually present.
 */
public final class EventGroup {

    private static final Map<String, EventGroup> MAP = new LinkedHashMap<>();

    /**
     * Returns every registered group by name.
     *
     * @return an unmodifiable view, in registration order
     */
    public static Map<String, EventGroup> getGroups() {
        return Collections.unmodifiableMap(MAP);
    }

    /**
     * Declares a group.
     *
     * @param name the global scripts will see
     * @return the new group, not yet registered
     */
    public static EventGroup of(String name) {
        return new EventGroup(name);
    }

    /** The global name, e.g. {@code ServerEvents}. */
    public final String name;

    private final Map<String, EventHandler> handlers = new LinkedHashMap<>();

    private EventGroup(String name) {
        this.name = name;
    }

    /** Publishes this group, so scripts get it as a global. */
    public void register() {
        MAP.put(name, this);
    }

    /**
     * Adds an event to this group.
     *
     * @param name the event's name within the group
     * @param scriptType which script types may listen
     * @param eventType the event class
     * @return the new event, for chaining {@code extra} and {@code hasResult}
     */
    public EventHandler add(String name, ScriptTypePredicate scriptType,
                            Supplier<Class<? extends EventJS>> eventType) {
        var handler = new EventHandler(this, name, scriptType, eventType);
        handlers.put(name, handler);
        return handler;
    }

    /** Adds an event only startup scripts may listen to. */
    public EventHandler startup(String name, Supplier<Class<? extends EventJS>> eventType) {
        return add(name, ScriptType.STARTUP, eventType);
    }

    /** Adds an event only server scripts may listen to. */
    public EventHandler server(String name, Supplier<Class<? extends EventJS>> eventType) {
        return add(name, ScriptType.SERVER, eventType);
    }

    /** Adds an event only client scripts may listen to. */
    public EventHandler client(String name, Supplier<Class<? extends EventJS>> eventType) {
        return add(name, ScriptType.CLIENT, eventType);
    }

    /** Adds an event both server and client scripts may listen to. */
    public EventHandler common(String name, Supplier<Class<? extends EventJS>> eventType) {
        return add(name, ScriptTypePredicate.COMMON, eventType);
    }

    /**
     * Returns the events in this group.
     *
     * @return the events by name, in declaration order
     */
    public Map<String, EventHandler> getHandlers() {
        return handlers;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return obj == this || obj instanceof EventGroup g && name.equals(g.name);
    }
}
