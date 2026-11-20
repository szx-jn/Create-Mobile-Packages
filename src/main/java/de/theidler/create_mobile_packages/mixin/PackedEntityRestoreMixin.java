package de.theidler.create_mobile_packages.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedMobSpawnEgg;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = PackageItem.class, remap = false)
public abstract class PackedEntityRestoreMixin {
    @WrapOperation(method = "open", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/EntityType;spawn(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/MobSpawnType;ZZ)Lnet/minecraft/world/entity/Entity;"), require = 1)
    private Entity cmp$restoreSnapshot(EntityType<?> type, ServerLevel level, ItemStack stack, Player player, BlockPos pos,
            MobSpawnType reason, boolean align, boolean invert, Operation<Entity> original) {
        if (stack.getItem() instanceof PackedMobSpawnEgg && stack.has(net.minecraft.core.component.DataComponents.ENTITY_DATA)) {
            return PackedMobSpawnEgg.restoreSnapshot(level, stack, pos);
        }
        return original.call(type, level, stack, player, pos, reason, align, invert);
    }
}
