package com.github.gubejs.event;

import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import com.github.gubejs.script.ScriptTypePredicate;
import com.github.gubejs.util.ValueUtils;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.jetbrains.annotations.Nullable;

/**
 * One event scripts can listen to, such as {@code ItemEvents.rightClicked}.
 *
 * <p>Scripts call it as a function — {@code ServerEvents.recipes(event => { ... })} — which is why
 * it is a {@link ProxyExecutable}: Graal makes a host object callable from JavaScript only if it
 * is a functional interface or a proxy, and this class is neither a functional interface nor
 * something that could sensibly become one.
 *
 * <p>Listeners are stored per script type, and separately per id for events that have one. Posting
 * an event with no listeners costs a null check, which matters because several of these fire on
 * every tick of every entity.
 */
public final class EventHandler implements ProxyExecutable {

    /** The group this event belongs to, e.g. {@code ItemEvents}. */
    public final EventGroup group;

    /** The event's name within its group, e.g. {@code rightClicked}. */
    public final String name;

    /** Which script types may listen. */
    public final ScriptTypePredicate scriptTypePredicate;

    /** The event class, for generated type definitions and error messages. */
    public final Supplier<Class<? extends EventJS>> eventType;

    /** The shape of this event's id, or {@code null} if it takes none. */
    @Nullable
    public Extra extra;

    private boolean hasResult;

    /**
     * Listeners with no id, indexed by {@link ScriptType#ordinal()}.
     *
     * <p>Null until something listens, which is the state almost every event stays in.
     */
    @Nullable
    private EventHandlerContainer[] eventContainers;

    @Nullable
    private Map<Object, EventHandlerContainer[]> extraEventContainers;

    EventHandler(EventGroup group, String name, ScriptTypePredicate scriptTypePredicate,
                 Supplier<Class<? extends EventJS>> eventType) {
        this.group = group;
        this.name = name;
        this.scriptTypePredicate = scriptTypePredicate;
        this.eventType = eventType;
    }

    /**
     * Declares that this event does something with {@code event.cancel()}.
     *
     * <p>Listeners on an event without this get an error rather than silently having their
     * cancellation ignored.
     *
     * @return this handler
     */
    public EventHandler hasResult() {
        hasResult = true;
        return this;
    }

    public boolean getHasResult() {
        return hasResult;
    }

    /**
     * Gives this event an id argument.
     *
     * @param extra the id's shape
     * @return this handler
     */
    public EventHandler extra(Extra extra) {
        this.extra = extra;
        return this;
    }

    /** Whether anything at all is listening. */
    public boolean hasListeners() {
        return eventContainers != null || extraEventContainers != null;
    }

    /**
     * Whether anything is listening either without an id or against this exact one.
     *
     * <p>The id is <em>not</em> transformed first, so this is only useful with
     * {@link Extra#identity} ids where the caller already holds the key. Everything else should
     * use {@link #hasListeners()}.
     *
     * @param extraId a key, already in transformed form
     * @return {@code true} if posting could reach a listener
     */
    public boolean hasListeners(Object extraId) {
        return eventContainers != null
            || extraEventContainers != null && extraEventContainers.containsKey(extraId);
    }

    /**
     * Forgets every listener registered from one script type.
     *
     * @param type the type being reloaded
     */
    public void clear(ScriptType type) {
        if (eventContainers != null) {
            eventContainers[type.ordinal()] = null;

            if (EventHandlerContainer.isEmpty(eventContainers)) {
                eventContainers = null;
            }
        }

        if (extraEventContainers != null) {
            var entries = extraEventContainers.entrySet().iterator();

            while (entries.hasNext()) {
                var entry = entries.next();
                entry.getValue()[type.ordinal()] = null;

                if (EventHandlerContainer.isEmpty(entry.getValue())) {
                    entries.remove();
                }
            }

            if (extraEventContainers.isEmpty()) {
                extraEventContainers = null;
            }
        }
    }

    // --- registration ------------------------------------------------------------------------

