package com.github.gubejs.server;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A command a script invented, reached through {@code /gubejs custom_command <id>}.
 *
 * <pre>{@code
 * ServerEvents.customCommand('daily', event => {
 *     event.player.give('minecraft:diamond')
 * })
 * }</pre>
 *
 * <p>The simple alternative to {@code ServerEvents.commandRegistry}: no Brigadier, and the
 * listener can be changed by a reload without rebuilding the command tree — which matters because
 * a registered command's tree is built once, when the server starts.
 */
public final class CustomCommandEventJS extends EventJS implements ScriptTypeHolder {

    private final String id;

    private final CommandSourceStack source;

    public CustomCommandEventJS(String id, CommandSourceStack source) {
        this.id = id;
        this.source = source;
    }

    /**
     * Returns which custom command was run.
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Returns who or what ran it.
     *
     * @return the command source
     */
    public CommandSourceStack getSource() {
        return source;
    }

    /**
     * Returns the player who ran it.
     *
     * @return the player, or {@code null} when it came from the console or a command block
     */
    @Nullable
    public ServerPlayer getPlayer() {
        return source.getEntity() instanceof ServerPlayer player ? player : null;
    }

    /**
     * Returns the server.
     *
     * @return the server
     */
    public MinecraftServer getServer() {
        return source.getServer();
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.SERVER;
    }
}
