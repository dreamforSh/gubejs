package com.github.gubejs;

import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.item.IngredientJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.CustomCommandEventJS;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.registries.ForgeRegistries;

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
                .then(Commands.literal("client_scripts")
                    .executes(ctx -> reloadClient(ctx.getSource())))
                .then(Commands.literal("startup_scripts")
                    .executes(ctx -> reloadStartup(ctx.getSource())))
                .then(Commands.literal("config").executes(ctx -> reloadConfig(ctx.getSource())))
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
            .then(Commands.literal("hand")
                .executes(ctx -> describeHeldItem(ctx.getSource(), InteractionHand.MAIN_HAND)))
            .then(Commands.literal("offhand")
                .executes(ctx -> describeHeldItem(ctx.getSource(), InteractionHand.OFF_HAND)))
            .then(Commands.literal("hotbar").executes(ctx -> describeSlots(ctx.getSource(), 9)))
            .then(Commands.literal("inventory").executes(ctx -> describeSlots(ctx.getSource(), 36)))
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

    /**
     * Re-reads {@code config/gubejs/common.properties}.
     *
     * <p>Not everything in it takes effect: the engine settings are read as a script context is
     * built, so those wait for the next reload of whichever script type they belong to.
     */
    private static int reloadConfig(CommandSourceStack source) {
        CommonProperties.reload();
        source.sendSuccess(Component.literal(
            "Reloaded the config. Settings that configure the script engine itself apply from the "
                + "next script reload."), false);
        return 1;
    }

    /**
     * Reloads the resource packs, which is what carries client scripts.
     *
     * <p>Only possible in a client's own game. A command runs on the server, and a server cannot
     * make somebody else's client reload — so on a dedicated server this says what to press
     * instead.
     */
    private static int reloadClient(CommandSourceStack source) {
        if (!com.github.gubejs.bindings.PlatformWrapper.isClient()) {
            source.sendSuccess(Component.literal(
                "Client scripts belong to the client, and reload with its resource packs. "
                    + "Press F3+T there."), false);
            return 0;
        }

        // Behind the check and in a class of its own, because everything it touches is client-only
        // and naming it here would have a dedicated server try to load it.
        com.github.gubejs.client.GubejsClient.reloadResources();
        source.sendSuccess(Component.literal("Reloading the resource packs..."), false);
        return 1;
    }

    /**
     * Explains why startup scripts cannot be reloaded.
     *
     * <p>They run once, inside the mod's constructor, and what they do is create registry entries.
     * Those registries are frozen by the time a command can be typed, so running the scripts again
     * would either do nothing or fail halfway — leaving a pack in a state neither the author nor
     * this mod could describe. KubeJS runs them anyway; this says what would have to happen instead.
     */
    private static int reloadStartup(CommandSourceStack source) {
        source.sendSuccess(Component.literal(
            "Startup scripts run once, while the game loads, and the registries they add to are "
                + "closed afterwards. Restart the game to reload them."), false);
        return 0;
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

    /**
     * Describes what a player is holding, in the several ways a script could name it.
     *
     * <p>Every line is what a pack would actually write, and clicking one copies it. That is the
     * whole point of the command: the answer to "how do I refer to this" is rarely the item id
     * alone — it is as often the tag it shares with everything else of its kind, or the mod it
     * came from.
     *
     * @param source who ran the command
     * @param hand which hand to look at
     * @return 1
     * @throws com.mojang.brigadier.exceptions.CommandSyntaxException if a non-player ran it
     */
    private static int describeHeldItem(CommandSourceStack source, InteractionHand hand) throws
        com.mojang.brigadier.exceptions.CommandSyntaxException {
        var stack = source.getPlayerOrException().getItemInHand(hand);

        if (stack.isEmpty()) {
            source.sendFailure(Component.literal("Nothing in that hand"));
            return 0;
        }

        source.sendSuccess(Component.literal(hand == InteractionHand.MAIN_HAND
            ? "Item in hand:" : "Item in off hand:"), false);

        var described = describe(stack);
        source.sendSuccess(copyable(quoted(described), ChatFormatting.GREEN, "Item"), false);

        // The plain id as well, when the string above carries a count or NBT: a recipe usually
        // wants one and a give command the other, and working out which by hand is a nuisance.
        var id = String.valueOf(ForgeRegistries.ITEMS.getKey(stack.getItem()));

        if (!described.equals(id)) {
            source.sendSuccess(copyable(quoted(id), ChatFormatting.DARK_GREEN,
                "Item, ignoring the count and NBT"), false);
        }

        stack.getTags().map(tag -> tag.location().toString()).sorted().forEach(tag ->
            source.sendSuccess(copyable(quoted("#" + tag), ChatFormatting.YELLOW,
                "Tag, matching " + count(IngredientJS.parse("#" + tag)) + " items"), false));

        var mod = id.split(":")[0];
        source.sendSuccess(copyable(quoted("@" + mod), ChatFormatting.AQUA,
            "Mod, matching " + count(IngredientJS.ofMod(mod)) + " items"), false);
        return 1;
    }

    /**
     * Describes the first {@code slots} slots of a player's inventory.
     *
     * @param source who ran the command
     * @param slots how many slots to look at — nine for the hotbar, thirty-six for everything
     * @return how many slots had something in them
     * @throws com.mojang.brigadier.exceptions.CommandSyntaxException if a non-player ran it
     */
    private static int describeSlots(CommandSourceStack source, int slots) throws
        com.mojang.brigadier.exceptions.CommandSyntaxException {
        var inventory = source.getPlayerOrException().getInventory();
        var found = 0;

        for (var slot = 0; slot < Math.min(slots, inventory.items.size()); slot++) {
            var stack = inventory.items.get(slot);

            if (stack.isEmpty()) {
                continue;
            }

            source.sendSuccess(copyable(quoted(describe(stack)), ChatFormatting.GREEN,
                "Slot " + slot), false);
            found++;
        }

        if (found == 0) {
            source.sendFailure(Component.literal("Nothing there"));
        }

        return found;
    }

    /**
     * Returns the string a script would write for a stack — {@code '4x minecraft:stick{a:1}'}.
     *
     * <p>Through the cast, because the method belongs to an interface a mixin adds to
     * {@link net.minecraft.world.item.ItemStack} and the compiler cannot see it there.
     */
    private static String describe(net.minecraft.world.item.ItemStack stack) {
        return ((com.github.gubejs.core.ItemStackKJS) (Object) stack).toItemString();
    }

    /** Wraps text in the quotes a script would need around it. */
    private static String quoted(String text) {
        return "'" + text + "'";
    }

    /** How many items an ingredient matches, for the "is this tag the one I want" question. */
    private static int count(net.minecraft.world.item.crafting.Ingredient ingredient) {
        return ingredient.getItems().length;
    }

    /**
     * Builds a chat line that copies itself to the clipboard when clicked.
     *
     * @param text what to show and copy
     * @param color what colour to show it in
     * @param info what the line is, shown on hover
     * @return the component
     */
    private static Component copyable(String text, ChatFormatting color, String info) {
        return Component.literal("- ").withStyle(ChatFormatting.GRAY)
            .append(Component.literal(text).withStyle(style -> style
                .withColor(color)
                .withClickEvent(new net.minecraft.network.chat.ClickEvent(
                    net.minecraft.network.chat.ClickEvent.Action.COPY_TO_CLIPBOARD, text))
                .withHoverEvent(new net.minecraft.network.chat.HoverEvent(
                    net.minecraft.network.chat.HoverEvent.Action.SHOW_TEXT,
                    Component.literal(info + " — click to copy")))));
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
