package de.theidler.create_mobile_packages.items.portable_stock_ticker.trash_menu;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.LogisticallyLinkedItem;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.PortableStockTicker;
import de.theidler.create_mobile_packages.robo.RoboManager;
import de.theidler.create_mobile_packages.robo.RoboTrashStore;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class OpenTrashMenuPacket extends SimplePacketBase {

    public OpenTrashMenuPacket() {
    }

    public OpenTrashMenuPacket(FriendlyByteBuf buf) {
    }

    @Override
    public void write(FriendlyByteBuf buf) {
    }

    @Override
    public boolean handle(net.minecraftforge.network.NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !player.isAlive()) return;

            ItemStack pstItem = PortableStockTicker.find(player.getInventory());
            if (pstItem == null) return;
            PortableStockTicker pst = (PortableStockTicker) pstItem.getItem();
            UUID networkId = LogisticallyLinkedItem.networkFromStack(pstItem);
            if (networkId == null) return;

            RoboTrashStore trashStore = RoboManager.get(player.serverLevel()).getTrashStore(networkId, player.getUUID());
            String targetAddress = trashStore != null ? trashStore.getTargetAddress() : "";

            player.closeContainer();
            player.openMenu(new SimpleMenuProvider(
                    (id, inv, p) -> new TrashMenu(id, inv, pst, targetAddress),
                    Component.translatable("item.create_mobile_packages.portable_stock_ticker.trash_menu")
            ));
        });
        return true;
    }
}
