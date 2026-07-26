package com.github.gubejs.bindings;

import com.github.gubejs.util.ValueUtils;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.jetbrains.annotations.Nullable;

/**
 * The {@code UUID} global: reading and writing the ids the game identifies players and entities by.
 *
 * <p>Both spellings are accepted everywhere — the dashed {@code 069a79f4-44e9-4726-a5be-fca90e38aaf5}
 * a player sees and the undashed form that turns up in NBT and in web APIs.
 */
public final class UUIDWrapper {

    private UUIDWrapper() {
    }

    /**
     * Reads a UUID from whatever a script passed.
     *
     * @param value a string in either spelling, or a {@link UUID} already
     * @return the UUID, or {@code null} if the value is not one
     */
    @Nullable
    public static UUID of(@Nullable Object value) {
        var unwrapped = ValueUtils.unwrap(value);

        if (unwrapped instanceof UUID uuid) {
            return uuid;
        } else if (unwrapped instanceof CharSequence text) {
            return fromString(text.toString());
        }

        return null;
    }

    /**
     * Parses a UUID.
     *
     * @param text a string in either spelling
     * @return the UUID, or {@code null} if the text is not one
     */
    @Nullable
    public static UUID fromString(String text) {
        var s = text.trim();

        try {
            if (s.length() == 32) {
                // The undashed form, which is how Mojang's API and most NBT write it.
                return UUID.fromString(s.substring(0, 8) + "-" + s.substring(8, 12) + "-"
                    + s.substring(12, 16) + "-" + s.substring(16, 20) + "-" + s.substring(20));
            }

            return s.length() == 36 ? UUID.fromString(s) : null;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    /**
     * Renders a UUID in the dashed form.
     *
     * @param value the UUID
     * @return the string, or an empty string if the value is not a UUID
     */
    public static String toString(@Nullable Object value) {
        var uuid = of(value);
        return uuid == null ? "" : uuid.toString();
    }

    /**
     * Renders a UUID without its dashes.
     *
     * @param value the UUID
     * @return the string, or an empty string if the value is not a UUID
     */
    public static String toShortString(@Nullable Object value) {
        return toString(value).replace("-", "");
    }

    /**
     * Returns a new random UUID.
     *
     * @return the UUID
     */
    public static UUID random() {
        return UUID.randomUUID();
    }

    /**
     * Builds the same UUID every time from the same name.
     *
     * <p>What the game itself uses for offline-mode players and for fake players, so a script that
     * needs a stable id for something that has no account can produce the same one.
     *
     * @param name the name to derive it from
     * @return the UUID
     */
    public static UUID fromName(String name) {
        return UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Reports whether a string is a UUID in either spelling.
     *
     * @param text the text to test
     * @return {@code true} if it parses
     */
    public static boolean isUUID(String text) {
        return fromString(text) != null;
    }
}
