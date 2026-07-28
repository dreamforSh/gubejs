/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/command/KubeJSCommands.java
 *
 * This program is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Lesser General Public License, version 3, as published by the Free Software
 * Foundation.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program. If not, see <https://www.gnu.org/licenses/>.
 */
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
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
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
                    .suggests(SCRIPT_TYPES)
                    .executes(ctx -> listMessagesFor(ctx.getSource(),
                        StringArgumentType.getString(ctx, "type"), true))))
            .then(Commands.literal("warnings")
                .executes(ctx -> listMessages(ctx.getSource(), ScriptType.SERVER, false))
                .then(Commands.argument("type", StringArgumentType.word())
                    .suggests(SCRIPT_TYPES)
                    .executes(ctx -> listMessagesFor(ctx.getSource(),
                        StringArgumentType.getString(ctx, "type"), false))))
            .then(Commands.literal("hand")
                .executes(ctx -> describeHeldItem(ctx.getSource(), InteractionHand.MAIN_HAND)))
            .then(Commands.literal("offhand")
                .executes(ctx -> describeHeldItem(ctx.getSource(), InteractionHand.OFF_HAND)))
            .then(Commands.literal("hotbar").executes(ctx -> describeSlots(ctx.getSource(), 9)))
            .then(Commands.literal("inventory").executes(ctx -> describeSlots(ctx.getSource(), 36)))
            .then(Commands.literal("custom_command")
                .then(Commands.argument("id", StringArgumentType.word())
                    .executes(ctx -> runCustomCommand(ctx.getSource(),
                        StringArgumentType.getString(ctx, "id")))))
            .then(Commands.literal("list_tag")
                // An id rather than a word or a quoted string: a bare word stops at '/' and ':', so
                // 'worldgen/biome' and 'forge:biome_modifiers' could not be typed at all, and a
                // quoted string would make every registry name need quotes. A registry name is a
                // resource location, so the game's own argument type accepts exactly the set that
                // resolves.
                .then(Commands.argument("registry", ResourceLocationArgument.id())
                    .suggests(REGISTRIES)
                    .then(Commands.argument("tag", StringArgumentType.string())
                        .suggests(TAGS)
                        .executes(ctx -> listTag(ctx.getSource(),
                            ResourceLocationArgument.getId(ctx, "registry"),
                            StringArgumentType.getString(ctx, "tag"))))))
            .then(Commands.literal("dump_registry")
                .then(Commands.argument("registry", ResourceLocationArgument.id())
                    .suggests(REGISTRIES)
                    .executes(ctx -> dumpRegistry(ctx.getSource(),
                        ResourceLocationArgument.getId(ctx, "registry")))))
            .then(Commands.literal("export").executes(ctx -> export(ctx.getSource())))
            .then(Commands.literal("stages")
                .then(Commands.literal("list")
                    .executes(ctx -> listStages(ctx.getSource(),
                        ctx.getSource().getPlayerOrException()))
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> listStages(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player")))))
                .then(Commands.literal("add")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("stage", StringArgumentType.word())
                            .executes(ctx -> setStage(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "stage"), true)))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("stage", StringArgumentType.word())
                            .suggests(PLAYER_STAGES)
                            .executes(ctx -> setStage(ctx.getSource(),
                                EntityArgument.getPlayer(ctx, "player"),
                                StringArgumentType.getString(ctx, "stage"), false)))))
                .then(Commands.literal("clear")
                    .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> clearStages(ctx.getSource(),
                            EntityArgument.getPlayer(ctx, "player"))))))
            .then(Commands.literal("pack_stages")
                .then(Commands.literal("list").executes(ctx -> listPackStages(ctx.getSource())))
                .then(Commands.literal("add")
                    .then(Commands.argument("stage", StringArgumentType.word())
                        .executes(ctx -> setPackStage(ctx.getSource(),
                            StringArgumentType.getString(ctx, "stage"), true))))
                .then(Commands.literal("remove")
                    .then(Commands.argument("stage", StringArgumentType.word())
                        .suggests(PACK_STAGES)
                        .executes(ctx -> setPackStage(ctx.getSource(),
                            StringArgumentType.getString(ctx, "stage"), false))))
                .then(Commands.literal("clear")
                    .executes(ctx -> clearPackStages(ctx.getSource())))));
    }

    // --- tags and registries -------------------------------------------------------------------

    /**
     * Lists what a tag currently matches.
     *
     * <p>What a pack author is really asking is "is this the tag I want", and the only honest answer
     * is the list of things in it after every datapack and every mod has had its say. Each line
     * copies itself, since the next step is usually to paste one into a recipe.
     *
     * @param source who ran the command
     * @param registryName the registry the tag belongs to, e.g. {@code item}
     * @param tagName the tag id, with or without a leading {@code #}
     * @return how many entries the tag has
     */
    private static int listTag(CommandSourceStack source,
                               net.minecraft.resources.ResourceLocation registryName,
                               String tagName) {
        var registry = registryOf(source.registryAccess(), registryName);

        if (registry == null) {
            source.sendFailure(Component.literal("There is no registry called '" + registryName
                + "'. Try: item, block, fluid, entity_type, worldgen/biome"));
            return 0;
        }

        var trimmed = tagName.startsWith("#") ? tagName.substring(1) : tagName;
        var tagId = net.minecraft.resources.ResourceLocation.tryParse(
            trimmed.indexOf(':') == -1 ? "minecraft:" + trimmed : trimmed);

        if (tagId == null) {
            source.sendFailure(Component.literal("Not a tag id: '" + tagName + "'"));
            return 0;
        }

        return listTagOf(source, registry, tagId);
    }

    /**
     * Lists one tag of one registry.
     *
     * <p>Generic because a {@link net.minecraft.tags.TagKey} has to agree with the registry it is
     * looked up in, and a {@code Registry<?>} cannot state that agreement — the type variable is
     * what carries it, and capture conversion supplies it here.
     */
    private static <T> int listTagOf(CommandSourceStack source,
                                     net.minecraft.core.Registry<T> registry,
                                     net.minecraft.resources.ResourceLocation tagId) {
        var tag = registry.getTag(net.minecraft.tags.TagKey.create(registry.key(), tagId));

        if (tag.isEmpty()) {
            source.sendFailure(Component.literal("There is no '#" + tagId + "' tag in "
                + registry.key().location() + ". A tag no datapack fills does not exist at all, "
                + "which is the same thing the game tells a recipe that asks for one."));
            return 0;
        }

        var ids = new java.util.ArrayList<String>();

        for (var holder : tag.get()) {
            holder.unwrapKey().ifPresent(key -> ids.add(key.location().toString()));
        }

        java.util.Collections.sort(ids);

        source.sendSuccess(Component.literal("#" + tagId + " in " + registry.key().location()
            + " [" + ids.size() + "]").withStyle(ChatFormatting.GOLD), false);

        for (var id : ids) {
            source.sendSuccess(copyable(quoted(id), ChatFormatting.GREEN, "Entry"), false);
        }

        return ids.size();
    }

    /**
     * Writes every id in a registry to a file.
     *
     * <p>A file rather than chat, because the interesting registries have thousands of entries and
     * chat keeps a few hundred lines. The path is printed and opens on click.
     *
     * @param source who ran the command
     * @param registryName the registry, e.g. {@code item}
     * @return how many entries were written
     */
    private static int dumpRegistry(CommandSourceStack source,
                                    net.minecraft.resources.ResourceLocation registryName) {
        var registry = registryOf(source.registryAccess(), registryName);

        if (registry == null) {
            source.sendFailure(Component.literal("There is no registry called '" + registryName
                + "'"));
            return 0;
        }

        var ids = new java.util.ArrayList<String>();
        registry.keySet().forEach(id -> ids.add(id.toString()));
        java.util.Collections.sort(ids);

        var name = registry.key().location().toString().replace(':', '_').replace('/', '_');
        var file = GubejsPaths.EXPORT.resolve(name + ".txt");

        try {
            java.nio.file.Files.write(file, ids);
        } catch (java.io.IOException ex) {
            Gubejs.LOGGER.error("Could not write " + file, ex);
            source.sendFailure(Component.literal("Could not write " + file + ": "
                + ex.getMessage()));
            return 0;
        }

        source.sendSuccess(Component.literal("Wrote " + ids.size() + " ids to ")
            .append(Component.literal(file.getFileName().toString()).withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                    file.toAbsolutePath().toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal(file.toAbsolutePath().toString()))))), true);
        return ids.size();
    }

    /**
     * Writes what the game ended up with to files, for reading outside it.
     *
     * <p>The command a pack author reaches for when a recipe is not behaving: the recipes are dumped
     * as the game holds them, after every script and every datapack, along with the item, block and
     * fluid lists and the tags of each. Chat cannot hold any of that, and the answer to "what is
     * actually loaded" is not something a script can be trusted to summarise.
     *
     * @param source who ran the command
     * @return how many files were written
     */
    private static int export(CommandSourceStack source) {
        var server = source.getServer();
        var root = GubejsPaths.EXPORT;
        var written = 0;

        try {
            written += exportRecipes(server, root);
            written += exportRegistry(source, root, "items",
                net.minecraft.core.Registry.ITEM_REGISTRY);
            written += exportRegistry(source, root, "blocks",
                net.minecraft.core.Registry.BLOCK_REGISTRY);
            written += exportRegistry(source, root, "fluids",
                net.minecraft.core.Registry.FLUID_REGISTRY);
            written += exportRegistry(source, root, "entity_types",
                net.minecraft.core.Registry.ENTITY_TYPE_REGISTRY);
        } catch (Exception ex) {
            Gubejs.LOGGER.error("Could not export", ex);
            source.sendFailure(Component.literal("Could not export: " + ex));
            return 0;
        }

        var count = written;
        source.sendSuccess(Component.literal("Exported " + count + " file(s) to ")
            .append(Component.literal("export").withStyle(style -> style
                .withColor(ChatFormatting.GREEN)
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                    root.toAbsolutePath().toString()))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                    Component.literal(root.toAbsolutePath().toString()))))), true);
        return count;
    }

    /**
     * Writes every loaded recipe, grouped by type, and one file listing the types.
     *
     * <p>The recipe objects rather than the files they came from: a recipe a mod added in code never
     * was a file, and a recipe another script has rewritten is no longer the file it was. What is
     * written is the id, the type and the result, which is what a pack author is looking for — the
     * whole of a recipe's contents cannot be read back out of a deserialised recipe of an unknown
     * type, and pretending otherwise would produce files that are wrong in a way nobody could see.
     */
    private static int exportRecipes(net.minecraft.server.MinecraftServer server,
                                     java.nio.file.Path root) throws java.io.IOException {
        var byType = new java.util.TreeMap<String, java.util.List<String>>();

        for (var recipe : server.getRecipeManager().getRecipes()) {
            var type = String.valueOf(
                net.minecraft.core.Registry.RECIPE_TYPE.getKey(recipe.getType()));
            var result = recipe.getResultItem();
            byType.computeIfAbsent(type, t -> new java.util.ArrayList<>())
                .add(recipe.getId() + " -> " + (result.isEmpty() ? "(decided when crafted)"
                    : result.getCount() + "x " + net.minecraftforge.registries.ForgeRegistries.ITEMS
                    .getKey(result.getItem())));
        }

        var directory = root.resolve("recipes");
        java.nio.file.Files.createDirectories(directory);
        var summary = new java.util.ArrayList<String>();
        var written = 0;

        for (var entry : byType.entrySet()) {
            var lines = entry.getValue();
            java.util.Collections.sort(lines);
            summary.add(entry.getValue().size() + "\t" + entry.getKey());
            java.nio.file.Files.write(
                directory.resolve(entry.getKey().replace(':', '_').replace('/', '_') + ".txt"),
                lines);
            written++;
        }

        java.nio.file.Files.write(root.resolve("recipe_types.txt"), summary);
        return written + 1;
    }

    /** Writes one registry's ids, and the tags of each, as two files. */
    private static <T> int exportRegistry(CommandSourceStack source, java.nio.file.Path root,
                                          String name,
                                          net.minecraft.resources.ResourceKey<
                                              net.minecraft.core.Registry<T>> key)
        throws java.io.IOException {
        var registry = source.registryAccess().registry(key).orElse(null);

        if (registry == null) {
            return 0;
        }

        var ids = new java.util.ArrayList<String>();
        registry.keySet().forEach(id -> ids.add(id.toString()));
        java.util.Collections.sort(ids);
        java.nio.file.Files.write(root.resolve(name + ".txt"), ids);

        var tags = new java.util.ArrayList<String>();

        registry.getTagNames().forEach(tag -> {
            var members = new java.util.ArrayList<String>();
            registry.getTag(tag).ifPresent(holders -> holders.forEach(holder ->
                holder.unwrapKey().ifPresent(id -> members.add(id.location().toString()))));
            java.util.Collections.sort(members);
            tags.add("#" + tag.location() + " [" + members.size() + "]");
            members.forEach(member -> tags.add("\t" + member));
        });

        java.nio.file.Files.write(root.resolve(name + "_tags.txt"), tags);
        return 2;
    }

    /**
     * Finds a registry by the name a script would use.
     *
     * <p>Through the command source's registries rather than the static root registry, because the
     * root holds only half of what the command is asked about. A dynamic registry — {@code
     * worldgen/biome}, {@code worldgen/structure}, {@code dimension_type} — is built per world out
     * of the datapacks and never enters the root, which left the tag most worth listing, a biome
     * tag, answering "there is no registry called that". A {@code RegistryAccess} is asked about
     * its own registries first and falls back to the root, so items, blocks and every Forge
     * registry with a root wrapper resolve exactly as they did.
     *
     * @param registries the registries the command is running against
     * @param name the registry id, whose namespace is {@code minecraft} unless it says otherwise
     * @return the registry, or {@code null} if nothing goes by that name
     */
    @org.jetbrains.annotations.Nullable
    private static net.minecraft.core.Registry<?> registryOf(
        net.minecraft.core.RegistryAccess registries,
        net.minecraft.resources.ResourceLocation name) {
        net.minecraft.resources.ResourceKey<net.minecraft.core.Registry<Object>> key =
            net.minecraft.resources.ResourceKey.createRegistryKey(name);
        return registries.registry(key).orElse(null);
    }

    // --- stages --------------------------------------------------------------------------------

    /**
     * Lists a player's stages.
     *
     * @param source who ran the command
     * @param player whose stages to list
     * @return how many they have
     */
    private static int listStages(CommandSourceStack source,
                                  net.minecraft.server.level.ServerPlayer player) {
        var stages = new com.github.gubejs.core.StageManager(player).getAll();

        if (stages.isEmpty()) {
            source.sendSuccess(Component.literal(player.getGameProfile().getName()
                + " has no stages"), false);
            return 0;
        }

        source.sendSuccess(Component.literal(player.getGameProfile().getName() + "'s stages ["
            + stages.size() + "]").withStyle(ChatFormatting.GOLD), false);

        for (var stage : stages) {
            source.sendSuccess(copyable(quoted(stage), ChatFormatting.GREEN, "Stage"), false);
        }

        return stages.size();
    }

    /**
     * Gives a player a stage, or takes it away.
     *
     * <p>Through {@link com.github.gubejs.core.StageManager} rather than by writing the data
     * directly, so the change is stored where death does not lose it, synced to that player's
     * client, and announced to {@code GameStageEvents} — a command that skipped any of the three
     * would leave the game in a state no script could have produced.
     *
     * @param add whether to give the stage rather than take it
     * @return 1 if anything changed
     */
    private static int setStage(CommandSourceStack source,
                                net.minecraft.server.level.ServerPlayer player, String stage,
                                boolean add) {
        var stages = new com.github.gubejs.core.StageManager(player);
        var changed = add ? stages.add(stage) : stages.remove(stage);
        var name = player.getGameProfile().getName();

        if (!changed) {
            source.sendFailure(Component.literal(name + (add ? " already has '" : " does not have '")
                + stage + "'"));
            return 0;
        }

        source.sendSuccess(Component.literal((add ? "Gave " : "Took ") + stage + (add ? " to " : " from ")
            + name), true);
        return 1;
    }

    private static int clearStages(CommandSourceStack source,
                                   net.minecraft.server.level.ServerPlayer player) {
        var removed = new com.github.gubejs.core.StageManager(player).clear();
        source.sendSuccess(Component.literal("Removed " + removed + " stage(s) from "
            + player.getGameProfile().getName()), true);
        return removed;
    }

    /**
     * Lists the stages the whole pack has reached.
     *
     * @return how many are set
     */
    private static int listPackStages(CommandSourceStack source) {
        var stages = com.github.gubejs.core.PackStages.getAll();

        if (stages.isEmpty()) {
            source.sendSuccess(Component.literal("The pack has no stages"), false);
            return 0;
        }

        source.sendSuccess(Component.literal("Pack stages [" + stages.size() + "]")
            .withStyle(ChatFormatting.GOLD), false);

        for (var stage : stages) {
            source.sendSuccess(copyable(quoted(stage), ChatFormatting.GREEN, "Pack stage"), false);
        }

        return stages.size();
    }

    /**
     * Sets or unsets a stage for the whole pack.
     *
     * <p>The recipes gated on it change at the next datapack reload, not now, and the message says
     * so: a recipe condition is asked once while the recipe is read, so a pack author who is not
     * told would conclude the command did nothing.
     *
     * @param add whether to set the stage rather than unset it
     * @return 1 if anything changed
     */
    private static int setPackStage(CommandSourceStack source, String stage, boolean add) {
        var changed = add ? com.github.gubejs.core.PackStages.add(stage)
            : com.github.gubejs.core.PackStages.remove(stage);

        if (!changed) {
            source.sendFailure(Component.literal("The pack " + (add ? "already has '" : "does not have '")
                + stage + "'"));
            return 0;
        }

        source.sendSuccess(Component.literal((add ? "Set" : "Unset") + " pack stage '" + stage
            + "'. Recipes gated on it change at the next ")
            .append(Component.literal("/gubejs reload").withStyle(style -> style
                .withColor(ChatFormatting.AQUA)
                .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,
                    "/gubejs reload")))), true);
        return 1;
    }

    private static int clearPackStages(CommandSourceStack source) {
        var removed = com.github.gubejs.core.PackStages.clear();
        source.sendSuccess(Component.literal("Removed " + removed + " pack stage(s)"), true);
        return removed;
    }

    /**
     * Completes the registry argument, with the vanilla namespace left off where it is implied.
     *
     * <p>Both halves of what {@link #registryOf} resolves, so completion can neither offer a name the
     * command will reject nor hide one it would have accepted — which is how the dynamic registries
     * stayed a secret. The world's own registries hold the dynamic ones and the root holds the rest;
     * a set merges them, since a registry that is in both would otherwise be offered twice.
     */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
        REGISTRIES = (ctx, builder) -> {
            var names = new java.util.TreeSet<String>();

            ctx.getSource().registryAccess().registries()
                .forEach(entry -> names.add(shortName(entry.key().location())));
            net.minecraft.core.Registry.REGISTRY.keySet()
                .forEach(id -> names.add(shortName(id)));

            for (var name : names) {
                builder.suggest(name);
            }

            return builder.buildFuture();
        };

    /** Leaves the vanilla namespace off a registry id, since the command assumes it. */
    private static String shortName(net.minecraft.resources.ResourceLocation id) {
        return id.getNamespace().equals("minecraft") ? id.getPath() : id.toString();
    }

    /** Completes the tag argument, from the tags the chosen registry actually has. */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
        TAGS = (ctx, builder) -> {
            var registry = registryOf(ctx.getSource().registryAccess(),
                ResourceLocationArgument.getId(ctx, "registry"));

            if (registry != null) {
                registry.getTagNames().forEach(tag ->
                    builder.suggest("\"" + tag.location() + "\""));
            }

            return builder.buildFuture();
        };

    /** Completes a stage the named player actually has, since those are the removable ones. */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
        PLAYER_STAGES = (ctx, builder) -> {
            try {
                new com.github.gubejs.core.StageManager(EntityArgument.getPlayer(ctx, "player"))
                    .getAll().forEach(builder::suggest);
            } catch (com.mojang.brigadier.exceptions.CommandSyntaxException ignored) {
                // Half-typed player name; there is nothing to complete from yet.
            }

            return builder.buildFuture();
        };

    /** Completes a pack stage that is currently set. */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
        PACK_STAGES = (ctx, builder) -> {
            com.github.gubejs.core.PackStages.getAll().forEach(builder::suggest);
            return builder.buildFuture();
        };

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

        var server = source.getServer();
        var startedAt = System.currentTimeMillis();

        // The vanilla reload's own steps rather than '/reload' itself, for one reason: running the
        // command hands back nothing, and what this needs is the future. A reload is asynchronous,
        // so without it there is no moment at which to say whether it worked -- which left the
        // command saying "reloading" and then nothing at all, however long it took or however
        // badly it went.
        //
        // Through the whole datapack reload rather than the scripts alone, as before: recipes and
        // tags are rebuilt from the scripts, and reloading one without the other would leave the
        // two disagreeing.
        server.reloadResources(discoverPacks(server))
            .handleAsync((ignored, error) -> {
                if (error != null) {
                    Gubejs.LOGGER.error("Failed to reload", error);
                    source.sendFailure(Component.literal(
                        "The reload failed: " + rootCauseOf(error)
                            + ". The server is still running what it had loaded before; "
                            + "see logs/latest.log."));
                } else {
                    reportReload(source, System.currentTimeMillis() - startedAt);
                }

                return null;
            }, server);

        return 1;
    }

    /**
     * The packs the reload should use, including any that appeared on disk since the last one.
     *
     * <p>What {@code /reload} does before reloading, repeated here because that logic is private to
     * the vanilla command and dropping it would mean a pack added while the server ran stayed
     * invisible until a restart.
     */
    private static java.util.Collection<String> discoverPacks(net.minecraft.server.MinecraftServer server) {
        var repository = server.getPackRepository();
        repository.reload();

        var selected = new java.util.ArrayList<>(repository.getSelectedIds());
        var disabled = server.getWorldData().getDataPackConfig().getDisabled();

        for (var id : repository.getAvailableIds()) {
            if (!disabled.contains(id) && !selected.contains(id)) {
                selected.add(id);
            }
        }

        return selected;
    }

    /**
     * Says the reload finished, and what the scripts had to say about it.
     *
     * <p>The counts are the point. A reload that "worked" but logged forty errors is not a reload
     * that worked, and a pack author watching chat should not have to go and ask.
     *
     * @param source who asked for the reload
     * @param millis how long it took
     */
    private static void reportReload(CommandSourceStack source, long millis) {
        var seconds = millis / 1000D;
        var errors = ScriptType.SERVER.errors.size();
        var warnings = ScriptType.SERVER.warnings.size();

        if (errors == 0 && warnings == 0) {
            source.sendSuccess(Component.literal("Reloaded in " + seconds + "s")
                .withStyle(ChatFormatting.GREEN), true);
            return;
        }

        source.sendSuccess(Component.literal("Reloaded in " + seconds + "s, with "
                + errors + " error(s) and " + warnings + " warning(s)")
            .withStyle(errors > 0 ? ChatFormatting.RED : ChatFormatting.GOLD), true);

        // Clickable, so the next step is one click rather than a command to remember.
        if (errors > 0) {
            source.sendSuccess(ScriptType.SERVER.errorsComponent("/gubejs errors server_scripts"),
                false);
        }

        if (warnings > 0) {
            source.sendSuccess(
                ScriptType.SERVER.warningsComponent("/gubejs warnings server_scripts"), false);
        }
    }

    /** The message worth showing out of a chain of wrapper exceptions. */
    private static String rootCauseOf(Throwable error) {
        var cause = error;

        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }

        return cause.getMessage() == null ? cause.toString() : cause.getMessage();
    }

    /**
     * Re-reads the property files under {@code config/}.
     *
     * <p>Not everything in them takes effect: the engine settings are read as a script context is
     * built, so those wait for the next reload of whichever script type they belong to. The logging
     * switches in {@code dev.properties} apply from the next datapack reload, which is the next time
     * anything they describe happens.
     */
    private static int reloadConfig(CommandSourceStack source) {
        CommonProperties.reload();
        DevProperties.reload();
        // Read from disk like the properties are, and edited from outside the game just as often --
        // a pack author turning a stage on to see what unlocks does it in a text editor.
        com.github.gubejs.core.PackStages.reload();
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
            source.sendSuccess(Component.literal(errors ? "No errors found!" : "No warnings found!")
                .withStyle(ChatFormatting.GREEN), false);
            // "No errors" is not the same as "nothing to look at", and a pack author who stopped
            // reading there would miss the warnings entirely.
            pointAtWarnings(source, type, errors);
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

        if (errors) {
            // Chat truncates a stack trace and a long one scrolls away; the file has all of it.
            source.sendSuccess(Component.literal("More info in ")
                .append(Component.literal("'logs/" + Gubejs.MOD_ID + "/" + type.name + ".log'")
                    .withStyle(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE,
                            type.getLogFile().toAbsolutePath().toString()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            Component.literal(type.getLogFile().toAbsolutePath().toString())))))
                .withStyle(ChatFormatting.DARK_RED), false);
        }

        pointAtWarnings(source, type, errors);
        return messages.size();
    }

    /**
     * Adds the "and there are warnings too" line after a list of errors.
     *
     * <p>Only after the errors: listing the warnings and then pointing at the warnings would be
     * telling somebody what they are already looking at.
     *
     * @param wasErrors whether the list just printed was the errors
     */
    private static void pointAtWarnings(CommandSourceStack source, ScriptType type,
                                        boolean wasErrors) {
        if (wasErrors && !type.warnings.isEmpty()) {
            source.sendSuccess(type.warningsComponent("/gubejs warnings " + type.name), false);
        }
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

    /**
     * Reads a script type from what was typed.
     *
     * <p>Both spellings, because the rest of the command tree uses the longer one: somebody who has
     * just typed {@code /gubejs reload startup_scripts} will type {@code /gubejs errors
     * startup_scripts} next, and being handed the server's errors instead — silently, which is what
     * this used to do — is worse than being told the name was not understood.
     *
     * @param name what was typed
     * @return the type, or {@code null} if nothing goes by that name
     */
    @org.jetbrains.annotations.Nullable
    private static ScriptType typeOf(String name) {
        for (var type : ScriptType.VALUES) {
            if (type.name.equalsIgnoreCase(name) || (type.name + "_scripts").equalsIgnoreCase(name)) {
                return type;
            }
        }

        return null;
    }

    /** Completes the script type argument, in both the spellings {@link #typeOf} accepts. */
    private static final com.mojang.brigadier.suggestion.SuggestionProvider<CommandSourceStack>
        SCRIPT_TYPES = (ctx, builder) -> {
            for (var type : ScriptType.VALUES) {
                builder.suggest(type.name + "_scripts");
            }

            return builder.buildFuture();
        };

    /**
     * Lists one script type's messages, or says why it cannot.
     *
     * @param name the script type as it was typed
     * @param errors whether to list errors rather than warnings
     * @return how many were listed
     */
    private static int listMessagesFor(CommandSourceStack source, String name, boolean errors) {
        var type = typeOf(name);

        if (type == null) {
            var known = new java.util.ArrayList<String>();

            for (var known_type : ScriptType.VALUES) {
                known.add(known_type.name + "_scripts");
            }

            source.sendFailure(Component.literal("There is no script type called '" + name
                + "'. Try one of: " + String.join(", ", known)));
            return 0;
        }

        return listMessages(source, type, errors);
    }
}
