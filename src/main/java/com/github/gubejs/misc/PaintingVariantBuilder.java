package com.github.gubejs.misc;

import com.github.gubejs.registry.BuilderBase;
import com.github.gubejs.registry.RegistryInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.decoration.PaintingVariant;

/**
 * Builds a painting — {@code event.create('sunrise').width(2).height(1)}.
 *
 * <p>The size is in blocks and the texture must match it: a 2×1 painting is a 32×16 image at
 * {@code assets/<namespace>/textures/painting/<path>.png}.
 *
 * <p>A new variant is registered but not placeable until it is in the
 * {@code #minecraft:placeable} painting variant tag — vanilla picks what a blank canvas becomes
 * from that tag, and an entry outside it can only be given out by a script or a command. Add it
 * from a server script:
 *
 * <pre>{@code
 * ServerEvents.tags('painting_variant', event => {
 *     event.add('minecraft:placeable', 'mypack:sunrise')
 * })
 * }</pre>
 */
public class PaintingVariantBuilder extends BuilderBase<PaintingVariant> {

    /** How many blocks wide it is. */
    protected int width = 1;

    /** How many blocks tall it is. */
    protected int height = 1;

    public PaintingVariantBuilder(ResourceLocation id) {
        super(id);
    }

    /**
     * Sets how many blocks wide the painting is.
     *
     * @param width the width in blocks; the texture is 16 pixels per block
     * @return this builder
     */
    public PaintingVariantBuilder width(int width) {
        this.width = width;
        return this;
    }

    /**
     * Sets how many blocks tall the painting is.
     *
     * @param height the height in blocks; the texture is 16 pixels per block
     * @return this builder
     */
    public PaintingVariantBuilder height(int height) {
        this.height = height;
        return this;
    }

    /**
     * Sets both dimensions at once.
     *
     * @param width the width in blocks
     * @param height the height in blocks
     * @return this builder
     */
    public PaintingVariantBuilder size(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }

    @Override
    public PaintingVariant createObject() {
        // In pixels, not blocks: the constructor takes the texture's own dimensions.
        return new PaintingVariant(width * 16, height * 16);
    }

    /** Registers the painting types scripts can create. */
    public static void registerTypes() {
        RegistryInfo.PAINTING_VARIANT.addType("basic", PaintingVariantBuilder::new)
            .defaultType("basic");
    }
}
