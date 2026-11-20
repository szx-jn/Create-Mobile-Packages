package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.index.CMPPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.createmod.catnip.net.base.ClientboundPacketPayload;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public record ShowOversizedNbtScreenPacket() implements ClientboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, ShowOversizedNbtScreenPacket> STREAM_CODEC = StreamCodec.unit(new ShowOversizedNbtScreenPacket());
    @Override @OnlyIn(Dist.CLIENT) public void handle(LocalPlayer player) { Minecraft.getInstance().setScreen(new OversizedNbtScreen()); }
    @Override public PacketTypeProvider getTypeProvider() { return CMPPackets.SHOW_OVERSIZED_NBT_SCREEN; }
}
