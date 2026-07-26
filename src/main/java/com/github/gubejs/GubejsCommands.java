package com.github.gubejs;

import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.CustomCommandEventJS;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;

/**
 * The {@code /gubejs} command.
 *
 * <p>Small on purpose: reloading, and finding out what went wrong. Anything a pack wants beyond
 * that it can register itself through {@code ServerEvents.commandRegistry}.
 */
public final class GubejsCommands {

    private GubejsCommands() {
    }

    /**
     * Registers the command.
     *
     * @param dispatcher the server's command dispatcher
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("gubejs")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("reload")
                .then(Commands.literal("server_scripts").executes(ctx -> reloadServer(ctx.getSource())))
                .then(Commands.literal("client_scripts").executes(ctx -> {
                    ctx.getSource().sendSuccess(Component.literal(
                        "Client scripts reload with the resource packs; press F3+T."), false);
                    return 1;
                }))
                .then(Commands.literal("startup_scripts").executes(ctx -> {
                    ctx.getSource().sendSuccess(Component.literal(
                        "Startup scripts only run once, while the game loads. Restart to reload them."),
                        false);
                    return 1;
                }))
                .executes(ctx -> reloadServer(ctx.getSource())))
            .then(Commands.literal("errors")
                .executes(ctx -> listMessages(ctx.getSource(), ScriptType.SERVER, true))
                .then(Commands.argument("type", StringArgumentType.word())
                    .executes(ctx -> listMessages(ctx.getSource(),
                        typeOf(StringArgumentType.getString(ctx, "type")), true))))
            .then(Commands.literal("warnings")
                .executes(ctx -> listMessages(ctx.getSource(), ScriptType.SERVER, false))
                .then(Commands.argument("type", StringArgumentType.word())
                    .executes(ctx -> listMessages(ctx.getSource(),
                        typeOf(StringArgumentType.getString(ctx, "type")), false))))
            .then(Commands.literal("hand").executes(ctx -> describeHeldItem(ctx.getSource())))
            .then(Commands.literal("custom_command")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> runCustomCommand(ctx.getSource(),
                        StringArgumentType.getString(ctx, "id"))))));
    }

    /**
     * Runs a listener registered with {@code ServerEvents.customCommand}.
     *
     * @param source who ran it
     * @param id the command name a script chose
     * @return 1 if a listener handled it, 0 if nothing was listening
     */
    private static int runCustomCommand(CommandSourceStack source, String id) {
        if (!ServerEvents.CUSTOM_COMMAND.hasListeners()) {
            source.sendFailure(Component.literal("No script is listening for '" + id + "'"));
            return 0;
        }

        var result = ServerEvents.CUSTOM_COMMAND.post(
            ScriptType.SERVER, id, new CustomCommandEventJS(id, source));

        if (result.pass()) {
            source.sendFailure(Component.literal("No script is listening for '" + id + "'"));
            return 0;
        }

        return result.interruptFalse() ? 0 : 1;
    }

    /**
     * Tells a joining player their scripts logged something, so a broken pack is not silent.
     *
     * @param player the player who just joined
     */
    public static void announceErrors(Player player) {
        if (!CommonProperties.get().announceErrorsInChat || !player.hasPermissions(2)) {
            return;
        }

        for (var type : ScriptType.VALUES) {
            if (!type.errors.isEmpty()) {
                player.sendSystemMessage(type.errorsComponent("/gubejs errors " + type.name));
            } else if (!type.warnings.isEmpty()) {
                player.sendSystemMessage(type.warningsComponent("/gubejs warnings " + type.name));
            }
        }
    }

    private static int reloadServer(CommandSourceStack source) {
        source.sendSuccess(Component.literal("Reloading server scripts..."), true);
        // Through the vanilla reload rather than reloading scripts alone: recipes and tags are
        // rebuilt from the scripts, and reloading one without the other would leave the two
        // disagreeing.
        source.getServer().getCommands().performPrefixedCommand(source, "reload");
        return 1;
    }

    private static int listMessages(CommandSourceStack source, ScriptType type, boolean errors) {
        var messages = errors ? type.errors : type.warnings;

        if (messages.isEmpty()) {
            source.sendSuccess(Component.literal("No " + type.name + " script "
                + (errors ? "errors" : "warnings")).withStyle(ChatFormatting.GREEN), false);
            return 0;
        }

        source.sendSuccess(Component.literal(type.name + " script "
                + (errors ? "errors" : "warnings") + " [" + messages.size() + "]")
            .withStyle(errors ? ChatFormatting.DARK_RED : ChatFormatting.GOLD), false);

        var index = 1;

        for (var message : messages) {
            source.sendSuccess(Component.literal(index++ + ") " + message)
                .withStyle(errors ? ChatFormatting.RED : ChatFormatting.YELLOW), false);
        }

        return messages.size();
    }

    private static int describeHeldItem(CommandSourceStack source) throws
        com.mojang.brigadier.exceptions.CommandSyntaxException {
        var stack = source.getPlayerOrException().getMainHandItem();
        var id = net.minecraftforge.registries.ForgeRegistries.ITEMS.getKey(stack.getItem());
        var text = (stack.getCount() > 1 ? stack.getCount() + "x " + id : String.valueOf(id))
            + (stack.hasTag() ? String.valueOf(stack.getTag()) : "");

        // Click to copy: the whole point of this command is pasting the result into a script.
        source.sendSuccess(Component.literal("'" + text + "'").withStyle(style -> style
            .withColor(ChatFormatting.AQUA)
            .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, "'" + text + "'"))
            .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                Component.literal("Click to copy")))), false);
        return 1;
    }

    private static ScriptType typeOf(String name) {
        for (var type : ScriptType.VALUES) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }

        return ScriptType.SERVER;
    }
}
