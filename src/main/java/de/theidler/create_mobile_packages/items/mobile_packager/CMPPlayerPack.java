package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.CreateMobilePackages;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class CMPPlayerPack {
    public static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(Registries.MOB_EFFECT, CreateMobilePackages.MODID);
    public static final DeferredHolder<MobEffect, MobEffect> IMMUNITY = EFFECTS.register("recently_packed", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xD6A66B) {});
    private CMPPlayerPack() {}
}
