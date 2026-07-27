package com.github.gubejs.item;

import com.github.gubejs.registry.RegistryInfo;
import com.github.gubejs.util.ConsoleJS;
import com.github.gubejs.util.ValueUtils;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.RecordItem;
import net.minecraft.world.item.Rarity;
import org.jetbrains.annotations.Nullable;

/**
 * Builds a music disc — {@code event.create('overture', 'music_disc').sound('mypack:overture')}.
 *
 * <p>The sound is a {@link com.github.gubejs.misc.SoundEventBuilder sound event}, which a startup
 * script can create in the same run:
 *
 * <pre>{@code
 * StartupEvents.registry('sound_event', event => {
 *     event.create('overture').stream(true)
 * })
 *
 * StartupEvents.registry('item', event => {
 *     event.create('overture_disc', 'music_disc')
 *         .sound('mypack:overture')
 *         .lengthInSeconds(184)
 *         .analogOutput(15)
 * })
 * }</pre>
 *
 * <p>The length is what a jukebox uses to know when to stop; getting it wrong does not cut the
 * track short, it only leaves the jukebox thinking it is still playing. The analog output is what
 * a comparator reads next to the jukebox, which is how vanilla's thirteen discs are told apart.
 *
 * <p>The description line under the name — "C418 - cat" — is a translation, generated from the
 * display name.
 */
public class MusicDiscItemBuilder extends ItemBuilder {

    /** What the jukebox plays, as a sound event id. */
    @Nullable
    protected Object sound;

    /** How long the track is, in seconds. */
    protected int lengthInSeconds = 60;

    /** What a comparator next to the jukebox reads. */
    protected int analogOutput = 1;

    /** The line shown under the item's name, or {@code null} to derive one. */
    @Nullable
    protected String description;

    public MusicDiscItemBuilder(ResourceLocation id) {
        super(id);
        this.maxStackSize = 1;
        this.rarity = Rarity.RARE;
        this.tab = net.minecraft.world.item.CreativeModeTab.TAB_MISC;
    }

    /**
     * Sets what the jukebox plays.
     *
     * @param sound the sound event id
     * @return this builder
     */
    public MusicDiscItemBuilder sound(Object sound) {
        this.sound = ValueUtils.unwrap(sound);
        return this;
    }

    /**
     * Sets how long the track is.
     *
     * @param lengthInSeconds the length in seconds
     * @return this builder
     */
    public MusicDiscItemBuilder lengthInSeconds(int lengthInSeconds) {
        this.lengthInSeconds = lengthInSeconds;
        return this;
    }

    /**
     * Sets what a comparator next to the playing jukebox reads.
     *
     * @param analogOutput 1 to 15; vanilla's discs use one value each
     * @return this builder
     */
    public MusicDiscItemBuilder analogOutput(int analogOutput) {
        this.analogOutput = analogOutput;
        return this;
    }

    /**
     * Sets the line shown under the item's name and on the jukebox's status message.
     *
     * @param description the text, conventionally "artist - track"
     * @return this builder
     */
    public MusicDiscItemBuilder description(Object description) {
        this.description = String.valueOf(ValueUtils.unwrap(description));
        return this;
    }

    @Override
    public Item createObject() {
        // A supplier, not the sound itself: the item registry is filled before the sound registry
        // in some orderings, and a disc built with a sound that does not exist yet would hold null
        // for the rest of the game.
        return new RecordItem(analogOutput, this::resolveSound, createProperties(),
            lengthInSeconds * 20) { };
    }

    private SoundEvent resolveSound() {
        if (sound instanceof SoundEvent found) {
            return found;
        }

        var soundId = sound == null ? null : ResourceLocation.tryParse(String.valueOf(sound));
        var found = soundId == null ? null : Registry.SOUND_EVENT.get(soundId);

        if (found == null) {
            ConsoleJS.STARTUP.error("Music disc " + id + " names the sound '" + sound
                + "', which is not registered");
            return net.minecraft.sounds.SoundEvents.MUSIC_DISC_11;
        }

        return found;
    }

    @Override
    public Map<String, String> getTranslations() {
        var translations = new LinkedHashMap<>(super.getTranslations());
        var key = "item." + id.getNamespace() + "." + id.getPath().replace('/', '.');
        translations.put(key + ".desc", description == null ? getDisplayName() : description);
        return translations;
    }

    /** Registers the music disc type scripts can create. */
    public static void registerTypes() {
        RegistryInfo.ITEM.addType("music_disc", MusicDiscItemBuilder::new);
    }
}
