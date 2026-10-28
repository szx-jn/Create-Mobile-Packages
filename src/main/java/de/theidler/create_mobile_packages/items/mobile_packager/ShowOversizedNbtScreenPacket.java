package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

public class ShowOversizedNbtScreenPacket extends SimplePacketBase {
    public ShowOversizedNbtScreenPacket() {
    }

    public ShowOversizedNbtScreenPacket(FriendlyByteBuf buffer) {
    }

    @Override
    public void write(FriendlyByteBuf buffer) {
    }

    @Override
    public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> Minecraft.getInstance().setScreen(new OversizedNbtScreen()));
        context.setPacketHandled(true);
        return true;
    }
}
