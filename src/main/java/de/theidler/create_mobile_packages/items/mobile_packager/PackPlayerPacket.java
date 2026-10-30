package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.foundation.networking.SimplePacketBase;
import de.theidler.create_mobile_packages.index.CMPPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

public class PackPlayerPacket extends SimplePacketBase {
    private final int targetId;
    private final boolean self;
    public PackPlayerPacket(FriendlyByteBuf buf) { targetId = buf.readInt(); self = buf.readBoolean(); }
    public PackPlayerPacket(int targetId, boolean self) { this.targetId = targetId; this.self = self; }
    @Override public void write(FriendlyByteBuf buf) { buf.writeInt(targetId); buf.writeBoolean(self); }
    @Override public boolean handle(NetworkEvent.Context context) {
        context.enqueueWork(() -> {
            ServerPlayer source = context.getSender();
            if (source == null || !source.isShiftKeyDown()) return;
            ItemStack held = source.getMainHandItem();
            if (!(held.getItem() instanceof MobilePackager)) held = source.getOffhandItem();
            if (!(held.getItem() instanceof MobilePackager)) return;
            Player target = self ? source : source.serverLevel().getEntity(targetId) instanceof Player p ? p : null;
            if (target == null || target.level() != source.level() || source.distanceToSqr(target) > 36) return;
            pack(source, target, self);
        });
        return true;
    }
    public static void pack(ServerPlayer source, Player target, boolean self) {
        if (!self && target.hasEffect(CMPPlayerPack.IMMUNITY.get())) {
            source.displayClientMessage(Component.translatable("create_mobile_packages.player_pack.recently_packed").withStyle(net.minecraft.ChatFormatting.YELLOW), false);
            return;
        }
        if (!(target instanceof ServerPlayer serverTarget) || PackedPlayerState.isPacked(target)) return;
        PackedPlayerState.setPacked(serverTarget, true, !self);
        if (!self) source.displayClientMessage(Component.translatable("create_mobile_packages.player_pack.packed", target.getDisplayName()), false);
    }
    public static void sync(ServerPlayer player) {
        CMPPackets.getChannel().send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player), new PackedPlayerSyncPacket(player.getId(), PackedPlayerState.isPacked(player)));
    }
}
