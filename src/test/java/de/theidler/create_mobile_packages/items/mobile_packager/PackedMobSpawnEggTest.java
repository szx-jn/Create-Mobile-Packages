package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PackedMobSpawnEggTest {

    @Test
    void removesOldLocationAndAngerStateBeforePackaging() {
        CompoundTag entityTag = new CompoundTag();
        entityTag.put("Pos", new ListTag());
        entityTag.put("Motion", new ListTag());
        entityTag.put("Rotation", new ListTag());
        entityTag.putInt("SleepingX", 12);
        entityTag.putInt("SleepingY", 64);
        entityTag.putInt("SleepingZ", -9);
        entityTag.putUUID("AngryAt", java.util.UUID.randomUUID());
        entityTag.putInt("AngerTime", 400);
        entityTag.putInt("Anger", 1);
        entityTag.putShort("Fuse", (short) 37);

        CompoundTag brain = new CompoundTag();
        CompoundTag memories = new CompoundTag();
        memories.putUUID("minecraft:angry_at", java.util.UUID.randomUUID());
        memories.put("minecraft:home", new CompoundTag());
        memories.put("minecraft:job_site", new CompoundTag());
        brain.put("memories", memories);
        entityTag.put("Brain", brain);

        PackedEntityNbt.sanitizeForPackaging(entityTag);

        assertFalse(entityTag.contains("Pos"));
        assertFalse(entityTag.contains("Motion"));
        assertFalse(entityTag.contains("Rotation"));
        assertFalse(entityTag.contains("SleepingX"));
        assertFalse(entityTag.contains("SleepingY"));
        assertFalse(entityTag.contains("SleepingZ"));
        assertFalse(entityTag.contains("AngryAt"));
        assertFalse(entityTag.contains("AngerTime"));
        assertFalse(entityTag.contains("Anger"));
        assertFalse(memories.contains("minecraft:angry_at"));
        assertFalse(memories.contains("minecraft:home"));
        assertFalse(memories.contains("minecraft:job_site"));
        assertEquals(37, entityTag.getShort("Fuse"));
    }
}
