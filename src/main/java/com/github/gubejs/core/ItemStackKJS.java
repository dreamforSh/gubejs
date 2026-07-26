package com.github.gubejs.core;

import com.github.gubejs.util.NbtHelper;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * What a script can do with an item stack, mixed into {@link ItemStack} itself.
 *
 * <p>{@code stack.id}, {@code stack.nbt} and {@code stack.count} are what a pack writes; the game
 * spells the first two differently and has no notion of the third being settable on a copy.
 */
public interface ItemStackKJS {

    /**
     * Returns this, as the stack it is.
     *
     * <p>Through {@code Object} because {@link ItemStack} is final, so javac knows it does not
     * implement this interface and rejects the direct cast. It does implement it at runtime —
     * the mixin adds it — and the two-step cast is how that is spelled.
     *
     * @return this stack
     */
    default ItemStack gjs$self() {
        return (ItemStack) (Object) this;
    }

    /**
     * Returns the item's id, e.g. {@code minecraft:diamond}.
     *
     * @return the id
     */
    default String getId() {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(gjs$self().getItem()));
    }

    /**
     * Returns the stack's NBT.
     *
     * <p>The same tag {@code getTag()} returns, under the name a pack uses. Editing it edits the
     * stack.
     *
     * @return the tag, or {@code null} if the stack has none
     */
    @Nullable
    default CompoundTag getNbt() {
        return gjs$self().getTag();
    }

    /**
     * Replaces the stack's NBT.
     *
     * @param value the tag, or an object to convert into one
     */
    default void setNbt(@Nullable Object value) {
        gjs$self().setTag(value == null ? null : NbtHelper.compound(value));
    }

    /**
     * Returns a copy of this stack with a different count.
     *
     * @param count how many
     * @return the copy
     */
    default ItemStack withCount(int count) {
        var copy = gjs$self().copy();
        copy.setCount(count);
        return copy;
    }

    /**
     * Returns a copy of this stack with NBT merged in.
     *
     * @param value the keys to set
     * @return the copy
     */
    default ItemStack withNbt(@Nullable Object value) {
        var copy = gjs$self().copy();
        var tag = copy.getTag();

        if (tag == null) {
            copy.setTag(NbtHelper.compound(value));
        } else {
            tag.merge(NbtHelper.compound(value));
        }

        return copy;
    }

    /**
     * Reports whether the item is in a tag.
     *
     * @param tag the tag id, with or without the leading {@code #}
     * @return {@code true} if it is
     */
    default boolean hasTag(String tag) {
        var id = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        return id != null && gjs$self().is(net.minecraft.tags.TagKey.create(
            net.minecraft.core.Registry.ITEM_REGISTRY, id));
    }

    /**
     * Reports whether this stack is a particular item, ignoring count and NBT.
     *
     * @param id the item id
     * @return {@code true} if it is
     */
    default boolean isItem(String id) {
        return getId().equals(id.indexOf(':') == -1 ? "minecraft:" + id : id);
    }

    /**
     * Returns an ingredient matching exactly this item.
     *
     * @return the ingredient
     */
    default Ingredient getIngredient() {
        return Ingredient.of(gjs$self());
    }

    /**
     * Returns the enchantments on this stack, as ids and levels.
     *
     * <p>{@code stack.enchantments['minecraft:sharpness']} — reading the raw NBT would mean
     * walking a list of compounds and looking each id up.
     *
     * @return the enchantments
     */
    default Map<String, Integer> getEnchantments() {
        var map = new LinkedHashMap<String, Integer>();

        EnchantmentHelper.getEnchantments(gjs$self()).forEach((enchantment, level) ->
            map.put(String.valueOf(ForgeRegistries.ENCHANTMENTS.getKey(enchantment)), level));

        return map;
    }

    /**
     * Adds an enchantment, or raises the level of one already there.
     *
     * @param id the enchantment id, e.g. {@code minecraft:sharpness}
     * @param level how strong
     */
    default void enchant(String id, int level) {
        var enchantment = ForgeRegistries.ENCHANTMENTS.getValue(ResourceLocation.tryParse(
            id.indexOf(':') == -1 ? "minecraft:" + id : id));

        if (enchantment != null) {
            gjs$self().enchant(enchantment, level);
        }
    }

    /**
     * Returns the stack as the string form {@code Item.of} reads back.
     *
     * <p>What to write into a config file or a log line and read again later.
     *
     * @return the string, e.g. {@code 4x minecraft:diamond}
     */
    default String toItemString() {
        var stack = gjs$self();

        if (stack.isEmpty()) {
            return "minecraft:air";
        }

        var text = new StringBuilder();

        if (stack.getCount() > 1) {
            text.append(stack.getCount()).append("x ");
        }

        text.append(getId());

        if (stack.hasTag()) {
            text.append(stack.getTag());
        }

        return text.toString();
    }
}
