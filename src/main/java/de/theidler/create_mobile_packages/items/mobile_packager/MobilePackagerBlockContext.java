package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Scopes the tool's delegated cardboard-box operation and its optional extended compatibility. */
public final class MobilePackagerBlockContext {
    public static final String MARKER = "create_mobile_packages:from_mobile_packager";
    public static final String SABLE_SHIP_MARKER = "create_mobile_packages:mobile_packager_ship";
    private static final ThreadLocal<Boolean> PACKING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> EXTENDED_COMPATIBILITY = new ThreadLocal<>();

    private MobilePackagerBlockContext() {}

    public static boolean isPacking() {
        return Boolean.TRUE.equals(PACKING.get());
    }

    public static boolean isExtendedCompatibility() {
        return Boolean.TRUE.equals(EXTENDED_COMPATIBILITY.get());
    }

    public static InteractionResult pack(Item cardboardBox, UseOnContext context) {
        return pack(cardboardBox, context, false);
    }

    public static InteractionResult pack(Item cardboardBox, UseOnContext context, boolean extendedCompatibility) {
        Boolean previousPacking = PACKING.get();
        Boolean previousCompatibility = EXTENDED_COMPATIBILITY.get();
        PACKING.set(true);
        EXTENDED_COMPATIBILITY.set(extendedCompatibility);
        try {
            return cardboardBox.onItemUseFirst(new ItemStack(cardboardBox), context);
        } finally {
            if (previousPacking == null) PACKING.remove();
            else PACKING.set(previousPacking);
            if (previousCompatibility == null) EXTENDED_COMPATIBILITY.remove();
            else EXTENDED_COMPATIBILITY.set(previousCompatibility);
        }
    }
}
