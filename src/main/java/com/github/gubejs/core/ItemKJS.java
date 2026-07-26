package com.github.gubejs.core;

import com.github.gubejs.item.ItemModifications;
import org.jetbrains.annotations.Nullable;

/**
 * Where an item keeps the properties a script changed on it.
 *
 * <p>A field on the item rather than a map keyed by item: the mixin that reads these runs inside
 * {@code getMaxStackSize}, which the game calls for every stack in every inventory slot it draws,
 * and a hash lookup there would be felt.
 */
public interface ItemKJS {

    /**
     * Returns what a script changed on this item.
     *
     * @return the modifications, or {@code null} if nothing was changed
     */
    @Nullable
    ItemModifications gjs$getModifications();

    /**
     * Returns what a script changed on this item, creating the record if there is none.
     *
     * @return the modifications
     */
    ItemModifications gjs$getOrCreateModifications();
}
