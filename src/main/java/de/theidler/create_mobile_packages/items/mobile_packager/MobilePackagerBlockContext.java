package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Marks only BlockData created during the tool's delegated packing operation. */
public final class MobilePackagerBlockContext {
    public static final String MARKER = "create_mobile_packages:from_mobile_packager";
    private static final ThreadLocal<Boolean> PACKING = new ThreadLocal<>();

    private MobilePackagerBlockContext() {}

    public static boolean isPacking() {
        return Boolean.TRUE.equals(PACKING.get());
    }

    public static InteractionResult pack(Item cardboardBox, UseOnContext context) {
        Boolean previous = PACKING.get();
        PACKING.set(true);
        try {
            return cardboardBox.onItemUseFirst(new ItemStack(cardboardBox), context);
        } finally {
            if (previous == null) PACKING.remove();
            else PACKING.set(previous);
        }
    }
}
