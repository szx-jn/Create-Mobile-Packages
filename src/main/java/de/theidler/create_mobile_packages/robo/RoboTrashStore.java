package de.theidler.create_mobile_packages.robo;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RoboTrashStore {
    private final UUID playerUUID;
    private final UUID networkUUID;
    private final List<ItemStack> itemStacks;
    private String targetAddress = "";

    public RoboTrashStore(UUID playerUUID, UUID networkUUID, List<ItemStack> itemStacks) {
        this.playerUUID = playerUUID;
        this.networkUUID = networkUUID;
        this.itemStacks = itemStacks;
    }

    public static RoboTrashStore load(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        UUID playerUUID = tag.getUUID("PlayerUUID");
        UUID networkUUID = tag.getUUID("NetworkUUID");
        if (!tag.contains("ItemStacks", Tag.TAG_LIST) || !(tag.get("ItemStacks") instanceof ListTag list)
                || (!list.isEmpty() && list.getElementType() != Tag.TAG_COMPOUND)) {
            throw new IllegalArgumentException("Invalid saved drone trash inventory");
        }
        ListTag itemsTag = tag.getList("ItemStacks", Tag.TAG_COMPOUND);
        List<ItemStack> itemStacks = new ArrayList<>(itemsTag.stream()
                .map(itemTag -> ((CompoundTag) itemTag).isEmpty() ? ItemStack.EMPTY : ItemStack.parse(registries, itemTag).orElseThrow(() -> new IllegalArgumentException("Invalid stored trash item")))
                .toList());

        RoboTrashStore store = new RoboTrashStore(playerUUID, networkUUID, itemStacks);
        if (tag.contains("TargetAddress")) {
            store.setTargetAddress(tag.getString("TargetAddress"));
        }
        return store;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public void setTargetAddress(String address) {
        this.targetAddress = address;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public UUID getNetworkUUID() {
        return networkUUID;
    }

    public List<ItemStack> getItemStacks() {
        return itemStacks;
    }

    /**
     * Returns true if at least one slot contains a non-empty ItemStack.
     */
    public boolean hasItems() {
        return itemStacks.stream().anyMatch(stack -> !stack.isEmpty());
    }

    public CompoundTag save(net.minecraft.core.HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putUUID("PlayerUUID", playerUUID);
        tag.putUUID("NetworkUUID", networkUUID);
        ListTag itemsTag = new ListTag();
        itemStacks.forEach(itemStack -> itemsTag.add(itemStack.saveOptional(registries)));
        tag.put("ItemStacks", itemsTag);
        tag.putString("TargetAddress", targetAddress);
        return tag;
    }
}
