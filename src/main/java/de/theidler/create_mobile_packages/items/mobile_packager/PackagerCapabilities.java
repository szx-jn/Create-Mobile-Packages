package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.items.mobile_packager.ability.PackagerCapability;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shared capability lookup used by tools, event hooks and external automation. */
public final class PackagerCapabilities {
    private PackagerCapabilities() {}

    public static boolean has(ItemStack stack, PackagerCapability capability) {
        return has(stack.getItem(), capability);
    }

    public static boolean has(Item item, PackagerCapability capability) {
        if (item instanceof PackagerCapabilityProvider provider) {
            return provider.getPackagerCapabilities().contains(capability);
        }
        return item instanceof MobilePackager && capability == PackagerCapability.PLAYER_PACKING;
    }
}
