package de.theidler.create_mobile_packages;

import de.theidler.create_mobile_packages.entities.robo_entity.RoboEntity;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import de.theidler.create_mobile_packages.robo.RoboManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.LogicalSide;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber
public class CommonEvents {

    @SubscribeEvent
    public static void onServerWorldTick(TickEvent.LevelTickEvent event) {
        if (event.phase == TickEvent.Phase.START) return;
        if (event.side == LogicalSide.CLIENT) return;
        if (!(event.level instanceof net.minecraft.server.level.ServerLevel)) return;
        RoboManager.get((ServerLevel) event.level).tick((ServerLevel) event.level);
    }

    @SubscribeEvent
    public static void onEntityTravelToDimension(EntityTravelToDimensionEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity() instanceof RoboEntity)) return;
        event.setCanceled(true);
    }

    /**
     * RightClickBlock — fires when the player right-clicks a block.
     * Scan nearby entities and pack the first one found.
     * Uses LOW priority so we run before chest/dispenser/etc handlers.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getSide() == LogicalSide.CLIENT) return;
        if (!isHoldingPackager(event.getEntity())) return;

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
                event.setUseBlock(Event.Result.DENY);
                event.setUseItem(Event.Result.DENY);
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
            event.setResult(Event.Result.DENY);
        }
    }

    private static boolean isHoldingPackager(Player player) {
        ItemStack main = player.getMainHandItem();
        ItemStack off = player.getOffhandItem();
        return main.is(CMPItems.MOBILE_PACKAGER.get()) || off.is(CMPItems.MOBILE_PACKAGER.get());
    }
}
