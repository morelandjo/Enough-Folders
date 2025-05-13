package com.enoughfolders.integrations.ftb;

import com.enoughfolders.util.DebugLogger;
import net.neoforged.fml.ModList;

/**
 * Factory for creating a SidebarProvider implementation based on whether FTB Library is available.
 */
public class SidebarProviderFactory {
    /**
     * Private constructor to prevent instantiation.
     */
    private SidebarProviderFactory() {
        // No instantiation
    }
    
    /**
     * Creates a SidebarProvider implementation based on FTB Library availability.
     *
     * @return A SidebarProvider implementation
     */
    public static SidebarProvider create() {
        boolean ftbLibraryAvailable = ModList.get().isLoaded("ftblibrary");
        
        if (ftbLibraryAvailable) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library detected, creating FTB sidebar provider");
            return new FTBSidebarProviderImpl();
        } else {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library not detected, creating dummy sidebar provider");
            return new DummySidebarProvider();
        }
    }
    
    /**
     * Dummy implementation that always returns empty/false values.
     */
    private static class DummySidebarProvider implements SidebarProvider {
        @Override
        public boolean isSidebarAvailable() {
            return false;
        }
        
        @Override
        public java.util.Optional<net.minecraft.client.renderer.Rect2i> getSidebarArea() {
            return java.util.Optional.empty();
        }
    }
}
