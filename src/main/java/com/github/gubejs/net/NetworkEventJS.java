package com.github.gubejs.net;

import com.github.graal.minecraft.NbtProxy;
import com.github.gubejs.event.EventJS;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.script.ScriptTypeHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

/**
 * Data that arrived over {@code player.sendData} or {@code Client.sendData}.
 *
 * <pre>{@code
 * // server_scripts
 * NetworkEvents.dataReceived('open_shop', event => {
 *     event.player.tell('shop id ' + event.data.shop)
 * })
 *
 * // client_scripts
 * Client.sendData('open_shop', { shop: 3 })
 * }</pre>
 */
public final class NetworkEventJS extends EventJS implements ScriptTypeHolder {

    private final String channel;

    private final CompoundTag data;

    @Nullable
    private final ServerPlayer player;

    public NetworkEventJS(String channel, CompoundTag data, @Nullable ServerPlayer player) {
        this.channel = channel;
        this.data = data;
        this.player = player;
    }

    /**
     * Returns which channel the data came in on.
     *
     * @return the channel name
     */
    public String getChannel() {
        return channel;
    }

    /**
     * Returns the payload as a plain object.
     *
     * <p>Reads and writes go straight through to the tag underneath, so {@code event.data.count++}
     * does what it looks like — though nothing sends the change back.
     *
     * @return the payload
     */
    public Object getData() {
        return NbtProxy.of(data);
    }

    /**
     * Returns the payload as the tag it arrived as.
     *
     * @return the compound
     */
    public CompoundTag getNbt() {
        return data;
    }

    /**
     * Returns who sent it.
     *
     * @return the player, or {@code null} when the server sent this to a client
     */
    @Nullable
    public ServerPlayer getPlayer() {
        return player;
    }

    @Override
    public ScriptType gjs$getScriptType() {
        return player == null ? ScriptType.CLIENT : ScriptType.SERVER;
    }
}
