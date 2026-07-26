package com.github.gubejs.core;

import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.net.GubejsNetwork;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jetbrains.annotations.Nullable;

/**
 * What a script can do with a player, mixed into {@link Player} itself.
 *
 * <p>The same arrangement as {@link EntityKJS}, one level down: {@code event.player} is the game's
 * own player object and still answers {@code give}, {@code tell} and {@code stages}.
 */
public interface PlayerKJS extends EntityKJS {

    /**
     * Returns this, as the player it is.
     *
     * @return this player
     */
    default Player gjs$player() {
        return (Player) this;
    }

    // --- identity ------------------------------------------------------------------------------

    /**
     * Returns the player's name as plain text.
     *
     * <p>Not {@code getName()}: the game's own returns a {@link net.minecraft.network.chat.Component},
     * which is what a host method taking one still needs.
     *
     * @return the name
     */
    default String getUsername() {
        return gjs$player().getGameProfile().getName();
    }

    /**
     * Reports whether the player has a permission level, the way a command does.
     *
     * @param level the level to check, {@code 2} for most operator commands
     * @return {@code true} if they have it
     */
    default boolean hasPermission(int level) {
        return gjs$player().hasPermissions(level);
    }

    // --- items ---------------------------------------------------------------------------------

    /**
     * Puts an item in the player's inventory, dropping what does not fit.
     *
     * @param item an item id, a stack, or an object naming one
     */
    default void give(@Nullable Object item) {
        var stack = ItemStackJS.of(item);

        if (!stack.isEmpty()) {
            ItemHandlerHelper.giveItemToPlayer(gjs$player(), stack.copy());
        }
    }

    /**
     * Replaces what the player is holding in their main hand.
     *
     * @param item an item id, a stack, or an object naming one
     */
    default void giveInHand(@Nullable Object item) {
        gjs$player().setItemInHand(InteractionHand.MAIN_HAND, ItemStackJS.of(item).copy());
    }

    /**
     * Returns what the player is holding in their main hand.
     *
     * @return the stack
     */
    default ItemStack getMainHandItem() {
        return gjs$player().getItemInHand(InteractionHand.MAIN_HAND);
    }

    /**
     * Returns what the player is holding in their off hand.
     *
     * @return the stack
     */
    default ItemStack getOffHandItem() {
        return gjs$player().getItemInHand(InteractionHand.OFF_HAND);
    }

    /**
     * Counts how many of an item the player is carrying.
     *
     * @param item what to count, matched on item and NBT the way a recipe would
     * @return how many there are, across every slot
     */
    default int countItem(@Nullable Object item) {
        var wanted = ItemStackJS.of(item);

        if (wanted.isEmpty()) {
            return 0;
        }

        var inventory = gjs$player().getInventory();
        var count = 0;

        for (var i = 0; i < inventory.getContainerSize(); i++) {
            var stack = inventory.getItem(i);

            if (ItemStack.isSameItemSameTags(stack, wanted)) {
                count += stack.getCount();
            }
        }

        return count;
    }

    /**
     * Stops the player using an item for a while, the way an ender pearl does.
     *
     * @param item what to put on cooldown
     * @param ticks how long for
     */
    default void addItemCooldown(@Nullable Object item, int ticks) {
        var stack = ItemStackJS.of(item);

        if (!stack.isEmpty()) {
            gjs$player().getCooldowns().addCooldown(stack.getItem(), ticks);
        }
    }

    /**
     * Reports whether an item is on cooldown.
     *
     * @param item what to check
     * @return {@code true} if the player cannot use it yet
     */
    default boolean isItemOnCooldown(@Nullable Object item) {
        var stack = ItemStackJS.of(item);
        return !stack.isEmpty() && gjs$player().getCooldowns().isOnCooldown(stack.getItem());
    }

    // --- messages ------------------------------------------------------------------------------

    /**
     * Shows a message above the hotbar, where the game puts the name of a held item.
     *
     * @param message a string, a component, or an array of either
     */
    default void setStatusMessage(@Nullable Object message) {
        gjs$player().displayClientMessage(TextWrapper.of(message), true);
    }

