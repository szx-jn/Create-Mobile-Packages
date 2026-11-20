package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class PackedEntityNbt {

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

        for (var passenger : entityTag.getList("Passengers", Tag.TAG_COMPOUND)) {
            sanitizeForPackaging((CompoundTag) passenger);
        }

        if (!entityTag.contains("Brain", Tag.TAG_COMPOUND)) return;

        CompoundTag brain = entityTag.getCompound("Brain");
        if (!brain.contains("memories", Tag.TAG_COMPOUND)) return;

        CompoundTag memories = brain.getCompound("memories");
        memories.remove("minecraft:angry_at");
        memories.remove("minecraft:attack_target");
        memories.remove("minecraft:hurt_by_entity");
        POSITIONAL_BRAIN_MEMORIES.forEach(memories::remove);
    }

    static @Nullable CompoundTag getEntityTag(net.minecraft.world.item.ItemStack stack) {
        var data = stack.get(net.minecraft.core.component.DataComponents.ENTITY_DATA);
        return data == null ? null : data.copyTag();
    }
}
