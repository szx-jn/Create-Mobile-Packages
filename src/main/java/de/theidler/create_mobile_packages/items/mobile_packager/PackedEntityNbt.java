package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

final class PackedEntityNbt {

    static final String ENTITY_TAG = "EntityTag";
    private static final String LEGACY_ENTITY_TAG = "entity_tag";

    private PackedEntityNbt() {
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
}
