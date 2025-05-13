package com.enoughfolders.integrations.ftb;

import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.renderer.Rect2i;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Implementation of SidebarProvider that uses the FTB Library classes directly.
 */
public class FTBSidebarProviderImpl implements SidebarProvider {
    /**
     * The fully qualified name of the FTB sidebar button class
     */
    private static final String FTB_SIDEBAR_BUTTON_CLASS = "dev.ftb.mods.ftblibrary.sidebar.SidebarGroupGuiButton";

    /**
     * Flag to track if we've successfully loaded the FTB Library classes
     */
    private final boolean ftbLoaded;

    /**
     * Constructs a new FTB sidebar provider implementation.
     */
    public FTBSidebarProviderImpl() {
        boolean modListContains = ModList.get().isLoaded("ftblibrary");
        boolean classesAvailable = false;
        
        try {
            // Check if we can load the necessary FTB Library classes
            Class.forName(FTB_SIDEBAR_BUTTON_CLASS);
            classesAvailable = true;
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library classes loaded successfully");
        } catch (ClassNotFoundException e) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library classes not found");
        }
        
        this.ftbLoaded = modListContains && classesAvailable;
        DebugLogger.debugValues(DebugLogger.Category.INTEGRATION, 
            "FTB sidebar provider initialized - ModList contains ftblibrary: {}, classes loaded: {}",
            modListContains, classesAvailable);
    }

    @Override
    public boolean isSidebarAvailable() {
        return ftbLoaded;
    }

    @Override
    public Optional<Rect2i> getSidebarArea() {
        if (!ftbLoaded) {
            return Optional.empty();
        }
        
        try {
            // Use reflection to access the lastDrawnArea field
            Class<?> buttonClass = Class.forName(FTB_SIDEBAR_BUTTON_CLASS);
            java.lang.reflect.Field lastDrawnAreaField = buttonClass.getDeclaredField("lastDrawnArea");
            lastDrawnAreaField.setAccessible(true);
            
            // This field should be static
            Object lastDrawnArea = lastDrawnAreaField.get(null);
            
            if (lastDrawnArea instanceof Rect2i rect) {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                    "Found FTB sidebar area: {}x{} at position {}x{}",
                    rect.getWidth(), rect.getHeight(), rect.getX(), rect.getY());
                return Optional.of(rect);
            }
        } catch (Exception e) {
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Error accessing FTB sidebar area: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
}
