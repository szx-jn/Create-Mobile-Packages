package de.theidler.create_mobile_packages.index;

import com.tterrag.registrate.util.entry.ItemEntry;
import de.theidler.create_mobile_packages.CreateMobilePackages;
import de.theidler.create_mobile_packages.items.portable_stock_ticker.PortableStockTicker;
import de.theidler.create_mobile_packages.items.robo_bee.RoboBeeItem;
import de.theidler.create_mobile_packages.items.mobile_packager.MobilePackager;
import de.theidler.create_mobile_packages.items.mobile_packager.PackedMobSpawnEgg;


public class CMPItems {

    public static final ItemEntry<PortableStockTicker> PORTABLE_STOCK_TICKER =
            CreateMobilePackages.REGISTRATE.item("portable_stock_ticker", PortableStockTicker::new)
                    .register();

    public static final ItemEntry<RoboBeeItem> ROBO_BEE =
            CreateMobilePackages.REGISTRATE.item("robo_bee",RoboBeeItem::new)
                    .register();

    public static final ItemEntry<MobilePackager> MOBILE_PACKAGER =
            CreateMobilePackages.REGISTRATE.item("mobile_packager", MobilePackager::new)
                    .register();

    public static final ItemEntry<PackedMobSpawnEgg> PACKED_MOB_SPAWN_EGG =
            CreateMobilePackages.REGISTRATE.item("packed_mob_spawn_egg", PackedMobSpawnEgg::new)
                    .register();

    public static void register() {
    }
}
