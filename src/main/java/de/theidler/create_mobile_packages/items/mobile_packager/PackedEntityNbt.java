package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class PackedEntityNbt {

    static final String ENTITY_TAG = "EntityTag";
    private static final String LEGACY_ENTITY_TAG = "entity_tag";
    private static final List<String> POSITIONAL_BRAIN_MEMORIES = List.of(
            "minecraft:home",
            "minecraft:job_site",
            "minecraft:potential_job_site",
            "minecraft:meeting_point",
            "minecraft:secondary_job_site",
            "minecraft:nearest_bed",
            "minecraft:hiding_place",
            "minecraft:celebrate_location",
            "minecraft:disturbance_location",
            "minecraft:liked_noteblock",
            "minecraft:sniffer_explored_positions",
            "minecraft:sniffer_sniffing_target"
    );

    private PackedEntityNbt() {
    }

    /**
     * Drops world-bound and temporary combat state so an unpacked entity does
     * not teleport back to, target, or resume behaviour from its old location.
     */
    static void sanitizeForPackaging(CompoundTag entityTag) {
        entityTag.remove("Pos");
        entityTag.remove("Motion");
        entityTag.remove("Rotation");
        entityTag.remove("Dimension");
        entityTag.remove("PortalCooldown");
        entityTag.remove("FallDistance");
        entityTag.remove("HurtByTimestamp");

        entityTag.remove("SleepingX");
        entityTag.remove("SleepingY");
        entityTag.remove("SleepingZ");
        entityTag.remove("Leash");

        entityTag.remove("Anger");
        entityTag.remove("AngryAt");
        entityTag.remove("AngerTime");

        if (!entityTag.contains("Brain", Tag.TAG_COMPOUND)) return;

        CompoundTag brain = entityTag.getCompound("Brain");
        if (!brain.contains("memories", Tag.TAG_COMPOUND)) return;

        CompoundTag memories = brain.getCompound("memories");
        memories.remove("minecraft:angry_at");
        memories.remove("minecraft:attack_target");
        memories.remove("minecraft:hurt_by_entity");
        POSITIONAL_BRAIN_MEMORIES.forEach(memories::remove);
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
