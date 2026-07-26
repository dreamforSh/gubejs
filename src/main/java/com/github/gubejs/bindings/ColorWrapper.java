package com.github.gubejs.bindings;

import com.github.gubejs.util.ValueUtils;
import java.util.Locale;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code Color} global: the several ways a pack writes a colour, all reduced to one int.
 *
 * <pre>{@code
 * Color.of('#ff0000')
 * Color.rgb(255, 0, 0)
 * Color.hsb(0, 1, 1)
 * Color.RED
 * }</pre>
 *
 * <p>Everything that takes a colour — a text component, a tooltip, a particle — wants an ARGB int,
 * so that is what these return rather than a colour object with methods. A script that wants the
 * parts back has {@link #getRed} and friends.
 */
public final class ColorWrapper {

    /** Fully opaque black. */
    public static final int BLACK = 0xFF000000;

    /** Fully opaque dark blue, matching {@code §1}. */
    public static final int DARK_BLUE = 0xFF0000AA;

    /** Fully opaque dark green, matching {@code §2}. */
    public static final int DARK_GREEN = 0xFF00AA00;

    /** Fully opaque dark aqua, matching {@code §3}. */
    public static final int DARK_AQUA = 0xFF00AAAA;

    /** Fully opaque dark red, matching {@code §4}. */
    public static final int DARK_RED = 0xFFAA0000;

    /** Fully opaque dark purple, matching {@code §5}. */
    public static final int DARK_PURPLE = 0xFFAA00AA;

    /** Fully opaque gold, matching {@code §6}. */
    public static final int GOLD = 0xFFFFAA00;

    /** Fully opaque gray, matching {@code §7}. */
    public static final int GRAY = 0xFFAAAAAA;

    /** Fully opaque dark gray, matching {@code §8}. */
    public static final int DARK_GRAY = 0xFF555555;

    /** Fully opaque blue, matching {@code §9}. */
    public static final int BLUE = 0xFF5555FF;

    /** Fully opaque green, matching {@code §a}. */
    public static final int GREEN = 0xFF55FF55;

    /** Fully opaque aqua, matching {@code §b}. */
    public static final int AQUA = 0xFF55FFFF;

    /** Fully opaque red, matching {@code §c}. */
    public static final int RED = 0xFFFF5555;

    /** Fully opaque light purple, matching {@code §d}. */
    public static final int LIGHT_PURPLE = 0xFFFF55FF;

    /** Fully opaque yellow, matching {@code §e}. */
    public static final int YELLOW = 0xFFFFFF55;

    /** Fully opaque white, matching {@code §f}. */
    public static final int WHITE = 0xFFFFFFFF;

    /** Fully transparent. */
    public static final int TRANSPARENT = 0;

    private ColorWrapper() {
    }

    /**
     * Reads a colour from whatever a script passed.
     *
     * <p>Accepts {@code '#ff0000'}, {@code 'ff0000'}, {@code 'red'}, a number, or an object with
     * {@code r}/{@code g}/{@code b} keys.
     *
     * @param value what names the colour
     * @return the colour as ARGB, opaque unless an alpha was given
     */
    public static int of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped == null) {
            return TRANSPARENT;
        } else if (unwrapped instanceof Number number) {
            var rgb = number.intValue();
            // A plain 0xRRGGBB means opaque. Only a value that actually carries alpha bits keeps
            // them, so Color.of(0xFF0000) is red rather than invisible.
            return (rgb & 0xFF000000) == 0 ? rgb | 0xFF000000 : rgb;
        } else if (unwrapped instanceof CharSequence text) {
            return fromString(text.toString().trim());
        } else if (unwrapped instanceof Map<?, ?> map) {
            return fromMap(map);
        }

        return TRANSPARENT;
    }

    /**
     * Builds an opaque colour from its parts.
     *
     * @param red 0-255
     * @param green 0-255
     * @param blue 0-255
     * @return the colour as ARGB
     */
    public static int rgb(int red, int green, int blue) {
        return 0xFF000000 | clamp(red) << 16 | clamp(green) << 8 | clamp(blue);
    }

    /**
     * Builds a colour from its parts, with transparency.
     *
     * @param red 0-255
     * @param green 0-255
     * @param blue 0-255
     * @param alpha 0-255, where 0 is invisible
     * @return the colour as ARGB
     */
    public static int rgba(int red, int green, int blue, int alpha) {
        return clamp(alpha) << 24 | clamp(red) << 16 | clamp(green) << 8 | clamp(blue);
    }

    /**
     * Builds a colour from hue, saturation and brightness.
     *
     * <p>What a script uses to walk a rainbow or to shade one colour, since stepping a hue is a
     * single addition and stepping an RGB triple is not.
     *
     * @param hue 0-1, wrapping
     * @param saturation 0-1
     * @param brightness 0-1
     * @return the colour as ARGB
     */
    public static int hsb(float hue, float saturation, float brightness) {
        // Written out rather than taken from java.awt.Color, which a dedicated server has no
        // business loading -- the class initialises the AWT toolkit on some platforms.
        var h = (hue - (float) Math.floor(hue)) * 6F;
        var s = Math.max(0F, Math.min(1F, saturation));
        var v = Math.max(0F, Math.min(1F, brightness));

        var sector = (int) h;
        var f = h - sector;
        var p = v * (1F - s);
        var q = v * (1F - s * f);
        var t = v * (1F - s * (1F - f));

        return switch (sector) {
            case 0 -> rgb(scale(v), scale(t), scale(p));
            case 1 -> rgb(scale(q), scale(v), scale(p));
            case 2 -> rgb(scale(p), scale(v), scale(t));
            case 3 -> rgb(scale(p), scale(q), scale(v));
            case 4 -> rgb(scale(t), scale(p), scale(v));
            default -> rgb(scale(v), scale(p), scale(q));
        };
    }

    private static int scale(float part) {
        return (int) (part * 255F + 0.5F);
    }

    /**
     * Returns the red part.
     *
     * @param color an ARGB colour
     * @return 0-255
     */
    public static int getRed(int color) {
        return color >> 16 & 0xFF;
    }

    /**
     * Returns the green part.
     *
     * @param color an ARGB colour
     * @return 0-255
     */
    public static int getGreen(int color) {
        return color >> 8 & 0xFF;
    }

    /**
     * Returns the blue part.
     *
     * @param color an ARGB colour
     * @return 0-255
     */
    public static int getBlue(int color) {
        return color & 0xFF;
    }

    /**
     * Returns the alpha part.
     *
     * @param color an ARGB colour
     * @return 0-255, where 0 is invisible
     */
    public static int getAlpha(int color) {
        return color >>> 24 & 0xFF;
    }

    /**
     * Renders a colour as {@code #rrggbb}.
     *
     * @param color an ARGB colour
     * @return the hex form, with the alpha kept only when it is not fully opaque
     */
    public static String toHex(int color) {
        return getAlpha(color) == 0xFF
            ? String.format("#%06X", color & 0xFFFFFF)
            : String.format("#%08X", color);
    }

    /**
     * Mixes two colours.
     *
     * @param from the colour at {@code 0}
     * @param to the colour at {@code 1}
     * @param amount how far between them, 0-1
     * @return the mixed colour
     */
    public static int lerp(int from, int to, float amount) {
        var t = Math.max(0F, Math.min(1F, amount));
        return rgba(
            (int) (getRed(from) + (getRed(to) - getRed(from)) * t),
            (int) (getGreen(from) + (getGreen(to) - getGreen(from)) * t),
            (int) (getBlue(from) + (getBlue(to) - getBlue(from)) * t),
            (int) (getAlpha(from) + (getAlpha(to) - getAlpha(from)) * t));
    }

    private static int fromString(String text) {
        if (text.isEmpty()) {
            return TRANSPARENT;
        }

        var hex = text.startsWith("#") ? text.substring(1) : text;

        try {
            var value = (int) Long.parseLong(hex, 16);
            return hex.length() <= 6 ? value | 0xFF000000 : value;
        } catch (NumberFormatException ignored) {
            // Not hex, so it is a name -- the sixteen chat formatting colours, which is the set a
            // pack shares with the rest of the game.
        }

        return switch (text.toLowerCase(Locale.ROOT).replace('-', '_')) {
            case "black" -> BLACK;
            case "dark_blue" -> DARK_BLUE;
            case "dark_green" -> DARK_GREEN;
            case "dark_aqua" -> DARK_AQUA;
            case "dark_red" -> DARK_RED;
            case "dark_purple" -> DARK_PURPLE;
            case "gold", "orange" -> GOLD;
            case "gray", "grey" -> GRAY;
            case "dark_gray", "dark_grey" -> DARK_GRAY;
            case "blue" -> BLUE;
            case "green", "lime" -> GREEN;
            case "aqua", "cyan" -> AQUA;
            case "red" -> RED;
            case "light_purple", "pink", "magenta" -> LIGHT_PURPLE;
            case "yellow" -> YELLOW;
            case "white" -> WHITE;
            default -> TRANSPARENT;
        };
    }

    private static int fromMap(Map<?, ?> map) {
        return rgba(intOf(map, "r", "red"), intOf(map, "g", "green"), intOf(map, "b", "blue"),
            map.containsKey("a") || map.containsKey("alpha") ? intOf(map, "a", "alpha") : 255);
    }

    private static int intOf(Map<?, ?> map, String shortKey, String longKey) {
        var value = map.containsKey(shortKey) ? map.get(shortKey) : map.get(longKey);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static int clamp(int part) {
        return Math.max(0, Math.min(255, part));
    }
}
