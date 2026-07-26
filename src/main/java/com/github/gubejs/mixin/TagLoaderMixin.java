package com.github.gubejs.mixin;

import com.github.gubejs.bindings.event.ServerEvents;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.server.ServerScriptManager;
import com.github.gubejs.server.tag.TagEventJS;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.tags.TagManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands the loaded tag entries to {@code ServerEvents.tags} before they are resolved.
 *
 * <p>Editing entries rather than finished tags is what makes a script's addition indistinguishable
 * from a datapack's: nested tags still expand, optional entries still tolerate missing mods, and
 * everything that reads a tag afterwards sees one consistent result.
 */
@Mixin(TagLoader.class)
public abstract class TagLoaderMixin {

    /**
     * Which registry this loader is for, worked out from the directory it reads.
     *
     * <p>A {@code TagLoader} is told a directory — {@code tags/items} — and nothing else, so the
     * mapping has to be inverted from the registry side. Built once; the registry list does not
     * change after startup.
     */
    private static Map<String, ResourceKey<?>> gubejs$registriesByDirectory;

    @Shadow
    @Final
    private String directory;

    @Inject(method = "load", at = @At("RETURN"))
    private void gubejs$editTags(ResourceManager resourceManager,
                                 CallbackInfoReturnable<Map<ResourceLocation,
                                     List<TagLoader.EntryWithSource>>> cir) {
        // Tags can load on the client too, for a pack that ships its own; there is no server
        // script context there, and nothing to post to.
        if (!ServerEvents.TAGS.hasListeners()) {
            return;
        }

        ServerScriptManager.ensureLoaded(resourceManager);

        var registry = gubejs$registryFor(directory);

        if (registry != null) {
            ServerEvents.TAGS.post(ScriptType.SERVER, registry,
                new TagEventJS(registry, cir.getReturnValue()));
        }
    }

    private static ResourceKey<?> gubejs$registryFor(String directory) {
        var map = gubejs$registriesByDirectory;

        if (map == null) {
            map = new HashMap<>();

            for (var id : Registry.REGISTRY.keySet()) {
                var key = ResourceKey.createRegistryKey(id);
                map.put(TagManager.getTagDir(key), key);
            }

            gubejs$registriesByDirectory = map;
        }

        return map.get(directory);
    }
}
