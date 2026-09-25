package de.theidler.create_mobile_packages.robo;

import de.theidler.create_mobile_packages.CMPHelper;
import de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity;
import de.theidler.create_mobile_packages.blocks.bee_port.ModCapabilities;
import de.theidler.create_mobile_packages.blocks.bee_port.RoboRequest;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class RoboManager extends SavedData {

    /** Interval in milliseconds for the global orphan robo check. */
    private static final long ORPHAN_CHECK_INTERVAL_MS = 5000;

    public Map<UUID, VirtualRobo> robos;
    public List<RoboRequest> beePortRoboRequests;
    public List<RoboTrashStore> roboTrashStores;
    private long lastOrphanCheck = 0;

    public RoboManager() {
        init();
    }

    public static RoboManager load(ServerLevel level, CompoundTag tag) {
        RoboManager manager = new RoboManager();

        ListTag robosList = tag.getList("robos", Tag.TAG_COMPOUND);
        for (int i = 0; i < robosList.size(); i++) {
            CompoundTag roboTag = robosList.getCompound(i);
            VirtualRobo robo = VirtualRobo.deserializeNBT(level, roboTag);
            manager.robos.put(robo.getId(), robo);
        }

        ListTag trashSlotsTag = tag.getList("trashSlots", Tag.TAG_COMPOUND);
        for (int i = 0; i < trashSlotsTag.size(); i++) {
            CompoundTag trashStoreTag = trashSlotsTag.getCompound(i);
            RoboTrashStore roboTrashStore = RoboTrashStore.load(trashStoreTag);
            manager.roboTrashStores.add(roboTrashStore);
        }
        return manager;
    }

    public static RoboManager get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent((tag) -> RoboManager.load(level, tag), RoboManager::new, "create_mobile_packages_robo_manager");
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag robosList = new ListTag();
        for (VirtualRobo robo : robos.values()) {
            robosList.add(robo.serializeNBT());
        }
        tag.put("robos", robosList);

        ListTag trashSlotsTag = new ListTag();
        for (RoboTrashStore roboTrashStore : roboTrashStores) {
            trashSlotsTag.add(roboTrashStore.save());
        }
        tag.put("trashSlots", trashSlotsTag);
        return tag;
    }

    public @Nullable VirtualRobo get(UUID roboId) {
        return robos.get(roboId);
    }

    public void remove(UUID roboId) {
        robos.remove(roboId);
        this.setDirty();
    }

    public void add(VirtualRobo robo) {
        robos.put(robo.getId(), robo);
        this.setDirty();
    }

    public @Nullable RoboTrashStore getTrashStore(@NotNull UUID networkId, @NotNull UUID playerId) {
        return roboTrashStores.stream()
                .filter((store) -> store.getNetworkUUID().equals(networkId))
                .filter((store) -> store.getPlayerUUID().equals(playerId))
                .findFirst().orElse(null);
    }

    public synchronized @Nullable List<ItemStack> takeTrashItems(@NotNull UUID networkId, @NotNull UUID playerId) {
        RoboTrashStore store = getTrashStore(networkId, playerId);
        if (store == null || !store.hasItems()) return null;
        List<ItemStack> snapshot = new ArrayList<>();
        for (ItemStack stack : store.getItemStacks()) {
            snapshot.add(stack.copy());
        }
        store.getItemStacks().clear();
        this.setDirty();
        return snapshot;
    }

    public synchronized void setTrashSlots(UUID networkId, UUID playerId, List<ItemStack> trashSlots) {
        RoboTrashStore existingStore = getTrashStore(networkId, playerId);
        if (existingStore != null) {
            existingStore.getItemStacks().clear();
            existingStore.getItemStacks().addAll(trashSlots);
        } else {
            roboTrashStores.add(new RoboTrashStore(playerId, networkId, new ArrayList<>(trashSlots)));
        }
        setDirty();
    }

    public synchronized void setTrashTargetAddress(UUID networkId, UUID playerId, String address) {
        RoboTrashStore store = getTrashStore(networkId, playerId);
        if (store != null) {
            store.setTargetAddress(address);
            setDirty();
        } else {
            List<ItemStack> emptySlots = new ArrayList<>(Collections.nCopies(9, ItemStack.EMPTY));
            RoboTrashStore newStore = new RoboTrashStore(playerId, networkId, emptySlots);
            newStore.setTargetAddress(address);
            roboTrashStores.add(newStore);
            setDirty();
        }
    }

    public void tick(ServerLevel level) {
        robos.values().forEach(robo -> {
            robo.tick(level);
            // Two-strike orphan detection
            if (robo.getEntityId() == null && robo.getTarget() == null && robo.getItemStack().isEmpty()) {
                if (robo.isOrphanSuspected()) {
                    robo.setRemoved(level);
                } else {
                    robo.setOrphanSuspected(true);
                }
            } else {
                robo.setOrphanSuspected(false);
            }
        });
        getPendingRoboRequests().forEach(roboRequest -> tryHandlingRequest(roboRequest, level));
        long now = System.currentTimeMillis();
        beePortRoboRequests.removeIf(r -> (r.getStatus() == RoboRequest.Status.DONE || r.getStatus() == RoboRequest.Status.CANCELLED) && (now - r.getCreatedAt()) > 60_000);
        this.setDirty();
    }

    private void tryHandlingRequest(RoboRequest request, ServerLevel level) {
        level.getCapability(ModCapabilities.BEE_PORT_ENTITY_TRACKER_CAP).ifPresent(tracker -> {
            List<BeePortBlockEntity> allBEs = new ArrayList<>(tracker.getAllByNetwork(request.getLogisticsNetworkId()));
            allBEs.removeIf(BlockEntity::isRemoved);
            allBEs.removeIf(be -> !BlockPos.containing(request.getTargetPos()).equals(be.getBlockPos()));
            allBEs.removeIf(be -> be.getRoboBeeInventory().getStackInSlot(0).getCount() <= 0);
            allBEs.stream().min(Comparator.comparingDouble(a -> CMPHelper.getGlobalCenter(a.getLevel(), a.getBlockPos()).distanceToSqr(request.getTargetPos())))
                    .ifPresent(target -> target.handleRequest(request));
        });
    }

    private boolean hasActiveTrashRequest(UUID playerId, UUID networkId) {
        for (RoboRequest request : beePortRoboRequests) {
            if (!request.getLogisticsNetworkId().equals(networkId)) continue;
            if (!(request.getTarget() instanceof PlayerTarget target)) continue;

            if (target.asPlayer() == null || !target.asPlayer().getUUID().equals(playerId)) continue;

            if (request.getStatus() == RoboRequest.Status.PENDING) {
                if (request.getMission() == RoboRequest.Mission.PICKUP) {
                    return true;
                }
            }

            if (request.getStatus() == RoboRequest.Status.IN_PROGRESS) {
                boolean roboExists = robos.values().stream()
                        .anyMatch(robo -> robo.getRequest() == request);

                if (roboExists) {
                    return true;
                } else {
                    request.setStatus(RoboRequest.Status.CANCELLED);
                }
            }
        }
        return false;
    }

    public UUID newRobo(ServerLevel level, ItemStack itemStack, BlockPos spawnPos, UUID logisticsNetworkId, float packageHeightScale, @Nullable BlockPos homePort, boolean returnToHomeAfterDelivery) {
        UUID id = UUID.randomUUID();
        VirtualRobo robo = new VirtualRobo(level, id, itemStack, spawnPos, logisticsNetworkId);
        robo.setPackageHeightScale(packageHeightScale);
        robo.setHomePortPos(homePort);
        robo.setReturnToHomeAfterDelivery(returnToHomeAfterDelivery);
        this.add(robo);
        setDirty();
        return id;
    }

    public void newRequestRobo(ServerLevel level, BlockPos spawnPos, RoboRequest request) {
        UUID id = UUID.randomUUID();
        VirtualRobo robo = new VirtualRobo(level, id, ItemStack.EMPTY, spawnPos, request.getLogisticsNetworkId());
        robo.setRequest(request);
        this.add(robo);
        setDirty();
    }

    public void requestRobo(RoboTarget roboTarget, UUID logisticsNetworkId, RoboRequest.Mission mission) {
        requestRobo(new RoboRequest(roboTarget, logisticsNetworkId, mission));
    }

    public synchronized void requestRobo(RoboRequest request) {
        beePortRoboRequests.add(request);
        setDirty();
    }

    public List<RoboRequest> getRoboRequestsWithStatus(RoboRequest.Status status) {
        return beePortRoboRequests.stream().filter(request -> request.getStatus() == status).toList();
    }

    public List<RoboRequest> getPendingRoboRequests() {
        return getRoboRequestsWithStatus(RoboRequest.Status.PENDING);
    }

    public List<RoboRequest> getRoboRequests(BlockPos pos) {
        return beePortRoboRequests.stream().filter(request -> isTargetingPortAt(pos, request.getTarget())).toList();
    }

    public List<VirtualRobo> getInboundRobo(BlockPos pos) {
        List<VirtualRobo> inboundRobos = new ArrayList<>();
        for (VirtualRobo robo : robos.values()) {
            RoboTarget target = robo.getTarget();
            if (target != null && isTargetingPortAt(pos, target)) {
                inboundRobos.add(robo);
            }
        }
        return inboundRobos;
    }

    public List<Integer> getETAs(BlockPos pos) {
        List<Integer> eta = new ArrayList<>();
        getRoboRequests(pos).stream().map(RoboRequest::getEta).forEach(eta::add);
        getInboundRobo(pos).stream().map(robo -> {
            RoboTarget target = robo.getTarget();
            if (target != null) return target.getETA();
            return -1;
        }).forEach(eta::add);
        return eta.stream().filter(integer -> integer >= 0).toList();
    }

    private void init() {
        this.robos = new ConcurrentHashMap<>();
        this.beePortRoboRequests = new CopyOnWriteArrayList<>();
        this.roboTrashStores = new CopyOnWriteArrayList<>();
    }

    private boolean isTargetingPortAt(BlockPos pos, @Nullable RoboTarget target) {
        if (target == null) {
            return false;
        }
        BeePortBlockEntity targetPort = target.asBeePortBlockEntity();
        if (targetPort != null) {
            return targetPort.getBlockPos().equals(pos);
        }
        return target.getTargetPos() != null && BlockPos.containing(target.getTargetPos()).equals(pos);
    }
}
