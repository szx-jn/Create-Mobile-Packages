package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

public class EscapePackedPlayerPacket extends SimplePacketBase {
    public EscapePackedPlayerPacket(FriendlyByteBuf ignored) {}
    public EscapePackedPlayerPacket() {}
    @Override public void write(FriendlyByteBuf buf) {}
    @Override public boolean handle(NetworkEvent.Context context) { context.enqueueWork(() -> { if (context.getSender()!=null) PackedPlayerState.escape(context.getSender()); }); return true; }
}
