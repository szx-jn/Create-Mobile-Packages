package de.theidler.create_mobile_packages;

import de.theidler.create_mobile_packages.index.*;
import de.theidler.create_mobile_packages.items.mobile_packager.*;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.trash_menu.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = CreateMobilePackages.MODID, value = Dist.CLIENT)
public final class CMPClientCheck {
    private static int stage, ticks, escapes;
    private static final long DEADLINE = System.nanoTime() + java.util.concurrent.TimeUnit.MINUTES.toNanos(2);
    @SubscribeEvent public static void check(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("dimensionworks.verifyCMP") || stage == 9) return;
        var client=Minecraft.getInstance();require(System.nanoTime()<DEADLINE,"CMP client timed out at stage "+stage+": "+client.screen);
        if (client.screen instanceof net.minecraft.client.gui.screens.AccessibilityOnboardingScreen) {
            client.options.onboardingAccessibilityFinished();client.setScreen(new net.minecraft.client.gui.screens.TitleScreen());
        }
        if(stage==0 && client.screen instanceof net.minecraft.client.gui.screens.TitleScreen && client.getOverlay()==null) {
            stage=1;client.options.renderDistance().set(2);client.options.simulationDistance().set(5);
            client.options.framerateLimit().set(30);client.options.pauseOnLostFocus=false;
            var settings=new net.minecraft.world.level.LevelSettings("DW CMP Check",net.minecraft.world.level.GameType.CREATIVE,false,
                net.minecraft.world.Difficulty.PEACEFUL,true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
            client.createWorldOpenFlows().createFreshLevel("dw-cmp-check",settings,new net.minecraft.world.level.levelgen.WorldOptions(7,false,false),
                access->access.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).createWorldDimensions(),client.screen);
        }
        if(client.player==null || client.getConnection()==null || client.getOverlay()!=null) return;
        ticks++;
        if(stage==1) {
            stage=2;client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            var id=client.player.getUUID();client.getSingleplayerServer().execute(()->{
                var player=client.getSingleplayerServer().getPlayerList().getPlayer(id);player.setShiftKeyDown(true);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,CMPItems.MOBILE_PACKAGER.asStack());player.inventoryMenu.broadcastChanges();
                var manager=de.theidler.create_mobile_packages.robo.RoboManager.get(player.serverLevel());
                manager.newRobo(player.serverLevel(),com.simibubi.create.content.logistics.box.PackageItem.containing(java.util.List.of(new ItemStack(Items.DIAMOND))),
                    player.blockPosition().offset(0,1,2),java.util.UUID.randomUUID(),1,null,false);
            });
        } else if(stage==2 && client.player.getMainHandItem().is(CMPItems.MOBILE_PACKAGER.get())) {
            stage=3;ticks=0;net.createmod.catnip.platform.CatnipServices.NETWORK.sendToServer(new PackPlayerPacket(client.player.getId(),true));
        } else if(stage==3 && PackedPlayerState.isPacked(client.player)) {
            require(client.player.getDimensions(net.minecraft.world.entity.Pose.STANDING).height()==0.5F,"Client packed dimensions failed");
            if(ticks<30)return;
            var bees=client.level.entitiesForRendering();boolean found=false;
            for(var entity:bees)if(entity instanceof de.theidler.create_mobile_packages.entities.RoboBeeEntity bee) {
                require(client.getEntityRenderDispatcher().getRenderer(bee) instanceof de.theidler.create_mobile_packages.entities.render.DroneEntityRenderer,"Bee renderer missing");found=true;
            }
            require(found,"No native bee reached the client");
            CreateMobilePackages.LOGGER.info("CMP CLIENT PACKED CHECK PASSED: native payload, dimensions, third person renderer and bee model");
            stage=4;ticks=0;
        } else if(stage==4) {
            if(escapes<5 && ticks%4==0){escapes++;net.createmod.catnip.platform.CatnipServices.NETWORK.sendToServer(new EscapePackedPlayerPacket());}
            if(escapes==5 && !PackedPlayerState.isPacked(client.player)) {
                require(client.player.getDimensions(net.minecraft.world.entity.Pose.STANDING).height()>0.5F,"Client escape dimensions failed");
                stage=5;var id=client.player.getUUID();client.getSingleplayerServer().execute(()->{
                    var player=client.getSingleplayerServer().getPlayerList().getPlayer(id);var ticker=CMPItems.PORTABLE_STOCK_TICKER.asStack();
                    var network=java.util.UUID.randomUUID();de.theidler.create_mobile_packages.items.portable_stock_ticker.LogisticallyLinkedItem.assignFrequency(ticker,player,network);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ticker);player.inventoryMenu.broadcastChanges();
                    var item=new ItemStack(Items.DIAMOND,31);item.set(DataComponents.CUSTOM_NAME,Component.literal("retained client cargo"));
                    var manager=de.theidler.create_mobile_packages.robo.RoboManager.get(player.serverLevel());manager.setTrashSlots(network,id,java.util.List.of(item));manager.setTrashTargetAddress(network,id,"retained-client-route");
                });
            }
        } else if(stage==5 && client.player.getMainHandItem().is(CMPItems.PORTABLE_STOCK_TICKER.get())) {
            stage=6;ticks=0;net.createmod.catnip.platform.CatnipServices.NETWORK.sendToServer(OpenTrashMenuPacket.INSTANCE);
        } else if(stage==6 && client.screen instanceof TrashScreen && client.player.containerMenu instanceof TrashMenu menu) {
            if(ticks<30)return;
            require(menu.getTargetAddress().equals("retained-client-route"),"Trash route not synced");
            require(menu.slots.get(0).getItem().getCount()==31 && menu.slots.get(0).getItem().getHoverName().getString().equals("retained client cargo"),"Trash inventory not synced");
            CreateMobilePackages.LOGGER.info("CMP CLIENT TRASH CHECK PASSED: actual menu, address, item count and name synchronization");
            client.player.closeContainer();stage=7;ticks=0;var id=client.player.getUUID();client.getSingleplayerServer().execute(()->
                net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(client.getSingleplayerServer().getPlayerList().getPlayer(id),new ShowOversizedNbtScreenPacket()));
        } else if(stage==7 && client.screen instanceof OversizedNbtScreen && ticks>15) {
            CreateMobilePackages.LOGGER.info("CMP CLIENT CHECK PASSED: five-packet escape, restored dimensions and oversized-NBT warning screen");
            stage=9;client.stop();
        }
    }
    private static void require(boolean result,String message){if(!result)throw new AssertionError(message);}
}
