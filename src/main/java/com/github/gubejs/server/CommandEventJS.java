package com.github.gubejs.server;

import com.github.gubejs.event.EventJS;
import com.mojang.brigadier.ParseResults;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * A command about to run — {@code ServerEvents.command('gamemode', event => ...)}.
 *
 * <p>{@code event.cancel()} stops it before anything happens.
 */
public final class CommandEventJS extends EventJS {

    private final ParseResults<CommandSourceStack> results;

    private final String name;

    public CommandEventJS(ParseResults<CommandSourceStack> results, String name) {
        this.results = results;
        this.name = name;
    }

    /**
     * Returns the command's first word, which is also the id listeners register against.
     *
     * @return the command name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns who or what is running the command.
     *
     * @return the command source
     */
    public CommandSourceStack getSource() {
        return results.getContext().getSource();
    }

    /**
     * Returns the player running the command.
     *
     * @return the player, or {@code null} for the console, a command block or a function
     */
    @Nullable
    public ServerPlayer getPlayer() {
        return getSource().getEntity() instanceof ServerPlayer player ? player : null;
    }

    /**
     * Returns the command as typed.
     *
     * @return the full command text, without the leading slash
     */
    public String getInput() {
        return results.getReader().getString();
    }

    /**
     * Returns Brigadier's parse of the command, for reading its arguments.
     *
     * @return the parse results
     */
    public ParseResults<CommandSourceStack> getParseResults() {
        return results;
    }
}
