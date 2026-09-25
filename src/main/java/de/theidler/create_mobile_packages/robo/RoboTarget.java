package de.theidler.create_mobile_packages.robo;

import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public interface RoboTarget {
    Vec3 getTargetPos();

    @Nullable ServerLevel getTargetLevel();

    default BeePortBlockEntity asBeePortBlockEntity() {
        return null;
    }

    default Player asPlayer() {
        return null;
    }

    default BlockPos asBlockPos() {
        return null;
    }

    default boolean isValid(VirtualRobo robo) {
        return true;
    }

    void setETA(int eta);

    int getETA();
}
