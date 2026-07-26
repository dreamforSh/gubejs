package com.github.gubejs.registry;

import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for everything a script can create in a registry.
 *
 * <p>A builder is not the object; it is the description a script writes, kept until the game asks
 * that registry to be filled. Which is why every setter returns {@code this} and nothing is
 * validated until {@link #createObject()} runs — a script sets properties in whatever order reads
 * best, and the registry decides when.
 *
 * @param <T> what this builds
 */
public abstract class BuilderBase<T> {

    /** The id the object is registered under. */
    public final ResourceLocation id;

    /** The display name, or {@code null} to derive one from the id. */
    @Nullable
    protected String displayName;

    /** Extra translation keys this object needs, filled in by subclasses. */
    protected final Map<String, String> translations = new LinkedHashMap<>();

    @Nullable
    private T created;

    protected BuilderBase(ResourceLocation id) {
        this.id = id;
    }

    /**
     * Builds the object.
     *
     * <p>Called once, while the matching registry is being filled.
     *
     * @return the new object
     */
    public abstract T createObject();

    /**
     * Returns the object, building it on first use.
     *
     * @return the built object
     */
    public T get() {
        if (created == null) {
            created = createObject();
        }

        return created;
    }

    /**
     * Sets the name shown in the inventory and in tooltips.
     *
     * @param name the display name
     * @return this builder
     */
    public BuilderBase<T> displayName(Object name) {
        this.displayName = String.valueOf(ValueUtils.unwrap(name));
        return this;
    }

    /**
     * Returns the display name, deriving one from the id when none was set.
     *
     * <p>{@code mypack:steel_ingot} becomes {@code Steel Ingot}, which is right often enough that
     * most builders never set one.
     *
     * @return the display name
     */
    public String getDisplayName() {
        if (displayName != null) {
            return displayName;
        }

        var builder = new StringBuilder();

        for (var word : id.getPath().split("[_/]")) {
            if (word.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(' ');
            }

            builder.append(Character.toUpperCase(word.charAt(0))).append(word, 1, word.length());
        }

        return builder.toString();
    }

    /**
     * Returns the translation entries this object needs in the generated language file.
     *
     * @return translation key to English text
     */
    public Map<String, String> getTranslations() {
        return translations;
    }

    /**
     * Returns the assets this object needs, as pack paths to file contents.
     *
     * <p>Models, block states and anything else that would otherwise have to be written by hand.
     * A file the pack already provides under {@code assets/} wins, so a builder generating a model
     * never overwrites one an author wrote.
     *
     * @return pack path to JSON text, empty when nothing needs generating
     */
    public Map<String, String> getGeneratedAssets() {
        return Map.of();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" + id + ")";
    }
}
