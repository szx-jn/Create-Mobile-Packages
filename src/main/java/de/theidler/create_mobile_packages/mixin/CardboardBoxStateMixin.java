package de.theidler.create_mobile_packages.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Restores individual blocks without deleting temporarily separated doors, beds or plants. */
@Pseudo
@Mixin(targets = "mekanism.common.block.BlockCardboardBox", remap = false)
public abstract class CardboardBoxStateMixin {
    @Group(name = "cmp_unbox_neighbor_state", min = 1, max = 1)
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/block/Block;updateFromNeighbourShapes(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
        remap = false), remap = false, require = 0)
    private BlockState cmp$preserveSeparatedBlockMapped(BlockState state, LevelAccessor level, BlockPos pos) {
        return cmp$adjustWithoutDeleting(state, level, pos);
    }

    @Group(name = "cmp_unbox_neighbor_state", min = 1, max = 1)
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/block/Block;m_49931_(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;",
        remap = false), remap = false, require = 0)
    private BlockState cmp$preserveSeparatedBlockRuntime(BlockState state, LevelAccessor level, BlockPos pos) {
        return cmp$adjustWithoutDeleting(state, level, pos);
    }

    @Group(name = "cmp_unbox_block_state", min = 1, max = 1)
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        remap = false), remap = false, require = 0)
    private boolean cmp$restoreWithoutBreakingOtherPartsMapped(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
    }

    @Group(name = "cmp_unbox_block_state", min = 1, max = 1)
    @Redirect(method = {"use", "m_6227_"}, at = @At(value = "INVOKE", target =
        "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
        remap = false), remap = false, require = 0)
    private boolean cmp$restoreWithoutBreakingOtherPartsRuntime(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
    }

    @Unique
    private static BlockState cmp$adjustWithoutDeleting(BlockState state, LevelAccessor level, BlockPos pos) {
        // Keep normal chest/connection adjustment, but never turn a saved block into air.
        BlockState adjusted = Block.updateFromNeighbourShapes(state, level, pos);
        return adjusted.isAir() ? state : adjusted;
    }
}
