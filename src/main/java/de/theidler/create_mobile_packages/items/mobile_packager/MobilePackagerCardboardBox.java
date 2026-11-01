package de.theidler.create_mobile_packages.items.mobile_packager;

/** Marker stored directly on Mekanism cardboard-box block entities. */
public interface MobilePackagerCardboardBox {
    boolean createMobilePackages$isFromMobilePackager();

    void createMobilePackages$setFromMobilePackager(boolean value);
}
