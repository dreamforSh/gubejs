package com.github.gubejs.fluid;

import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fluids.FluidType;

/**
 * A fluid type that knows how it is drawn.
 *
 * <p>{@link FluidType} carries the physics — density, viscosity, temperature — and nothing about
 * appearance, because appearance is a client concern and the type is registered on both sides.
 * Forge's way across is {@link #initializeClient}, which the client calls once per type to ask for
 * the rendering hooks.
 *
 * <p>The client class is named only inside that method. A dedicated server never calls it, and the
 * JVM resolves the types in a method body the first time the method runs — so the class it names
 * is never looked for on a side that does not have it.
 */
public class GubejsFluidType extends FluidType {

    private final ResourceLocation stillTexture;

    private final ResourceLocation flowingTexture;

    private final ResourceLocation overlayTexture;

    /** The colour the textures are tinted with, as ARGB. */
    private final int tintColor;

    public GubejsFluidType(Properties properties, ResourceLocation stillTexture,
                           ResourceLocation flowingTexture, ResourceLocation overlayTexture,
                           int tintColor) {
        super(properties);
        this.stillTexture = stillTexture;
        this.flowingTexture = flowingTexture;
        this.overlayTexture = overlayTexture;
        this.tintColor = tintColor;
    }

    @Override
    public void initializeClient(Consumer<net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions> consumer) {
        consumer.accept(new net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions() {

            @Override
            public ResourceLocation getStillTexture() {
                return stillTexture;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return flowingTexture;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return overlayTexture;
            }

            @Override
            public int getTintColor() {
                return tintColor;
            }
        });
    }
}
