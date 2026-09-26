package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

    public static final String ENTITY_TAG = "EntityTag";
    private static final String LEGACY_ENTITY_TAG = "entity_tag";

    public PackedMobSpawnEgg(Properties properties) {
        super(EntityType.ENDER_DRAGON, 0x4B7A2B, 0x000000, properties);
    }

    // -- type resolution and legacy migration ---------------------------

    @Override
    public EntityType<?> getType(@Nullable CompoundTag tag) {
        CompoundTag entityTag = getOrMigrateEntityTag(tag);
        if (entityTag != null && entityTag.contains("id", Tag.TAG_STRING)) {
            Optional<EntityType<?>> type = EntityType.byString(entityTag.getString("id"));
            if (type.isPresent()) return type.get();
        }
        return super.getType(tag);
    }

    /**
     * Reads the current vanilla key and migrates packages produced by old
     * versions of this fork which used the non-standard "entity_tag" key.
     */
    static @Nullable CompoundTag getOrMigrateEntityTag(@Nullable CompoundTag stackTag) {
        if (stackTag == null) return null;

        if (stackTag.contains(ENTITY_TAG, Tag.TAG_COMPOUND)) {
            CompoundTag entityTag = stackTag.getCompound(ENTITY_TAG);
            repairLegacyForgeData(entityTag);
            return entityTag;
        }

        if (!stackTag.contains(LEGACY_ENTITY_TAG, Tag.TAG_COMPOUND)) return null;

        CompoundTag entityTag = stackTag.getCompound(LEGACY_ENTITY_TAG).copy();
        repairLegacyForgeData(entityTag);
        stackTag.put(ENTITY_TAG, entityTag);
        stackTag.remove(LEGACY_ENTITY_TAG);
        return entityTag;
    }

    /**
     * Older builds wrote IForgeEntity.serializeNBT() into "ForgeData". That
     * serialized the whole entity again and obscured the real persistent data.
     * Recover the nested original ForgeData when present, otherwise drop the
     * recursive snapshot.
     */
    private static void repairLegacyForgeData(CompoundTag entityTag) {
        if (!entityTag.contains("ForgeData", Tag.TAG_COMPOUND)) return;

        CompoundTag wrappedData = entityTag.getCompound("ForgeData");
        if (!looksLikeEntitySnapshot(wrappedData)) return;

        if (wrappedData.contains("ForgeData", Tag.TAG_COMPOUND)) {
            entityTag.put("ForgeData", wrappedData.getCompound("ForgeData").copy());
        } else {
            entityTag.remove("ForgeData");
        }
    }

    private static boolean looksLikeEntitySnapshot(CompoundTag tag) {
        return tag.contains("id", Tag.TAG_STRING)
                && (tag.contains("Pos", Tag.TAG_LIST)
                || tag.contains("Motion", Tag.TAG_LIST)
                || tag.contains("Rotation", Tag.TAG_LIST)
                || tag.contains("ForgeCaps", Tag.TAG_COMPOUND));
    }

    // -- display name ---------------------------------------------------

    @Override
    public Component getName(ItemStack stack) {
        CompoundTag entityTag = getOrMigrateEntityTag(stack.getTag());
        if (entityTag != null && entityTag.contains("id", Tag.TAG_STRING)) {
            return Component.literal("\u5237\u602A\u86CB\uFF08" + entityTag.getString("id") + "\uFF09");
        }
        return super.getName(stack);
    }

    // -- tooltip --------------------------------------------------------

    @Override
    public void appendHoverText(ItemStack stack, @Nullable net.minecraft.world.level.Level level,
            List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        CompoundTag entityTag = getOrMigrateEntityTag(stack.getTag());
        if (entityTag != null && entityTag.contains("id", Tag.TAG_STRING)) {
            tooltip.add(Component.literal("\u5B9E\u4F53: " + entityTag.getString("id")).withStyle(ChatFormatting.GRAY));
        }
        super.appendHoverText(stack, level, tooltip, flag);
    }
}
