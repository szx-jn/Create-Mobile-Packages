package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.content.logistics.box.PackageItem;
import com.simibubi.create.content.logistics.box.PackageEntity;
import de.theidler.create_mobile_packages.entities.robo_entity.RoboEntity;
import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.index.CMPPackets;
import de.theidler.create_mobile_packages.compat.sable.SableCompat;
import de.theidler.create_mobile_packages.items.mobile_packager.ShowOversizedNbtScreenPacket;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
import net.minecraft.world.level.block.Block;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import de.theidler.create_mobile_packages.items.mobile_packager.ability.PackagerCapability;

public class MobilePackager extends Item {

    private static long lastPackTime = 0;

    public MobilePackager(Properties pProperties) {
        super(pProperties.stacksTo(1));
    }

    public boolean hasCapability(PackagerCapability capability) {
        return PackagerCapabilities.has(this, capability);
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
    public @NotNull InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        InteractionResult sableResult = hasCapability(PackagerCapability.SABLE_SHIP_PACKING)
            ? packSableShip(stack, context) : InteractionResult.PASS;
        if (sableResult.consumesAction()) return sableResult;
        if (!hasCapability(PackagerCapability.BLOCK_PACKING)) return InteractionResult.PASS;

        Item cardboardBox = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("mekanism", "cardboard_box"));
        if (cardboardBox == null || cardboardBox == Items.AIR) return InteractionResult.PASS;

