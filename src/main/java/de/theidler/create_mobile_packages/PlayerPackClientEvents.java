package de.theidler.create_mobile_packages;

import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.logistics.box.PackageRenderer;
import de.theidler.create_mobile_packages.index.CMPPackets;
import de.theidler.create_mobile_packages.items.mobile_packager.EscapePackedPlayerPacket;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedPlayerState;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreateMobilePackages.MODID, value = Dist.CLIENT)
public final class PlayerPackClientEvents {
    private static CameraType cameraBeforePacking;
    private static long promptExpiresAt;

    private PlayerPackClientEvents() {}

    public static void onLocalPackedStateChanged(boolean packed) {
        Minecraft mc = Minecraft.getInstance();
        if (packed) {
            cameraBeforePacking = mc.options.getCameraType();
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
            promptExpiresAt = System.currentTimeMillis() + 7_000L;
        } else {
            if (cameraBeforePacking != null) mc.options.setCameraType(cameraBeforePacking);
            cameraBeforePacking = null;
            promptExpiresAt = 0L;
        }
    }

    @SubscribeEvent
    public static void keepPackedCameraThirdPerson(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && PackedPlayerState.isPacked(mc.player)
                && mc.options.getCameraType() == CameraType.FIRST_PERSON) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    @SubscribeEvent
    public static void renderPackedPrompt(RenderGuiOverlayEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (!event.getOverlay().id().equals(VanillaGuiOverlay.HOTBAR.id()) || mc.player == null
                || !PackedPlayerState.isPacked(mc.player) || System.currentTimeMillis() >= promptExpiresAt) return;
        int x = mc.getWindow().getGuiScaledWidth() / 2;
        int y = mc.getWindow().getGuiScaledHeight() / 2 + 18;
        event.getGuiGraphics().drawCenteredString(mc.font,
                Component.translatable("create_mobile_packages.player_pack.packed_prompt"), x, y, 0xFFFFFFFF);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getAction() != 1 || event.getKey() != org.lwjgl.glfw.GLFW.GLFW_KEY_EQUAL || mc.player == null || !PackedPlayerState.isPacked(mc.player)) return;
        CMPPackets.getChannel().sendToServer(new EscapePackedPlayerPacket());
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void renderBox(RenderPlayerEvent.Pre event) {
        var player = event.getEntity();
        if (!PackedPlayerState.isPacked(player) || AllPartialModels.PACKAGES_TO_HIDE_AS.isEmpty()) return;

        event.setCanceled(true);
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType() == CameraType.FIRST_PERSON) return;

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        pose.translate(0, 2 / 16f, 0);
        pose.scale(0.8f, 0.8f, 0.8f);

        float movement = (float) player.position().subtract(player.xo, player.yo, player.zo).length();
        if (player.onGround()) {
            pose.translate(0, Math.min(Math.abs(net.minecraft.util.Mth.cos((AnimationTickHolder.getRenderTime() % 256) / 2.0f)) * 2 / 16f, movement * 5), 0);
        }

        float yaw = net.minecraft.util.Mth.lerp(event.getPartialTick(), player.yRotO, player.getYRot());
        int cardboardIndex = Math.floorMod(player.getUUID().hashCode(), AllPartialModels.PACKAGES_TO_HIDE_AS.size());
        PackageRenderer.renderBox(player, yaw, pose, event.getMultiBufferSource(), event.getPackedLight(),
                AllPartialModels.PACKAGES_TO_HIDE_AS.get(cardboardIndex));
        pose.popPose();
    }
}
