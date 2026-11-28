package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CardboardBoxPackagingPolicyTest {

    @Test
    void excludesOnlyBarrierAndTransferDispatchTerminal() {
        assertFalse(CardboardBoxPackagingPolicy.canPack(CardboardBoxPackagingPolicy.BARRIER));
        assertFalse(CardboardBoxPackagingPolicy.canPack(CardboardBoxPackagingPolicy.TRANSFER_DISPATCH_TERMINAL));
    }

    @Test
    void allowsVanillaTechnicalAndModBlocks() {
        assertTrue(CardboardBoxPackagingPolicy.canPack(ResourceLocation.fromNamespaceAndPath("minecraft", "bedrock")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(ResourceLocation.fromNamespaceAndPath("minecraft", "command_block")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(ResourceLocation.fromNamespaceAndPath("minecraft", "oak_door")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(ResourceLocation.fromNamespaceAndPath("ae2", "controller")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(ResourceLocation.fromNamespaceAndPath("mekanism", "ultimate_bin")));
    }
}
