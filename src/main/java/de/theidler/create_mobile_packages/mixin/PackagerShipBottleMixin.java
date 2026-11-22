package de.theidler.create_mobile_packages.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import de.theidler.create_mobile_packages.items.mobile_packager.PackagedSableShip;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

@Pseudo
@Mixin(targets = "ace.actually.sableinbottle.item.ShipInABottleItem", remap = false)
public abstract class PackagerShipBottleMixin {
    @WrapMethod(method = "releaseShip")
    private InteractionResult cmp$consumeReleasedToolBottle(ItemStack stack, Level level, Player player,
            BlockPos clickedPos, Operation<InteractionResult> original) {
        // The native release removes CUSTOM_DATA, including our origin marker.
        boolean fromPackager = PackagedSableShip.isPackagerBottle(stack);
        InteractionResult result = original.call(stack, level, player, clickedPos);
        if (fromPackager && result.consumesAction()) stack.shrink(1);
        return result;
    }
}
