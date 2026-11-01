package de.theidler.create_mobile_packages.mixin;

import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerCardboardBox;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Suppresses Mekanism's empty-box drop only for boxes packed by Mobile Packager. */
@Pseudo
@Mixin(targets = "mekanism.common.block.BlockCardboardBox", remap = false)
public abstract class CardboardBoxDropMixin {

    @Unique
    private static final ThreadLocal<Boolean> CMP$SUPPRESS_BOX_DROP = ThreadLocal.withInitial(() -> false);

    @Inject(method = {"use", "m_6227_"}, at = @At("HEAD"), remap = false, require = 1)
    private void cmp$readPackagerMarker(BlockState state, Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        CMP$SUPPRESS_BOX_DROP.set(blockEntity instanceof MobilePackagerCardboardBox cardboardBox
                && cardboardBox.createMobilePackages$isFromMobilePackager());
    }

    @Group(name = "cmp_box_drop", min = 1, max = 1)
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target =
            "Lmekanism/common/block/BlockCardboardBox;m_49840_(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            remap = false), remap = false, require = 0)
    private void cmp$suppressRuntimeDrop(Level world, BlockPos pos, ItemStack stack) {
        cmp$dropIfOrdinaryBox(world, pos, stack);
    }

    @Group(name = "cmp_box_drop", min = 1, max = 1)
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target =
            "Lmekanism/common/block/BlockCardboardBox;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V",
            remap = false), remap = false, require = 0)
    private void cmp$suppressDevelopmentDrop(Level world, BlockPos pos, ItemStack stack) {
        cmp$dropIfOrdinaryBox(world, pos, stack);
    }

    @Unique
    private static void cmp$dropIfOrdinaryBox(Level world, BlockPos pos, ItemStack stack) {
        if (!CMP$SUPPRESS_BOX_DROP.get()) Block.popResource(world, pos, stack);
        CMP$SUPPRESS_BOX_DROP.remove();
    }

    @Inject(method = {"use", "m_6227_"}, at = @At("RETURN"), remap = false, require = 1)
    private void cmp$clearMarker(BlockState state, Level world, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        CMP$SUPPRESS_BOX_DROP.remove();
    }
}
