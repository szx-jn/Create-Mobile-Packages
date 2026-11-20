package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import com.simibubi.create.content.logistics.box.PackageItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Spawn egg for a packed entity. The entity snapshot is stored under the
 * native ENTITY_DATA component. CMP restores these snapshots directly because
 * the ordinary spawn-egg path replaces their UUIDs and skips protected entity data.
 */
public class PackedMobSpawnEgg extends SpawnEggItem {

    public PackedMobSpawnEgg(Properties properties) {
        super(EntityType.ENDER_DRAGON, 0x4B7A2B, 0x000000, properties);
    }

    /**
     * Recovers Create packages stored inside legacy CMP spawn eggs. Earlier
     * builds could package Create's own PackageEntity, producing a spawn egg
     * that summoned another package and could repeat indefinitely.
     */
    public static ItemStack recoverLegacyCreatePackage(ItemStack stack, net.minecraft.core.HolderLookup.Provider registries) {
        if (!(stack.getItem() instanceof PackedMobSpawnEgg)) return ItemStack.EMPTY;

        CompoundTag entityTag = PackedEntityNbt.getEntityTag(stack);
        if (entityTag == null || !"create:package".equals(entityTag.getString("id"))
                || !entityTag.contains("Box", Tag.TAG_COMPOUND)) {
            return ItemStack.EMPTY;
        }

        ItemStack recovered = ItemStack.parse(registries, entityTag.getCompound("Box")).orElseThrow(() -> new IllegalArgumentException("Invalid saved nested Create package"));
        return PackageItem.isPackage(recovered) ? recovered : ItemStack.EMPTY;
    }

    /** Restores only server-created CMP snapshots, retaining entity and passenger identities. */
    public static net.minecraft.world.entity.Entity restoreSnapshot(net.minecraft.server.level.ServerLevel level, ItemStack stack,
            net.minecraft.core.BlockPos pos) {
        var snapshot = PackedEntityNbt.getEntityTag(stack);
        if (snapshot == null) return null;
        var restored = EntityType.loadEntityRecursive(snapshot, level, entity -> {
            entity.moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, entity.getYRot(), entity.getXRot());
            return entity;
        });
        return restored != null && level.tryAddFreshEntityWithPassengers(restored) ? restored : null;
    }

    @Override public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        var stack = context.getItemInHand();
        var level = context.getLevel();
        if (PackedEntityNbt.getEntityTag(stack) == null || level.getBlockEntity(context.getClickedPos()) instanceof net.minecraft.world.level.Spawner) {
            return super.useOn(context);
        }
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return net.minecraft.world.InteractionResult.SUCCESS;
        var clicked = context.getClickedPos();
        var pos = level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty() ? clicked : clicked.relative(context.getClickedFace());
        if (restoreSnapshot(server, stack, pos) == null) return net.minecraft.world.InteractionResult.FAIL;
        stack.shrink(1);
        level.gameEvent(context.getPlayer(), net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, pos);
        return net.minecraft.world.InteractionResult.CONSUME;
    }

    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            net.minecraft.world.entity.player.Player player, net.minecraft.world.InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (PackedEntityNbt.getEntityTag(stack) == null) return super.use(level, player, hand);
        var hit = getPlayerPOVHitResult(level, player, net.minecraft.world.level.ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != net.minecraft.world.phys.HitResult.Type.BLOCK) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!(level instanceof net.minecraft.server.level.ServerLevel server)) return net.minecraft.world.InteractionResultHolder.success(stack);
        var pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof net.minecraft.world.level.block.LiquidBlock)) return net.minecraft.world.InteractionResultHolder.pass(stack);
        if (!level.mayInteract(player, pos) || !player.mayUseItemAt(pos, hit.getDirection(), stack)) return net.minecraft.world.InteractionResultHolder.fail(stack);
        var restored = restoreSnapshot(server, stack, pos);
        if (restored == null) return net.minecraft.world.InteractionResultHolder.fail(stack);
        stack.shrink(1); // A captured identity cannot remain available for another spawn in creative mode.
        player.awardStat(net.minecraft.stats.Stats.ITEM_USED.get(this));
        level.gameEvent(player, net.minecraft.world.level.gameevent.GameEvent.ENTITY_PLACE, restored.position());
        return net.minecraft.world.InteractionResultHolder.consume(stack);
    }

    // -- type resolution ------------------------------------------------

    @Override
    public EntityType<?> getType(ItemStack stack) {
        CompoundTag entityTag = PackedEntityNbt.getEntityTag(stack);
        if (entityTag != null && entityTag.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            Optional<EntityType<?>> type = EntityType.byString(entityTag.getString("id"));
            return type.orElseThrow(() -> new IllegalArgumentException("Unknown packed entity: " + entityTag.getString("id")));
        }
        return super.getType(stack);
    }

    // -- display name ---------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag entityTag = PackedEntityNbt.getEntityTag(stack);
        if (entityTag != null && entityTag.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            return Component.literal("\u5237\u602A\u86CB\uFF08" + entityTag.getString("id") + "\uFF09");
        }
        return super.getName(stack);
    }

    // -- tooltip --------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
            List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        CompoundTag entityTag = PackedEntityNbt.getEntityTag(stack);
        if (entityTag != null && entityTag.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            tooltip.add(Component.literal("\u5B9E\u4F53: " + entityTag.getString("id")).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, context, tooltip, flag);
    }
}
