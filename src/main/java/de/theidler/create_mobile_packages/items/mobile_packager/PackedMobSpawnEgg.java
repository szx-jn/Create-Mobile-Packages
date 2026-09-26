package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SpawnEggItem;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Spawn egg for a packed entity. The entity snapshot is stored under the
 * vanilla "EntityTag" key so Create's PackageItem.open() restores it through
 * the normal EntityType.spawn() path.
 */
public class PackedMobSpawnEgg extends SpawnEggItem {

    public PackedMobSpawnEgg(Properties properties) {
        super(EntityType.ENDER_DRAGON, 0x4B7A2B, 0x000000, properties);
    }

    // -- type resolution and legacy migration ---------------------------

    @Override
    public EntityType<?> getType(@Nullable CompoundTag tag) {
        CompoundTag entityTag = PackedEntityNbt.getOrMigrateEntityTag(tag);
        if (entityTag != null && entityTag.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            Optional<EntityType<?>> type = EntityType.byString(entityTag.getString("id"));
            if (type.isPresent()) return type.get();
        }
        return super.getType(tag);
    }

    // -- display name ---------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag entityTag = PackedEntityNbt.getOrMigrateEntityTag(stack.getTag());
        if (entityTag != null && entityTag.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            return Component.literal("\u5237\u602A\u86CB\uFF08" + entityTag.getString("id") + "\uFF09");
        }
        return super.getName(stack);
    }

    // -- tooltip --------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level,
            List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        CompoundTag entityTag = PackedEntityNbt.getOrMigrateEntityTag(stack.getTag());
        if (entityTag != null && entityTag.contains("id", net.minecraft.nbt.Tag.TAG_STRING)) {
            tooltip.add(Component.literal("\u5B9E\u4F53: " + entityTag.getString("id")).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
