package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.index.CMPPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public record EscapePackedPlayerPacket() implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, EscapePackedPlayerPacket> STREAM_CODEC = StreamCodec.unit(new EscapePackedPlayerPacket());
    @Override public void handle(ServerPlayer player) { PackedPlayerState.escape(player); }
    @Override public PacketTypeProvider getTypeProvider() { return CMPPackets.ESCAPE_PACKED_PLAYER; }
}
