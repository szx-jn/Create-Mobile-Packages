package de.theidler.create_mobile_packages.mixin;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerCardboardBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Stores the Mobile Packager origin marker in the cardboard box's own saved NBT. */
@Pseudo
@Mixin(targets = "mekanism.common.tile.TileEntityCardboardBox", remap = false)
abstract class CardboardBoxMarkerMixin implements MobilePackagerCardboardBox {

    @Unique
    private boolean cmp$fromMobilePackager;

    @Override
    public boolean createMobilePackages$isFromMobilePackager() {
        return cmp$fromMobilePackager || ((BlockEntity) (Object) this).saveWithoutMetadata()
                .getCompound("data").getBoolean("create_mobile_packages:from_mobile_packager");
    }

    @Override
    public void createMobilePackages$setFromMobilePackager(boolean value) {
        cmp$fromMobilePackager = value;
        ((BlockEntity) (Object) this).setChanged();
    }

    @Inject(method = {"saveAdditional", "m_183515_"}, at = @At("TAIL"), remap = false, require = 1)
    private void cmp$saveMarker(CompoundTag tag, CallbackInfo ci) {
        if (cmp$fromMobilePackager) {
            tag.putBoolean("create_mobile_packages:from_mobile_packager", true);
        }
    }

    @Inject(method = {"load", "m_142466_"}, at = @At("TAIL"), remap = false, require = 1)
    private void cmp$loadMarker(CompoundTag tag, CallbackInfo ci) {
        cmp$fromMobilePackager = tag.getBoolean("create_mobile_packages:from_mobile_packager")
                || tag.getCompound("ForgeData").getBoolean("create_mobile_packages:cardboard_box_from_mobile_packager");
    }
}
