package com.github.gubejs.bindings;

import com.github.gubejs.util.ConsoleJS;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.graalvm.polyglot.Value;
import org.graalvm.polyglot.proxy.ProxyExecutable;
import org.graalvm.polyglot.proxy.ProxyObject;

/**
 * Stands in for a KubeJS global that no longer exists, so an old script fails with an explanation.
 *
 * <p>{@code onEvent('item.right_click', ...)} and {@code settings.logAddedRecipes} were how scripts
 * were written before KubeJS 1902, and packs from that era are still copied from. Without something
 * under the name, the error is {@code onEvent is not defined} — which says nothing about what to
 * write instead.
 *
 * <p>Calling one throws, because a listener that never registers would leave the pack quietly
 * broken. Assigning to one only warns, since a setting that no longer exists changes nothing
 * either way and stopping the script over it would be worse than ignoring it.
 */
public final class LegacyCodeHandler implements ProxyObject, ProxyExecutable {

    /** What each removed global should be replaced with. */
    private static final java.util.Map<String, String> ADVICE = java.util.Map.of(
        "onEvent", "listen through the event group instead -- "
            + "ItemEvents.rightClicked(event => ...), ServerEvents.recipes(event => ...)",
        "settings", "the settings it held are in config/gubejs/common.properties and "
            + "config/gubejs/client.properties",
        "java", "use Java.loadClass('fully.qualified.Name')");

    /** Names already warned about, so a script in a loop does not fill the log. */
    private static final Set<String> WARNED = ConcurrentHashMap.newKeySet();

    private final String name;

    public LegacyCodeHandler(String name) {
        this.name = name;
    }

    @Override
    public Object execute(Value... arguments) {
        throw new UnsupportedOperationException(message());
    }

    @Override
    public Object getMember(String key) {
        return new LegacyCodeHandler(name + "." + key);
    }

    @Override
    public boolean hasMember(String key) {
        // Not `then`: an object claiming a `then` member is a thenable, and awaiting anything
        // holding one of these would hang instead of reporting the real problem.
        return !key.equals("then");
    }

    @Override
    public Object getMemberKeys() {
        return List.of();
    }

    @Override
    public void putMember(String key, Value value) {
        warn();
    }

    /** Logs the explanation once per removed global. */
    public void warn() {
        if (WARNED.add(name)) {
            ConsoleJS.getCurrent(ConsoleJS.STARTUP).warn(message());
        }
    }

    private String message() {
        var root = name.indexOf('.') == -1 ? name : name.substring(0, name.indexOf('.'));
        var advice = ADVICE.get(root);
        return "'" + name + "' was removed in KubeJS 1902 and Gubejs does not have it"
            + (advice == null ? "" : "; " + advice);
    }

    @Override
    public String toString() {
        return name;
    }
}
