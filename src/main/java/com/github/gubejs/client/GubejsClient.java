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
    public static void init(IEventBus modBus) {
        modBus.addListener(GubejsClient::registerReloadListener);
        modBus.addListener(GubejsClient::clientSetup);
        MinecraftForge.EVENT_BUS.register(GubejsClient.class);
        GubejsPlugins.forEachPlugin(GubejsPlugin::clientInit);
    }

    private static void registerReloadListener(RegisterClientReloadListenersEvent event) {
        event.registerReloadListener((ResourceManagerReloadListener) GubejsClient::reloadScripts);
    }

    private static void reloadScripts(ResourceManager resourceManager) {
        Gubejs.getClientScriptManager().reload(resourceManager);
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

        persistent.getCompound("PlayerPersisted")
            .put("gubejs:stages", data.getList("stages", net.minecraft.nbt.Tag.TAG_STRING));
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
