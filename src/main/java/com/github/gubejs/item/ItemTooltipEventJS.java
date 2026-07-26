package com.github.gubejs.item;

import com.github.gubejs.bindings.TextWrapper;
import com.github.gubejs.event.EventJS;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * The lines under an item's name in its tooltip, before they are drawn.
 *
 * <p>Client-side, and called for every item in every inventory slot the mouse passes over, so a
 * listener here runs far more often than almost any other.
 */
public final class ItemTooltipEventJS extends EventJS {

    private final ItemStack item;

    private final List<Component> lines;

    private final boolean advanced;

    public ItemTooltipEventJS(ItemStack item, List<Component> lines, boolean advanced) {
        this.item = item;
        this.lines = lines;
        this.advanced = advanced;
    }

    /**
     * Returns the item being hovered.
     *
     * @return the stack
     */
    public ItemStack getItem() {
        return item;
    }

    /**
     * Returns the item's id.
     *
     * @return the id
     */
    public String getId() {
        return String.valueOf(ForgeRegistries.ITEMS.getKey(item.getItem()));
    }

    /**
     * Whether the player has advanced tooltips turned on.
     *
     * @return {@code true} in advanced mode
     */
    public boolean isAdvanced() {
        return advanced;
    }

    /**
     * Returns the tooltip lines, which can be edited in place.
     *
     * @return the live list of lines
     */
    public List<Component> getLines() {
        return lines;
    }

    /**
     * Appends a line.
     *
     * @param text the line, as text or a component
     * @return this event
     */
    public ItemTooltipEventJS add(Object text) {
        lines.add(TextWrapper.of(text));
        return this;
    }

    /**
     * Inserts a line at a position, counting the item's own name as line zero.
     *
     * @param index where to put it
     * @param text the line
     * @return this event
     */
    public ItemTooltipEventJS insert(int index, Object text) {
        lines.add(Math.max(0, Math.min(index, lines.size())), TextWrapper.of(text));
        return this;
    }
}
