package de.theidler.create_mobile_packages.mixin;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.items.mobile_packager.PackagedSableShip;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = PackageItem.class, remap = false)
public abstract class PackagedSableShipReleaseMixin {
    @Inject(method = "open", at = @At("HEAD"), remap = false, require = 1)
    private void cmp$releasePackagedShips(net.minecraft.world.level.Level level, Player player,
            InteractionHand hand, CallbackInfoReturnable<?> callback) {
        if (level.isClientSide || !(level instanceof ServerLevel)) return;

        ItemStack packageStack = player.getItemInHand(hand);
        if (!PackageItem.isPackage(packageStack)) return;

        HitResult hit = player.pick(6.0, 0.0F, false);
        BlockHitResult blockHit = hit instanceof BlockHitResult block
            ? block
            : new BlockHitResult(Vec3.atCenterOf(player.blockPosition().below()),
                Direction.UP, player.blockPosition().below(), false);
        PackagedSableShip.release((ServerLevel) level, player, hand, packageStack, blockHit);
    }
}
