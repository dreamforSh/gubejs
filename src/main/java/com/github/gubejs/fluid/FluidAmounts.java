package com.github.gubejs.fluid;

/**
 * The fluid amounts a pack writes out over and over, in millibuckets.
 *
 * <p>Bound as {@code FluidAmounts}, so a recipe reads {@code FluidAmounts.BUCKET * 4} rather than
 * {@code 4000} and stays right if a mod ever changes what a bucket holds.
 */
public final class FluidAmounts {

    /** One bucket. */
    public static final int BUCKET = 1000;

    /** A cauldron or a fluid block, which hold the same as a bucket. */
    public static final int BLOCK = 1000;

    /** A bottle, which is a third of a bucket rounded down the way vanilla brewing does. */
    public static final int BOTTLE = 250;

    /** The molten-metal-per-ingot amount most tech mods settled on. */
    public static final int INGOT = 90;

    /** A ninth of an ingot, matching a nugget. */
    public static final int NUGGET = 10;

    /** Nine ingots, matching a storage block. */
    public static final int METAL_BLOCK = 810;

    private FluidAmounts() {
    }

    /**
     * Converts a number of buckets to millibuckets.
     *
     * @param buckets how many buckets
     * @return the amount in millibuckets
     */
    public static int buckets(double buckets) {
        return (int) (buckets * BUCKET);
    }

    /**
     * Converts a number of ingots to millibuckets.
     *
     * @param ingots how many ingots
     * @return the amount in millibuckets
     */
    public static int ingots(double ingots) {
        return (int) (ingots * INGOT);
    }
}
