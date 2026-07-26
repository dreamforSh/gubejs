package com.github.gubejs.script.data;

import com.github.gubejs.Gubejs;
import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.script.ScriptType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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
 * A datapack whose files come from {@code ServerEvents.highPriorityData} and
 * {@code lowPriorityData} rather than from disk.
 *
 * <h2>When the event runs</h2>
 *
 * <p>In the constructor, which is reached through the supplier {@link Pack} keeps — and that
 * supplier is called once per reload, as the pack is opened. So the files are rebuilt every time
 * the datapacks reload, which is what a pack author expects from {@code /reload}, and never at any
 * other moment.
 *
 * <h2>Why two of them</h2>
 *
 * <p>Load order is the only difference. The high-priority pack sits above every other datapack, so
 * a file it writes wins outright; the low-priority one sits at the bottom, so it only supplies a
 * file nothing else did.
 */
public final class VirtualDataPack implements PackResources {

    private final String name;

    private final boolean highPriority;

    private final Map<ResourceLocation, byte[]> files = new LinkedHashMap<>();

    private final Set<String> namespaces = new LinkedHashSet<>();

    private VirtualDataPack(String name, boolean highPriority) {
        this.name = name;
        this.highPriority = highPriority;

        var handler = highPriority ? ServerEvents.HIGH_DATA : ServerEvents.LOW_DATA;

        if (handler.hasListeners()) {
            handler.post(ScriptType.SERVER, null, new DataPackEventJS(files, highPriority));
        }

        for (var id : files.keySet()) {
            namespaces.add(id.getNamespace());
        }

        if (!files.isEmpty()) {
            Gubejs.LOGGER.info("{} supplied {} datapack file(s)", name, files.size());
        }
    }

    /**
     * Registers both virtual packs.
     *
     * @param event Forge's pack discovery event, which fires once per pack type
     */
    public static void register(AddPackFindersEvent event) {
        if (event.getPackType() != PackType.SERVER_DATA) {
            return;
        }

        add(event, "gubejs_data_high", "Gubejs Data (high priority)", true, Pack.Position.TOP);
        add(event, "gubejs_data_low", "Gubejs Data (low priority)", false, Pack.Position.BOTTOM);
    }

    private static void add(AddPackFindersEvent event, String id, String title,
                            boolean highPriority, Pack.Position position) {
        event.addRepositorySource((consumer, constructor) -> {
            var pack = Pack.create(id, true, () -> new VirtualDataPack(title, highPriority),
                constructor, position, PackSource.DEFAULT);

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
        var bytes = type == PackType.SERVER_DATA ? files.get(id) : null;

        if (bytes == null) {
            throw new FileNotFoundException(id.toString());
        }

        return new ByteArrayInputStream(bytes);
    }

    @Override
    public Collection<ResourceLocation> getResources(PackType type, String namespace, String path,
                                                     Predicate<ResourceLocation> filter) {
        if (type != PackType.SERVER_DATA) {
            return List.of();
        }

        var prefix = path.endsWith("/") ? path : path + "/";
        var found = new java.util.ArrayList<ResourceLocation>();

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
        return type == PackType.SERVER_DATA && files.containsKey(id);
    }

    @Override
    public Set<String> getNamespaces(PackType type) {
        return type == PackType.SERVER_DATA ? namespaces : Set.of();
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
        return name;
    }

    @Override
    public void close() {
        // Nothing to release: every byte is already in memory and goes when this object does.
    }

    /** Whether this is the pack whose files override the others. */
    public boolean isHighPriority() {
        return highPriority;
    }

    private static final String META = """
        {
          "pack": {
            "description": "Gubejs virtual data",
            "pack_format": 10
          }
        }""";
}
