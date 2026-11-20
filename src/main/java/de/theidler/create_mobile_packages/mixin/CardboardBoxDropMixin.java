package de.theidler.create_mobile_packages.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalBooleanRef;
import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerCardboardBox;
import mekanism.common.block.BlockCardboardBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BlockCardboardBox.class, remap = false)
public abstract class CardboardBoxDropMixin {
    @Inject(method = "useWithoutItem", at = @At("HEAD"), require = 1)
    private void cmp$readOrigin(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> callback, @Share("cmpSuppressDrop") LocalBooleanRef suppress) {
        suppress.set(level.getBlockEntity(pos) instanceof MobilePackagerCardboardBox box && box.createMobilePackages$isFromMobilePackager());
    }
    @WrapOperation(method = "useWithoutItem", at = @At(value = "INVOKE", target = "Lmekanism/common/block/BlockCardboardBox;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"), require = 1)
    private void cmp$dropOrdinaryBox(Level level, BlockPos pos, ItemStack item, Operation<Void> original,
            @Share("cmpSuppressDrop") LocalBooleanRef suppress) {
        if (!suppress.get()) original.call(level, pos, item);
    }
}