    /**
     * Sends the player a message that only they see, addressed to them.
     *
     * @param message a string, a component, or an array of either
     */
    default void sendMessage(@Nullable Object message) {
        tell(message);
    }

    // --- stages --------------------------------------------------------------------------------

    /**
     * Returns the player's game stages.
     *
     * <pre>{@code
     * if (!player.stages.has('nether')) {
     *     player.stages.add('nether')
     * }
     * }</pre>
     *
     * @return the stage manager
     */
    default StageManager getStages() {
        return new StageManager(gjs$player());
    }

    // --- persistent data -----------------------------------------------------------------------

    /**
     * Returns the part of the player's data that survives death.
     *
     * <p>{@code player.persistentData} is Forge's, and it is wiped when the player respawns —
     * which is almost never what a pack wanted when it stored something there.
     *
     * @return the tag, created if it was not there
     */
    default CompoundTag getPersistedData() {
        var data = gjs$player().getPersistentData();

        if (!data.contains("PlayerPersisted", Tag.TAG_COMPOUND)) {
            data.put("PlayerPersisted", new CompoundTag());
        }

        return data.getCompound("PlayerPersisted");
    }

    // --- network -------------------------------------------------------------------------------

    /**
     * Sends data to this player's client, where {@code NetworkEvents.dataReceived} picks it up.
     *
     * @param channel the channel name a client script listens on
     * @param data the payload
     */
    default void sendData(String channel, @Nullable Object data) {
        if (gjs$player() instanceof ServerPlayer serverPlayer) {
            GubejsNetwork.sendToPlayer(serverPlayer, channel, data);
        }
    }

    // --- the screen ----------------------------------------------------------------------------

    /**
     * Draws things over this player's game.
     *
     * <pre>{@code
     * player.paint({
     *     hp: { type: 'text', text: 'HP', x: 10, y: 10, color: '#FF5555', shadow: true },
     *     bar: { type: 'rectangle', x: 10, y: 22, w: 100, h: 6, color: '#8800FF00' }
     * })
     * }</pre>
     *
     * <p>Keyed by name, so a bar redrawn every tick replaces only itself. An entry set to an empty
     * object is removed; {@link #clearOverlay()} takes everything away.
     *
     * @param objects the descriptions, by name
     */
    default void paint(@Nullable Object objects) {
        var data = new CompoundTag();
        data.put("objects", com.github.gubejs.util.NbtHelper.compound(objects));
        sendData(com.github.gubejs.net.GubejsNetwork.PAINT_CHANNEL, data);
    }

    /**
     * Shows a pop-up in the corner of this player's screen.
     *
     * <pre>{@code
     * player.notify({
     *     title: 'Quest complete',
     *     subtitle: 'Mine a diamond',
     *     icon: 'minecraft:diamond'
     * })
     * }</pre>
     *
     * @param notification the description — {@code title}, {@code subtitle}, {@code icon},
     *     {@code color} and {@code duration} in milliseconds
     */
    default void notify(@Nullable Object notification) {
        sendData(com.github.gubejs.net.GubejsNetwork.NOTIFY_CHANNEL,
            com.github.gubejs.util.NbtHelper.compound(notification));
    }

    /**
     * Removes everything {@link #paint} drew.
     */
    default void clearOverlay() {
        var data = new CompoundTag();
        data.putBoolean("clear", true);
        sendData(com.github.gubejs.net.GubejsNetwork.PAINT_CHANNEL, data);
    }

    // --- screens -------------------------------------------------------------------------------

    /**
     * Closes whatever screen the player has open.
     */
    default void closeInventory() {
        gjs$player().closeContainer();
    }

    /**
     * Returns how much experience the player has, counting the levels.
     *
     * @return the total experience points
     */
    default int getTotalXp() {
        var player = gjs$player();
        return Math.round(player.experienceProgress * player.getXpNeededForNextLevel())
            + xpForLevels(player.experienceLevel);
    }

    /**
     * Returns how much experience is needed to reach a level from nothing.
     *
     * <p>The three-piece formula the game uses; there is no vanilla method that answers it.
     */
    private static int xpForLevels(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        } else if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }

        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }
}
