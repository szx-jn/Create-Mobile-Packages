package de.theidler.create_mobile_packages.items.mobile_packager;

import com.simibubi.create.content.logistics.box.PackageItem;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.UUID;

public final class PackedPlayerState {
    public static final String PACKED = "cmp_packed_player";
    public static final String ESCAPES = "cmp_escape_clicks";
    public static final String PACKED_BY_OTHER = "cmp_packed_by_other";
    public static final UUID SPEED_ID = UUID.fromString("46e0d75a-1ea4-4698-b3bf-40d9f9c0d30f");
    private PackedPlayerState() {}

    public static boolean isPacked(net.minecraft.world.entity.player.Player player) {
        return player.getPersistentData().getBoolean(PACKED);
    }

    public static ItemStack visualPackage() { return PackageItem.containing(List.of()); }

    public static void setPacked(ServerPlayer player, boolean value, boolean byOther) {
        CompoundTag data = player.getPersistentData();
        data.putBoolean(PACKED, value);
        data.putBoolean(PACKED_BY_OTHER, value && byOther);
        data.putInt(ESCAPES, 0);
        player.refreshDimensions();
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(SPEED_ID);
            if (value) attribute.addTransientModifier(new AttributeModifier(SPEED_ID, "CMP packed player", -0.10D, AttributeModifier.Operation.MULTIPLY_TOTAL));
        }
        PackPlayerPacket.sync(player);
    }

    public static void escape(ServerPlayer player) {
        if (!isPacked(player)) return;
        CompoundTag data = player.getPersistentData();
        int clicks = data.getInt(ESCAPES) + 1;
        data.putInt(ESCAPES, clicks);
        if (clicks >= 5) {
            boolean wasPackedByOther = data.getBoolean(PACKED_BY_OTHER);
            setPacked(player, false, false);
            if (wasPackedByOther) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(CMPPlayerPack.IMMUNITY.get(), 3 * 60 * 20, 0, false, true, true));
            }
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("create_mobile_packages.player_pack.escaped"), true);
        } else {
            player.displayClientMessage(net.minecraft.network.chat.Component.translatable("create_mobile_packages.player_pack.escape_progress", clicks, 5), true);
        }
    }
}
