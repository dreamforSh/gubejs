package com.github.gubejs.block;

import org.jetbrains.annotations.Nullable;

/**
 * The properties a script changed on a block that already existed.
 *
 * <p>Every field starts as {@code null}, meaning "the script said nothing about this" — the same
 * arrangement as {@link com.github.gubejs.item.ItemModifications}, and for the same reason:
 * {@code 0} is a real hardness and {@code false} is a real answer to whether a tool is needed.
 */
public final class BlockModifications {

    /** How long the block takes to break, or {@code null} to leave it. */
    @Nullable
    public Float hardness;

    /** How well it resists explosions, or {@code null} to leave it. */
    @Nullable
    public Float resistance;

    /** Whether the right tool is needed for it to drop anything, or {@code null} to leave it. */
    @Nullable
    public Boolean requiresTool;

    /**
     * Sets how long the block takes to break.
     *
     * @param value the hardness; stone is 1.5, obsidian is 50, {@code -1} is unbreakable
     */
    public void setHardness(float value) {
        hardness = value;
    }

    /**
     * Sets how well the block resists explosions.
     *
     * @param value the resistance; stone is 6, obsidian is 1200
     */
    public void setResistance(float value) {
        resistance = value;
    }

    /**
     * Sets whether the right tool is needed for the block to drop anything.
     *
     * @param value {@code true} to require the tool
     */
    public void setRequiresTool(boolean value) {
        requiresTool = value;
    }
}
