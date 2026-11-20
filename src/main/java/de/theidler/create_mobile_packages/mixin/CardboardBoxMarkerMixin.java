package de.theidler.create_mobile_packages.mixin;

import de.theidler.create_mobile_packages.index.CMPDataComponents;
import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerCardboardBox;
import mekanism.common.tile.TileEntityCardboardBox;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

/** Native components carry origin across save, loot and placement. */
@Mixin(value = TileEntityCardboardBox.class, remap = false)
public abstract class CardboardBoxMarkerMixin implements MobilePackagerCardboardBox {
    @Override public boolean createMobilePackages$isFromMobilePackager() {
        return Boolean.TRUE.equals(((BlockEntity) (Object) this).components().get(CMPDataComponents.PACKAGER_ORIGIN));
    }
    @Override public void createMobilePackages$setFromMobilePackager(boolean value) {
        var tile = (BlockEntity) (Object) this;
        tile.setComponents(DataComponentMap.builder().addAll(tile.components()).set(CMPDataComponents.PACKAGER_ORIGIN, value).build());
        tile.setChanged();
    }
    @Inject(method = "getRemapEntries", at = @At("RETURN"), cancellable = true, require = 1)
    private void cmp$copyOrigin(CallbackInfoReturnable<List<DataComponentType<?>>> result) {
        var entries = new ArrayList<>(result.getReturnValue());
        entries.add(CMPDataComponents.PACKAGER_ORIGIN);
        result.setReturnValue(List.copyOf(entries));
    }
}
