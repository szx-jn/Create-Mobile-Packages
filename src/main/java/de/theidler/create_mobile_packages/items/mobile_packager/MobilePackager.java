package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.entities.robo_entity.RoboEntity;
import de.theidler.create_mobile_packages.index.CMPItems;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MobilePackager extends Item {

    private static long lastPackTime = 0;

    public MobilePackager(Properties pProperties) {
        super(pProperties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        if (!pLevel.isClientSide) {
            if (System.currentTimeMillis() - lastPackTime < 200) {
                return InteractionResultHolder.pass(pPlayer.getItemInHand(pUsedHand));
            }
            ItemStack stack = pPlayer.getItemInHand(pUsedHand);
            if (pPlayer.isShiftKeyDown()) {
                pPlayer.openMenu(new SimpleMenuProvider((id, inv, player) -> new MobilePackagerMenu(id, inv, this), stack.getDisplayName()));
            } else {
                pPlayer.openMenu(new SimpleMenuProvider((id, inv, player) -> new MobilePackagerEditMenu(id, inv, new MobilePackagerEdit(), PackageItem.containing(List.of())), stack.getDisplayName()));
            }
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();

        // 1. Scan nearby block area (covers entities on/near the block)
        List<Entity> entities = level.getEntities(null,
                new net.minecraft.world.phys.AABB(context.getClickedPos()).inflate(2.0));
        for (Entity entity : entities) {
            Entity root = resolveRoot(entity);
            if (root instanceof Player || root instanceof RoboEntity) continue;
            InteractionResult result = packEntity(player, root);
            if (result.consumesAction()) return result;
        }

        // 2. Raycast along player's look direction (covers flying entities, TNT, etc.)
        net.minecraft.world.phys.HitResult hit = player.pick(6.0, 0.0F, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            Entity hitEntity = ((net.minecraft.world.phys.EntityHitResult) hit).getEntity();
            Entity root = resolveRoot(hitEntity);
            if (root instanceof Player || root instanceof RoboEntity) return InteractionResult.PASS;
            return packEntity(player, root);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull net.minecraft.world.entity.LivingEntity target, @NotNull InteractionHand hand) {
        Entity root = resolveRoot(target);
        if (root instanceof Player || root instanceof RoboEntity) return InteractionResult.PASS;
        return packEntity(player, root);
    }

    // ── entity resolution ───────────────────────────────────────────────

    public static Entity resolveRoot(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof EnderDragonPart part) return part.parentMob;
        Entity r = tryField(entity, "parentMob", Entity.class);
        if (r != null) return r;
        r = tryField(entity, "parent", Entity.class);
        if (r != null) return r;
        r = tryField(entity, "owner", Entity.class);
        if (r != null) return r;
        r = tryField(entity, "master", Entity.class);
        if (r != null) return r;
        if (entity.getVehicle() != null) return entity.getVehicle();
        return entity;
    }

    private static Entity tryField(Entity entity, String name, Class<?> type) {
        try {
            var f = entity.getClass().getField(name);
            if (type.isAssignableFrom(f.getType()) && f.get(entity) instanceof Entity e) return e;
        } catch (NoSuchFieldException | IllegalAccessException ignored) {}
        return null;
    }

    // ── packing — full data including Forge Capabilities ─────────────────

    /**
     * Packs any Entity into a spawn-egg item inside a package.
     *
     * Captures the FULL entity data:
     *   1. Vanilla NBT via saveWithoutId()
     *   2. Forge Capabilities via IForgeEntity.serializeNBT() — this is where
     *      mods like Ice and Fire store dragon size, age, gender, etc.
     */
    public static @NotNull InteractionResult packEntity(@NotNull Player player, @NotNull Entity target) {
        if (target instanceof Player || target instanceof RoboEntity) return InteractionResult.PASS;
        if (target.level().isClientSide) return InteractionResult.SUCCESS;

        // 1. Vanilla NBT (addAdditionalSaveData)
        CompoundTag tag = new CompoundTag();
        target.saveWithoutId(tag);

        // 2. Forge Capabilities — the critical part for mod entities
        //    IForgeEntity exposes serializeNBT() which writes all registered
        //    capabilities into a CompoundTag. We store this under "ForgeData".
        try {
            net.minecraftforge.common.extensions.IForgeEntity forgeEntity =
                    (net.minecraftforge.common.extensions.IForgeEntity) target;
            CompoundTag capData = forgeEntity.serializeNBT();
            if (capData != null && !capData.isEmpty()) {
                tag.put("ForgeData", capData);
            }
        } catch (Exception ignored) {
            // Some entities may throw — fall back to vanilla-only data
        }

        // Strip transient / positional / metadata
        tag.remove("UUID");
        tag.remove("Pos");
        tag.remove("Motion");
        tag.remove("Dimension");
        tag.remove("PortalCooldown");
        tag.remove("HurtByTimestamp");
        tag.remove("Anger");
        tag.remove("AngerTime");
        if (tag.contains("Brain")) {
            CompoundTag brain = tag.getCompound("Brain");
            if (brain.contains("memories")) {
                brain.getCompound("memories").remove("minecraft:angry_at");
            }
        }

        // Ensure correct entity type ID
        tag.putString("id", EntityType.getKey(target.getType()).toString());

        ItemStack egg = new ItemStack(CMPItems.PACKED_MOB_SPAWN_EGG.get());
        egg.getOrCreateTag().put("entity_tag", tag);

        if (!insertIntoExistingPackage(player, egg)) {
            ItemStack box = PackageItem.containing(List.of(egg));
            player.getInventory().placeItemBackInInventory(box);
        }

        target.discard();
        player.level().playSound(null, target.blockPosition(), SoundEvents.BARREL_CLOSE, SoundSource.PLAYERS, 1.0F, 0.6F);
        lastPackTime = System.currentTimeMillis();
        return InteractionResult.SUCCESS;
    }

    private static boolean insertIntoExistingPackage(@NotNull Player player, @NotNull ItemStack egg) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack candidate = inv.getItem(i);
            if (!PackageItem.isPackage(candidate)) continue;
            ItemStackHandler contents = PackageItem.getContents(candidate);
            for (int slot = 0; slot < contents.getSlots(); slot++) {
                if (!contents.insertItem(slot, egg, false).isEmpty()) continue;
                ItemStack updated = PackageItem.containing(contents);
                String address = PackageItem.getAddress(candidate);
                if (!address.isEmpty()) PackageItem.addAddress(updated, address);
                inv.setItem(i, updated);
                return true;
            }
        }
        return false;
    }
}
