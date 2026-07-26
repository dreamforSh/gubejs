package com.github.gubejs.script;

/**
 * Something that knows which script type it belongs to.
 *
 * <p>Implemented by {@link ScriptType} itself and by the game objects an event can be posted
 * against — a level, a server, a player — so that posting an event does not need the caller to
 * work out whether it is on the logical client or server.
 */
@FunctionalInterface
public interface ScriptTypeHolder {

    /**
     * Returns the script type events about this object go to.
     *
     * @return the script type
     */
    ScriptType gjs$getScriptType();
}
