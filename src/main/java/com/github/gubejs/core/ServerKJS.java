package com.github.gubejs.core;

import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.event.IEventHandler;
import com.github.gubejs.net.GubejsNetwork;
import com.github.gubejs.server.ScheduledEvents;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * What a script can do with the server, mixed into {@link MinecraftServer} itself.
 *
 * <p>{@code event.server.runCommandSilent(...)} and {@code event.server.scheduleInTicks(...)} are
 * what a KubeJS pack writes; the game's own object has neither.
 */
public interface ServerKJS {

    /**
     * Returns this, as the server it is.
     *
     * @return this server
     */
    default MinecraftServer gjs$self() {
        return (MinecraftServer) this;
    }

    // --- commands ------------------------------------------------------------------------------

    /**
     * Runs a command with full permissions, at the world spawn.
     *
     * @param command the command, without the leading slash
     * @return what the command returned
     */
    default int runCommand(String command) {
        var server = gjs$self();
        return server.getCommands()
            .performPrefixedCommand(server.createCommandSourceStack(), command);
    }

    /**
     * Runs a command with full permissions, without its output appearing in chat.
     *
     * <p>What a pack wants nearly every time: a command run from a listener that also broadcasts
     * "[Server] gave 1 diamond to Steve" turns a quiet script into chat spam.
     *
     * @param command the command, without the leading slash
     * @return what the command returned
     */
    default int runCommandSilent(String command) {
        var server = gjs$self();
        return server.getCommands().performPrefixedCommand(
            server.createCommandSourceStack().withSuppressedOutput(), command);
    }

    // --- players -------------------------------------------------------------------------------

    /**
     * Returns every player on the server.
     *
     * @return the players
     */
    default List<ServerPlayer> getPlayers() {
        return new ArrayList<>(gjs$self().getPlayerList().getPlayers());
    }

    /**
     * Looks a player up by name.
     *
     * @param name the player's name, case-insensitively
     * @return the player, or {@code null} if nobody by that name is online
     */
    @Nullable
    default ServerPlayer getPlayer(String name) {
        return gjs$self().getPlayerList().getPlayerByName(name);
    }

    /**
     * Sends a message to everyone on the server.
     *
     * @param message a string, a component, or an array of either
     */
    default void tell(@Nullable Object message) {
        for (var player : gjs$self().getPlayerList().getPlayers()) {
            player.sendSystemMessage(TextWrapper.of(message));
        }
    }

    /**
     * Sends data to every player's client, where {@code NetworkEvents.dataReceived} picks it up.
     *
     * @param channel the channel name a client script listens on
     * @param data the payload
     */
    default void sendDataToAll(String channel, @Nullable Object data) {
        GubejsNetwork.sendToAll(gjs$self(), channel, data);
    }

    // --- levels --------------------------------------------------------------------------------

    /**
     * Looks a level up by dimension id.
     *
     * @param dimension the dimension id, e.g. {@code minecraft:the_nether}
     * @return the level, or {@code null} if no such dimension is loaded
     */
    @Nullable
    default ServerLevel getLevel(String dimension) {
        var id = ResourceLocation.tryParse(
            dimension.indexOf(':') == -1 ? "minecraft:" + dimension : dimension);
        return id == null ? null
            : gjs$self().getLevel(ResourceKey.create(net.minecraft.core.Registry.DIMENSION_REGISTRY, id));
    }

    /**
     * Returns every loaded level.
     *
     * @return the levels
     */
    default List<ServerLevel> getLevels() {
        var levels = new ArrayList<ServerLevel>();
        gjs$self().getAllLevels().forEach(levels::add);
        return levels;
    }

    // --- scheduling ----------------------------------------------------------------------------

    /**
     * Runs a function after a delay.
     *
     * <p>What a pack should use instead of counting in {@code ServerEvents.tick}: this costs
     * nothing between firings, and a tick listener costs something on every one.
     *
     * @param ticks how long to wait, twenty ticks to the second
     * @param callback what to run
     */
    default void scheduleInTicks(long ticks, IEventHandler callback) {
        ScheduledEvents.schedule(ticks, callback, null, null);
    }

    /**
     * Runs a function after a delay, handing it a value.
     *
     * @param ticks how long to wait
     * @param data what to hand the callback as {@code event.data}
     * @param callback what to run
     */
    default void scheduleInTicks(long ticks, @Nullable Object data, IEventHandler callback) {
        ScheduledEvents.schedule(ticks, callback, data, null);
    }

    /**
     * Runs a function over and over, on an interval.
     *
     * <p>Rescheduled from inside itself rather than by a repeating flag on the queue, so a
     * callback that throws stops repeating — a broken script that fires twenty times a second
     * forever is worse than one that stops.
     *
     * @param ticks how long between runs
     * @param callback what to run
     */
    default void scheduleRepeatingInTicks(long ticks, IEventHandler callback) {
        // A one-element array because the handler has to name itself, and a local cannot be
        // referred to from its own initialiser.
        var self = new IEventHandler[1];

        self[0] = event -> {
            callback.onEvent(event);
            ScheduledEvents.schedule(ticks, self[0], null, null);
        };

        ScheduledEvents.schedule(ticks, self[0], null, null);
    }

    /**
     * Runs a function after a delay, in seconds.
     *
     * @param seconds how long to wait
     * @param callback what to run
     */
    default void scheduleInSeconds(double seconds, IEventHandler callback) {
        ScheduledEvents.schedule(Math.round(seconds * 20), callback, null, null);
    }

    // --- identity ------------------------------------------------------------------------------

    /**
     * Reports whether this is a single-player world rather than a dedicated server.
     *
     * @return {@code true} for single player
     */
    default boolean isSinglePlayer() {
        return gjs$self().isSingleplayer();
    }

    /**
     * Returns the persistent data attached to the world, which survives restarts.
     *
     * <p>Where a pack keeps state that belongs to the world rather than to a player.
     *
     * @return the tag
     */
    default net.minecraft.nbt.CompoundTag getPersistentData() {
        return gjs$self().overworld().getDataStorage()
            .computeIfAbsent(com.github.gubejs.server.ServerData::load,
                com.github.gubejs.server.ServerData::new, "gubejs").data;
    }
}
