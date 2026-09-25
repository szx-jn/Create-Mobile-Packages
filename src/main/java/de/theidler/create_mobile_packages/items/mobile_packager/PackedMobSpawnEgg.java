package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Spawn egg for a packed entity. Stores full entity NBT in "entity_tag".
 * Extends SpawnEggItem so Create's PackageItem.open() works automatically.
 *
 * Overrides useOn() to manually load NBT after spawn, because
 * EntityType.spawn() only calls load() for Mob subclasses —
 * non-Mob entities (TNT, EndCrystal, projectiles, etc.) would lose their data.
 */
public class PackedMobSpawnEgg extends SpawnEggItem {

    public PackedMobSpawnEgg(Properties properties) {
        super(EntityType.ENDER_DRAGON, 0x4B7A2B, 0x000000, properties);
    }

    // ── type resolution from NBT ────────────────────────────────────────

    @Override
    public EntityType<?> getType(CompoundTag tag) {
        if (tag != null && tag.contains("entity_tag")) {
            CompoundTag entityTag = tag.getCompound("entity_tag");
            if (entityTag.contains("id")) {
                Optional<EntityType<?>> type = EntityType.byString(entityTag.getString("id"));
                if (type.isPresent()) return type.get();
            }
        }
        return EntityType.ENDER_DRAGON;
    }

    // ── spawn with full NBT restore ─────────────────────────────────────

    /**
     * Custom spawn logic: EntityType.spawn() only loads NBT for Mob subclasses.
     * We override to ensure ALL entity types (TNT, EndCrystal, projectiles, etc.)
     * get their full NBT restored.
     */
    @Override
    public @NotNull InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        ItemStack stack = context.getItemInHand();
        CompoundTag tag = stack.getTag();

        if (tag == null || !tag.contains("entity_tag") || level.isClientSide) {
            return InteractionResult.PASS;
        }

        CompoundTag entityTag = tag.getCompound("entity_tag");
        EntityType<?> entityType = getType(tag);
        if (entityType == null) return InteractionResult.PASS;

        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        Entity entity = entityType.create(serverLevel);
        if (entity == null) return InteractionResult.PASS;

        // Load full entity data from stored NBT
        entity.load(entityTag);

        // Position at clicked block
        entity.setPos(
                context.getClickedPos().getX() + 0.5,
                context.getClickedPos().getY() + 1,
                context.getClickedPos().getZ() + 0.5
        );
        entity.setUUID(java.util.UUID.randomUUID());

        serverLevel.addFreshEntity(entity);

        // Consume item
        Player player = context.getPlayer();
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResult.SUCCESS;
    }

    // ── display name ────────────────────────────────────────────────────

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("entity_tag")) {
            CompoundTag entityTag = tag.getCompound("entity_tag");
            if (entityTag.contains("id")) {
                return Component.literal("\u5237\u602A\u86CB\uFF08" + entityTag.getString("id") + "\uFF09");
            }
        }
        return super.getName(stack);
    }

    // ── tooltip ─────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level,
            List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("entity_tag")) {
            CompoundTag entityTag = tag.getCompound("entity_tag");
            if (entityTag.contains("id")) {
                tooltip.add(Component.literal("\u5B9E\u4F53: " + entityTag.getString("id")).withStyle(ChatFormatting.GRAY));
            }
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
