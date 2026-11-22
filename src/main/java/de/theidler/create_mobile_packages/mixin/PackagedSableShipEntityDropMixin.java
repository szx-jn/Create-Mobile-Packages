package de.theidler.create_mobile_packages.mixin;

import com.simibubi.create.content.logistics.box.PackageEntity;
import de.theidler.create_mobile_packages.items.mobile_packager.PackagedSableShip;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.ref.WeakReference;

@Mixin(value = PackageEntity.class, remap = false)
public abstract class PackagedSableShipEntityDropMixin {
    @Inject(method = "dropAllDeathLoot", at = @At("HEAD"), remap = false, require = 1)
    private void cmp$releasePackagedShips(ServerLevel level, DamageSource source, CallbackInfo callback) {
        PackageEntity entity = (PackageEntity) (Object) this;
        Player player = source.getEntity() instanceof Player attacker ? attacker : null;
        if (player == null && entity.tossedBy != null) {
            WeakReference<Player> owner = entity.tossedBy;
            player = owner.get();
        }
        if (player == null) {
            player = FakePlayerFactory.getMinecraft(level);
            player.setPos(entity.position());
        }

        BlockPos pos = entity.blockPosition();
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        PackagedSableShip.release(level, player, player.getUsedItemHand(), entity.getBox(), hit);
    }
}
