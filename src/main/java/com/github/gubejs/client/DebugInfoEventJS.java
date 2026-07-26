package com.github.gubejs.client;

import com.github.gubejs.util.ValueUtils;
import java.util.List;

/**
 * One of the two columns of F3 text, while it is being assembled.
 *
 * <pre>{@code
 * ClientEvents.rightDebugInfo(event => {
 *     event.add('Coins: ' + Client.player.persistentData.coins)
 * })
 * }</pre>
 *
 * <p>Fires every frame the debug screen is open, so build the string from something already
 * computed rather than computing it here.
 */
public final class DebugInfoEventJS extends ClientEventJS {

    private final List<String> lines;

    public DebugInfoEventJS(List<String> lines) {
        this.lines = lines;
    }

    /**
     * Returns the lines collected so far.
     *
     * @return the live list, so editing it edits the screen
     */
    public List<String> getLines() {
        return lines;
    }

    /**
     * Adds a line at the bottom of the column.
     *
     * @param text what to show; {@code null} adds a blank line, which is how vanilla separates
     *     groups
     * @return this event
     */
    public DebugInfoEventJS add(Object text) {
        lines.add(text == null ? "" : String.valueOf(ValueUtils.unwrap(text)));
        return this;
    }
}
