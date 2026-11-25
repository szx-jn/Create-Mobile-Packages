package de.theidler.create_mobile_packages;

import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.entities.robo_entity.RoboEntity;
import de.theidler.create_mobile_packages.robo.PlayerNameCache;
import de.theidler.create_mobile_packages.robo.RoboManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTravelToDimensionEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;



import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import de.theidler.create_mobile_packages.items.mobile_packager.PackagerCapabilities;
import de.theidler.create_mobile_packages.items.mobile_packager.ability.PackagerCapability;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.LogicalSide;

import java.util.List;

@net.neoforged.fml.common.EventBusSubscriber
public class CommonEvents {

    @SubscribeEvent
    public static void onServerWorldTick(LevelTickEvent.Pre event) {
        Level level = event.getLevel();
        if (level instanceof ServerLevel serverLevel)
            RoboManager.get(serverLevel).tick(serverLevel);
    }

    @EventBusSubscriber
    public static class ModBusEvents {

        @SubscribeEvent
        public static void registerCapabilities(net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event) {
            BeePortBlockEntity.registerCapabilities(event);
        }
    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof RoboEntity)) return;
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerNameCache.get(serverPlayer.serverLevel())
                    .addPlayerName(serverPlayer.getUUID(), serverPlayer.getGameProfile().getName());
        }
    }

    /**
     * RightClickBlock — fires when the player right-clicks a block.
     * Scan nearby entities and pack the first one found.
     * Uses LOW priority so we run before chest/dispenser/etc handlers.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == LogicalSide.CLIENT) return;
        if (!isHoldingPackager(event.getEntity(), event.getHand())) return;

        Player player = event.getEntity();
        ServerLevel level = (ServerLevel) player.level();

        // Scan a generous area around the clicked position
        List<Entity> entities = level.getEntities(null,
                new net.minecraft.world.phys.AABB(event.getPos()).inflate(2.0));

        for (Entity entity : entities) {
            Entity root = MobilePackager.resolveRoot(entity);
            if (root instanceof Player || root instanceof RoboEntity) continue;

            var result = MobilePackager.packEntity(player, root);
            if (result.consumesAction()) {
                event.setCanceled(true);
                event.setUseBlock(net.neoforged.neoforge.common.util.TriState.FALSE);
                event.setUseItem(net.neoforged.neoforge.common.util.TriState.FALSE);
                return;
            }
        }
    }

    /**
     * EntityInteract — fires when the player right-clicks an entity directly.
     * This is the ONLY way to catch entities when the player aims at them
     * without a block behind (e.g. Ender Dragon flying, TNT in air, etc.).
     * Only fires when the entity's own interact() returns PASS.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getSide() == LogicalSide.CLIENT) return;
        if (!isHoldingPackager(event.getEntity())) return;

        Player player = event.getEntity();
        Entity target = event.getTarget();
        if (target == null) return;

        Entity root = MobilePackager.resolveRoot(target);
        if (root instanceof Player || root instanceof RoboEntity) return;

        var result = MobilePackager.packEntity(player, root);
        if (result.consumesAction()) {
            event.setCanceled(true);
            event.setCancellationResult(net.minecraft.world.InteractionResult.SUCCESS);
        }
    }

    @SubscribeEvent
    public static void preserveBoxOrigin(net.neoforged.neoforge.event.level.BlockDropsEvent event) {
        if (!(event.getBlockEntity() instanceof de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerCardboardBox box)
                || !box.createMobilePackages$isFromMobilePackager()) return;
        for (var drop : event.getDrops()) {
            if (drop.getItem().getItem() instanceof mekanism.common.item.block.ItemBlockCardboardBox) {
                drop.getItem().set(de.theidler.create_mobile_packages.index.CMPDataComponents.PACKAGER_ORIGIN, true);
            }
        }
    }

    private static boolean isHoldingPackager(Player player, net.minecraft.world.InteractionHand hand) {
        return PackagerCapabilities.has(player.getItemInHand(hand), PackagerCapability.ENTITY_PACKING);
    }
}
