package com.github.gubejs;

import com.github.gubejs.bindings.event.StartupEvents;
import com.github.gubejs.event.StartupEventJS;
import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryEventJS;
import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.script.ScriptManager;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.ServerScriptManager;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.GubejsPlugins;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The mod entry point, and the order everything happens in.
 *
 * <p>Startup scripts run inside the constructor, before any registry is filled — which is the only
 * moment a script can still add an item. Everything else is wired from here and happens later.
 */
@Mod(Gubejs.MOD_ID)
public final class Gubejs {

    /** The mod id, and the default namespace for anything a script creates. */
    public static final String MOD_ID = "gubejs";

    /** This mod's logger, for anything that is not a script's business. */
    public static final Logger LOGGER = LoggerFactory.getLogger("Gubejs");

    /** The Minecraft version, as the number a pack branches on. */
    public static final int MC_VERSION_NUMBER = 1902;

    /** The Minecraft version, as it is written. */
    public static final String MC_VERSION_STRING = "1.19.2";

    private static ScriptManager startupScriptManager;

    private static ScriptManager clientScriptManager;

    /**
     * Returns the manager running startup scripts.
     *
     * @return the manager
     */
    public static ScriptManager getStartupScriptManager() {
        return startupScriptManager;
    }

    /**
     * Returns the manager running client scripts.
     *
     * @return the manager
     */
    public static ScriptManager getClientScriptManager() {
        return clientScriptManager;
    }

    /**
     * Builds an id in this mod's namespace.
     *
     * @param path the path
     * @return the id
     */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public Gubejs() {
        writeReadme();

        GubejsPlugins.load(new BuiltinGubejsPlugin());
        GubejsPlugins.forEachPlugin(GubejsPlugin::init);
        GubejsPlugins.forEachPlugin(GubejsPlugin::registerEvents);

        com.github.gubejs.net.GubejsNetwork.init();

        startupScriptManager = new ScriptManager(ScriptType.STARTUP, GubejsPaths.STARTUP_SCRIPTS);
        clientScriptManager = new ScriptManager(ScriptType.CLIENT, GubejsPaths.CLIENT_SCRIPTS);

        ScriptType.STARTUP.setManager(() -> startupScriptManager);
        ScriptType.CLIENT.setManager(() -> clientScriptManager);
        ScriptType.SERVER.setManager(ServerScriptManager::get);

        // No resource manager yet -- there is no game to have one. Startup scripts therefore come
        // from the pack directory only, which is also the only place they would be useful from.
        startupScriptManager.reload(null);
        StartupEvents.INIT.post(ScriptType.STARTUP, new StartupEventJS());

        // Everything a script asked to create, built now so that the registry events below only
        // have to hand over finished objects. Doing it here also means a failure names the script
        // that caused it while the startup console is still the current one.
        buildRegistryEntries();

        var modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::registerObjects);
        modBus.addListener(this::addPackFinders);
        modBus.addListener(this::loadComplete);

        MinecraftForge.EVENT_BUS.register(new GubejsEventHandler());

