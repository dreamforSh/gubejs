package com.github.gubejs.bindings;

import com.github.gubejs.item.ItemStackJS;
import com.github.gubejs.util.NbtHelper;
import com.github.gubejs.util.ValueUtils;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Item} global.
 *
 * <pre>{@code
 * Item.of('minecraft:diamond', 4)
 * Item.of('minecraft:diamond_sword', '{Damage:10}')
 * Item.getList().filter(i => i.id.startsWith('minecraft:'))
 * }</pre>
 *
 * <p>The overloads all take {@code Object} rather than being typed on {@code int} and
 * {@code String}. JavaScript has one number type and one string type, and asking Graal to choose
 * between {@code of(Object, int)} and {@code of(Object, Object)} for a guest number is asking for
 * the wrong one — so the choice is made here, from the value.
 */
public final class ItemWrapper {

    private ItemWrapper() {
    }

    /**
     * Builds an item stack.
     *
     * @param value anything that names an item
     * @return the stack, empty if the value names nothing
     */
    public static ItemStack of(@Nullable Object value) {
        return ItemStackJS.of(value);
    }

    /**
     * Builds an item stack with either a count or NBT.
     *
     * @param value anything that names an item
     * @param countOrNbt a number for the count, anything else for NBT
     * @return the stack
     */
    public static ItemStack of(@Nullable Object value, @Nullable Object countOrNbt) {
        var stack = ItemStackJS.of(value).copy();
        var second = ValueUtils.unwrap(countOrNbt);

        if (second instanceof Number number) {
            stack.setCount(number.intValue());
        } else if (second != null) {
            stack.setTag(NbtHelper.compound(second));
        }

        return stack;
    }

    /**
     * Builds an item stack with a count and NBT.
     *
     * @param value anything that names an item
     * @param count how many
     * @param nbt the tag to attach, as an object or SNBT
     * @return the stack
     */
    public static ItemStack of(@Nullable Object value, @Nullable Object count, @Nullable Object nbt) {
        var stack = of(value, count);
        stack.setTag(NbtHelper.compound(nbt));
        return stack;
    }

    /**
     * Returns an empty stack, which is how "no item" is spelled.
     *
     * @return the empty stack
     */
    public static ItemStack getEmpty() {
        return ItemStack.EMPTY;
    }

    /**
     * Returns one stack of every registered item.
     *
     * @return the stacks, in registry order
     */
    public static List<ItemStack> getList() {
        var list = new ArrayList<ItemStack>();

        for (var item : ForgeRegistries.ITEMS.getValues()) {
            list.add(new ItemStack(item));
        }

        return list;
    }

    /**
     * Returns every registered item id.
     *
     * @return the ids, as strings
     */
    public static List<String> getTypeList() {
        var list = new ArrayList<String>();

        for (var key : ForgeRegistries.ITEMS.getKeys()) {
            list.add(key.toString());
        }

        return list;
    }

    /**
     * Returns every item in a tag.
     *
     * @param tag the tag id, with or without a leading {@code #}
     * @return one stack per item in the tag, empty if the tag does not exist
     */
    public static List<ItemStack> getItemsInTag(String tag) {
        var id = ResourceLocation.tryParse(tag.startsWith("#") ? tag.substring(1) : tag);
        var list = new ArrayList<ItemStack>();

        if (id == null) {
            return list;
        }

        // Through the vanilla registry rather than the Forge wrapper: tags are datapack state and
        // only the vanilla registry exposes their contents.
        Registry.ITEM.getTag(TagKey.create(Registry.ITEM_REGISTRY, id)).ifPresent(holders ->
            holders.forEach(holder -> list.add(new ItemStack(holder.value()))));
        return list;
    }

    /**
     * Looks up an item by id.
     *
     * @param id the registry name
     * @return the item, or {@code null} if nothing is registered under it
     */
    @Nullable
    public static Item getItem(String id) {
        return ItemStackJS.getItem(id);
    }

    /**
     * Reports whether an id names a registered item.
     *
     * @param id the registry name
     * @return {@code true} if the item exists
     */
    public static boolean exists(String id) {
        return ItemStackJS.getItem(id) != null;
    }
}
