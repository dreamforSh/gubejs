package com.github.gubejs.client;

import com.github.gubejs.util.ValueUtils;
import java.util.Map;

/**
 * The language table being built, for one language.
 *
 * <pre>{@code
 * ClientEvents.lang('en_us', event => {
 *     event.add('item.minecraft.diamond', 'Shiny Rock')
 *     event.renameItem('minecraft:dirt', 'Soil')
 * })
 * }</pre>
 *
 * <p>Entries added here win over the ones the resource packs loaded, which is the point: a pack
 * renames a vanilla item without shipping a language file that would conflict with every other
 * pack doing the same.
 */
public final class LangEventJS extends ClientEventJS {

    private final String language;

    private final Map<String, String> entries;

    public LangEventJS(String language, Map<String, String> entries) {
        this.language = language;
        this.entries = entries;
    }

    /**
     * Returns which language is being built, e.g. {@code en_us}.
     *
     * @return the language code
     */
    public String getLanguage() {
        return language;
    }

    /**
     * Sets one translation key.
     *
     * @param key the translation key
     * @param value the text
     * @return this event
     */
    public LangEventJS add(String key, Object value) {
        entries.put(key, String.valueOf(ValueUtils.unwrap(value)));
        return this;
    }

    /**
     * Sets several translation keys at once.
     *
     * @param values key to text
     * @return this event
     */
    public LangEventJS addAll(Object values) {
        var map = ValueUtils.unwrap(values);

        if (map instanceof Map<?, ?> entriesToAdd) {
            entriesToAdd.forEach((key, value) ->
                entries.put(String.valueOf(key), String.valueOf(ValueUtils.unwrap(value))));
        }

        return this;
    }

    /**
     * Renames an item, without having to know how its translation key is spelled.
     *
     * @param id the item id
     * @param name the new name
     * @return this event
     */
    public LangEventJS renameItem(Object id, Object name) {
        var item = com.github.gubejs.item.ItemStackJS.getItem(
            String.valueOf(ValueUtils.unwrap(id)));
        return item == null ? this : add(item.getDescriptionId(), name);
    }

    /**
     * Renames a block, and with it the block's item.
     *
     * @param id the block id
     * @param name the new name
     * @return this event
     */
    public LangEventJS renameBlock(Object id, Object name) {
        var block = com.github.gubejs.bindings.BlockWrapper.getBlock(
            String.valueOf(ValueUtils.unwrap(id)));
        return block == null ? this : add(block.getDescriptionId(), name);
    }

    /**
     * Returns everything collected so far.
     *
     * @return the live map
     */
    public Map<String, String> getEntries() {
        return entries;
    }
}
