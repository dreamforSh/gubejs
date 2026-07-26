package com.github.gubejs.player;

import com.github.gubejs.bindings.TextWrapper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

/**
 * A chat message a player sent, before anyone has seen it.
 *
 * <p>{@code event.cancel()} drops the message. Assigning to {@code event.message} rewrites it.
 */
public final class PlayerChatEventJS extends PlayerEventJS {

    private final String message;

    @Nullable
    private Component component;

    public PlayerChatEventJS(Player player, String message) {
        super(player);
        this.message = message;
    }

    /**
     * Returns what the player typed.
     *
     * @return the raw message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Returns the message as it will be sent, including any rewrite.
     *
     * @return the component, or {@code null} if the message was not rewritten
     */
    @Nullable
    public Component getComponent() {
        return component;
    }

    /**
     * Replaces the message.
     *
     * @param value the new message, as text or a component
     */
    public void setComponent(@Nullable Object value) {
        component = value == null ? null : TextWrapper.of(value);
    }
}
