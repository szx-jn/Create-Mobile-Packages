package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.CreateMobilePackages;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
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
}
