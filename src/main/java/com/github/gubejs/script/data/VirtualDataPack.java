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
import net.minecraft.server.packs.resources.MultiPackResourceManager;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.Nullable;

/**
 * A datapack whose files come from {@code ServerEvents.highPriorityData} and
 * {@code lowPriorityData} rather than from disk.
 *
 * <h2>When the event runs, and why it is not a pack the repository knows about</h2>
 *
 * <p>These used to be registered through Forge's pack finder, built by a supplier the repository
 * called. That could not work: the repository opens its packs before the datapack load begins, and
 * server scripts do not load until the load is under way — so the event was posted before anything
 * was listening to it. On a fresh server that meant a script's files simply did not exist; after a
 * {@code /reload} they were whatever the previous load had written. The supplier was also called an
 * extra time to read the pack's metadata, so the event ran more than once per reload.
 *
 * <p>So the packs are not in the repository at all. {@link #wrap} builds them at the start of the
 * datapack load, once the scripts have run, and hands back a resource manager with them in it. That
 * is also the only order that works: a resource manager indexes every pack's namespaces as it is
 * constructed, so a pack that is still empty at that moment is one whose files can never be found —
 * which is the bug the obvious "add the packs, then fill them" arrangement has.
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
    }

    /**
     * Returns a resource manager holding everything {@code original} does, plus whatever the
     * datapack events wrote.
     *
     * <p>Called at the start of the datapack load, after the scripts have run, with the manager the
     * reload was about to use. The original is returned unchanged when no script wrote anything,
     * which is the usual case and saves indexing every pack a second time.
     *
     * @param original the resource manager the reload built
     * @return the one it should use instead
     */
    public static ResourceManager wrap(ResourceManager original) {
        var low = new VirtualDataPack("Gubejs Data (low priority)", false);
        var high = new VirtualDataPack("Gubejs Data (high priority)", true);

        low.fill();
        high.fill();

        if (low.files.isEmpty() && high.files.isEmpty()) {
            return original;
        }

        // Last wins: a resource manager searches its packs back to front. So the low-priority pack
        // goes in front of everything, where it is only reached for a file nothing else supplied,
        // and the high-priority one goes last, where it overrides.
        var packs = new ArrayList<PackResources>();
        packs.add(low);
        original.listPacks().forEach(packs::add);
        packs.add(high);

        return new MultiPackResourceManager(PackType.SERVER_DATA, packs);
    }

    /** Posts this pack's event and works out which namespaces it ended up holding. */
    private void fill() {
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
