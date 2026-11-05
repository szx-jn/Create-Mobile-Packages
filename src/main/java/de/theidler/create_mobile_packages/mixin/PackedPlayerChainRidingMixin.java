package de.theidler.create_mobile_packages.mixin;

import com.simibubi.create.content.kinetics.chainConveyor.ChainConveyorRidingHandler;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import java.util.function.Predicate;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Lets packed players stay on Create chains without requiring a held wrench. */
@Mixin(value = ChainConveyorRidingHandler.class, remap = false)
public abstract class PackedPlayerChainRidingMixin {
    @Redirect(
        method = "clientTick",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/player/LocalPlayer;isHolding(Ljava/util/function/Predicate;)Z"))
    private static boolean allowPackedPlayerRide(LocalPlayer player, Predicate<ItemStack> wrenchCheck) {
        return PackedPlayerState.isPacked(player) || player.isHolding(wrenchCheck);
    }
}
