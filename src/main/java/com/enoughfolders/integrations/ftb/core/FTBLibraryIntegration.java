package com.enoughfolders.integrations.ftb.core;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.ftb.FTBIntegration;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

/**
 * Integration with FTB Library.
 */
public class FTBLibraryIntegration implements ModIntegration {

    /**
     * Whether the integration is loaded and available
     */
    private final boolean isLoaded;
    
    /**
     * Creates a new FTB Library integration.
     */
    public FTBLibraryIntegration() {
        this.isLoaded = FTBIntegration.isFTBLibraryLoaded();
        
        if (isLoaded) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library integration initialized");
        } else {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library not found, integration disabled");
        }
    }
    
    /**
     * Initialize the integration.
     */
    @Override
    public void initialize() {
        if (isLoaded) {
            // Verify the sidebar is accessible
            FTBIntegration.getSidebarExclusionAreas();
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library integration successfully initialized");
        }
    }

    @Override
    public String getModName() {
        return "FTB Library";
    }

    @Override
    public boolean isAvailable() {
        return isLoaded;
    }
    
    /**
     * Adjusts a UI rectangle to avoid overlapping with FTB sidebar.
     * 
     * @param rectangle The rectangle to adjust
     * @return Adjusted rectangle that doesn't overlap with FTB sidebar
     */
    public Rect2i avoidExclusionAreas(Rect2i rectangle) {
        return FTBIntegration.avoidExclusionAreas(rectangle);
    }

    // These methods are not applicable for FTB integration as we don't handle FTB ingredients
    // but the interface requires them to be implemented

    @Override
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        return Optional.empty();
    }

    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        return Optional.empty();
    }

    @Override
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        return Optional.empty();
    }
}
