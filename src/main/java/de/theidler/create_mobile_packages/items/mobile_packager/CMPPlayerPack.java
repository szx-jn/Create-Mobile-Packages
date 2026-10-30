package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.CreateMobilePackages;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class CMPPlayerPack {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, CreateMobilePackages.MODID);
    public static final RegistryObject<MobEffect> IMMUNITY = EFFECTS.register("recently_packed", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD6A66B) {});
    private CMPPlayerPack() {}
}
