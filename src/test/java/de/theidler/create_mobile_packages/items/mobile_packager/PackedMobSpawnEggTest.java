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
    void migratesLegacyEntityTagToVanillaEntityTag() {
        CompoundTag stackTag = new CompoundTag();
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putString("id", "minecraft:cat");
        legacyTag.putString("variant", "minecraft:black");
        stackTag.put("entity_tag", legacyTag);

        CompoundTag migrated = PackedEntityNbt.getOrMigrateEntityTag(stackTag);

        assertNotNull(migrated);
        assertSame(migrated, stackTag.getCompound(PackedEntityNbt.ENTITY_TAG));
        assertFalse(stackTag.contains("entity_tag"));
        assertEquals("minecraft:cat", migrated.getString("id"));
        assertEquals("minecraft:black", migrated.getString("variant"));
    }

    @Test
    void restoresPersistentForgeDataFromBuggyWrapper() {
        CompoundTag stackTag = new CompoundTag();
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putString("id", "minecraft:tnt");
        legacyTag.putShort("Fuse", (short) 37);

        CompoundTag persistentData = new CompoundTag();
        persistentData.putInt("ExampleValue", 42);

        CompoundTag wrappedSnapshot = new CompoundTag();
        wrappedSnapshot.putString("id", "minecraft:tnt");
        wrappedSnapshot.put("Pos", new ListTag());
        wrappedSnapshot.put("Motion", new ListTag());
        wrappedSnapshot.put("ForgeData", persistentData.copy());
        legacyTag.put("ForgeData", wrappedSnapshot);
        stackTag.put("entity_tag", legacyTag);

        CompoundTag migrated = PackedEntityNbt.getOrMigrateEntityTag(stackTag);

        assertNotNull(migrated);
        assertEquals(37, migrated.getShort("Fuse"));
        assertTrue(migrated.contains("ForgeData"));
        assertEquals(42, migrated.getCompound("ForgeData").getInt("ExampleValue"));
        assertFalse(migrated.getCompound("ForgeData").contains("Pos"));
    }

    @Test
    void dropsRecursiveForgeDataWhenNoPersistentDataExists() {
        CompoundTag stackTag = new CompoundTag();
        CompoundTag legacyTag = new CompoundTag();
        legacyTag.putString("id", "minecraft:tnt");

        CompoundTag wrappedSnapshot = new CompoundTag();
        wrappedSnapshot.putString("id", "minecraft:tnt");
        wrappedSnapshot.put("Pos", new ListTag());
        legacyTag.put("ForgeData", wrappedSnapshot);
        stackTag.put("entity_tag", legacyTag);

        CompoundTag migrated = PackedEntityNbt.getOrMigrateEntityTag(stackTag);

        assertNotNull(migrated);
        assertFalse(migrated.contains("ForgeData"));
    }

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
