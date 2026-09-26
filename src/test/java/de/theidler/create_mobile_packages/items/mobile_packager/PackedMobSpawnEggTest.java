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
}
