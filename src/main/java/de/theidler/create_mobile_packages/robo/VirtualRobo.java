package de.theidler.create_mobile_packages.robo;

import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.CMPHelper;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.RoboRequest;
import de.theidler.create_mobile_packages.entities.RoboBeeEntity;
import de.theidler.create_mobile_packages.entities.robo_entity.RoboBeeBehaviorController;
import de.theidler.create_mobile_packages.index.CMPEntities;
import de.theidler.create_mobile_packages.index.config.CMPConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

import static de.theidler.create_mobile_packages.CMPHelper.*;


public class VirtualRobo {
    /** Minimum travel speed in blocks per second (player walking speed). */
    public static final double MIN_SPEED_BPS = 4.317;
    /** Interval in milliseconds at which the travel speed is recalculated. */
    public static final long SPEED_RECALC_INTERVAL_MS = 3000;

    private final UUID id;
    private final UUID logisticsNetworkId;
    private ItemStack itemStack;
    private Vec3 currentPos = Vec3.ZERO;
    private float yaw;
    private float pitch;
    private UUID entityId; // if a RoboEntity is spawned
    private double travelSpeedPerTick = MIN_SPEED_BPS / 20.0; // recalculated during transport
    private long transportStartTime = -1; // when the current transport (navigation) started
    private long lastSpeedRecalc = 0;
    private boolean orphanSuspected = false; // first-strike flag of the two-strike orphan confirmation
    private final RoboBeeBehaviorController behaviorController;
    private @Nullable RoboTarget target;
    private String targetAddress;
    private Vec3 targetVelocity = Vec3.ZERO;
    private ServerLevel serverLevel;
    private float packageHeightScale;
    private RoboRequest request = null;
    private @Nullable BlockPos homePortPos;
    private @Nullable net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> homePortDimension;
    private boolean returnToHomeAfterDelivery;

    public VirtualRobo(ServerLevel level, UUID id, ItemStack itemStack, BlockPos spawnPos, UUID logisticsNetworkId) {
        this.id = id;
        this.logisticsNetworkId = logisticsNetworkId;
        this.serverLevel = level;
        this.homePortDimension = level.dimension();
        this.itemStack = itemStack;
        setTargetFromItemStack(itemStack);
        this.currentPos = CMPHelper.projectOutOfSubLevel(level, spawnPos.getCenter()).subtract(0, 0.5, 0);
        this.behaviorController = new RoboBeeBehaviorController();
    }

    public static VirtualRobo deserializeNBT(ServerLevel level, CompoundTag roboTag) {
        UUID id = roboTag.getUUID("id");
        Vec3 pos = readVec3FromTag(roboTag, "pos");
        UUID logisticsNetworkId = roboTag.getUUID("logisticsNetworkId");

        ItemStack itemStack = ItemStack.EMPTY;
        if (roboTag.contains("itemStack", Tag.TAG_COMPOUND)) {
            itemStack = ItemStack.of(roboTag.getCompound("itemStack"));
        }

        VirtualRobo virtualRobo = new VirtualRobo(level, id, itemStack, BlockPos.containing(pos), logisticsNetworkId);
        if (roboTag.contains("homePortPos")) {
            virtualRobo.setHomePortPos(BlockPos.of(roboTag.getLong("homePortPos")));
        }
        if (roboTag.contains("homePortDimension")) {
            virtualRobo.homePortDimension = net.minecraft.resources.ResourceKey.create(
                    net.minecraft.core.registries.Registries.DIMENSION,
                    new net.minecraft.resources.ResourceLocation(roboTag.getString("homePortDimension")));
        }
        virtualRobo.setReturnToHomeAfterDelivery(roboTag.getBoolean("returnToHomeAfterDelivery"));
        if (!virtualRobo.getItemStack().isEmpty()) {
            virtualRobo.setPackageHeightScale(1.0f);
        }
        return virtualRobo;
    }

    /**
     * Calculates the snap angle for a given angle. (45, 135, 225, 315)
     */
    private int getSnapAngle(double angle) {
        return (int) Math.abs(Math.round(angle / 90) * 90 - 45);
    }

