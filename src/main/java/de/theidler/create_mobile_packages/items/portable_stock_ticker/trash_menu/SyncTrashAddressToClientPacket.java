package de.theidler.create_mobile_packages.items.portable_stock_ticker.trash_menu;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;

public class SyncTrashAddressToClientPacket extends SimplePacketBase {

    private final String address;

    public SyncTrashAddressToClientPacket(String address) {
        this.address = address;
    }

    public SyncTrashAddressToClientPacket(FriendlyByteBuf buf) {
        this.address = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(address);
    }

    @Override
    public boolean handle(net.minecraftforge.network.NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof TrashMenu trashMenu) {
                trashMenu.setTargetAddress(address);
            }
        });
        return true;
    }
}
