package com.github.gubejs.item;

import com.github.gubejs.Gubejs;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.util.ConsoleJS;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorMaterial;
import org.jetbrains.annotations.Nullable;

/**
 * Where a pack invents an armour material — {@code ItemEvents.armorTierRegistry(event => ...)}.
 *
 * <p>Fires before any item is built, so a material defined here is available to every
 * {@code event.create(..., 'helmet').material('steel')} in the same run.
 *
 * <p>The material's name is the full id, {@code mypack:steel} rather than {@code steel}, because
 * the name is also where the armour's texture comes from: the renderer builds
 * {@code <namespace>:textures/models/armor/<path>_layer_1.png} out of it. Two files are needed —
 * {@code _layer_1} for the helmet, chestplate and boots, and {@code _layer_2} for the leggings.
 */
public class ItemArmorTierRegistryEventJS extends EventJS {

    private static final Map<String, ArmorMaterial> MATERIALS = new LinkedHashMap<>();

    /**
     * Adds an armour material based on iron.
     *
     * @param id the name armour uses, with {@code gubejs:} assumed when no namespace is given
     * @param callback fills in the material
     */
    public void add(String id, Consumer<ScriptArmorMaterial> callback) {
        add(id, "iron", callback);
    }

    /**
     * Adds an armour material based on a vanilla one.
     *
     * @param id the name armour uses, with {@code gubejs:} assumed when no namespace is given
     * @param parent the vanilla material to start from, e.g. {@code diamond}
     * @param callback fills in what differs from the parent
     */
    public void add(String id, String parent, Consumer<ScriptArmorMaterial> callback) {
        var parsed = ResourceLocation.tryParse(
            id.indexOf(':') == -1 ? Gubejs.MOD_ID + ":" + id : id);

        if (parsed == null) {
            ConsoleJS.STARTUP.error("'" + id + "' is not a valid armour material id");
            return;
        }

        var material = new ScriptArmorMaterial(parsed.toString(),
            ScriptArmorMaterial.vanilla(parent));
        callback.accept(material);

        MATERIALS.put(parsed.toString(), material);
        MATERIALS.put(parsed.getPath(), material);
    }

    /**
     * Looks up a material a script defined.
     *
     * @param name the material name, with or without a namespace
     * @return the material, or {@code null} if no script defined one under that name
     */
    @Nullable
    public static ArmorMaterial get(String name) {
        return MATERIALS.get(name);
    }

    /** Drops every script-defined material, so a startup reload does not define them twice. */
    public static void clear() {
        MATERIALS.clear();
    }
}
