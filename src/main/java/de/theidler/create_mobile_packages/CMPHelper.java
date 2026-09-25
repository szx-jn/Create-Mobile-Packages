package de.theidler.create_mobile_packages;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.ModCapabilities;
import de.theidler.create_mobile_packages.compat.sable.SableCompat;
import de.theidler.create_mobile_packages.index.config.CMPConfigs;
import de.theidler.create_mobile_packages.robo.VirtualRobo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class CMPHelper {

    public static Vec3 readVec3FromTag(CompoundTag tag, String key) {
        double x = tag.getDouble(key + "X");
        double y = tag.getDouble(key + "Y");
        double z = tag.getDouble(key + "Z");
        return new Vec3(x, y, z);
    }

    public static CompoundTag writeVec3ToTag(CompoundTag tag, String key, Vec3 vec) {
        tag.putDouble(key + "X", vec.x);
        tag.putDouble(key + "Y", vec.y);
        tag.putDouble(key + "Z", vec.z);
        return tag;
    }

    public static boolean isWithinRange(Level level, BlockPos targetPos, BlockPos originPos) {
        int maxDistance = CMPConfigs.server().beeMaxDistance.get();
        if (targetPos == null || originPos == null) return false;
        if (maxDistance == -1) return true;
        Vec3 projectedTarget = projectOutOfSubLevel(level, Vec3.atCenterOf(targetPos));
        Vec3 projectedOrigin = projectOutOfSubLevel(level, Vec3.atCenterOf(originPos));
        return projectedTarget.distanceToSqr(projectedOrigin) <= maxDistance * maxDistance;
    }

    /**
     * Finds the closest BeePortBlockEntity to this RoboEntity, optionally filtered by an address.
     */
    public static BeePortBlockEntity getClosestBeePort(Level level, String address, BlockPos origin, VirtualRobo entity, UUID logisticsNetworkId) {
        final BeePortBlockEntity[] closest = {null};
        level.getCapability(ModCapabilities.BEE_PORT_ENTITY_TRACKER_CAP).ifPresent(tracker -> {
            List<BeePortBlockEntity> allBEs = new ArrayList<>(tracker.getAllByNetwork(logisticsNetworkId));
            if (allBEs.isEmpty()) {
                allBEs.addAll(tracker.getAll());
            }
            allBEs.removeIf(BlockEntity::isRemoved);
            allBEs.removeIf(dpbe -> !isWithinRange(level, dpbe.getBlockPos(), origin));
            if (address != null && !address.isEmpty()) {
                allBEs.removeIf(dpbe -> !PackageItem.matchAddress(address, dpbe.addressFilter));
            }
            allBEs.removeIf(dpbe -> !dpbe.canAcceptEntity(entity, (entity != null && !entity.getItemStack().isEmpty())));
            Vec3 projectedOrigin = projectOutOfSubLevel(level, Vec3.atCenterOf(origin));
            closest[0] = allBEs.stream()
                    .min(Comparator.comparingDouble(a -> getGlobalCenter(level, a.getBlockPos()).distanceToSqr(projectedOrigin)))
                    .orElse(null);
        });
        return closest[0];
    }


    /**
     * Calculates the estimated time of arrival (ETA) to the specified targetPosition.
     *
     * @param targetPosition The Vec3 to calculate the ETA for.
     * @param currentPosition The Vec3 of the current position.
     * @return The ETA in seconds.
     */
    public static int calcETA(Vec3 targetPosition, Vec3 currentPosition) {
        if (targetPosition == null || currentPosition == null) return -1;
        double distance = targetPosition.distanceTo(currentPosition);
        // Use a reasonable speed estimate (player walking speed) for ETA
        double speedBPS = 4.317; // blocks per second
        return (int) (distance / speedBPS) + 1;
    }

    public static boolean doesAddressMatchPlayer(Player player, String address) {
        if (address == null) return false;
        String playerName = player.getName().getString();
        int atIndex = address.lastIndexOf('@');
        if (atIndex == -1) {
            return address.equals(playerName);
        }
        return address.substring(atIndex + 1).equals(playerName);
    }

    public static @Nullable BeePortBlockEntity getPortAtPos(ServerLevel serverLevel, @Nullable BlockPos pos) {
        if (pos == null) return null;
        BlockEntity be = serverLevel.getBlockEntity(pos);
        if (be instanceof BeePortBlockEntity bpbe) {
            return bpbe;
        }
        return null;
    }

    public static Vec3 getGlobalCenter(@Nullable Level level, BlockPos pos) {
        return projectOutOfSubLevel(level, Vec3.atCenterOf(pos));
    }

    public static Vec3 projectOutOfSubLevel(@Nullable Level level, Vec3 pos) {
        if (level == null || pos == null) {
            return pos;
        }
        return SableCompat.projectOutOfSubLevel(level, pos);
    }
}
