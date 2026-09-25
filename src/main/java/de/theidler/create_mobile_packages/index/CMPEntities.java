package de.theidler.create_mobile_packages.index;

import de.theidler.create_mobile_packages.CreateMobilePackages;
import de.theidler.create_mobile_packages.entities.RoboBeeEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CMPEntities {
    private static final DeferredRegister<EntityType<?>> ENTITY_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.ENTITY_TYPES, CreateMobilePackages.MODID);

    public static final RegistryObject<EntityType<RoboBeeEntity>> ROBO_BEE_ENTITY =
            ENTITY_TYPES.register("robo_bee", () -> EntityType.Builder
                    .of(RoboBeeEntity::createEmpty, MobCategory.CREATURE)
                    .sized(0.6F, 0.6F)
                    .build("robo_bee"));

    private CMPEntities() {
    }

    public static void register(IEventBus modEventBus) {
        ENTITY_TYPES.register(modEventBus);
        modEventBus.addListener(CMPEntities::registerAttributes);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ROBO_BEE_ENTITY.get(), RoboBeeEntity.createAttributes().build());
    }
}
