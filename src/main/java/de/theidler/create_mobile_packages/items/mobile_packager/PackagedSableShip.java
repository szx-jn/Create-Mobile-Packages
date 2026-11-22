package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.AllDataComponents;
import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.items.ItemStackHandler;

import java.util.stream.IntStream;

public final class PackagedSableShip {
    private PackagedSableShip() {}

    public static boolean release(ServerLevel level, Player player, InteractionHand hand,
            ItemStack packageStack, BlockHitResult hit) {
        if (!PackageItem.isPackage(packageStack)) return false;

        Item shipBottle = BuiltInRegistries.ITEM.get(
            ResourceLocation.fromNamespaceAndPath("sableinbottle", "ship_in_a_bottle"));
        if (shipBottle == null || shipBottle == net.minecraft.world.item.Items.AIR) return false;

        ItemStackHandler contents = PackageItem.getContents(packageStack);
        boolean changed = false;
        for (int slot = 0; slot < contents.getSlots(); slot++) {
            ItemStack bottle = contents.getStackInSlot(slot);
            if (bottle.isEmpty() || bottle.getItem() != shipBottle || !isPackagerBottle(bottle)) continue;

            ItemStack heldStack = player.getItemInHand(hand);
            player.setItemInHand(hand, bottle.copy());
            boolean wasShiftKeyDown = player.isShiftKeyDown();
            player.setShiftKeyDown(false);
            InteractionResult result;
            try {
                result = shipBottle.useOn(new UseOnContext(player, hand, hit));
            } finally {
                player.setShiftKeyDown(wasShiftKeyDown);
                player.setItemInHand(hand, heldStack);
            }
            if (result.consumesAction()) {
                contents.setStackInSlot(slot, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) {
            packageStack.set(AllDataComponents.PACKAGE_CONTENTS,
                net.minecraft.world.item.component.ItemContainerContents.fromItems(
                    IntStream.range(0, contents.getSlots()).mapToObj(contents::getStackInSlot).toList()));
        }
        return changed;
    }

    private static boolean isPackagerBottle(ItemStack stack) {
        CustomData data = stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        if (data == null) return false;
        var tag = data.copyTag();
        return tag.getBoolean(MobilePackagerBlockContext.SABLE_SHIP_MARKER)
            && tag.contains("ship", net.minecraft.nbt.Tag.TAG_COMPOUND);
    }
}
