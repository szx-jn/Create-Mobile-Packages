package de.theidler.create_mobile_packages.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import de.theidler.create_mobile_packages.items.mobile_packager.CardboardBoxPackagingPolicy;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Replaces Mekanism's block-type restrictions with the pack's two-block exclusion list. */
@Pseudo
@Mixin(targets = "mekanism.common.item.block.ItemBlockCardboardBox", remap = false)
public abstract class CardboardBoxPackagingPolicyMixin {
    @Group(name = "cmp_excluded_block_check", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z", remap = false),
        remap = false, require = 0)
    private boolean cmp$rejectExcludedBlocksMapped(BlockState state) {
        return cmp$isExcludedBlock(state);
    }

    @Group(name = "cmp_excluded_block_check", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;m_60795_()Z", remap = false),
        remap = false, require = 0)
    private boolean cmp$rejectExcludedBlocksRuntime(BlockState state) {
        return cmp$isExcludedBlock(state);
    }

    @Group(name = "cmp_destroy_speed_check", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
            remap = false),
        remap = false, require = 0)
    private float cmp$allowUnbreakableBlocksMapped(BlockState state, BlockGetter level, BlockPos pos) {
        return cmp$destroySpeedForPackaging(state);
    }

    @Group(name = "cmp_destroy_speed_check", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;m_60800_(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F",
            remap = false),
        remap = false, require = 0)
    private float cmp$allowUnbreakableBlocksRuntime(BlockState state, BlockGetter level, BlockPos pos) {
        return cmp$destroySpeedForPackaging(state);
    }

    @Group(name = "cmp_block_blacklist_check", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z",
            remap = false),
        remap = false, require = 0)
    private boolean cmp$ignoreMekanismBlockBlacklistMapped(BlockState state, TagKey<Block> blacklist) {
        return false;
    }

    @Group(name = "cmp_block_blacklist_check", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/state/BlockState;m_204336_(Lnet/minecraft/tags/TagKey;)Z",
            remap = false),
        remap = false, require = 0)
    private boolean cmp$ignoreMekanismBlockBlacklistRuntime(BlockState state, TagKey<Block> blacklist) {
        return false;
    }

    @Redirect(
        method = "onItemUseFirst",
        at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z", remap = false),
        remap = false)
    private boolean cmp$ignoreMekanismNamespaceBlacklist(List<?> blacklist, Object namespace) {
        return false;
    }

    @ModifyExpressionValue(
        method = "onItemUseFirst",
        at = @At(
            value = "INVOKE",
            target = "Lmekanism/common/tags/LazyTagLookup;contains(Ljava/lang/Object;)Z",
            remap = false))
    private boolean cmp$ignoreMekanismBlockEntityBlacklist(boolean blacklisted) {
        return false;
    }

    @Group(name = "cmp_pack_block_state", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;setBlockAndUpdate(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = false), remap = false, require = 0)
    private boolean cmp$packWithoutBreakingOtherPartsMapped(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
    }

    @Group(name = "cmp_pack_block_state", min = 1, max = 1)
    @Redirect(
        method = "onItemUseFirst",
        at = @At(value = "INVOKE", target =
            "Lnet/minecraft/world/level/Level;m_46597_(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Z",
            remap = false), remap = false, require = 0)
    private boolean cmp$packWithoutBreakingOtherPartsRuntime(Level level, BlockPos pos, BlockState state) {
        return level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE);
    }

    private static boolean cmp$isExcludedBlock(BlockState state) {
        return state.isAir() || !CardboardBoxPackagingPolicy.canPack(ForgeRegistries.BLOCKS.getKey(state.getBlock()));
    }

    private static float cmp$destroySpeedForPackaging(BlockState state) {
        return CardboardBoxPackagingPolicy.canPack(ForgeRegistries.BLOCKS.getKey(state.getBlock())) ? 0F : -1F;
    }
}
