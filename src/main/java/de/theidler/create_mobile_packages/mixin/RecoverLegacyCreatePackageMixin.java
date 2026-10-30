package de.theidler.create_mobile_packages.mixin;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedMobSpawnEgg;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.ItemStackHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Replaces malformed legacy package spawn eggs with their saved Create package item. */
@Mixin(PackageItem.class)
public abstract class RecoverLegacyCreatePackageMixin {

    @Inject(method = "open", at = @At("HEAD"))
    private void cmp$recoverLegacyPackage(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (level.isClientSide || !(player instanceof ServerPlayer)) return;

        ItemStack packageStack = player.getItemInHand(hand);
        if (!PackageItem.isPackage(packageStack)) return;

        ItemStackHandler contents = PackageItem.getContents(packageStack);
        boolean changed = false;
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack recovered = PackedMobSpawnEgg.recoverLegacyCreatePackage(contents.getStackInSlot(slot));
            if (recovered.isEmpty()) continue;
            contents.setStackInSlot(slot, recovered);
            changed = true;
        }

        if (changed) {
            CompoundTag packageTag = packageStack.getOrCreateTag();
            packageTag.put("Items", contents.serializeNBT());
        }
    }
}
