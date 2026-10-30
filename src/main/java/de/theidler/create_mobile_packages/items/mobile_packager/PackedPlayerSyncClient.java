package de.theidler.create_mobile_packages.items.mobile_packager;

import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class PackedPlayerSyncClient {
    private PackedPlayerSyncClient() {}
    public static void handle(int id, boolean packed) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        var entity = level.getEntity(id);
        if (entity != null) {
            boolean wasPacked = entity.getPersistentData().getBoolean(PackedPlayerState.PACKED);
            entity.getPersistentData().putBoolean(PackedPlayerState.PACKED, packed);
            if (entity instanceof net.minecraft.world.entity.player.Player player) {
                player.refreshDimensions();
                Minecraft mc = Minecraft.getInstance();
                if (player == mc.player && wasPacked != packed) {
                    PlayerPackClientEvents.onLocalPackedStateChanged(packed);
                }
            }
        }
    }
}