    /**
     * Registers a listener.
     *
     * @param type which script type is listening
     * @param extraId the id to listen against, or {@code null} for all of them
     * @param handler what to run
     * @param source where the listener came from, for error messages
     */
    public void listen(ScriptType type, @Nullable Object extraId, IEventHandler handler, String source) {
        var manager = type.getManager();

        if (manager == null || !manager.canListenEvents()) {
            throw new IllegalStateException(
                "Event handler '" + this + "' can only be registered while scripts are loading");
        }

        if (!scriptTypePredicate.test(type)) {
            throw new UnsupportedOperationException("Event handler '" + this
                + "' cannot be listened to from " + type + " scripts; valid types: "
                + scriptTypePredicate.getValidTypes());
        }

        var key = validateId(extraId);
        EventHandlerContainer[] containers;

        if (key == null) {
            if (eventContainers == null) {
                eventContainers = new EventHandlerContainer[ScriptType.VALUES.length];
            }

            containers = eventContainers;
        } else {
            if (extraEventContainers == null) {
                // Identity ids are registry keys, which are interned, so a LinkedHashMap keeps
                // registration order without paying for hashing a ResourceLocation every lookup.
                extraEventContainers = extra != null && extra.identity
                    ? new LinkedHashMap<>() : new HashMap<>();
            }

            containers = extraEventContainers.computeIfAbsent(
                key, ignored -> new EventHandlerContainer[ScriptType.VALUES.length]);
        }

        var index = type.ordinal();

        if (containers[index] == null) {
            containers[index] = new EventHandlerContainer(key, handler, source);
        } else {
            containers[index].add(key, handler, source);
        }
    }

    /**
     * Registers a listener from Java, outside of script loading.
     *
     * @param type which script type owns the listener
     * @param extraId the id to listen against, or {@code null}
     * @param handler what to run
     */
    public void listenJava(ScriptType type, @Nullable Object extraId, IEventHandler handler) {
        var manager = type.getManager();

        if (manager == null) {
            throw new IllegalStateException("No script manager for " + type + " yet");
        }

        manager.whileListening(() -> listen(type, extraId, handler, "java"));
    }

    /**
     * Called when a script uses this event as a function.
     *
     * <p>Two shapes: {@code Event(handler)} and {@code Event(id, handler)}, where {@code id} may
     * be an array to register the same listener against several ids at once.
     *
     * @param arguments what the script passed
     * @return always {@code null}; the call is for its effect
     */
    @Override
    public Object execute(Value... arguments) {
        var type = ScriptType.getCurrent();

        if (type == null) {
            throw new IllegalStateException(
                "Event handler '" + this + "' was called from outside a script");
        }

        if (arguments.length == 0) {
            type.console.error("Event handler '" + this + "' needs a function to call");
            return null;
        }

        var manager = type.getManager();
        var callback = arguments[arguments.length - 1];

        if (manager == null || !callback.canExecute()) {
            type.console.error("The last argument to '" + this + "' must be a function");
            return null;
        }

        var handler = manager.wrap(callback);
        var source = describeSource(callback);

        try {
            if (arguments.length == 1) {
                listen(type, null, handler, source);
            } else {
                for (var id : ValueUtils.listOf(arguments[0])) {
                    listen(type, id, handler, source);
                }
            }
        } catch (Exception ex) {
            // Reported rather than thrown: one bad listener should not abort the whole script
            // file, which is what an exception escaping into the guest would do.
            type.console.error(ex.getMessage() == null ? ex.toString() : ex.getMessage());
        }

        return null;
    }

    /**
     * Names where a listener came from, for the error message when it fails.
     *
     * <p>Graal knows where a function was declared, which is exactly the location a pack author
     * needs; it is only unavailable for host-side listeners and for functions the engine could not
     * attribute.
     */
    private static String describeSource(Value callback) {
        try {
            var location = callback.getSourceLocation();

            if (location != null) {
                return location.getSource().getName() + ":" + location.getStartLine();
            }
        } catch (Exception ignored) {
            // Source attribution is a convenience; never let it break registration.
        }

        return "<unknown source>";
    }

    @Nullable
    private Object validateId(@Nullable Object extraId) {
        var key = extra == null ? ValueUtils.unwrap(extraId) : extra.transform(extraId);

        if (extra == null) {
            if (key != null) {
                throw new IllegalArgumentException(
                    "Event handler '" + this + "' does not take an id");
            }

            return null;
        }

        if (key == null) {
            if (extra.required) {
                throw new IllegalArgumentException("Event handler '" + this + "' requires an id");
            }

            return null;
        }

        if (!extra.validator.test(key)) {
            throw new IllegalArgumentException("Event handler '" + this
                + "' does not accept the id '" + extra.describe(key) + "'");
        }

        return key;
    }

    // --- posting -----------------------------------------------------------------------------

    /**
     * Posts to the one script type this event belongs to.
     *
     * @param event the event to post
     * @return what the listeners decided
     */
    public EventResult post(EventJS event) {
        return post(typeOf(event), null, event, null);
    }

    /**
     * Posts to the one script type this event belongs to, against an id.
     *
     * @param event the event to post
     * @param extraId the id listeners must have registered against
     * @return what the listeners decided
     */
    public EventResult post(EventJS event, @Nullable Object extraId) {
        return post(typeOf(event), extraId, event, null);
    }

