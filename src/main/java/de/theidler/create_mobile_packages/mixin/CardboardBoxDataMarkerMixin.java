package de.theidler.create_mobile_packages.mixin;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerBlockContext;
import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerCardboardBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Carries origin through Mekanism's BlockData item, loot and placement serialization. */
@Pseudo
@Mixin(targets = "mekanism.common.block.BlockCardboardBox$BlockData", remap = false)
abstract class CardboardBoxDataMarkerMixin implements MobilePackagerCardboardBox {
    @Unique
    private boolean cmp$packedByTool;

    @Inject(method = "<init>", at = @At("RETURN"), remap = false, require = 1)
    private void cmp$markNewData(BlockState state, CallbackInfo ci) {
        cmp$packedByTool = MobilePackagerBlockContext.isPacking();
    }

    @Override
    public boolean createMobilePackages$isFromMobilePackager() {
        return cmp$packedByTool;
    }

    @Override
    public void createMobilePackages$setFromMobilePackager(boolean value) {
        cmp$packedByTool = value;
    }

    @Inject(method = "write", at = @At("RETURN"), remap = false, require = 1)
    private void cmp$writeOrigin(CompoundTag tag, CallbackInfoReturnable<CompoundTag> cir) {
        if (cmp$packedByTool) {
            cir.getReturnValue().putBoolean(MobilePackagerBlockContext.MARKER, true);
        }
    }

    @Inject(method = "read", at = @At("RETURN"), remap = false, require = 1)
    private static void cmp$readOrigin(Level level, CompoundTag tag, CallbackInfoReturnable<Object> cir) {
        ((MobilePackagerCardboardBox) cir.getReturnValue()).createMobilePackages$setFromMobilePackager(
                tag.getBoolean(MobilePackagerBlockContext.MARKER));
    }
}
