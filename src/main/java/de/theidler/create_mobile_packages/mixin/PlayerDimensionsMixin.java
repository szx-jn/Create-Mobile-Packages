package de.theidler.create_mobile_packages.mixin;

import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerDimensionsMixin {
    @Inject(method = "getDimensions", at = @At("RETURN"), cancellable = true)
    private void cmp$packedDimensions(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
        Player player = (Player) (Object) this;
        if (PackedPlayerState.isPacked(player)) {
            EntityDimensions current = cir.getReturnValue();
            cir.setReturnValue(EntityDimensions.scalable(current.width, 0.75F));
        }
    }
}
