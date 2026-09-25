package de.theidler.create_mobile_packages.items.portable_stock_ticker.trash_menu;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import de.theidler.create_mobile_packages.index.CMPPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class SyncTrashItemsToClientPacket extends SimplePacketBase {

    private final List<ItemStack> items;

    public SyncTrashItemsToClientPacket(List<ItemStack> items) {
        this.items = items;
    }

    public SyncTrashItemsToClientPacket(FriendlyByteBuf buf) {
        int size = buf.readInt();
        items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            items.add(buf.readItem());
        }
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(items.size());
        for (ItemStack item : items) {
            buf.writeItem(item);
        }
    }

    @Override
    public boolean handle(net.minecraftforge.network.NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof TrashMenu trashMenu) {
                trashMenu.updateTrashInventory(items);
            }
        });
        return true;
    }
}
