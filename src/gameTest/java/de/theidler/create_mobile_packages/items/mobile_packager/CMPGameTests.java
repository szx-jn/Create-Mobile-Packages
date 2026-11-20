package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.index.CMPItems;
import de.theidler.create_mobile_packages.index.CMPDataComponents;
import de.theidler.create_mobile_packages.robo.RoboTrashStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@GameTestHolder("create_mobile_packages")
@PrefixGameTestTemplate(false)
public class CMPGameTests {
    @GameTest(template = "empty") public static void packedPlayerState(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var base = player.getAttributeValue(Attributes.MOVEMENT_SPEED);
        PackedPlayerState.setPacked(player, true, true);
        helper.assertTrue(PackedPlayerState.isPacked(player), "Packed flag missing");
        helper.assertTrue(Math.abs(player.getAttributeValue(Attributes.MOVEMENT_SPEED) - base * 0.6) < 0.0001, "Packed speed changed");
        helper.assertTrue(player.getDimensions(Pose.STANDING).height() == 0.5F && player.getDimensions(Pose.STANDING).eyeHeight() == 0.4F, "Packed dimensions/eye height changed");
        var incoming = new net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent(player,
            new net.neoforged.neoforge.common.damagesource.DamageContainer(helper.getLevel().damageSources().generic(), 10F));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(incoming);
        helper.assertTrue(incoming.getAmount() == 7F, "Packed damage reduction changed");
        for (int count = 0; count < 4; count++) PackedPlayerState.escape(player);
        helper.assertTrue(PackedPlayerState.isPacked(player), "Escaped too soon");
        PackedPlayerState.escape(player);
        helper.assertTrue(!PackedPlayerState.isPacked(player) && player.hasEffect(CMPPlayerPack.IMMUNITY), "Five-click escape or immunity failed");
        player.removeEffect(CMPPlayerPack.IMMUNITY);
        helper.succeed();
    }
    @GameTest(template = "empty") public static void entitySnapshot(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        player.getInventory().clearContent();
        var tnt = EntityType.TNT.create(helper.getLevel());
        tnt.setFuse(123); tnt.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());
        tnt.getPersistentData().putInt("fixture", 47); helper.getLevel().addFreshEntity(tnt);
        helper.assertTrue(MobilePackager.packEntity(player, tnt).consumesAction() && tnt.isRemoved(), "TNT not packed");
        var box = player.getInventory().items.stream().filter(com.simibubi.create.content.logistics.box.PackageItem::isPackage).findFirst().orElseThrow();
        var egg = com.simibubi.create.content.logistics.box.PackageItem.getContents(box).getStackInSlot(0);
        var saved = PackedEntityNbt.getEntityTag(egg);
        helper.assertTrue(saved != null && saved.getShort("fuse") == 123 && !saved.contains("Pos"), "Fuse or transient position changed");
        helper.assertTrue(saved.getCompound("NeoForgeData").getInt("fixture") == 47, "Persistent entity data lost");
        var restored = ItemStack.parse(helper.getLevel().registryAccess(), egg.save(helper.getLevel().registryAccess())).orElseThrow();
        helper.assertTrue(ItemStack.isSameItemSameComponents(egg, restored) && ((PackedMobSpawnEgg) restored.getItem()).getType(restored) == EntityType.TNT, "Native entity component round trip failed");
        player.getInventory().clearContent(); helper.succeed();
    }
    @GameTest(template = "empty") public static void cardboardOrigin(GameTestHelper helper) {
        var player = FakePlayerFactory.getMinecraft(helper.getLevel());
        var pos = helper.absolutePos(new BlockPos(3,2,3)); helper.getLevel().setBlockAndUpdate(pos, Blocks.BEDROCK.defaultBlockState());
        var tool = CMPItems.MOBILE_PACKAGER.get().getDefaultInstance();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);player.setShiftKeyDown(false);
        var context = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false));
        helper.assertTrue(tool.getItem().onItemUseFirst(tool, context).consumesAction(), "Expanded bedrock policy failed");
        var tile = helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(tile instanceof MobilePackagerCardboardBox marker && marker.createMobilePackages$isFromMobilePackager(), "Native origin missing");
        var loaded = net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos, tile.getBlockState(), tile.saveWithFullMetadata(helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(loaded instanceof MobilePackagerCardboardBox marker && marker.createMobilePackages$isFromMobilePackager(), "Origin lost on disk round trip");
        helper.assertTrue(((mekanism.common.tile.TileEntityCardboardBox) tile).getRemapEntries().contains(CMPDataComponents.PACKAGER_ORIGIN), "Loot component mapping absent");
        helper.assertTrue(!MobilePackagerBlockContext.isPacking(), "Packing context leaked");
        helper.getLevel().destroyBlock(pos, true);
        var dropped = helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
            new net.minecraft.world.phys.AABB(pos).inflate(2)).stream()
            .filter(entity -> entity.getItem().getItem() instanceof mekanism.common.item.block.ItemBlockCardboardBox).findFirst().orElseThrow();
        var packedBox = dropped.getItem().copy(); dropped.discard();
        helper.assertTrue(Boolean.TRUE.equals(packedBox.get(CMPDataComponents.PACKAGER_ORIGIN)), "Origin lost on actual block drop");
        helper.getLevel().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, packedBox);
        var placement = new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(pos.below().getCenter(), net.minecraft.core.Direction.UP, pos.below(), false));
        helper.assertTrue(packedBox.useOn(placement).consumesAction(), "Packed box placement failed");
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) instanceof MobilePackagerCardboardBox marker && marker.createMobilePackages$isFromMobilePackager(), "Origin lost on actual placement");
        player.setShiftKeyDown(true);
        var hit = new net.minecraft.world.phys.BlockHitResult(pos.getCenter(), net.minecraft.core.Direction.UP, pos, false);
        helper.assertTrue(helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit).consumesAction(), "Native unboxing failed");
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.BEDROCK), "Restored block changed");
        helper.assertTrue(helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(pos).inflate(2)).stream()
            .noneMatch(entity -> entity.getItem().getItem() instanceof mekanism.common.item.block.ItemBlockCardboardBox), "Tool-origin unboxing duplicated an empty box");
        player.setShiftKeyDown(false);helper.succeed();
    }
    @GameTest(template = "empty") public static void trashInventory(GameTestHelper helper) {
        var item = new ItemStack(Items.DIAMOND, 43);item.set(DataComponents.CUSTOM_NAME, Component.literal("unchanged payload"));
        var owner = UUID.randomUUID(); var network = UUID.randomUUID();
        var store = new RoboTrashStore(owner, network, new ArrayList<>(List.of(ItemStack.EMPTY, item)));
        store.setTargetAddress("@owner/retained");var saved = store.save(helper.getLevel().registryAccess());
        var restored = RoboTrashStore.load(saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored.getPlayerUUID().equals(owner) && restored.getNetworkUUID().equals(network) && restored.getTargetAddress().equals(store.getTargetAddress()), "Trash identity/address changed");
        helper.assertTrue(restored.getItemStacks().get(0).isEmpty() && ItemStack.matches(item, restored.getItemStacks().get(1)), "Trash count/components/empty slot changed");
        saved.putString("ItemStacks", "not an inventory");boolean rejected=false;
        try { RoboTrashStore.load(saved, helper.getLevel().registryAccess()); } catch (IllegalArgumentException expected) { rejected=true; }
        helper.assertTrue(rejected, "Malformed trash silently became empty");helper.succeed();
    }
    @GameTest(template = "empty") public static void nestedPackage(GameTestHelper helper) {
        var box = com.simibubi.create.content.logistics.box.PackageItem.containing(List.of(new ItemStack(Items.EMERALD, 17)));
        box.set(DataComponents.CUSTOM_NAME, Component.literal("nested retained"));
        var entity = new net.minecraft.nbt.CompoundTag();entity.putString("id", "create:package");entity.put("Box", box.save(helper.getLevel().registryAccess()));
        var egg = CMPItems.PACKED_MOB_SPAWN_EGG.get().getDefaultInstance();egg.set(DataComponents.ENTITY_DATA, CustomData.of(entity));
        var restored = PackedMobSpawnEgg.recoverLegacyCreatePackage(egg, helper.getLevel().registryAccess());
        helper.assertTrue(ItemStack.matches(box, restored), "Nested package payload changed");helper.succeed();
    }
    @GameTest(template = "empty") public static void droneSave(GameTestHelper helper) {
        var manager = new de.theidler.create_mobile_packages.robo.RoboManager();
        var network = UUID.randomUUID(); var owner = UUID.randomUUID(); var id = UUID.randomUUID();
        var cargo = com.simibubi.create.content.logistics.box.PackageItem.containing(List.of(new ItemStack(Items.GOLD_INGOT, 51)));
        cargo.set(DataComponents.CUSTOM_NAME, Component.literal("retained cargo"));
        com.simibubi.create.content.logistics.box.PackageItem.addAddress(cargo, "retained-route");
        var drone = new de.theidler.create_mobile_packages.robo.VirtualRobo(helper.getLevel(), id, cargo, BlockPos.ZERO, network);
        var pos = new net.minecraft.world.phys.Vec3(41.25,64.75,-26.125);drone.setPos(pos);
        drone.setHomePortPos(new BlockPos(17,63,-19));drone.setReturnToHomeAfterDelivery(true);manager.add(drone);
        manager.setTrashSlots(network, owner, List.of(new ItemStack(Items.DIAMOND, 43)));
        var saved = manager.save(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
        var restored = de.theidler.create_mobile_packages.robo.RoboManager.load(saved, helper.getLevel());
        helper.assertTrue(restored.get(id).getCurrentPos().equals(pos) && ItemStack.matches(cargo, restored.get(id).getItemStack()), "Drone position/cargo changed on disk round trip");
        helper.assertTrue(restored.get(id).getTargetAddress().equals("retained-route"), "Drone route changed");
        var again = restored.save(new net.minecraft.nbt.CompoundTag(), helper.getLevel().registryAccess());
        helper.assertTrue(saved.equals(again), "Saved drone schema/identity/home data changed");
        var first = restored.takeTrashItems(network, owner);
        helper.assertTrue(first != null && first.get(0).getCount()==43 && restored.takeTrashItems(network, owner)==null, "Trash pickup duplicated cargo");
        saved.getList("robos", 10).add(saved.getList("robos", 10).getCompound(0).copy());boolean rejected=false;
        try { de.theidler.create_mobile_packages.robo.RoboManager.load(saved, helper.getLevel()); } catch (IllegalArgumentException expected) { rejected=true; }
        helper.assertTrue(rejected, "Duplicate drone UUID silently overwrote saved cargo");helper.succeed();
    }
    @GameTest(template = "empty") public static void oversizeRejected(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());player.getInventory().clearContent();
        var entity=EntityType.PIG.create(helper.getLevel());entity.getPersistentData().putString("large-fixture", "x".repeat(2*1024*1024));
        entity.setPos(helper.absolutePos(new BlockPos(3,2,3)).getCenter());helper.getLevel().addFreshEntity(entity);
        helper.assertTrue(MobilePackager.packEntity(player, entity)==net.minecraft.world.InteractionResult.FAIL && !entity.isRemoved(), "Oversized entity was deleted or packed");
        helper.assertTrue(player.getInventory().items.stream().allMatch(ItemStack::isEmpty), "Rejected entity created an inventory package");
        entity.discard();helper.succeed();
    }
    @GameTest(template = "empty") public static void protectedBlock(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());player.setShiftKeyDown(false);
        var pos=helper.absolutePos(new BlockPos(3,2,3));helper.getLevel().setBlockAndUpdate(pos, Blocks.STONE.defaultBlockState());
        var tool=CMPItems.MOBILE_PACKAGER.get().getDefaultInstance();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, tool);
        java.util.function.Consumer<net.neoforged.neoforge.event.level.BlockEvent.BreakEvent> denial=event->{if(event.getPos().equals(pos))event.setCanceled(true);};
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;bus.addListener(denial);
        try {
            var context=new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND,
                new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false));
            helper.assertTrue(tool.getItem().onItemUseFirst(tool,context)==net.minecraft.world.InteractionResult.FAIL, "Packing bypassed native protection");
            helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE) && !MobilePackagerBlockContext.isPacking(), "Protected block or context changed");
        } finally {bus.unregister(denial);}
        helper.succeed();
    }
    @GameTest(template = "empty") public static void chestPayload(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());player.setShiftKeyDown(false);
        var pos=helper.absolutePos(new BlockPos(3,2,3));helper.getLevel().setBlockAndUpdate(pos,Blocks.CHEST.defaultBlockState());
        var chest=(net.minecraft.world.level.block.entity.ChestBlockEntity)helper.getLevel().getBlockEntity(pos);
        var content=new ItemStack(Items.DIAMOND,37);content.set(DataComponents.CUSTOM_NAME,Component.literal("nested inventory identity"));
        var custom=new net.minecraft.nbt.CompoundTag();custom.putLong("fixture-counter",9007199254740993L);content.set(DataComponents.CUSTOM_DATA,CustomData.of(custom));
        var namedChest = new ItemStack(Items.CHEST); namedChest.set(DataComponents.CUSTOM_NAME, Component.literal("original chest"));
        chest.applyComponentsFromItemStack(namedChest); chest.setItem(13,content);
        var expectedContent = content.copy();
        var tool=CMPItems.MOBILE_PACKAGER.get().getDefaultInstance();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,tool);
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
        helper.assertTrue(tool.getItem().onItemUseFirst(tool,context).consumesAction(), "Chest packing failed");
        var tile=helper.getLevel().getBlockEntity(pos);var serialized=tile.saveWithFullMetadata(helper.getLevel().registryAccess());
        var reloaded=net.minecraft.world.level.block.entity.BlockEntity.loadStatic(pos,tile.getBlockState(),serialized,helper.getLevel().registryAccess());
        helper.getLevel().setBlockEntity(reloaded);player.setShiftKeyDown(true);
        helper.assertTrue(helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(),player,hit).consumesAction(),"Saved chest unboxing failed");
        var restored=(net.minecraft.world.level.block.entity.ChestBlockEntity)helper.getLevel().getBlockEntity(pos);
        helper.assertTrue(restored.getDisplayName().getString().equals("original chest") && ItemStack.matches(expectedContent,restored.getItem(13)),"Chest name/item components/count changed");
        player.setShiftKeyDown(false);helper.succeed();
    }
    @GameTest(template = "empty") public static void ordinaryBox(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());player.setShiftKeyDown(false);
        var pos=helper.absolutePos(new BlockPos(3,2,3));helper.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
        var box=net.minecraft.core.registries.BuiltInRegistries.ITEM.get(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("mekanism","cardboard_box")).getDefaultInstance();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,box);
        var hit=new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false);
        helper.assertTrue(box.getItem().onItemUseFirst(box,new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,hit)).consumesAction(),"Ordinary box packing failed");
        player.setShiftKeyDown(true);
        helper.getLevel().getBlockState(pos).useWithoutItem(helper.getLevel(),player,hit);
        helper.assertTrue(helper.getLevel().getBlockState(pos).is(Blocks.STONE),"Ordinary unboxing did not restore stone");
        var drops=helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,new net.minecraft.world.phys.AABB(pos).inflate(2));
        helper.assertTrue(drops.stream().filter(entity->entity.getItem().getItem() instanceof mekanism.common.item.block.ItemBlockCardboardBox).mapToInt(entity->entity.getItem().getCount()).sum()==1,"Ordinary box drop changed");
        player.setShiftKeyDown(false);helper.succeed();
    }

    @GameTest(template = "empty") public static void packageUnpackSnapshot(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());player.getInventory().clearContent();
        var pos=helper.absolutePos(new BlockPos(3,2,3));player.setPos(pos.getCenter());
        var entity=EntityType.TNT.create(helper.getLevel());entity.setPos(pos.getCenter());entity.setFuse(123);
        entity.getPersistentData().putInt("fixture",47);var originalId=entity.getUUID();helper.getLevel().addFreshEntity(entity);
        MobilePackager.packEntity(player,entity);
        var box=player.getInventory().items.stream().filter(com.simibubi.create.content.logistics.box.PackageItem::isPackage).findFirst().orElseThrow();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,box);
        ((com.simibubi.create.content.logistics.box.PackageItem)box.getItem()).open(helper.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        var spawned=helper.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.PrimedTnt.class,new net.minecraft.world.phys.AABB(pos).inflate(4));
        helper.assertTrue(spawned.size()==1,"Create did not unpack one captured TNT");var restored=spawned.get(0);
        helper.assertTrue(restored.getFuse()==123 && restored.getPersistentData().getInt("fixture")==47,"Create unpacking changed saved TNT state");
        helper.assertTrue(restored.getUUID().equals(originalId),"Create unpacking changed captured entity identity");
        restored.discard();player.getInventory().clearContent();helper.succeed();
    }

    @GameTest(template = "empty") public static void vehiclePassengers(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());player.getInventory().clearContent();
        var pos=helper.absolutePos(new BlockPos(3,2,3));player.setPos(pos.getCenter());
        var boat=EntityType.BOAT.create(helper.getLevel());boat.setPos(pos.getCenter());boat.setCustomName(Component.literal("retained vehicle"));
        var cow=EntityType.COW.create(helper.getLevel());cow.setAge(-12345);cow.setPos(pos.getCenter());cow.setDeltaMovement(4,2,1);
        cow.getPersistentData().putLong("retained",9007199254740993L);var boatId=boat.getUUID();var cowId=cow.getUUID();
        helper.getLevel().addFreshEntity(boat);helper.getLevel().addFreshEntity(cow);helper.assertTrue(cow.startRiding(boat,true),"Passenger fixture failed");
        helper.assertTrue(MobilePackager.packEntity(player,boat).consumesAction() && boat.isRemoved() && cow.isRemoved(),"Vehicle packing left a saved passenger alive");
        var box=player.getInventory().items.stream().filter(com.simibubi.create.content.logistics.box.PackageItem::isPackage).findFirst().orElseThrow();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,box);
        ((com.simibubi.create.content.logistics.box.PackageItem)box.getItem()).open(helper.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        var restored=helper.getLevel().getEntity(boatId);var passenger=helper.getLevel().getEntity(cowId);
        helper.assertTrue(restored instanceof net.minecraft.world.entity.vehicle.Boat && passenger instanceof net.minecraft.world.entity.animal.Cow && passenger.getVehicle()==restored,"Vehicle/passenger identity or riding relation changed");
        helper.assertTrue(((net.minecraft.world.entity.animal.Cow)passenger).getAge()==-12345 && passenger.getPersistentData().getLong("retained")==9007199254740993L,"Passenger state changed");
        helper.assertTrue(passenger.getDeltaMovement().lengthSqr()==0 && restored.getCustomName().getString().equals("retained vehicle"),"Passenger motion or vehicle name changed");
        passenger.discard();restored.discard();player.getInventory().clearContent();helper.succeed();
    }
    @GameTest(template = "empty") public static void protectedEntitySnapshot(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());player.getInventory().clearContent();
        var pos=helper.absolutePos(new BlockPos(3,3,3));player.setPos(pos.getCenter());
        var falling=net.minecraft.world.entity.item.FallingBlockEntity.fall(helper.getLevel(),pos,Blocks.ANVIL.defaultBlockState());
        falling.time=91;falling.dropItem=false;falling.blockData=new net.minecraft.nbt.CompoundTag();falling.blockData.putString("retained","payload");
        var id=falling.getUUID();helper.assertTrue(falling.onlyOpCanSetNbt(),"Protected entity fixture changed");MobilePackager.packEntity(player,falling);
        var box=player.getInventory().items.stream().filter(com.simibubi.create.content.logistics.box.PackageItem::isPackage).findFirst().orElseThrow();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,box);
        ((com.simibubi.create.content.logistics.box.PackageItem)box.getItem()).open(helper.getLevel(),player,net.minecraft.world.InteractionHand.MAIN_HAND);
        var restored=(net.minecraft.world.entity.item.FallingBlockEntity)helper.getLevel().getEntity(id);
        helper.assertTrue(restored!=null && restored.getBlockState().is(Blocks.ANVIL) && restored.time==91 && !restored.dropItem && restored.blockData.getString("retained").equals("payload"),"Protected entity snapshot was reset");
        restored.discard();player.getInventory().clearContent();helper.succeed();
    }
    @GameTest(template = "empty") public static void duplicateSnapshot(GameTestHelper helper) {
        var player=FakePlayerFactory.getMinecraft(helper.getLevel());var pos=helper.absolutePos(new BlockPos(3,2,3));
        var pig=EntityType.PIG.create(helper.getLevel());pig.setPos(pos.getCenter());helper.getLevel().addFreshEntity(pig);
        var tag=new net.minecraft.nbt.CompoundTag();pig.saveWithoutId(tag);tag.putString("id","minecraft:pig");PackedEntityNbt.sanitizeForPackaging(tag);
        var egg=CMPItems.PACKED_MOB_SPAWN_EGG.get().getDefaultInstance();egg.set(DataComponents.ENTITY_DATA,CustomData.of(tag));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,egg);
        var context=new net.minecraft.world.item.context.UseOnContext(player,net.minecraft.world.InteractionHand.MAIN_HAND,
            new net.minecraft.world.phys.BlockHitResult(pos.getCenter(),net.minecraft.core.Direction.UP,pos,false));
        helper.assertTrue(egg.useOn(context)==net.minecraft.world.InteractionResult.FAIL && egg.getCount()==1 && helper.getLevel().getEntity(pig.getUUID())==pig,"Duplicate UUID changed an entity or consumed the snapshot");
        pig.discard();player.getInventory().clearContent();helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 500) public static void droneDelivery(GameTestHelper helper) {
        var pos=helper.absolutePos(new BlockPos(3,2,3));var end=pos.offset(7,0,0);var level=helper.getLevel();
        level.setBlockAndUpdate(pos,de.theidler.create_mobile_packages.index.CMPBlocks.BEE_PORT.getDefaultState());
        level.setBlockAndUpdate(end,de.theidler.create_mobile_packages.index.CMPBlocks.BEE_PORT.getDefaultState());
        var source=(de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity)level.getBlockEntity(pos);
        var destination=(de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity)level.getBlockEntity(end);
        var network=source.getLogisticsNetworkId();destination.behaviour.freqId=network;destination.addressFilter="retained-delivery";
        var tracker=de.theidler.create_mobile_packages.blocks.bee_port.DronePortTracker.get(level);tracker.add(source);tracker.add(destination);
        var cargo=com.simibubi.create.content.logistics.box.PackageItem.containing(List.of(new ItemStack(Items.DIAMOND,31)));
        com.simibubi.create.content.logistics.box.PackageItem.addAddress(cargo,"retained-delivery");var expected=cargo.copy();
        var manager=de.theidler.create_mobile_packages.robo.RoboManager.get(level);var id=manager.newRobo(level,cargo,pos,network,0,pos,false);
        manager.tick(level);var drone=manager.get(id);
        helper.assertTrue(drone.getTarget()!=null,"New loaded drone never resolved its destination");
        var visible=(de.theidler.create_mobile_packages.entities.RoboBeeEntity)level.getEntity(drone.getEntityId());
        helper.assertTrue(visible!=null && visible.getAttributeValue(Attributes.MAX_HEALTH)==10 && visible.position().equals(drone.getCurrentPos()),"Native bee attributes or position sync failed");
        helper.succeedWhen(()->{
            helper.assertTrue(manager.get(id)==null,"Drone not returned to destination inventory");
            helper.assertTrue(destination.getRoboBeeInventory().getStackInSlot(0).getCount()==1,"Delivered bee count changed");
            var serialized=destination.saveWithFullMetadata(level.registryAccess());
            helper.assertTrue(serialized.toString().contains("minecraft:diamond"),"Delivered cargo missing");
            var inventory=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,end,null);
            helper.assertTrue(inventory!=null && java.util.stream.IntStream.range(0,inventory.getSlots()).mapToObj(inventory::getStackInSlot).anyMatch(stack->ItemStack.matches(stack,expected)),"Delivered cargo components/count changed");
        });
    }

    @GameTest(template = "empty", timeoutTicks = 500) public static void droneReturnHome(GameTestHelper helper) {
        var level=helper.getLevel();var pos=helper.absolutePos(new BlockPos(3,2,3));var end=pos.offset(7,0,0);
        level.setBlockAndUpdate(pos,de.theidler.create_mobile_packages.index.CMPBlocks.BEE_PORT.getDefaultState());
        level.setBlockAndUpdate(end,de.theidler.create_mobile_packages.index.CMPBlocks.BEE_PORT.getDefaultState());
        var source=(de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity)level.getBlockEntity(pos);
        var destination=(de.theidler.create_mobile_packages.blocks.bee_port.BeePortBlockEntity)level.getBlockEntity(end);
        var network=source.getLogisticsNetworkId();destination.behaviour.freqId=network;destination.addressFilter="retained-return";
        var tracker=de.theidler.create_mobile_packages.blocks.bee_port.DronePortTracker.get(level);tracker.add(source);tracker.add(destination);
        var cargo=com.simibubi.create.content.logistics.box.PackageItem.containing(List.of(new ItemStack(Items.GOLD_INGOT,29)));
        com.simibubi.create.content.logistics.box.PackageItem.addAddress(cargo,"retained-return");var expected=cargo.copy();
        var manager=de.theidler.create_mobile_packages.robo.RoboManager.get(level);var id=manager.newRobo(level,cargo,pos,network,0,pos,true);
        helper.succeedWhen(()->{
            helper.assertTrue(manager.get(id)==null && source.getRoboBeeInventory().getStackInSlot(0).getCount()==1,"Return-mode bee never returned home");
            helper.assertTrue(destination.getRoboBeeInventory().getStackInSlot(0).isEmpty(),"Return-mode left a second bee at destination");
            var inventory=level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK,end,null);
            helper.assertTrue(inventory!=null && java.util.stream.IntStream.range(0,inventory.getSlots()).mapToObj(inventory::getStackInSlot).anyMatch(stack->ItemStack.matches(stack,expected)),"Return-mode cargo changed");
        });
    }
    @GameTest(template = "empty") public static void unloadedDroneCargo(GameTestHelper helper) {
        var level=helper.getLevel();var pos=new BlockPos(1000000,75,1000000);
        helper.assertTrue(!level.hasChunk(pos.getX()>>4,pos.getZ()>>4),"Unloaded fixture was loaded");
        var manager=new de.theidler.create_mobile_packages.robo.RoboManager();
        var cargo=com.simibubi.create.content.logistics.box.PackageItem.containing(List.of(new ItemStack(Items.DIAMOND,59)));
        com.simibubi.create.content.logistics.box.PackageItem.addAddress(cargo,"@offline-fixture");var expected=cargo.copy();var id=UUID.randomUUID();
        var drone=new de.theidler.create_mobile_packages.robo.VirtualRobo(level,id,cargo,pos,UUID.randomUUID());manager.add(drone);
        for(int tick=0;tick<100;tick++)manager.tick(level);
        helper.assertTrue(!level.hasChunk(pos.getX()>>4,pos.getZ()>>4) && drone.getEntityId()==null,"Virtual cargo forced an unloaded chunk or spawned an entity");
        var saved=manager.save(new net.minecraft.nbt.CompoundTag(),level.registryAccess());
        var restored=de.theidler.create_mobile_packages.robo.RoboManager.load(saved,level);
        helper.assertTrue(restored.get(id)!=null && ItemStack.matches(expected,restored.get(id).getItemStack()),"Offline/unloaded cargo was deleted");helper.succeed();
    }

    @GameTest(template = "empty") public static void unloadedHomeBee(GameTestHelper helper) {
        var level=helper.getLevel();var pos=new BlockPos(1000064,75,1000064);
        helper.assertTrue(!level.hasChunk(pos.getX()>>4,pos.getZ()>>4),"Home fixture was loaded");
        var manager=new de.theidler.create_mobile_packages.robo.RoboManager();var id=UUID.randomUUID();
        var drone=new de.theidler.create_mobile_packages.robo.VirtualRobo(level,id,ItemStack.EMPTY,pos,UUID.randomUUID());
        drone.setHomePortPos(pos);drone.setReturnToHomeAfterDelivery(true);manager.add(drone);
        for(int tick=0;tick<100;tick++)manager.tick(level);
        helper.assertTrue(!level.hasChunk(pos.getX()>>4,pos.getZ()>>4),"Empty bee loaded its offline home chunk");
        helper.assertTrue(manager.get(id)!=null,"Unloaded home caused an empty bee to be deleted");helper.succeed();
    }

    @GameTest(template = "empty") public static void separatelyPackedDoubleBlocksRetainBothParts(GameTestHelper helper) {
        var level = helper.getLevel();
        var player = FakePlayerFactory.getMinecraft(level);
        var pos = helper.absolutePos(new BlockPos(3, 2, 3));
        int flags = net.minecraft.world.level.block.Block.UPDATE_ALL | net.minecraft.world.level.block.Block.UPDATE_KNOWN_SHAPE;
        for (var toolItem : java.util.List.of(mekanism.common.registries.MekanismBlocks.CARDBOARD_BOX.asItem(), CMPItems.MOBILE_PACKAGER.get())) {
            for (var block : java.util.List.of(Blocks.OAK_DOOR, Blocks.RED_BED, Blocks.SUNFLOWER)) {
                boolean bed = block == Blocks.RED_BED;
                var other = bed ? pos.north() : pos.above();
                var first = bed ? block.defaultBlockState().setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.FOOT)
                    : block.defaultBlockState().setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.LOWER);
                var second = bed ? first.setValue(net.minecraft.world.level.block.BedBlock.PART, net.minecraft.world.level.block.state.properties.BedPart.HEAD)
                    : first.setValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.DOUBLE_BLOCK_HALF, net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER);
                level.setBlock(pos.below(), Blocks.DIRT.defaultBlockState(), flags);
                level.setBlock(other.below(), bed ? Blocks.DIRT.defaultBlockState() : first, flags);
                level.setBlock(pos, first, flags); level.setBlock(other, second, flags);
                for (var target : java.util.List.of(pos, other)) {
                    player.setShiftKeyDown(false); var stack = new ItemStack(toolItem);
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, stack);
                    var hit = new net.minecraft.world.phys.BlockHitResult(target.getCenter(), net.minecraft.core.Direction.UP, target, false);
                    helper.assertTrue(stack.getItem().onItemUseFirst(stack, new net.minecraft.world.item.context.UseOnContext(player, net.minecraft.world.InteractionHand.MAIN_HAND, hit)).consumesAction(), "Part failed to pack: " + block);
                    if (target.equals(pos)) helper.assertTrue(level.getBlockState(other).equals(second), "Packing destroyed the other part: " + block);
                }
                for (var target : java.util.List.of(pos, other)) {
                    player.setShiftKeyDown(true);
                    var hit = new net.minecraft.world.phys.BlockHitResult(target.getCenter(), net.minecraft.core.Direction.UP, target, false);
                    helper.assertTrue(level.getBlockState(target).useWithoutItem(level, player, hit).consumesAction(), "Part failed to restore: " + block);
                }
                helper.assertTrue(level.getBlockState(pos).equals(first) && level.getBlockState(other).equals(second), "Paired states changed: " + block);
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), flags); level.setBlock(other, Blocks.AIR.defaultBlockState(), flags);
            }
        }
        player.setShiftKeyDown(false); player.getInventory().clearContent(); helper.succeed();
    }
}
