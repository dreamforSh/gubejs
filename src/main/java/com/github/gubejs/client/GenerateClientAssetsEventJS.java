package com.github.gubejs.client;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.JsonUtils;
import com.github.gubejs.util.ValueUtils;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Files a script adds to the resource pack — {@code ClientEvents.highPriorityAssets(event => ...)}.
 *
 * <pre>{@code
 * ClientEvents.highPriorityAssets(event => {
 *     ['ruby', 'sapphire', 'topaz'].forEach(gem => {
 *         event.addJson(`mypack:models/item/${gem}`, {
 *             parent: 'minecraft:item/generated',
 *             textures: { layer0: `mypack:item/${gem}` }
 *         })
 *     })
 * })
 * }</pre>
 *
 * <p>The path is a resource location whose path is everything under
 * {@code assets/&lt;namespace&gt;/}. The pack sits above every other resource pack, so what it
 * writes wins — including over the models the registry builders generated, which is the point: a
 * pack that wants a different model for something a script created writes it here rather than
 * fighting the generated one.
 *
 * <p>One thing to know about when this runs. A resource pack has to exist before the reload that
 * reads it, and client scripts are themselves reloaded by that same reload — so on the first launch
 * the scripts are read from the pack directory specially to make this event possible at all, and
 * afterwards a listener that was just added takes effect on the next reload rather than the one
 * that added it. Pressing {@code F3+T} twice is the whole of the workaround.
 */
public final class GenerateClientAssetsEventJS extends EventJS implements ScriptTypeHolder {

    private final Map<ResourceLocation, byte[]> files;

    public GenerateClientAssetsEventJS(Map<ResourceLocation, byte[]> files) {
        this.files = files;
    }

    /**
     * Adds a JSON file.
     *
     * @param id where it goes, e.g. {@code 'mypack:models/item/ruby'}
     * @param json the contents, as an object or a string
     */
    public void addJson(Object id, Object json) {
        var location = parse(id, ".json");

        if (location != null) {
            files.put(location, JsonUtils.toString(json).getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Adds a file of any kind, as text.
     *
     * <p>Give the path its extension: nothing is appended here, since a {@code .txt} beside a font
     * or a {@code .mcmeta} beside a texture is as likely as a model.
     *
     * @param id where it goes, e.g. {@code 'mypack:textures/item/ruby.png.mcmeta'}
     * @param contents the text
     */
    public void add(Object id, Object contents) {
        var location = parse(id, "");

        if (location != null) {
            files.put(location, String.valueOf(ValueUtils.unwrap(contents))
                .getBytes(StandardCharsets.UTF_8));
        }
    }

    /**
     * Returns everything added so far.
     *
     * @return the live map, keyed by path under {@code assets/}
     */
    public Map<ResourceLocation, byte[]> getFiles() {
        return files;
    }

    @Nullable
    private ResourceLocation parse(Object id, String defaultExtension) {
        var text = ValueUtils.asString(id);

        if (text == null) {
            ConsoleJS.CLIENT.error("A resource pack file needs a path");
            return null;
        }

        var location = ResourceLocation.tryParse(text);

        if (location == null) {
            ConsoleJS.CLIENT.error("'" + text + "' is not a valid resource pack file path");
            return null;
        }

        if (defaultExtension.isEmpty() || location.getPath().contains(".")) {
            return location;
        }

        return new ResourceLocation(location.getNamespace(),
            location.getPath() + defaultExtension);
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.CLIENT;
    }
}
