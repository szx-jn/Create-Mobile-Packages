package de.theidler.create_mobile_packages.mixin;

import de.theidler.create_mobile_packages.items.mobile_packager.CardboardBoxPackagingPolicy;
import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackagerBlockContext;
import mekanism.common.item.block.ItemBlockCardboardBox;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import java.util.List;

/** Keeps native world protection and tile security; preserves the pack's block exclusion policy. */
@Mixin(value = ItemBlockCardboardBox.class, remap = false)
public abstract class CardboardBoxPackagingPolicyMixin {
    @Redirect(method = "onItemUseFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isAir()Z"), require = 1)
    private boolean cmp$exclude(BlockState state) { return state.isAir() || !allowed(state); }
    @Redirect(method = "onItemUseFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroySpeed(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"), require = 1)
    private float cmp$allowHardBlocks(BlockState state, BlockGetter level, BlockPos pos) {
        return MobilePackagerBlockContext.isExtendedCompatibility()
            ? (allowed(state) ? 0F : -1F) : state.getDestroySpeed(level, pos);
    }
    @Redirect(method = "onItemUseFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;is(Lnet/minecraft/tags/TagKey;)Z"), require = 1)
    private boolean cmp$ignoreBlockBlacklist(BlockState state, TagKey<Block> blacklist) {
        return !MobilePackagerBlockContext.isExtendedCompatibility() && state.is(blacklist);
    }
    @Redirect(method = "onItemUseFirst", at = @At(value = "INVOKE", target = "Ljava/util/List;contains(Ljava/lang/Object;)Z"), require = 1)
    private boolean cmp$ignoreNamespaceBlacklist(List<?> blacklist, Object namespace) {
        return !MobilePackagerBlockContext.isExtendedCompatibility() && blacklist.contains(namespace);
    }
    @Redirect(method = "onItemUseFirst", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"), require = 1)
    private boolean cmp$keepOtherPartsWhilePacking(Level level, BlockPos pos, BlockState boxed, int flags) {
        return level.setBlock(pos, boxed, flags | Block.UPDATE_KNOWN_SHAPE);
    }
    private static boolean allowed(BlockState state) { return CardboardBoxPackagingPolicy.canPack(BuiltInRegistries.BLOCK.getKey(state.getBlock())); }
}