        // Share the expanded cardboard-box policy while retaining Mekanism's
        // world protection and tile security checks.
        InteractionResult result = MobilePackagerBlockContext.pack(cardboardBox, context,
            hasCapability(PackagerCapability.SPECIAL_BLOCK_PACKING), hasCapability(PackagerCapability.MOD_COMPATIBILITY));
        Level level = context.getLevel();
        if (result.consumesAction() && !level.isClientSide) {
            Block cardboardBoxBlock = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("mekanism", "cardboard_box"));
            if (cardboardBoxBlock != null && level.getBlockState(context.getClickedPos()).is(cardboardBoxBlock)) {
                if (level.getBlockEntity(context.getClickedPos()) instanceof MobilePackagerCardboardBox cardboardBoxEntity) {
                    cardboardBoxEntity.createMobilePackages$setFromMobilePackager(true);
                }
            }
        }
        return result;
    }

    private static InteractionResult packSableShip(ItemStack packager, UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (level.isClientSide || player == null || player.isShiftKeyDown()
            || !SableCompat.isInsideSubLevel(level, context.getClickedPos())) {
            return InteractionResult.PASS;
        }

        int freeSlot = player.getInventory().getFreeSlot();
        if (freeSlot < 0) return InteractionResult.FAIL;

        Item bottleItem = BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("sableinbottle", "ship_in_a_bottle"));
        if (bottleItem == Items.AIR) return InteractionResult.PASS;

        InteractionHand hand = context.getHand();
        ItemStack bottle = new ItemStack(bottleItem);
        player.setItemInHand(hand, bottle);
        InteractionResult captureResult;
        try {
            captureResult = bottleItem.useOn(new UseOnContext(player, hand, recreateHitResult(context, false)));
            bottle = player.getItemInHand(hand);
        } finally {
            player.setItemInHand(hand, packager);
        }
        if (!captureResult.consumesAction()) return captureResult;
        if (bottle.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) == null) {
            return InteractionResult.FAIL;
        }
        ItemStack createPackage = PackageItem.containing(List.of(bottle));
        if (isNbtTooLarge((CompoundTag) createPackage.save(player.registryAccess()))) {
            restoreSableBottle(player, hand, packager, bottle, context);
            if (!bottle.isEmpty()
                && bottle.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA) != null) {
                player.getInventory().setItem(freeSlot, bottle);
            }
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(
                    serverPlayer, new ShowOversizedNbtScreenPacket());
            }
            return InteractionResult.FAIL;
        }

        var bottleData = bottle.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag();
        bottleData.putBoolean(MobilePackagerBlockContext.SABLE_SHIP_MARKER, true);
        bottle.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.of(bottleData));
        createPackage = PackageItem.containing(List.of(bottle));
        player.getInventory().setItem(freeSlot, createPackage);
        player.level().playSound(null, context.getClickedPos(), SoundEvents.BARREL_CLOSE,
            SoundSource.PLAYERS, 1.0F, 0.6F);
        lastPackTime = System.currentTimeMillis();
        return InteractionResult.SUCCESS;
    }

    private static void restoreSableBottle(Player player, InteractionHand hand, ItemStack packager,
                                           ItemStack bottle, UseOnContext originalContext) {
        player.setItemInHand(hand, bottle);
        try {
            bottle.getItem().useOn(new UseOnContext(player, hand, recreateHitResult(originalContext, true)));
        } finally {
            player.setItemInHand(hand, packager);
        }
    }

    private static BlockHitResult recreateHitResult(UseOnContext context, boolean projectOutOfSubLevel) {
        net.minecraft.world.phys.Vec3 location = context.getClickLocation();
        BlockPos pos = context.getClickedPos();
        if (projectOutOfSubLevel) {
            location = SableCompat.projectOutOfSubLevel(context.getLevel(), location);
            pos = BlockPos.containing(location);
        }
        return new BlockHitResult(location, context.getClickedFace(), pos, context.isInside());
    }

    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        if (!hasCapability(PackagerCapability.ENTITY_PACKING) || context.getLevel().isClientSide) return InteractionResult.PASS;
        Player player = context.getPlayer();
        if (player == null) return InteractionResult.PASS;
        Level level = context.getLevel();

        net.minecraft.world.phys.HitResult hit = player.pick(6.0, 0.0F, false);
        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.ENTITY) {
            Entity hitEntity = ((net.minecraft.world.phys.EntityHitResult) hit).getEntity();
            Entity root = resolveRoot(hitEntity);
            if (isForbiddenTarget(root)) return InteractionResult.PASS;
            return packEntity(player, root);
        }

        return InteractionResult.PASS;
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull net.minecraft.world.entity.LivingEntity target, @NotNull InteractionHand hand) {
        if (player.isShiftKeyDown() && target instanceof Player other) {
            if (!hasCapability(PackagerCapability.PLAYER_PACKING)) return InteractionResult.PASS;
            if (!player.level().isClientSide && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                PackPlayerPacket.pack(serverPlayer, other, false);
            }
            return InteractionResult.sidedSuccess(player.level().isClientSide);
        }
        if (!hasCapability(PackagerCapability.ENTITY_PACKING)) return InteractionResult.PASS;
        Entity root = resolveRoot(target);
        if (isForbiddenTarget(root)) return InteractionResult.PASS;
        return packEntity(player, root);
    }

    private static boolean isForbiddenTarget(Entity target) {
        return target == null || target instanceof Player || target instanceof RoboEntity || target instanceof PackageEntity;
    }

    // ── entity resolution ───────────────────────────────────────────────

    public static Entity resolveRoot(Entity entity) {
        if (entity == null) return null;
        if (entity instanceof net.neoforged.neoforge.entity.PartEntity<?> part) return part.getParent();
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

    // ── packing — full entity data ───────────────────────────────────────

    /**
     * Packs any Entity into a spawn-egg item inside a package.
     *
     * saveWithoutId() is the complete entity snapshot: vanilla state,
     * NeoForge attachments and persistent data are all written by the same method.
     */
    public static @NotNull InteractionResult packEntity(@NotNull Player player, @NotNull Entity target) {
        if (isForbiddenTarget(target) || target.getSelfAndPassengers().anyMatch(MobilePackager::isForbiddenTarget)) return InteractionResult.PASS;
        if (target.level().isClientSide) return InteractionResult.SUCCESS;

        // saveWithoutId also writes attachments and the entity's persistent data.
        CompoundTag tag = new CompoundTag();
        target.saveWithoutId(tag);

        // Location, motion and combat targets are transient. Entity state such
        // as Fuse, cat variant and persistent mod data must survive the trip.
        PackedEntityNbt.sanitizeForPackaging(tag);

        // Ensure correct entity type ID
        tag.putString("id", EntityType.getKey(target.getType()).toString());

        ItemStack egg = new ItemStack(CMPItems.PACKED_MOB_SPAWN_EGG.get());
        egg.set(net.minecraft.core.component.DataComponents.ENTITY_DATA, net.minecraft.world.item.component.CustomData.of(tag));

        // Keep the complete serialized item NBT below vanilla's network NBT limit.
        if (isNbtTooLarge((CompoundTag) egg.save(player.registryAccess()))) {
            net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer) player, new ShowOversizedNbtScreenPacket());
            return InteractionResult.FAIL;
        }

        if (!insertIntoExistingPackage(player, egg)) {
            ItemStack box = PackageItem.containing(List.of(egg));
            player.getInventory().placeItemBackInInventory(box);
        }

        // Remove the whole saved vehicle tree, after the complete package was accepted.
        var captured = target.getSelfAndPassengers().toList();
        for (int index = captured.size() - 1; index >= 0; index--) captured.get(index).discard();
        player.level().playSound(null, target.blockPosition(), SoundEvents.BARREL_CLOSE, SoundSource.PLAYERS, 1.0F, 0.6F);
        lastPackTime = System.currentTimeMillis();
        return InteractionResult.SUCCESS;
    }

    static boolean isNbtTooLarge(CompoundTag tag) {
        if (tag == null) return false;
        if (!validNbtStrings(tag)) return true;
        final int maxBytes = 2 * 1024 * 1024 - 50 * 1024; // 50 KiB below vanilla's 2 MiB limit.
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
             DataOutputStream output = new DataOutputStream(bytes)) {
            NbtIo.write(tag, output);
            output.flush();
            return bytes.size() > maxBytes;
        } catch (IOException exception) {
            // If the local size check itself fails, do not risk creating an oversized item.
            return true;
        }
    }

    // Native StringTag.write logs and substitutes an empty string on UTF overflow.
    // Reject before writing so the size check cannot accept a truncated snapshot.
    private static boolean validNbtStrings(net.minecraft.nbt.Tag tag) {
        if (tag instanceof net.minecraft.nbt.StringTag string) return validModifiedUtf(string.getAsString());
        if (tag instanceof CompoundTag compound) {
            for (String key : compound.getAllKeys()) if (!validModifiedUtf(key) || !validNbtStrings(compound.get(key))) return false;
        } else if (tag instanceof net.minecraft.nbt.ListTag list) {
            for (var entry : list) if (!validNbtStrings(entry)) return false;
        }
        return true;
    }
    private static boolean validModifiedUtf(String string) {
        int bytes = 0;
        for (int index = 0; index < string.length(); index++) {
            char value = string.charAt(index);
            bytes += value >= 1 && value <= 127 ? 1 : value <= 2047 ? 2 : 3;
            if (bytes > 65535) return false;
        }
        return true;
    }

    private static boolean insertIntoExistingPackage(@NotNull Player player, @NotNull ItemStack egg) {
        var inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack candidate = inv.getItem(i);
            if (!PackageItem.isPackage(candidate)) continue;
            ItemStackHandler contents = PackageItem.getContents(candidate);
            for (int slot = 0; slot < contents.getSlots(); slot++) {
                if (!contents.insertItem(slot, egg, false).isEmpty()) continue;
                ItemStack updated = candidate.copy();
                updated.set(com.simibubi.create.AllDataComponents.PACKAGE_CONTENTS,
                    net.minecraft.world.item.component.ItemContainerContents.fromItems(java.util.stream.IntStream.range(0, contents.getSlots())
                        .mapToObj(contents::getStackInSlot).toList()));
                inv.setItem(i, updated);
                return true;
            }
        }
        return false;
    }
}
