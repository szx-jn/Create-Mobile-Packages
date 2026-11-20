package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.index.CMPPackets;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.codec.ByteBufCodecs;
import net.createmod.catnip.net.base.ServerboundPacketPayload;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record PackPlayerPacket(int targetId, boolean self) implements ServerboundPacketPayload {
    public static final StreamCodec<RegistryFriendlyByteBuf, PackPlayerPacket> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, PackPlayerPacket::targetId, ByteBufCodecs.BOOL, PackPlayerPacket::self, PackPlayerPacket::new);
    @Override public void handle(ServerPlayer source) {
        if (!source.isShiftKeyDown()) return;
        ItemStack held = source.getMainHandItem();
        if (!(held.getItem() instanceof MobilePackager)) held = source.getOffhandItem();
        if (!(held.getItem() instanceof MobilePackager)) return;
        Player target = self ? source : source.serverLevel().getEntity(targetId) instanceof Player player ? player : null;
        if (target == null || target.level() != source.level() || source.distanceToSqr(target) > 36) return;
        pack(source, target, self);
    }
    public static void pack(ServerPlayer source, Player target, boolean self) {
        if (!self && target.hasEffect(CMPPlayerPack.IMMUNITY)) {
            source.displayClientMessage(Component.translatable("create_mobile_packages.player_pack.recently_packed").withStyle(net.minecraft.ChatFormatting.YELLOW), false);
            return;
        }
        if (!(target instanceof ServerPlayer serverTarget) || PackedPlayerState.isPacked(target)) return;
        PackedPlayerState.setPacked(serverTarget, true, !self);
        if (!self) source.displayClientMessage(Component.translatable("create_mobile_packages.player_pack.packed", target.getDisplayName()), false);
    }
    public static void sync(ServerPlayer player) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new PackedPlayerSyncPacket(player.getId(), PackedPlayerState.isPacked(player)));
    }
    @Override public PacketTypeProvider getTypeProvider() { return CMPPackets.PACK_PLAYER; }
}