    private double getAngleToTarget() {
        Vec3 targetPos = getTargetPosition();
        return targetPos != null ? Math.atan2(targetPos.z - this.currentPos.z, targetPos.x - this.currentPos.x()) : 0;
    }

    public @Nullable Vec3 getTargetPosition() {
        updateTarget();
        if (target == null) return null;
        return target.getTargetPos();
    }

    private void setTargetFromItemStack(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) setTargetAddress(null, false);
        else setTargetAddress(PackageItem.getAddress(itemStack), false);
    }

    private void updateTarget() {
        if (target != null && target.isValid(this)) return;

        target = PlayerTarget.fromAddress(serverLevel, targetAddress, logisticsNetworkId);
        if (target != null && target.isValid(this)) {return;}

        BeePortBlockEntity targetBlockEntity = CMPHelper.getClosestBeePort(serverLevel, targetAddress, BlockPos.containing(currentPos), this, logisticsNetworkId);
        if (targetBlockEntity != null) {
            target = new BeePortBlockEntityTarget(targetBlockEntity);
        }
        if (target != null && target.isValid(this)) {
            return;
        }

        // check HomePort
        BeePortBlockEntity homePort = CMPHelper.getPortAtPos(serverLevel, homePortPos);
        if (homePort != null) {
            target = new BeePortBlockEntityTarget(homePort);
        }
        if (target != null && target.isValid(this)) {
            return;
        }

        target = null;
    }

    public @Nullable Vec3 getTargetPositionForETA() {
        if (target == null) return null;
        return target.getTargetPos();
    }

    public void tick(ServerLevel level) {
        if (entityId == null || level.getEntity(entityId) == null) {
            if (target != null || !itemStack.isEmpty()) {
                spawnAndRememberEntity();
            }
        }
        behaviorController.tick(this);
        currentPos = currentPos.add(targetVelocity);

        // dynamic speed recalculation
        long now = System.currentTimeMillis();
        if (transportStartTime > 0 && now - lastSpeedRecalc > SPEED_RECALC_INTERVAL_MS) {
            lastSpeedRecalc = now;
            double elapsed = (now - transportStartTime) / 1000.0;
            if (elapsed > 0) {
                int maxDist = CMPConfigs.server().beeMaxDistance.get();
                double targetDist = maxDist > 0 ? maxDist : 1000;
                double remaining = Math.max(0, targetDist);
                double remainingTime = Math.max(0.5, remaining / MIN_SPEED_BPS);
                double baseSpeed = Math.max(MIN_SPEED_BPS, remaining / remainingTime);
                this.travelSpeedPerTick = baseSpeed / 20.0;
            }
        }
    }

    public void startTransport() {
        this.transportStartTime = System.currentTimeMillis();
        this.lastSpeedRecalc = transportStartTime;
        int maxDist = CMPConfigs.server().beeMaxDistance.get();
        if (maxDist > 0) {
            this.travelSpeedPerTick = maxDist / (20.0 * 10.0);
        }
    }

    public void stopTransport() {
        this.transportStartTime = -1;
        this.travelSpeedPerTick = MIN_SPEED_BPS / 20.0;
    }

    public boolean isOrphanSuspected() {
        return orphanSuspected;
    }

    public void setOrphanSuspected(boolean suspected) {
        this.orphanSuspected = suspected;
    }

    private void updateEntity() {
        if (this.entityId != null && (serverLevel.getEntity(entityId) instanceof RoboBeeEntity roboEntity)) {
            roboEntity.syncFromVirtual(this);
        } else {
            entityId = null;
        }
    }

    public void despawnEntity() {
        Entity entity = serverLevel.getEntity(entityId);
        if (entity != null) {
            entity.discard();
        }
        entityId = null;
    }

    private void spawnAndRememberEntity() {
        Entity entity = new RoboBeeEntity(CMPEntities.ROBO_BEE_ENTITY.get(), serverLevel, id);
        entity.setPos(currentPos.x, currentPos.y, currentPos.z);
        serverLevel.addFreshEntity(entity);
        this.entityId = entity.getUUID();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("id", id);
        writeVec3ToTag(tag, "pos", currentPos);
        tag.putUUID("logisticsNetworkId", logisticsNetworkId);
        if (!getItemStack().isEmpty()) {
            tag.put("itemStack", getItemStack().save(new CompoundTag()));
        }
        if (homePortPos != null) {
            tag.putLong("homePortPos", homePortPos.asLong());
        }
        tag.putBoolean("returnToHomeAfterDelivery", returnToHomeAfterDelivery);
        return tag;
    }

    public double getTravelSpeedPerTick() {
        return travelSpeedPerTick;
    }

    public Vec3 getCurrentPos() {
        return currentPos;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public UUID getId() {
        return id;
    }

    private int rotateToAngle(float targetYaw) {
        float currentYaw = this.yaw;
        float deltaYaw = targetYaw - currentYaw;
        deltaYaw = (deltaYaw > 180) ? deltaYaw - 360 : (deltaYaw < -180) ? deltaYaw + 360 : deltaYaw;
        float rotationSpeed = CMPConfigs.server().beeRotationSpeed.get();
        if (Math.abs(deltaYaw) > rotationSpeed) {
            currentYaw += (deltaYaw > 0) ? rotationSpeed : -rotationSpeed;
        } else {
            currentYaw = targetYaw;
        }
        this.yaw = currentYaw;
        return (int) Math.ceil(Math.abs(deltaYaw) / rotationSpeed);
    }

    public @Nullable RoboTarget getTarget() {
        return target;
    }

    public ServerLevel getServerLevel() {
        return serverLevel;
    }

    public void setPos(Vec3 pos) {
        this.currentPos = pos;
    }

    public int rotateToSnap() {
        return rotateToAngle((float) getSnapAngle(getAngleToTarget()) + 90);
    }

    public @Nullable BeePortBlockEntity getStartBeePortBlockEntity() {
        if (serverLevel.getBlockEntity(BlockPos.containing(currentPos)) instanceof BeePortBlockEntity bpbe) {
            return bpbe;
        } else if (serverLevel.getBlockEntity(BlockPos.containing(currentPos.subtract(0,1,0))) instanceof BeePortBlockEntity bpbe) {
            return bpbe;
        } else if (serverLevel.getBlockEntity(BlockPos.containing(currentPos.subtract(0,2,0))) instanceof BeePortBlockEntity bpbe) {
            return bpbe;
        }
        return null;
    }

    public void setRemoved(ServerLevel level) {
        RoboManager.get(level).remove(this.getId());
        if (request != null && request.getStatus() == RoboRequest.Status.IN_PROGRESS) {
            request.setStatus(RoboRequest.Status.PENDING);
        }
        despawnEntity();
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String address, boolean update) {
        this.targetAddress = address;
        if (update) {
            updateTarget();
        }
    }

    public float getPackageHeightScale() {
        return packageHeightScale;
    }

    public void setPackageHeightScale(float scale) {
        if (scale < 0.0f || scale > 1.0f) return;
        this.packageHeightScale = scale;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public ItemStack getItemStack() {
        return itemStack;
    }

    public void setItemStack(ItemStack itemStack) {
        this.itemStack = itemStack;
    }

    public void setTargetVelocity(Vec3 targetVelocity) {
        this.targetVelocity = targetVelocity;
    }

    public void invalidateTarget() {
        this.targetVelocity = Vec3.ZERO;
        this.target = null;
    }

    public RoboRequest getRequest() {
        return request;
    }

    public void setRequest(RoboRequest request) {
        this.request = request;
        this.request.setStatus(RoboRequest.Status.IN_PROGRESS);
        this.target = request.getTarget();
    }

    public void clearRequest() {
        if (this.request != null) {
            this.request.setStatus(RoboRequest.Status.DONE);
            this.request = null;
        }
        invalidateTarget();
    }

    public UUID getLogisticsNetworkId() {
        return logisticsNetworkId;
    }

    public void setHomePortPos(@Nullable BlockPos homePort) {
        if (homePort == null) return;
        this.homePortPos = homePort;
    }

    public boolean shouldReturnToHomePort() {
        return returnToHomeAfterDelivery && request == null && (itemStack == null || itemStack.isEmpty());
    }

    public void setReturnToHomeAfterDelivery(boolean returnToHomeAfterDelivery) {
        this.returnToHomeAfterDelivery = returnToHomeAfterDelivery;
    }
}