        if (com.github.gubejs.bindings.PlatformWrapper.isClient()) {
            com.github.gubejs.client.GubejsClient.init(modBus);
        }
    }

    /**
     * Runs the registry listeners a pack wrote, filling {@link RegistryInfo} with builders.
     *
     * <p>Only the registries something actually listened to, since asking for the rest would build
     * nothing and log noise.
     */
    private void buildRegistryEntries() {
        for (var extraId : StartupEvents.REGISTRY.findUniqueExtraIds(ScriptType.STARTUP)) {
            if (!(extraId instanceof ResourceKey<?> key)) {
                continue;
            }

            var info = RegistryInfo.of(key);

            if (info == null) {
                ConsoleJS.STARTUP.error("Scripts cannot add to the registry '" + key.location()
                    + "'. Available: " + RegistryInfo.getAll().values());
                continue;
            }

            StartupEvents.REGISTRY.post(ScriptType.STARTUP, key, new RegistryEventJS(info));
        }
    }

    /**
     * Hands the built objects to Forge as each registry is filled.
     *
     * @param event Forge's registration event, fired once per registry
     */
    private void registerObjects(RegisterEvent event) {
        var info = RegistryInfo.of(event.getRegistryKey());

        if (info == null) {
            return;
        }

        for (var builder : info.getBuilders()) {
            try {
                event.register(castKey(info.key), builder.id, builder::get);
            } catch (Throwable ex) {
                ConsoleJS.STARTUP.handleError(ex, "Could not register " + builder.id);
            }
        }

        // A block registered without an item is a block nothing can obtain, so the matching item
        // is created here rather than making every script remember to ask for one.
        if (info == RegistryInfo.BLOCK) {
            registerBlockItems();
        }
    }

    private void registerBlockItems() {
        for (var builder : RegistryInfo.BLOCK.getBuilders()) {
            if (builder instanceof com.github.gubejs.block.BlockBuilder blockBuilder
                && blockBuilder.hasItem()) {
                RegistryInfo.ITEM.getBuilders().add(new BlockItemBuilder(blockBuilder));
            }
        }
    }

    /**
     * Adds the pack directory, and everything the builders generated, as real packs.
     *
     * <p>Through Forge's pack finder event rather than by wrapping the resource manager: a pack
     * added here participates in pack ordering, shows up in the pack list, and is reloaded like
     * any other.
     *
     * @param event Forge's pack discovery event, fired once per pack type
     */
    private void addPackFinders(AddPackFindersEvent event) {
        com.github.gubejs.script.data.GeneratedPack.register(event);
        com.github.gubejs.script.data.VirtualDataPack.register(event);
    }

    /**
     * Runs once every mod has finished loading.
     *
     * @param event Forge's load-complete event
     */
    private void loadComplete(FMLLoadCompleteEvent event) {
        GubejsPlugins.forEachPlugin(GubejsPlugin::afterInit);

        // Before postInit, and only here: every mod's registries are filled by now, which is what
        // a modification event needs, and nothing has started a world yet, which is what makes
        // writing into a block state safe.
        com.github.gubejs.bindings.event.ItemEvents.MODIFICATION.post(ScriptType.STARTUP,
            new com.github.gubejs.item.ItemModificationEventJS());
        com.github.gubejs.bindings.event.BlockEvents.MODIFICATION.post(ScriptType.STARTUP,
            new com.github.gubejs.block.BlockModificationEventJS());

        StartupEvents.POST_INIT.post(ScriptType.STARTUP, new StartupEventJS());
        ConsoleJS.STARTUP.flush();

        if (!ScriptType.STARTUP.errors.isEmpty()) {
            LOGGER.error("{} startup script error(s); see logs/gubejs/startup.log",
                ScriptType.STARTUP.errors.size());

            if (CommonProperties.get().startupErrorsAreFatal) {
                throw new IllegalStateException("Gubejs startup scripts failed: "
                    + String.join("; ", ScriptType.STARTUP.errors));
            }
        }
    }

    private void writeReadme() {
        if (Files.exists(GubejsPaths.README)) {
            return;
        }

        try {
            Files.writeString(GubejsPaths.README, """
                Gubejs — KubeJS-compatible scripting, running on GraalJS.

                startup_scripts  run once while the game loads. The only place new items, blocks
                                 and other registry entries can be added.
                server_scripts   run on every datapack reload (/reload). Recipes, tags, loot and
                                 gameplay events.
                client_scripts   run on every resource reload (F3+T). Tooltips and other things
                                 only the client knows about.

                assets           acts as a resource pack: assets/<namespace>/textures/...
                data             acts as a datapack: data/<namespace>/loot_tables/...
                config           settings, and the only directory scripts may write to freely.

                Per-type logs are in logs/gubejs/.
                """, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            LOGGER.error("Could not write {}", GubejsPaths.README, ex);
        }
    }

    /**
     * Narrows a registry key to the one {@link RegisterEvent#register} wants.
     *
     * <p>{@code RegistryInfo} is generic in the registry's element type and {@code RegisterEvent}
     * is generic in the same type, but neither can prove it to the other through the wildcard the
     * lookup returns.
     */
    @SuppressWarnings("unchecked")
    private static <T> ResourceKey<net.minecraft.core.Registry<T>> castKey(ResourceKey<?> key) {
        return (ResourceKey<net.minecraft.core.Registry<T>>) key;
    }

    /** Wraps a block builder so its block item registers alongside every other item. */
    private static final class BlockItemBuilder extends BuilderBase<Item> {

        private final com.github.gubejs.block.BlockBuilder block;

        private BlockItemBuilder(com.github.gubejs.block.BlockBuilder block) {
            super(block.id);
            this.block = block;
        }

        @Override
        public Item createObject() {
            var item = block.createBlockItem();
            return item == null ? new BlockItem((Block) block.get(), new Item.Properties()) : item;
        }

        @Override
        public java.util.Map<String, String> getTranslations() {
            // The block's own translation already covers the item: a BlockItem takes its name from
            // the block, so adding a second key would only produce an unused line.
            return java.util.Map.of();
        }
    }
}
