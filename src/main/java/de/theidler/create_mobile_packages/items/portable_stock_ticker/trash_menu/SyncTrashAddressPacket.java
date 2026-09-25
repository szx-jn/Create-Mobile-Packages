package de.theidler.create_mobile_packages.items.portable_stock_ticker.trash_menu;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.LogisticallyLinkedItem;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.PortableStockTicker;
import de.theidler.create_mobile_packages.robo.RoboManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class SyncTrashAddressPacket extends SimplePacketBase {

    private final String address;

    public SyncTrashAddressPacket(String address) {
        this.address = address;
    }

    public SyncTrashAddressPacket(FriendlyByteBuf buf) {
        this.address = buf.readUtf();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(address);
    }

    @Override
    public boolean handle(net.minecraftforge.network.NetworkEvent.Context ctx) {
        ctx.enqueueWork(() -> {
            ServerPlayer player = ctx.getSender();
            if (player == null || !(player.level() instanceof ServerLevel serverLevel)) return;

            ItemStack pstItem = PortableStockTicker.find(player.getInventory());
            if (pstItem == null) return;

            UUID networkId = LogisticallyLinkedItem.networkFromStack(pstItem);
            if (networkId == null) return;

            RoboManager roboManager = RoboManager.get(serverLevel);
            roboManager.setTrashTargetAddress(networkId, player.getUUID(), address);
        });
        return true;
    }
}
