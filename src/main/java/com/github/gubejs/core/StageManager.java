package com.github.gubejs.core;

import com.github.gubejs.net.GubejsNetwork;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * The game stages one player has reached — {@code player.stages.add('mined_diamond')}.
 *
 * <p>A pack uses stages to gate content: a recipe, a quest, a dimension. KubeJS reads them from
 * GameStages when it is installed and keeps its own set when it is not, and a script should not
 * have to know which. Stages set here are visible to a script either way.
 *
 * <p>Stored under {@code PlayerPersisted}, the one part of a player's Forge data that survives
 * death — a stage that a player loses by dying would gate content back off at the worst possible
 * moment.
 */
public final class StageManager {

    /** The Forge tag whose contents are copied to the new player object on respawn. */
    private static final String PERSISTED = "PlayerPersisted";

    /** Where the stage list lives inside that tag. */
    private static final String KEY = "gubejs:stages";

    private final Player player;

    public StageManager(Player player) {
        this.player = player;
    }

    /**
     * Reports whether the player has a stage.
     *
     * @param stage the stage name
     * @return {@code true} if they have it
     */
    public boolean has(String stage) {
        var list = read();

        for (var i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(stage)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gives the player a stage.
     *
     * @param stage the stage name
     * @return {@code true} if they did not already have it
     */
    public boolean add(String stage) {
        if (has(stage)) {
            return false;
        }

        var list = read();
        list.add(StringTag.valueOf(stage));
        write(list);
        return true;
    }

    /**
     * Takes a stage away.
     *
     * @param stage the stage name
     * @return {@code true} if they had it
     */
    public boolean remove(String stage) {
        var list = read();

        for (var i = 0; i < list.size(); i++) {
            if (list.getString(i).equals(stage)) {
                list.remove(i);
                write(list);
                return true;
            }
        }

        return false;
    }

    /**
     * Takes every stage away.
     *
     * @return how many were removed
     */
    public int clear() {
        var count = read().size();
        write(new ListTag());
        return count;
    }

    /**
     * Returns every stage the player has.
     *
     * @return the stage names
     */
    public List<String> getAll() {
        var list = read();
        var stages = new ArrayList<String>(list.size());

        for (var i = 0; i < list.size(); i++) {
            stages.add(list.getString(i));
        }

        return stages;
    }

    /**
     * Adds a stage if it is missing, removes it if it is there.
     *
     * @param stage the stage name
     * @return {@code true} if the player now has it
     */
    public boolean toggle(String stage) {
        if (has(stage)) {
            remove(stage);
            return false;
        }

        add(stage);
        return true;
    }

    @Override
    public String toString() {
        return String.join(", ", getAll());
    }

    private ListTag read() {
        return persisted().getList(KEY, Tag.TAG_STRING);
    }

    private void write(ListTag list) {
        persisted().put(KEY, list);

        // A stage the client does not know about would leave a client script -- a tooltip, a HUD
        // element -- looking at the wrong answer, so the player is told about its own stages.
        if (player instanceof ServerPlayer serverPlayer) {
            sync(serverPlayer);
        }
    }

    /**
     * Sends this player's stages to their client.
     *
     * <p>Called on every change and once on login, since a client that just connected has an
     * empty player object and no way to ask.
     *
     * @param player the player to tell
     */
    public static void sync(ServerPlayer player) {
        var data = new CompoundTag();
        data.put("stages", new StageManager(player).read());
        GubejsNetwork.sendToPlayer(player, GubejsNetwork.STAGES_CHANNEL, data);
    }

    /** The part of the player's persistent data that survives death. */
    private CompoundTag persisted() {
        var data = player.getPersistentData();

        if (!data.contains(PERSISTED, Tag.TAG_COMPOUND)) {
            data.put(PERSISTED, new CompoundTag());
        }

        return data.getCompound(PERSISTED);
    }
}
