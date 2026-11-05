package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.CreateMobilePackages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMobilePackages.MODID)
public final class PlayerPackEvents {
    private PlayerPackEvents() {}

    @SubscribeEvent
    public static void selfPack(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer) || !player.isShiftKeyDown()) return;
        ItemStack held = event.getItemStack();
        if (!(held.getItem() instanceof MobilePackager)) return;
        if (PackedPlayerState.isPacked(player)) return;
        PackPlayerPacket.pack(serverPlayer, player, true);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void restorePackedState(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PackedPlayerState.isPacked(player)) {
            PackedPlayerState.setPacked(player, true, player.getPersistentData().getBoolean(PackedPlayerState.PACKED_BY_OTHER));
        }
    }

    @SubscribeEvent
    public static void reducePackedPlayerDamage(LivingHurtEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && PackedPlayerState.isPacked(player)) {
            event.setAmount(event.getAmount() * 0.7F);
        }
    }

    /** Packed players use Create's normal belt passenger transport even though they are players. */
    @SubscribeEvent
    public static void carryPackedPlayers(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
            || !(event.player instanceof ServerPlayer player)
            || !player.isAlive()
            || !PackedPlayerState.isPacked(player)) {
            return;
        }

        net.minecraft.core.BlockPos feet = player.blockPosition();
        net.minecraft.core.BlockPos beltPos = com.simibubi.create.AllBlocks.BELT.has(player.level().getBlockState(feet))
            ? feet
            : com.simibubi.create.AllBlocks.BELT.has(player.level().getBlockState(feet.below())) ? feet.below() : null;
        if (beltPos == null) {
            return;
        }

        var state = player.level().getBlockState(beltPos);
        if (!com.simibubi.create.content.kinetics.belt.BeltBlock.canTransportObjects(state)) {
            return;
        }
        var belt = com.simibubi.create.content.kinetics.belt.BeltHelper.getControllerBE(player.level(), beltPos);
        if (belt == null || belt.passengers == null) {
            return;
        }

        var info = belt.passengers.get(player);
        if (info == null) {
            belt.passengers.put(player,
                new com.simibubi.create.content.kinetics.belt.transport.BeltMovementHandler.TransportedEntityInfo(beltPos, state));
            player.setOnGround(true);
        } else if (info.getTicksSinceLastCollision() != 0 || !beltPos.equals(player.blockPosition())) {
            info.refresh(beltPos, state);
        }
    }
}
