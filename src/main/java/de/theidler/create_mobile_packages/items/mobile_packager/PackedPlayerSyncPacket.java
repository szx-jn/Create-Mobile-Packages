package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

public class PackedPlayerSyncPacket extends SimplePacketBase {
    private final int id; private final boolean packed;
    public PackedPlayerSyncPacket(FriendlyByteBuf buf) { id=buf.readInt(); packed=buf.readBoolean(); }
    public PackedPlayerSyncPacket(int id, boolean packed) { this.id=id; this.packed=packed; }
    @Override public void write(FriendlyByteBuf buf) { buf.writeInt(id); buf.writeBoolean(packed); }
    @Override public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> PackedPlayerSyncClient.handle(id, packed)));
        return true;
    }
}
