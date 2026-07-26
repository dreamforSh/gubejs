package com.github.gubejs.bindings.event;

import com.github.gubejs.event.EventGroup;
import com.github.gubejs.event.EventHandler;
import com.github.gubejs.event.Extra;
import com.github.gubejs.net.NetworkEventJS;

/**
 * The {@code NetworkEvents} global: data a pack sends between the two sides itself.
 *
 * <p>The channel name is required, because a listener that ran for every message a pack ever sends
 * would have to sort them out itself.
 */
public interface NetworkEvents {

    EventGroup GROUP = EventGroup.of("NetworkEvents");

    /** Data arriving from the other side. */
    EventHandler DATA_RECEIVED = GROUP.common("dataReceived", () -> NetworkEventJS.class)
        .extra(Extra.REQUIRES_STRING).hasResult();
}
