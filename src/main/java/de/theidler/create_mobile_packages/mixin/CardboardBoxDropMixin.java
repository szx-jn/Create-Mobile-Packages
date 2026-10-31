package de.theidler.create_mobile_packages.mixin;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppresses Mekanism's empty-box drop only for boxes packed by Mobile Packager. */
@Pseudo
@Mixin(targets = "mekanism.common.block.BlockCardboardBox", remap = false)
public abstract class CardboardBoxDropMixin {

    private static final ThreadLocal<Boolean> CMP$SUPPRESS_BOX_DROP = ThreadLocal.withInitial(() -> false);

    @Inject(method = "use", at = @At("HEAD"), remap = false)
    private void cmp$readPackagerMarker(BlockState state, Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        CMP$SUPPRESS_BOX_DROP.set(false);
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (blockEntity != null && blockEntity.getPersistentData()
                .getBoolean(MobilePackager.CARDBOARD_BOX_FROM_MOBILE_PACKAGER)) {
            CMP$SUPPRESS_BOX_DROP.set(true);
        }
    }

    @Redirect(method = "use", at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            remap = true), remap = false)
    private void cmp$suppressEmptyBoxDrop(Level world, BlockPos pos, ItemStack stack) {
        if (!CMP$SUPPRESS_BOX_DROP.get()) {
            Block.popResource(world, pos, stack);
        }
    }

    @Inject(method = "use", at = @At("RETURN"), remap = false)
    private void cmp$clearPackagerMarker(BlockState state, Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        CMP$SUPPRESS_BOX_DROP.remove();
    }
}
