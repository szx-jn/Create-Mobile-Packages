package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.index.CMPPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record PackedPlayerSyncPacket(int id, boolean packed) implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, PackedPlayerSyncPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PackedPlayerSyncPacket::id, ByteBufCodecs.BOOL, PackedPlayerSyncPacket::packed, PackedPlayerSyncPacket::new);
    @Override @OnlyIn(Dist.CLIENT) public void handle(LocalPlayer player) { PackedPlayerSyncClient.handle(id, packed); }
    @Override public PacketTypeProvider getTypeProvider() { return CMPPackets.SYNC_PACKED_PLAYER; }
}
