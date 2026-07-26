package com.github.gubejs.event;

import com.github.gubejs.script.ScriptType;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * How an {@link EventGroup} looks from a script: an object whose members are its events.
 *
 * <p>A proxy rather than a plain map so that a typo names itself. {@code ServerEvent.recipes(...)}
 * against a map would read as {@code undefined} and fail with "undefined is not a function", which
 * says nothing about what was actually wrong; here it reports the group and the event that does
 * not exist.
 */
public final class EventGroupWrapper implements ProxyObject {

    private final ScriptType scriptType;

    private final EventGroup group;

    public EventGroupWrapper(ScriptType scriptType, EventGroup group) {
        this.scriptType = scriptType;
        this.group = group;
    }

    @Override
    public Object getMember(String key) {
        var handler = group.getHandlers().get(key);

        if (handler == null) {
            scriptType.console.error("Unknown event '" + group.name + "." + key
                + "'. Known events: " + String.join(", ", group.getHandlers().keySet()));
            return null;
        }

        return handler;
    }

    @Override
    public Object getMemberKeys() {
        return group.getHandlers().keySet().toArray(new String[0]);
    }

    /**
     * Claims every key exists.
     *
     * <p>So that a misspelled event reaches {@link #getMember} and gets a real error, rather than
     * being reported as missing by the engine before this class ever sees it.
     */
    @Override
    public boolean hasMember(String key) {
        return true;
    }

    @Override
    public void putMember(String key, Value value) {
        throw new UnsupportedOperationException(
            "Events cannot be assigned to; call " + group.name + "." + key + "(handler) instead");
    }

    @Override
    public String toString() {
        return group.name;
    }
}
