package com.github.gubejs.bindings;

import com.github.gubejs.net.GubejsNetwork;
import com.github.gubejs.util.ValueUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Client} global: the game as the person in front of it sees it.
 *
 * <p>Only bound in client scripts. Everything on it can be {@code null} outside a world, because a
 * client script's context outlives any particular world — it is built when the resource packs load
 * and survives until they are reloaded.
 */
public final class ClientWrapper {

    private ClientWrapper() {
    }

    /**
     * Returns the game instance.
     *
     * @return the client
     */
    public static Minecraft getInstance() {
        return Minecraft.getInstance();
    }

    /**
     * Returns the player at the keyboard.
     *
     * @return the player, or {@code null} outside a world
     */
    @Nullable
    public static LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * Returns the level the player is in.
     *
     * @return the level, or {@code null} outside a world
     */
    @Nullable
    public static ClientLevel getLevel() {
        return Minecraft.getInstance().level;
    }

    /**
     * Returns what the player is looking at.
     *
     * @return the hit result, or {@code null} outside a world
     */
    @Nullable
    public static net.minecraft.world.phys.HitResult getHitResult() {
        return Minecraft.getInstance().hitResult;
    }

    /**
     * Returns the item under the mouse in the open screen.
     *
     * @return the stack, empty when nothing is hovered
     */
    public static ItemStack getHoveredItem() {
        var screen = Minecraft.getInstance().screen;

        if (screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container) {
            var slot = container.getSlotUnderMouse();
            return slot == null ? ItemStack.EMPTY : slot.getItem();
        }

        return ItemStack.EMPTY;
    }

    /** Whether the game is paused, which in single-player means the world has stopped. */
    public static boolean isPaused() {
        return Minecraft.getInstance().isPaused();
    }

    /**
     * Returns the language the game is set to, e.g. {@code en_us}.
     *
     * @return the language code
     */
    public static String getLanguage() {
        var selected = Minecraft.getInstance().getLanguageManager().getSelected();
        return selected == null ? "en_us" : selected.getCode();
    }

    /**
     * Shows a message in the chat, visible to this player only.
     *
     * @param message the message, as text or a component
     */
    public static void tell(Object message) {
        var player = getPlayer();

        if (player != null) {
            player.displayClientMessage(TextWrapper.of(message), false);
        }
    }

    /**
     * Shows a message above the hotbar.
     *
     * @param message the message
     */
    public static void setStatusMessage(Object message) {
        var player = getPlayer();

        if (player != null) {
            player.displayClientMessage(TextWrapper.of(message), true);
        }
    }

    /**
     * Sends data to the server, where {@code NetworkEvents.dataReceived} picks it up.
     *
     * @param channel the channel name the server-side listener registered against
     * @param data anything that converts to a compound tag
     */
    /**
     * Returns what this client is drawing over the game.
     *
     * <p>The same thing a server script reaches through {@code player.paint}, for a client script
     * that has the information locally and does not need a round trip to show it.
     *
     * @return the painter
     */
    public static com.github.gubejs.client.painter.Painter getPainter() {
        return com.github.gubejs.client.painter.Painter.INSTANCE;
    }

    /**
     * Draws things over the game.
     *
     * @param objects the descriptions, by name
     */
    public static void paint(@Nullable Object objects) {
        com.github.gubejs.client.painter.Painter.INSTANCE.paint(objects);
    }

    /**
     * Shows a pop-up in the corner of the screen.
     *
     * @param notification the description — {@code title}, {@code subtitle}, {@code icon},
     *     {@code color} and {@code duration} in milliseconds
     */
    public static void notify(@Nullable Object notification) {
        com.github.gubejs.client.painter.ScriptToast.show(
            com.github.gubejs.util.NbtHelper.compound(notification));
    }

    public static void sendData(String channel, @Nullable Object data) {
        GubejsNetwork.sendToServer(channel, ValueUtils.unwrap(data));
    }
}
