package com.github.gubejs.item;

import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.Locale;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a piece of armour — {@code event.create('ruby_helmet', 'helmet').material('diamond')}.
 *
 * <p>The four slots are one builder rather than four, because the only thing that differs between
 * them is the slot itself: protection, toughness, durability and the sounds all come from the
 * material, which is the same object for a whole set.
 */
public class ArmorItemBuilder extends ItemBuilder {

    /** What the armour is made of. */
    protected ArmorMaterial material = ArmorMaterials.IRON;

    /** Which slot it goes in. */
    private final EquipmentSlot slot;

    public ArmorItemBuilder(ResourceLocation id, EquipmentSlot slot) {
        super(id);
        this.slot = slot;
        this.maxStackSize = 1;
    }

    /**
     * Sets what the armour is made of.
     *
     * <p>A material carries the whole set's numbers, so all four pieces should be given the same
     * one — mixing them produces a set whose parts protect differently.
     *
     * @param material a vanilla material name like {@code 'diamond'}, or an
     *     {@link ArmorMaterial} from a mod
     * @return this builder
     */
    public ArmorItemBuilder material(Object material) {
        var unwrapped = ValueUtils.unwrap(material);

        if (unwrapped instanceof ArmorMaterial armorMaterial) {
            this.material = armorMaterial;
            return this;
        }

        var name = String.valueOf(unwrapped);

        try {
            this.material = ArmorMaterials.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            ConsoleJS.STARTUP.error("Unknown armour material '" + name + "'. Known: "
                + java.util.Arrays.toString(ArmorMaterials.values())
                + ". A modded material has to be passed as the object itself, since it is not "
                + "in any registry this can look it up in.");
        }

        return this;
    }

    @Override
    public Item createObject() {
        return new ArmorItem(material, slot, createProperties());
    }

    /** Registers the armour types scripts can create. */
    public static void registerTypes() {
        register("helmet", EquipmentSlot.HEAD);
        register("chestplate", EquipmentSlot.CHEST);
        register("leggings", EquipmentSlot.LEGS);
        register("boots", EquipmentSlot.FEET);
    }

    private static void register(String name, EquipmentSlot slot) {
        RegistryInfo.ITEM.addType(name, id -> new ArmorItemBuilder(id, slot));
    }
}
