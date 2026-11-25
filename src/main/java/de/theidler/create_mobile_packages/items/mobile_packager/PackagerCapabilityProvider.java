package de.theidler.create_mobile_packages.items.mobile_packager;

import de.theidler.create_mobile_packages.items.mobile_packager.ability.PackagerCapability;
import java.util.Set;

/** Implemented by tools that compose a custom set of packager capabilities. */
public interface PackagerCapabilityProvider {
    Set<PackagerCapability> getPackagerCapabilities();
}
