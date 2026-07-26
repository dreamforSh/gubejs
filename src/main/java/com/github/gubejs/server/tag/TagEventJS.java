package com.github.gubejs.server.tag;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagLoader;
import org.jetbrains.annotations.Nullable;

/**
 * The event handed to {@code ServerEvents.tags('item', ...)}: edit tag contents.
 *
 * <pre>{@code
 * ServerEvents.tags('item', event => {
 *     event.add('forge:ingots/copper', 'mymod:copper_ingot')
 *     event.remove('minecraft:planks', 'minecraft:crimson_planks')
 *     event.removeAll('forge:ores')
 * })
 * }</pre>
 *
 * <p>Edits the loaded tag entries before they are resolved into real tags, so a tag a script adds
 * to behaves exactly as if a datapack had said so — including for recipes, advancements and other
 * tags that reference it.
 */
public final class TagEventJS extends EventJS {

    /** The source string entries added here are attributed to, as it appears in {@code /tag} output. */
    private static final String SOURCE = "gubejs";

    private final ResourceKey<?> registry;

    private final Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags;

    private int addedCount;

    private int removedCount;

    public TagEventJS(ResourceKey<?> registry, Map<ResourceLocation, List<TagLoader.EntryWithSource>> tags) {
        this.registry = registry;
        this.tags = tags;
    }

    /**
     * Returns which registry's tags are being edited.
     *
     * @return the registry key
     */
    public ResourceKey<?> getRegistry() {
        return registry;
    }

    /**
     * Adds entries to a tag, creating it if it does not exist.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @param values one or more ids to add; a {@code #tag} adds that whole tag
     * @return this event
     */
    public TagEventJS add(Object tag, Object values) {
        var id = parseId(tag);

        if (id == null) {
            return this;
        }

        var entries = tags.computeIfAbsent(id, k -> new ArrayList<>());

        for (var value : ValueUtils.listOf(values)) {
            var text = String.valueOf(ValueUtils.unwrap(value)).trim();
            var nested = text.startsWith("#");
            var entryId = ResourceLocation.tryParse(nested ? text.substring(1) : text);

            if (entryId == null) {
                ConsoleJS.SERVER.warn("Not an id: '" + text + "'");
                continue;
            }

            // Optional entries throughout: a pack that adds another mod's item to a tag should
            // not break the whole tag when that mod is absent, which a required entry would do.
            entries.add(new TagLoader.EntryWithSource(
                nested ? TagEntry.optionalTag(entryId) : TagEntry.optionalElement(entryId), SOURCE));
            addedCount++;
        }

        return this;
    }

    /**
     * Removes entries from a tag.
     *
     * @param tag the tag id
     * @param values the ids to remove
     * @return this event
     */
    public TagEventJS remove(Object tag, Object values) {
        var id = parseId(tag);
        var entries = id == null ? null : tags.get(id);

        if (entries == null) {
            return this;
        }

        for (var value : ValueUtils.listOf(values)) {
            var text = String.valueOf(ValueUtils.unwrap(value)).trim();
            var wanted = normalise(text);
            var before = entries.size();
            entries.removeIf(entry -> normalise(entry.entry().toString()).equals(wanted));
            removedCount += before - entries.size();
        }

        return this;
    }

    /**
     * Empties a tag without deleting it.
     *
     * @param tag the tag id
     * @return this event
     */
    public TagEventJS removeAll(Object tag) {
        var id = parseId(tag);
        var entries = id == null ? null : tags.get(id);

        if (entries != null) {
            removedCount += entries.size();
            entries.clear();
        }

        return this;
    }

    /**
     * Returns what a tag currently contains, as ids.
     *
     * @param tag the tag id
     * @return the entries, {@code #}-prefixed where one tag includes another
     */
    public List<String> get(Object tag) {
        var id = parseId(tag);
        var entries = id == null ? null : tags.get(id);
        var ids = new ArrayList<String>();

        if (entries != null) {
            entries.forEach(entry -> ids.add(entry.entry().toString()));
        }

        return ids;
    }

    /**
     * Removes an id from every tag in this registry.
     *
     * <p>What a pack uses to retire an item: leaving it in tags means recipes keep accepting it.
     *
     * @param values the ids to remove everywhere
     * @return how many entries were removed
     */
    public int removeAllTagsFrom(Object values) {
        var removed = 0;

        for (var value : ValueUtils.listOf(values)) {
            var wanted = normalise(String.valueOf(ValueUtils.unwrap(value)).trim());

            for (var entries : tags.values()) {
                var before = entries.size();
                entries.removeIf(entry -> normalise(entry.entry().toString()).equals(wanted));
                removed += before - entries.size();
            }
        }

        removedCount += removed;
        return removed;
    }

    /**
     * Returns every tag id in this registry.
     *
     * @return the ids
     */
    public List<String> getTagIds() {
        var ids = new ArrayList<String>();
        tags.keySet().forEach(id -> ids.add(id.toString()));
        return ids;
    }

    @Override
    protected void afterPosted(com.github.gubejs.event.EventResult result) {
        if (addedCount > 0 || removedCount > 0) {
            ConsoleJS.SERVER.info("Tags for " + registry.location() + ": "
                + addedCount + " added, " + removedCount + " removed");
        }
    }

    @Nullable
    private ResourceLocation parseId(Object tag) {
        var text = String.valueOf(ValueUtils.unwrap(tag)).trim();
        var id = ResourceLocation.tryParse(text.startsWith("#") ? text.substring(1) : text);

        if (id == null) {
            ConsoleJS.SERVER.warn("Not a tag id: '" + text + "'");
        }

        return id;
    }

    /**
     * Puts an entry into the form ids are compared in.
     *
     * <p>{@link TagEntry#toString()} is the only way to read one back in 1.19.2 — the id it holds
     * has no getter — and it already produces {@code #namespace:path} for a nested tag and
     * {@code namespace:path} for an element, which is exactly the form a script writes.
     */
    private static String normalise(String entry) {
        var tag = entry.startsWith("#");
        var id = tag ? entry.substring(1) : entry;
        return (tag ? "#" : "") + (id.indexOf(':') == -1 ? "minecraft:" + id : id);
    }
}
