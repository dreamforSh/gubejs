package com.github.gubejs.net;

import com.github.gubejs.Gubejs;
import com.github.gubejs.bindings.event.NetworkEvents;
import com.github.gubejs.script.ScriptType;
import com.github.gubejs.util.NbtHelper;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

/**
 * The channel {@code player.sendData} and {@code NetworkEvents.dataReceived} talk over.
 *
 * <p>One message type in each direction, carrying a channel name and a compound tag. A pack is
 * expected to invent its own channel names, which is why the payload is a free-form tag rather
 * than anything typed: a script cannot register a serialiser, so the wire format has to be one
 * the mod already knows how to read.
 *
 * <p>The protocol version is accepted from anything, including a plain vanilla client. A pack that
 * sends data to a client without this mod simply gets nothing back, which is a better failure than
 * refusing the connection.
 */
public final class GubejsNetwork {

    private static final String PROTOCOL = "1";

    /**
     * The channel this mod talks to itself on.
     *
     * <p>Handled before the event is posted and never handed to a script: a pack listening for
     * "some data arrived" has no business seeing the bookkeeping that keeps {@code player.stages}
     * answering the same thing on both sides.
     */
    public static final String STAGES_CHANNEL = "gubejs:stages";

    /** The channel {@code player.paint} sends its screen descriptions over. */
    public static final String PAINT_CHANNEL = "gubejs:paint";

    /** The channel {@code player.notify} sends its toasts over. */
    public static final String NOTIFY_CHANNEL = "gubejs:notify";

    /** The channel itself, registered as the mod loads. */
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        Gubejs.id("data"), () -> PROTOCOL,
        NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));

    private GubejsNetwork() {
    }

    /** Registers the message types. Called once, while the mod is constructed. */
    public static void init() {
        CHANNEL.registerMessage(0, DataPacket.class, DataPacket::encode, DataPacket::decode,
            DataPacket::handle);
    }

    /**
     * Sends data to one player.
     *
     * @param player who to send it to
     * @param channel the channel name the listener registered against
     * @param data the payload, converted from whatever the script passed
     */
    public static void sendToPlayer(ServerPlayer player, String channel, @Nullable Object data) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
            new DataPacket(channel, tagOf(data)));
    }

    /**
     * Sends data to every player on the server.
     *
     * @param server the running server
     * @param channel the channel name
     * @param data the payload
     */
    public static void sendToAll(MinecraftServer server, String channel, @Nullable Object data) {
        var packet = new DataPacket(channel, tagOf(data));

        for (var player : server.getPlayerList().getPlayers()) {
            CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
        }
    }

    /**
     * Sends data from the client to the server.
     *
     * @param channel the channel name
     * @param data the payload
     */
    public static void sendToServer(String channel, @Nullable Object data) {
        CHANNEL.sendToServer(new DataPacket(channel, tagOf(data)));
    }

    /**
     * Applies a stage update on the client.
     *
     * <p>A method of its own so the dedicated server never verifies a body that names a client
     * class — the check above keeps it from being called there, and keeping the reference out of
     * the calling method keeps it from being resolved either.
     */
    private static void receiveStages(CompoundTag data) {
        if (com.github.gubejs.bindings.PlatformWrapper.isClient()) {
            com.github.gubejs.client.GubejsClient.applyStages(data);
        }
    }

    /** Applies a screen description on the client, for the same reason {@link #receiveStages} is
     * a method of its own. */
    private static void receivePaint(CompoundTag data) {
        if (com.github.gubejs.bindings.PlatformWrapper.isClient()) {
            com.github.gubejs.client.painter.Painter.INSTANCE.receive(data);
        }
    }

    /** Raises a toast on the client. */
    private static void receiveNotification(CompoundTag data) {
        if (com.github.gubejs.bindings.PlatformWrapper.isClient()) {
            com.github.gubejs.client.painter.ScriptToast.show(data);
        }
    }

    private static CompoundTag tagOf(@Nullable Object data) {
        var tag = NbtHelper.compound(data);
        return tag == null ? new CompoundTag() : tag;
    }

    /** A channel name and a payload, in both directions. */
    public static final class DataPacket {

        private final String channel;

        private final CompoundTag data;

        DataPacket(String channel, CompoundTag data) {
            this.channel = channel;
            this.data = data;
        }

        static void encode(DataPacket packet, FriendlyByteBuf buf) {
            buf.writeUtf(packet.channel);
            buf.writeNbt(packet.data);
        }

        static DataPacket decode(FriendlyByteBuf buf) {
            var channel = buf.readUtf();
            var tag = buf.readNbt();
            return new DataPacket(channel, tag == null ? new CompoundTag() : tag);
        }

        static void handle(DataPacket packet, Supplier<NetworkEvent.Context> supplier) {
            var context = supplier.get();

            // enqueueWork, not the netty thread: a listener will touch the world, and the world
            // belongs to the server or render thread.
            context.enqueueWork(() -> {
                if (context.getSender() == null) {
                    if (packet.channel.equals(STAGES_CHANNEL)) {
                        receiveStages(packet.data);
                        return;
                    }

                    if (packet.channel.equals(PAINT_CHANNEL)) {
                        receivePaint(packet.data);
                        return;
                    }

                    if (packet.channel.equals(NOTIFY_CHANNEL)) {
                        receiveNotification(packet.data);
                        return;
                    }
                }

                if (!NetworkEvents.DATA_RECEIVED.hasListeners()) {
                    return;
                }

                var sender = context.getSender();
                var type = sender == null ? ScriptType.CLIENT : ScriptType.SERVER;

                NetworkEvents.DATA_RECEIVED.post(type, packet.channel,
                    new NetworkEventJS(packet.channel, packet.data, sender));
            });

            context.setPacketHandled(true);
        }
    }
}
