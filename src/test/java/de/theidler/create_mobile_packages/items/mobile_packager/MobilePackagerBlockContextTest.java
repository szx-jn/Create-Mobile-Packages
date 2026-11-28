package de.theidler.create_mobile_packages.items.mobile_packager;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MobilePackagerBlockContextTest {

    @Test
    void nestedScopesRestoreTheOuterPolicy() {
        assertFalse(MobilePackagerBlockContext.isPacking());
        try (var outer = MobilePackagerBlockContext.openScope(true, false)) {
            assertTrue(MobilePackagerBlockContext.isPacking());
            assertTrue(MobilePackagerBlockContext.canPackSpecialBlocks());
            assertFalse(MobilePackagerBlockContext.hasModCompatibility());

            try (var inner = MobilePackagerBlockContext.openScope(false, true)) {
                assertTrue(MobilePackagerBlockContext.isPacking());
                assertFalse(MobilePackagerBlockContext.canPackSpecialBlocks());
                assertTrue(MobilePackagerBlockContext.hasModCompatibility());
            }

            assertTrue(MobilePackagerBlockContext.isPacking());
            assertTrue(MobilePackagerBlockContext.canPackSpecialBlocks());
            assertFalse(MobilePackagerBlockContext.hasModCompatibility());
        }

        assertFalse(MobilePackagerBlockContext.isPacking());
        assertFalse(MobilePackagerBlockContext.canPackSpecialBlocks());
        assertFalse(MobilePackagerBlockContext.hasModCompatibility());
    }

    @Test
    void closingScopeAfterFailureRestoresThePreviousState() {
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> {
            try (var ignored = MobilePackagerBlockContext.openScope(true, true)) {
                assertTrue(MobilePackagerBlockContext.isPacking());
                throw new IllegalStateException("fixture");
            }
        });

        assertTrue(failure.getMessage().equals("fixture"));
        assertFalse(MobilePackagerBlockContext.isPacking());
        assertFalse(MobilePackagerBlockContext.canPackSpecialBlocks());
        assertFalse(MobilePackagerBlockContext.hasModCompatibility());
    }

    @Test
    void scopeFlagsAreIndependentAndDefaultToDisabled() {
        try (var ignored = MobilePackagerBlockContext.openScope(false, false)) {
            assertTrue(MobilePackagerBlockContext.isPacking());
            assertFalse(MobilePackagerBlockContext.canPackSpecialBlocks());
            assertFalse(MobilePackagerBlockContext.hasModCompatibility());
        }

        try (var ignored = MobilePackagerBlockContext.openScope(true, true)) {
            assertTrue(MobilePackagerBlockContext.canPackSpecialBlocks());
            assertTrue(MobilePackagerBlockContext.hasModCompatibility());
        }
    }
}
