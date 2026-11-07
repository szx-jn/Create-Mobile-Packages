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
        assertTrue(CardboardBoxPackagingPolicy.canPack(new ResourceLocation("minecraft", "bedrock")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(new ResourceLocation("minecraft", "command_block")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(new ResourceLocation("minecraft", "oak_door")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(new ResourceLocation("ae2", "controller")));
        assertTrue(CardboardBoxPackagingPolicy.canPack(new ResourceLocation("mekanism", "ultimate_bin")));
    }
}
