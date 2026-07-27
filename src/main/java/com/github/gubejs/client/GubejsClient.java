/*
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Gubejs - KubeJS-compatible scripting for Minecraft, on GraalJS
 * Copyright (C) 2026 xinian and Gubejs contributors
 *
 * This file is derived from KubeJS (branch 1902),
 * Copyright (C) LatvianModder and KubeJS contributors, originally at
 * common/src/main/java/dev/latvian/mods/kubejs/client/KubeJSClient.java
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
package com.github.gubejs.client;

import com.github.gubejs.Gubejs;
import com.github.gubejs.GubejsPlugin;
import com.github.gubejs.bindings.event.ClientEvents;
import com.github.gubejs.bindings.event.ItemEvents;
import com.github.gubejs.item.ItemTooltipEventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.GubejsPlugins;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Everything that only exists on a client.
 *
 * <p>Client scripts reload with the resource packs, so unlike server scripts they can simply be a
 * reload listener: nothing vanilla needs them to have run first.
 */
@Mod.EventBusSubscriber(modid = Gubejs.MOD_ID, value = Dist.CLIENT)
public final class GubejsClient {

    private GubejsClient() {
    }

    /**
     * Wires up the client side.
     *
     * @param modBus the mod event bus, for the reload listener registration
     */
    /**
     * Reloads the resource packs, and with them the client scripts.
     *
     * <p>Queued onto the render thread rather than run where it is called: {@code /gubejs reload
     * client_scripts} is a command, and a command runs on the server thread even in a single
     * player game.
     */
    public static void reloadResources() {
        var minecraft = net.minecraft.client.Minecraft.getInstance();
        minecraft.execute(minecraft::reloadResourcePacks);
    }

    public static void init(IEventBus modBus) {
        modBus.addListener(GubejsClient::registerReloadListener);
        modBus.addListener(GubejsClient::clientSetup);
        modBus.addListener(GubejsParticles::register);
        // Here rather than beside the other pack finders, which are registered from the common
        // entry point: everything this pack touches is client-only, and naming the class from
        // there would have a dedicated server try to load it.
        modBus.addListener(VirtualAssetPack::register);
        modBus.addListener(GubejsClient::stitchAtlas);
        MinecraftForge.EVENT_BUS.register(GubejsClient.class);
        GubejsPlugins.forEachPlugin(GubejsPlugin::clientInit);
    }

    /** Whether {@link VirtualAssetPack} already loaded the scripts for the reload in progress. */
    private static boolean loadedByPack;

