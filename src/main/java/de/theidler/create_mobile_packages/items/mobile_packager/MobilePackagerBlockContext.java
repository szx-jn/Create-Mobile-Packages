package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** Scopes special block and mod compatibility to the delegated packing operation. */
public final class MobilePackagerBlockContext {
    public static final String MARKER = "create_mobile_packages:from_mobile_packager";
    public static final String SABLE_SHIP_MARKER = "create_mobile_packages:mobile_packager_ship";
    private static final ThreadLocal<State> CURRENT = new ThreadLocal<>();

    private MobilePackagerBlockContext() {}

    public static boolean isPacking() {
        return CURRENT.get() != null;
    }

    public static boolean canPackSpecialBlocks() {
        State state = CURRENT.get();
        return state != null && state.specialBlockPacking();
    }

    public static boolean hasModCompatibility() {
        State state = CURRENT.get();
        return state != null && state.modCompatibility();
    }

    public static Scope openScope(boolean specialBlockPacking, boolean modCompatibility) {
        State previous = CURRENT.get();
        State current = new State(specialBlockPacking, modCompatibility);
        CURRENT.set(current);
        return new Scope(Thread.currentThread(), previous, current);
    }

    public static InteractionResult pack(Item cardboardBox, UseOnContext context) {
        return pack(cardboardBox, context, false, false);
    }

    public static InteractionResult pack(Item cardboardBox, UseOnContext context,
                                         boolean specialBlockPacking, boolean modCompatibility) {
        try (Scope ignored = openScope(specialBlockPacking, modCompatibility)) {
            InteractionResult result = cardboardBox.onItemUseFirst(new ItemStack(cardboardBox), context);
            if (result.consumesAction() && !context.getLevel().isClientSide
                    && context.getLevel().getBlockEntity(context.getClickedPos()) instanceof MobilePackagerCardboardBox box) {
                box.createMobilePackages$setFromMobilePackager(true);
            }
            return result;
        }
    }

    private record State(boolean specialBlockPacking, boolean modCompatibility) {}

    public static final class Scope implements AutoCloseable {
        private final Thread owner;
        private final State previous;
        private final State current;
        private boolean closed;

        private Scope(Thread owner, State previous, State current) {
            this.owner = owner;
            this.previous = previous;
            this.current = current;
        }

        @Override
        public void close() {
            if (closed) return;
            if (Thread.currentThread() != owner) {
                throw new IllegalStateException("Packing scope must close on its owning thread");
            }
            if (CURRENT.get() != current) {
                throw new IllegalStateException("Packing scopes must close in reverse order");
            }
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
            closed = true;
        }
    }
}
