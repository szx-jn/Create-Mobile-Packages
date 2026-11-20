package de.theidler.create_mobile_packages.mixin;

import mekanism.common.attachments.BlockData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = BlockData.class, remap = false)
public abstract class CardboardBoxStateMixin {
    @Redirect(method = "tryPlaceIntoWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;updateFromNeighbourShapes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"), require = 1)
    private BlockState cmp$keepSeparatedParts(BlockState saved, LevelAccessor level, BlockPos pos) {
        var adjusted = Block.updateFromNeighbourShapes(saved, level, pos);
        return adjusted.isAir() ? saved : adjusted;
    }
    @Redirect(method = "tryPlaceIntoWorld", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z"), require = 1)
    private boolean cmp$restoreSeparatedParts(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
    }
}
