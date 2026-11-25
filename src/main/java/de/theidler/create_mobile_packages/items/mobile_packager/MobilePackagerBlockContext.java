package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Scopes special block and mod compatibility to the delegated packing operation. */
public final class MobilePackagerBlockContext {
    public static final String MARKER = "create_mobile_packages:from_mobile_packager";
    public static final String SABLE_SHIP_MARKER = "create_mobile_packages:mobile_packager_ship";
    private static final ThreadLocal<Boolean> PACKING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> SPECIAL_BLOCK_PACKING = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> MOD_COMPATIBILITY = new ThreadLocal<>();

    private MobilePackagerBlockContext() {}

    public static boolean isPacking() {
        return Boolean.TRUE.equals(PACKING.get());
    }

    public static boolean canPackSpecialBlocks() {
        return Boolean.TRUE.equals(SPECIAL_BLOCK_PACKING.get());
    }

    public static boolean hasModCompatibility() {
        return Boolean.TRUE.equals(MOD_COMPATIBILITY.get());
    }

    public static InteractionResult pack(Item cardboardBox, UseOnContext context) {
        return pack(cardboardBox, context, false, false);
    }

    public static InteractionResult pack(Item cardboardBox, UseOnContext context,
                                         boolean specialBlockPacking, boolean modCompatibility) {
        Boolean previousPacking = PACKING.get();
        Boolean previousSpecialBlocks = SPECIAL_BLOCK_PACKING.get();
        Boolean previousModCompatibility = MOD_COMPATIBILITY.get();
        PACKING.set(true);
        SPECIAL_BLOCK_PACKING.set(specialBlockPacking);
        MOD_COMPATIBILITY.set(modCompatibility);
        try {
            return cardboardBox.onItemUseFirst(new ItemStack(cardboardBox), context);
        } finally {
            restore(PACKING, previousPacking);
            restore(SPECIAL_BLOCK_PACKING, previousSpecialBlocks);
            restore(MOD_COMPATIBILITY, previousModCompatibility);
        }
    }

    private static void restore(ThreadLocal<Boolean> value, Boolean previous) {
        if (previous == null) value.remove();
        else value.set(previous);
    }
}
