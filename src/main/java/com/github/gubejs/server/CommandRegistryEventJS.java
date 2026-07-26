package com.github.gubejs.server;

import com.github.gubejs.event.EventJS;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/**
 * The event handed to {@code ServerEvents.commandRegistry}: register commands from a script.
 *
 * <pre>{@code
 * ServerEvents.commandRegistry(event => {
 *     const { commands: Commands, arguments: Arguments } = event
 *     event.register(
 *         Commands.literal('heal')
 *             .requires(src => src.hasPermission(2))
 *             .executes(ctx => { ctx.source.player.setHealth(20); return 1 })
 *     )
 * })
 * }</pre>
 *
 * <p>Brigadier's own builders are exposed rather than wrapped. They are already fluent, a script
 * can pass a function wherever Brigadier wants a functional interface, and anything this mod put in
 * front of them would only be a smaller version of the same API.
 */
public final class CommandRegistryEventJS extends EventJS {

    private final CommandDispatcher<CommandSourceStack> dispatcher;

    private final CommandBuildContext buildContext;

    private final Commands.CommandSelection selection;

    public CommandRegistryEventJS(CommandDispatcher<CommandSourceStack> dispatcher,
                                  CommandBuildContext buildContext,
                                  Commands.CommandSelection selection) {
        this.dispatcher = dispatcher;
        this.buildContext = buildContext;
        this.selection = selection;
    }

    /**
     * Registers a command.
     *
     * @param command the command tree to register
     */
    public void register(LiteralArgumentBuilder<CommandSourceStack> command) {
        dispatcher.register(command);
    }

    /**
     * Starts a literal node, i.e. a fixed word in the command.
     *
     * @param name the word
     * @return the builder
     */
    public LiteralArgumentBuilder<CommandSourceStack> literal(String name) {
        return Commands.literal(name);
    }

    /**
     * Starts an argument node.
     *
     * @param name the argument name, as {@code ctx.getArgument} will spell it
     * @param type the argument type
     * @param <T> what the argument parses to
     * @return the builder
     */
    public <T> RequiredArgumentBuilder<CommandSourceStack, T> argument(
        String name, com.mojang.brigadier.arguments.ArgumentType<T> type) {
        return Commands.argument(name, type);
    }

    /**
     * Returns the dispatcher, for anything the helpers above do not cover.
     *
     * @return the command dispatcher
     */
    public CommandDispatcher<CommandSourceStack> getDispatcher() {
        return dispatcher;
    }

    /**
     * Returns the context argument types need in order to see registry contents.
     *
     * @return the build context
     */
    public CommandBuildContext getBuildContext() {
        return buildContext;
    }

    /**
     * Returns whether this is a dedicated server, an integrated one, or a command block context.
     *
     * @return the selection
     */
    public Commands.CommandSelection getSelection() {
        return selection;
    }
}
