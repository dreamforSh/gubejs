package com.github.gubejs.client;

import com.github.gubejs.Gubejs;
import com.github.gubejs.bindings.event.ClientEvents;
import com.github.gubejs.script.ScriptType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.metadata.MetadataSectionSerializer;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.event.AddPackFindersEvent;
import org.jetbrains.annotations.Nullable;

/**
 * A resource pack whose files come from {@code ClientEvents.highPriorityAssets} rather than disk.
 *
 * <p>The client's counterpart to
 * {@link com.github.gubejs.script.data.VirtualDataPack the virtual datapack}, and built the same
 * way: the event runs in the constructor, which the {@link Pack} supplier reaches once per resource
 * reload.
 *
 * <p>Only the high-priority half exists. A low-priority resource pack would sit below the generated
 * one, and everything it could usefully write — a model, a blockstate, a translation — is something
 * the registry builders already generate when nothing else provides it. There is no gap left for it
 * to fill.
 */
public final class VirtualAssetPack implements PackResources {

    private final Map<ResourceLocation, byte[]> files = new LinkedHashMap<>();

    private final Set<String> namespaces = new LinkedHashSet<>();

    private VirtualAssetPack() {
        // On the very first reload the client scripts have never run, so there is nothing
        // listening yet and the pack would come out empty. Loading them here is what makes the
        // event work on the first launch rather than the second.
        GubejsClient.ensureScriptsLoaded();

        if (ClientEvents.HIGH_ASSETS.hasListeners()) {
            ClientEvents.HIGH_ASSETS.post(ScriptType.CLIENT,
                new GenerateClientAssetsEventJS(files));
        }

        for (var id : files.keySet()) {
            namespaces.add(id.getNamespace());
        }

        if (!files.isEmpty()) {
            Gubejs.LOGGER.info("Scripts supplied {} resource pack file(s)", files.size());
        }
    }

    /**
     * Registers the pack.
     *
     * @param event Forge's pack discovery event, which fires once per pack type
     */
    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.CLIENT_RESOURCES) {
            return;
        }

        event.addRepositorySource((consumer, constructor) -> {
            var pack = Pack.create("gubejs_assets_high", true, VirtualAssetPack::new,
                constructor, Pack.Position.TOP, PackSource.DEFAULT);

            if (pack != null) {
                consumer.accept(pack);
            }
        });
    }

    @Nullable
    @Override
    public InputStream getRootResource(String path) {
        return PACK_META.equals(path)
            ? new ByteArrayInputStream(META.getBytes(StandardCharsets.UTF_8)) : null;
    }

    @Override
    public InputStream getResource(PackType type, ResourceLocation id) throws FileNotFoundException {
        var bytes = type == PackType.CLIENT_RESOURCES ? files.get(id) : null;

        if (bytes == null) {
            throw new FileNotFoundException(id.toString());
        }

        return new ByteArrayInputStream(bytes);
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String path,
                                                     Predicate<ResourceLocation> filter) {
        if (type != PackType.CLIENT_RESOURCES) {
            return List.of();
        }

        var prefix = path.endsWith("/") ? path : path + "/";
        var found = new ArrayList<ResourceLocation>();

        for (var id : files.keySet()) {
            if (id.getNamespace().equals(namespace)
                && id.getPath().startsWith(prefix)
                && filter.test(id)) {
                found.add(id);
            }
        }

        return found;
    }

    @Override
    public boolean hasResource(PackType type, ResourceLocation id) {
        return type == PackType.CLIENT_RESOURCES && files.containsKey(id);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.CLIENT_RESOURCES ? namespaces : Set.of();
    }

    @Nullable
    @Override
    public <T> T getMetadataSection(MetadataSectionSerializer<T> serializer) {
        var json = JsonParser.parseString(META);

        if (json instanceof JsonObject object && object.has(serializer.getMetadataSectionName())) {
            return serializer.fromJson(
                object.getAsJsonObject(serializer.getMetadataSectionName()));
        }

        return null;
    }

    @Override
    public String getName() {
        return "Gubejs Assets (high priority)";
    }

    @Override
    public void close() {
        // Nothing to release: every byte is already in memory and goes when this object does.
    }

    private static final String META = """
        {
          "pack": {
            "description": "Gubejs virtual assets",
            "pack_format": 9
          }
        }""";
}
