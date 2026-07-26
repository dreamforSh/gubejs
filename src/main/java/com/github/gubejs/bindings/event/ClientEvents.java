package com.github.gubejs.bindings.event;

import com.github.gubejs.client.ClientEventJS;
import com.github.gubejs.client.ClientInitEventJS;
import com.github.gubejs.client.DebugInfoEventJS;
import com.github.gubejs.client.LangEventJS;
import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;

/**
 * The {@code ClientEvents} global: things only the machine with a screen knows about.
 *
 * <p>Nothing here exists on a dedicated server, and a listener registered from a server script
 * would never fire — hence {@code client}, which refuses the registration outright rather than
 * leaving a pack author wondering.
 */
public interface ClientEvents {

    EventGroup GROUP = EventGroup.of("ClientEvents");

    /** Fires once the client has finished setting up. Listened to from a startup script. */
    EventHandler INIT = GROUP.startup("init", () -> ClientInitEventJS.class);

    /** Fires when this client joins a world, single-player or otherwise. */
    EventHandler LOGGED_IN = GROUP.client("loggedIn", () -> ClientEventJS.class);

    /** Fires when this client leaves a world. */
    EventHandler LOGGED_OUT = GROUP.client("loggedOut", () -> ClientEventJS.class);

    /**
     * Fires every client tick, twenty times a second.
     *
     * <p>Keeps ticking in menus and while the game is paused in single-player, so check
     * {@code event.player} before using it.
     */
    EventHandler TICK = GROUP.client("tick", () -> ClientEventJS.class);

    /** The left-hand column of the F3 screen being assembled. Fires every frame it is open. */
    EventHandler DEBUG_LEFT = GROUP.client("leftDebugInfo", () -> DebugInfoEventJS.class);

    /** The right-hand column of the F3 screen being assembled. */
    EventHandler DEBUG_RIGHT = GROUP.client("rightDebugInfo", () -> DebugInfoEventJS.class);

    /**
     * The translation table being built — {@code ClientEvents.lang('en_us', event => ...)}.
     *
     * <p>Requires the language code, because the entries a pack adds are per-language and a
     * listener that ran for all of them would have no way to tell which one it was writing.
     */
    EventHandler LANG = GROUP.client("lang", () -> LangEventJS.class).extra(Extra.REQUIRES_STRING);
}
