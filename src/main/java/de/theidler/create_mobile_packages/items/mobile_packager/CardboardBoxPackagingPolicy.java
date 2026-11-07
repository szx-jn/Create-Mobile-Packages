package de.theidler.create_mobile_packages.items.mobile_packager;

import java.util.Set;
import net.minecraft.resources.ResourceLocation;

/** Block eligibility for Mekanism cardboard-box packaging. */
public final class CardboardBoxPackagingPolicy {
    public static final ResourceLocation BARRIER = new ResourceLocation("minecraft", "barrier");
    public static final ResourceLocation TRANSFER_DISPATCH_TERMINAL =
        new ResourceLocation("dimensionworks_transfer", "dispatch_terminal");

    private static final Set<ResourceLocation> EXCLUDED = Set.of(BARRIER, TRANSFER_DISPATCH_TERMINAL);

    private CardboardBoxPackagingPolicy() {}

    public static boolean canPack(ResourceLocation blockId) {
        return blockId != null && !EXCLUDED.contains(blockId);
    }
}
