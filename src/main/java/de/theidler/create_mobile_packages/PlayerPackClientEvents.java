package de.theidler.create_mobile_packages;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.logistics.box.PackageItem;
import de.theidler.create_mobile_packages.index.CMPPackets;
import de.theidler.create_mobile_packages.items.mobile_packager.EscapePackedPlayerPacket;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMobilePackages.MODID, value = Dist.CLIENT)
public final class PlayerPackClientEvents {
    private PlayerPackClientEvents() {}

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() != 1 || event.getKey() != org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL || mc.player == null || !PackedPlayerState.isPacked(mc.player)) return;
        CMPPackets.getChannel().sendToServer(new EscapePackedPlayerPacket());
    }

    @SubscribeEvent
    public static void renderBox(RenderPlayerEvent.Post event) {
        if (!PackedPlayerState.isPacked(event.getEntity())) return;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, 0.28, 0);
        pose.scale(0.8F, 0.8F, 0.8F);
        Minecraft mc = Minecraft.getInstance();
        mc.getItemRenderer().renderStatic(PackageItem.containing(java.util.List.of()), ItemDisplayContext.NONE,
                event.getPackedLight(), net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,
                pose, event.getMultiBufferSource(), event.getEntity().level(), 0);
        pose.popPose();
    }
}
