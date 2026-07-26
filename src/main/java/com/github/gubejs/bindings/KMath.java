package com.github.gubejs.bindings;

import net.minecraft.util.Mth;

/**
 * The {@code KMath} global: the arithmetic a pack keeps reaching for.
 *
 * <p>JavaScript's own {@code Math} covers the trigonometry; what it has none of is clamping,
 * interpolation and range mapping, which is most of what a pack does with numbers. Minecraft's
 * {@link Mth} has them and is used here so a script and the game round the same way.
 */
public final class KMath {

    /** Pi, as the game rounds it. */
    public static final float PI = (float) Math.PI;

    /** Two pi, for a full turn. */
    public static final float TWO_PI = (float) (Math.PI * 2);

    /** Half pi, for a quarter turn. */
    public static final float HALF_PI = (float) (Math.PI / 2);

    /** Degrees in one radian. */
    public static final float DEG_TO_RAD = PI / 180F;

    /** Radians in one degree. */
    public static final float RAD_TO_DEG = 180F / PI;

    private KMath() {
    }

    /**
     * Keeps a number inside a range.
     *
     * @param value the number
     * @param min the lowest allowed
     * @param max the highest allowed
     * @return the number, moved into range if it was outside
     */
    public static double clamp(double value, double min, double max) {
        return Mth.clamp(value, min, max);
    }

    /**
     * Mixes two numbers.
     *
     * @param from the value at {@code 0}
     * @param to the value at {@code 1}
     * @param amount how far between them
     * @return the mixed value
     */
    public static double lerp(double from, double to, double amount) {
        return from + (to - from) * amount;
    }

    /**
     * Moves a number from one range into another.
     *
     * <p>{@code KMath.map(hp, 0, 20, 0, 100)} turns a health value into a percentage.
     *
     * @param value the number
     * @param fromMin the low end of the range it is in
     * @param fromMax the high end of the range it is in
     * @param toMin the low end of the range to put it in
     * @param toMax the high end of the range to put it in
     * @return the mapped number
     */
    public static double map(double value, double fromMin, double fromMax,
                             double toMin, double toMax) {
        // A zero-width source range has no answer; the low end is the least surprising one and
        // keeps a division by zero out of a tick loop.
        return fromMax == fromMin ? toMin
            : toMin + (value - fromMin) * (toMax - toMin) / (fromMax - fromMin);
    }

    /**
     * Rounds towards negative infinity, the way block coordinates work.
     *
     * @param value the number
     * @return the floor, as an int
     */
    public static int floor(double value) {
        return Mth.floor(value);
    }

    /**
     * Rounds towards positive infinity.
     *
     * @param value the number
     * @return the ceiling, as an int
     */
    public static int ceil(double value) {
        return Mth.ceil(value);
    }

    /**
     * Rounds to the nearest whole number.
     *
     * @param value the number
     * @return the rounded value, as an int
     */
    public static int round(double value) {
        return Math.round((float) value);
    }

    /**
     * Returns the distance from zero.
     *
     * @param value the number
     * @return its absolute value
     */
    public static double abs(double value) {
        return Math.abs(value);
    }

    /**
     * Returns the smaller of two numbers.
     *
     * @param a one number
     * @param b the other
     * @return the smaller
     */
    public static double min(double a, double b) {
        return Math.min(a, b);
    }

    /**
     * Returns the larger of two numbers.
     *
     * @param a one number
     * @param b the other
     * @return the larger
     */
    public static double max(double a, double b) {
        return Math.max(a, b);
    }

    /**
     * Wraps an angle into the -180 to 180 range the game stores rotations in.
     *
     * @param degrees the angle
     * @return the same angle, written the way an entity's yaw is
     */
    public static float wrapDegrees(double degrees) {
        return Mth.wrapDegrees((float) degrees);
    }

    /**
     * Converts degrees to radians.
     *
     * @param degrees the angle
     * @return the same angle in radians
     */
    public static double toRadians(double degrees) {
        return Math.toRadians(degrees);
    }

    /**
     * Converts radians to degrees.
     *
     * @param radians the angle
     * @return the same angle in degrees
     */
    public static double toDegrees(double radians) {
        return Math.toDegrees(radians);
    }

    /**
     * Returns the distance between two points.
     *
     * @param x1 the first point's x
     * @param y1 the first point's y
     * @param z1 the first point's z
     * @param x2 the second point's x
     * @param y2 the second point's y
     * @param z2 the second point's z
     * @return the distance
     */
    public static double dist(double x1, double y1, double z1,
                              double x2, double y2, double z2) {
        var dx = x2 - x1;
        var dy = y2 - y1;
        var dz = z2 - z1;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Returns the squared distance between two points.
     *
     * <p>What a range check should use: comparing squared distances gives the same answer and
     * skips the square root, which matters in anything running per tick per entity.
     *
     * @param x1 the first point's x
     * @param y1 the first point's y
     * @param z1 the first point's z
     * @param x2 the second point's x
     * @param y2 the second point's y
     * @param z2 the second point's z
     * @return the squared distance
     */
    public static double distSq(double x1, double y1, double z1,
                                double x2, double y2, double z2) {
        var dx = x2 - x1;
        var dy = y2 - y1;
        var dz = z2 - z1;
        return dx * dx + dy * dy + dz * dz;
    }

    /**
     * Returns a random whole number in a range, both ends included.
     *
     * @param min the lowest possible result
     * @param max the highest possible result
     * @return the number
     */
    public static int randomInt(int min, int max) {
        return min >= max ? min : UtilsWrapper.getRandom().nextInt(max - min + 1) + min;
    }

    /**
     * Returns a random number in a range.
     *
     * @param min the lowest possible result
     * @param max the highest possible result
     * @return the number
     */
    public static double randomDouble(double min, double max) {
        return min + UtilsWrapper.getRandom().nextDouble() * (max - min);
    }

    /**
     * Returns the sign of a number.
     *
     * @param value the number
     * @return {@code -1}, {@code 0} or {@code 1}
     */
    public static int sign(double value) {
        return (int) Math.signum(value);
    }
}