    private static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) GubejsClient::reloadScripts);
    }

    /**
     * Loads the client scripts if nothing has yet during this reload.
     *
     * <p>Called by {@link VirtualAssetPack}, which is opened before any reload listener runs and
     * needs listeners registered before it can ask them for anything. On the very first reload
     * there are none, so it loads them here — from the pack directory only, since the resource
     * manager being built is not something a pack being opened for it can read from.
     *
     * <p>{@link #reloadScripts} then skips its own load, so the scripts run once per reload rather
     * than twice. Every reload after the first finds them already loaded and leaves them to the
     * listener, which does have the resource manager and so picks up the scripts other mods ship.
     */
    static void ensureScriptsLoaded() {
        var manager = Gubejs.getClientScriptManager();

        if (manager == null || manager.isLoaded()) {
            return;
        }

        manager.reload(null);
        loadedByPack = true;
    }

    private static void reloadScripts(ResourceManager resourceManager) {
        if (loadedByPack) {
            loadedByPack = false;
        } else {
            Gubejs.getClientScriptManager().reload(resourceManager);
        }

        ScriptType.CLIENT.console.flush();
    }

    /**
     * Posts {@code ClientEvents.init} once the client is set up.
     *
     * <p>A startup event, so it is the startup context that runs it — client scripts have not been
     * read at this point and would have nothing to run.
     *
     * @param event Forge's client setup event
     */
    private static void clientSetup(FMLClientSetupEvent event) {
        if (ClientEvents.INIT.hasListeners()) {
            event.enqueueWork(() ->
                ClientEvents.INIT.post(ScriptType.STARTUP, new ClientInitEventJS()));
        }

        // Here rather than beside the other startup events, for the same reason this class exists:
        // what it registers lives in the item renderer, which a dedicated server does not have.
        if (ItemEvents.MODEL_PROPERTIES.hasListeners()) {
            event.enqueueWork(() -> ItemEvents.MODEL_PROPERTIES.post(ScriptType.STARTUP,
                new ItemModelPropertiesEventJS()));
        }

        event.enqueueWork(GubejsClient::registerRenderTypes);
    }

    /**
     * Puts each created block in the render pass its builder asked for.
     *
     * <p>Client-side and after registration, because the map this writes to lives in the renderer
     * and is keyed by the block object, which does not exist until the block registry has been
     * filled. A block left out of it is drawn in the solid pass, where transparency in its texture
     * comes out black.
     */
    private static void registerRenderTypes() {
        for (var builder : com.github.gubejs.registry.RegistryInfo.BLOCK.getBuilders()) {
            if (!(builder instanceof com.github.gubejs.block.BlockBuilder blockBuilder)) {
                continue;
            }

            var type = renderTypeOf(blockBuilder.getRenderType());

            if (type != null) {
                net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(
                    blockBuilder.get(), type);
            }
        }
    }

    /**
     * Returns the render pass a script named.
     *
     * @param name the pass name
     * @return the pass, or {@code null} for the solid pass every block is in already
     */
    @org.jetbrains.annotations.Nullable
    private static net.minecraft.client.renderer.RenderType renderTypeOf(String name) {
        return switch (name) {
            case "solid" -> null;
            case "cutout" -> net.minecraft.client.renderer.RenderType.cutout();
            case "cutout_mipped" -> net.minecraft.client.renderer.RenderType.cutoutMipped();
            case "translucent" -> net.minecraft.client.renderer.RenderType.translucent();
            case "tripwire" -> net.minecraft.client.renderer.RenderType.tripwire();
            default -> {
                com.github.gubejs.util.ConsoleJS.STARTUP.warn("There is no render type called '"
                    + name + "'; the block will be drawn in the solid pass");
                yield null;
            }
        };
    }

    /**
     * Lets a script add a texture to an atlas as it is stitched.
     *
     * <p>The atlas is named by the path its texture sits at — {@code minecraft:textures/atlas/
     * blocks.png} — and a pack names it {@code minecraft:blocks}, which is what the game calls it
     * everywhere else. The full path is accepted too, since a script reading the log will have
     * seen that form.
     *
     * @param event Forge's texture stitching event, fired once per atlas
     */
    private static void stitchAtlas(net.minecraftforge.client.event.TextureStitchEvent.Pre event) {
        if (!ClientEvents.ATLAS_SPRITE_REGISTRY.hasListeners()) {
            return;
        }

        var full = event.getAtlas().location();
        var path = full.getPath();

        if (path.startsWith("textures/atlas/") && path.endsWith(".png")) {
            path = path.substring("textures/atlas/".length(), path.length() - ".png".length());
        }

        var wrapped = new AtlasSpriteRegistryEventJS(event);
        var shortId = new net.minecraft.resources.ResourceLocation(full.getNamespace(), path);

        ClientEvents.ATLAS_SPRITE_REGISTRY.post(ScriptType.CLIENT, shortId, wrapped);

        if (!shortId.equals(full)) {
            ClientEvents.ATLAS_SPRITE_REGISTRY.post(ScriptType.CLIENT, full, wrapped);
        }
    }

    /**
     * Copies the stage list the server just sent onto the client's own player object.
     *
     * <p>Without this, {@code player.stages} on the client would answer from an empty tag: a
     * player's persistent data is server-side state and nothing syncs it. A tooltip or HUD element
     * that gates on a stage needs the answer on the side that draws it.
     *
     * @param data the payload of the internal stages message
     */
    public static void applyStages(net.minecraft.nbt.CompoundTag data) {
        var player = net.minecraft.client.Minecraft.getInstance().player;

        if (player == null) {
            return;
        }

        var persistent = player.getPersistentData();

        if (!persistent.contains("PlayerPersisted", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            persistent.put("PlayerPersisted", new net.minecraft.nbt.CompoundTag());
        }

        var stages = new com.github.gubejs.core.StageManager(player);
        var before = stages.getAll();

        persistent.getCompound("PlayerPersisted")
            .put("gubejs:stages", data.getList("stages", net.minecraft.nbt.Tag.TAG_STRING));

        announceStageChanges(player, before, stages.getAll());
    }

    /**
     * Fires the stage events for whatever this message changed.
     *
     * <p>The server sends the whole list rather than a change, because a client that just
     * connected has no list to apply a change to. So the change is worked out here — which is what
     * lets {@code GameStageEvents} fire in a client script at all, and what keeps it a common event
     * rather than a server one that quietly does nothing on the other side.
     *
     * @param player the local player
     * @param before what they had
     * @param after what they have now
     */
    private static void announceStageChanges(net.minecraft.world.entity.player.Player player,
                                             java.util.List<String> before,
                                             java.util.List<String> after) {
        if (before.equals(after)) {
            return;
        }

        for (var stage : after) {
            if (!before.contains(stage)) {
                com.github.gubejs.core.StageManager.announce(player,
                    com.github.gubejs.bindings.event.GameStageEvents.ADDED, stage);
            }
        }

        for (var stage : before) {
            if (!after.contains(stage)) {
                com.github.gubejs.core.StageManager.announce(player,
                    com.github.gubejs.bindings.event.GameStageEvents.REMOVED, stage);
            }
        }
    }

    // --- game events -------------------------------------------------------------------------

    /**
     * Hands an item's tooltip to {@code ItemEvents.tooltip}.
     *
     * <p>Static and on the Forge bus rather than registered in {@link #init}, so it is subscribed
     * before the first tooltip is ever drawn.
     *
     * @param event Forge's tooltip event
     */
    @SubscribeEvent
    public static void itemTooltip(ItemTooltipEvent event) {
        if (!ItemEvents.TOOLTIP.hasListeners()) {
            return;
        }

        var stack = event.getItemStack();

        ItemEvents.TOOLTIP.post(ScriptType.CLIENT, stack.getItem(), new ItemTooltipEventJS(
            stack, event.getToolTip(), event.getFlags().isAdvanced()));
    }

    @SubscribeEvent
    public static void loggedIn(ClientPlayerNetworkEvent.LoggingIn event) {
        if (ClientEvents.LOGGED_IN.hasListeners()) {
            ClientEvents.LOGGED_IN.post(ScriptType.CLIENT, new ClientEventJS());
        }
    }

    @SubscribeEvent
    public static void loggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        if (ClientEvents.LOGGED_OUT.hasListeners()) {
            ClientEvents.LOGGED_OUT.post(ScriptType.CLIENT, new ClientEventJS());
        }
    }

    @SubscribeEvent
    public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ClientEvents.TICK.hasListeners()) {
            ClientEvents.TICK.post(ScriptType.CLIENT, new ClientEventJS());
        }
    }

    /**
     * Draws whatever a script asked to have on screen.
     *
     * <p>After the game's own interface rather than in place of any part of it, so nothing a
     * script draws can hide the hotbar or the health bar by accident.
     *
     * @param event Forge's post-GUI render event
     */
    @SubscribeEvent
    public static void renderOverlay(net.minecraftforge.client.event.RenderGuiEvent.Post event) {
        var painter = com.github.gubejs.client.painter.Painter.INSTANCE;

        if (!painter.isEmpty()) {
            painter.draw(event.getPoseStack(), event.getWindow().getGuiScaledWidth(),
                event.getWindow().getGuiScaledHeight());
        }

        // After the painter, so a script drawing per frame can put something over what the server
        // asked for rather than under it.
        if (ClientEvents.PAINT_SCREEN.hasListeners()) {
            ClientEvents.PAINT_SCREEN.post(ScriptType.CLIENT,
                new com.github.gubejs.client.painter.PaintScreenEventJS(
                    event.getPoseStack(), event.getWindow(), event.getPartialTick()));
        }
    }

    /**
     * Clears the screen when leaving a world.
     *
     * <p>Otherwise what one server drew stays up on the main menu and into the next world, since
     * nothing else ever removes it.
     *
     * @param event Forge's logging-out event
     */
    @SubscribeEvent
    public static void clearOverlay(ClientPlayerNetworkEvent.LoggingOut event) {
        com.github.gubejs.client.painter.Painter.INSTANCE.clear();
    }

    @SubscribeEvent
    public static void debugText(CustomizeGuiOverlayEvent.DebugText event) {
        if (ClientEvents.DEBUG_LEFT.hasListeners()) {
            ClientEvents.DEBUG_LEFT.post(ScriptType.CLIENT, new DebugInfoEventJS(event.getLeft()));
        }

        if (ClientEvents.DEBUG_RIGHT.hasListeners()) {
            ClientEvents.DEBUG_RIGHT.post(ScriptType.CLIENT, new DebugInfoEventJS(event.getRight()));
        }
    }
}
