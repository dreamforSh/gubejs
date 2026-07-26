package com.github.gubejs.client;

import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Base class for events that only happen on a client.
 *
 * <p>Everything here can be {@code null}: a client script's context exists from the moment the
 * resource packs load, which is long before there is a world or a player.
 */
public class ClientEventJS extends EventJS implements ScriptTypeHolder {

    /**
     * Returns the game instance.
     *
     * @return the client
     */
    public Minecraft getClient() {
        return Minecraft.getInstance();
    }

    /**
     * Returns the player at the keyboard.
     *
     * @return the player, or {@code null} outside a world
     */
    @Nullable
    public LocalPlayer getPlayer() {
        return Minecraft.getInstance().player;
    }

    /**
     * Returns the level the player is in.
     *
     * @return the level, or {@code null} outside a world
     */
    @Nullable
    public ClientLevel getLevel() {
        return Minecraft.getInstance().level;
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return ScriptType.CLIENT;
    }
}