    /**
     * Posts to a chosen script type.
     *
     * @param type where the event happened
     * @param event the event to post
     * @return what the listeners decided
     */
    public EventResult post(ScriptTypeHolder type, EventJS event) {
        return post(type, null, event, null);
    }

    /**
     * Posts to a chosen script type, against an id.
     *
     * @param type where the event happened
     * @param extraId the id listeners must have registered against
     * @param event the event to post
     * @return what the listeners decided
     */
    public EventResult post(ScriptTypeHolder type, @Nullable Object extraId, EventJS event) {
        return post(type, extraId, event, null);
    }

    /**
     * Posts an event to every listener that matches, stopping at the first interruption.
     *
     * <p>Listeners registered from startup scripts run too, after the ones belonging to
     * {@code type}: a startup script is the only place a pack can listen to something before the
     * relevant script type has loaded.
     *
     * @param type where the event happened
     * @param extraId the id listeners must have registered against, or {@code null}
     * @param event the event to post
     * @param exceptionHandler consulted about a listener that throws, or {@code null}
     * @return what the listeners decided, {@link EventResult#PASS} if none interrupted
     */
    public EventResult post(ScriptTypeHolder type, @Nullable Object extraId, EventJS event,
                            @Nullable EventExceptionHandler exceptionHandler) {
        if (!hasListeners()) {
            return EventResult.PASS;
        }

        var scriptType = type.gjs$getScriptType();
        var key = extra == null ? null : extra.transform(extraId);
        var result = EventResult.PASS;

        try {
            var forId = key == null || extraEventContainers == null
                ? null : extraEventContainers.get(key);

            if (forId != null) {
                run(scriptType, forId, event, exceptionHandler);
            }

            if (eventContainers != null) {
                run(scriptType, eventContainers, event, exceptionHandler);
            }
        } catch (EventExit exit) {
            if (exit.result.type() == EventResult.Type.ERROR) {
                scriptType.console.handleError((Throwable) exit.result.value(),
                    "Error while handling event '" + this + "'");
            } else {
                result = exit.result;

                if (!hasResult) {
                    scriptType.console.error("Event '" + this
                        + "' was interrupted, but its result is ignored by the game");
                }
            }
        }

        event.afterPosted(result);
        return result;
    }

    /** Runs the listeners belonging to {@code type}, then the startup ones. */
    private void run(ScriptType type, EventHandlerContainer[] containers, EventJS event,
                     @Nullable EventExceptionHandler exceptionHandler) {
        var own = containers[type.ordinal()];

        if (own != null) {
            own.handle(event, exceptionHandler);
        }

        if (!type.isStartup()) {
            var startup = containers[ScriptType.STARTUP.ordinal()];

            if (startup != null) {
                startup.handle(event, exceptionHandler);
            }
        }
    }

    /**
     * Where an event happened, asked of the event itself before the handler.
     *
     * <p>A common event can be posted from either side, so the handler cannot say which one this
     * is — but the event usually can, because it holds a level or a player and those know which
     * side they are on. Asking it first is what lets {@code post(event, id)} work for a common
     * event at all; without it, every caller would have to repeat the side it already handed over.
     */
    private ScriptTypeHolder typeOf(EventJS event) {
        return event instanceof ScriptTypeHolder holder ? holder : requireSingleType();
    }

    private ScriptTypeHolder requireSingleType() {
        if (scriptTypePredicate instanceof ScriptTypeHolder holder) {
            return holder;
        }

        throw new IllegalStateException(
            "Event '" + this + "' spans several script types; say which one to post to");
    }

    // --- introspection -----------------------------------------------------------------------

    /**
     * Walks every listener registered from one script type.
     *
     * @param type the script type to walk
     * @param callback receives each listener
     */
    public void forEachListener(ScriptType type, Consumer<EventHandlerContainer> callback) {
        if (eventContainers != null) {
            for (var c = eventContainers[type.ordinal()]; c != null; c = c.child) {
                callback.accept(c);
            }
        }

        if (extraEventContainers != null) {
            for (var containers : extraEventContainers.values()) {
                for (var c = containers[type.ordinal()]; c != null; c = c.child) {
                    callback.accept(c);
                }
            }
        }
    }

    /**
     * Returns every id one script type registered listeners against.
     *
     * <p>Used by the registry event, which has to know which registries a pack cares about before
     * those registries are filled.
     *
     * @param type the script type to look at
     * @return the ids, in registration order
     */
    public Set<Object> findUniqueExtraIds(ScriptType type) {
        if (!hasListeners()) {
            return Set.of();
        }

        var ids = new LinkedHashSet<>();
        forEachListener(type, c -> {
            if (c.extraId != null) {
                ids.add(c.extraId);
            }
        });
        return ids;
    }

    @Override
    public String toString() {
        return group.name + "." + name;
    }
}
