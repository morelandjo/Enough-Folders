package com.enoughfolders.integrations.ftb.core;

import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.base.AbstractIntegration;
import com.enoughfolders.integrations.ftb.FTBIntegration;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.util.Optional;

/**
 * Integration with FTB Library.
 */
public class FTBLibraryIntegration extends AbstractIntegration {

    /**
     * FTB Library mod identifier
     */
    private static final String MOD_ID = "ftblibrary";
    
    /**
     * FTB integration instance
     */
    private final FTBIntegration ftbIntegration;
    
    /**
     * Creates a new FTB Library integration.
     */
    public FTBLibraryIntegration() {
        super(MOD_ID, "FTB Library");
        this.ftbIntegration = new FTBIntegration();
        DebugLogger.debug(DebugLogger.Category.INTEGRATION, "FTB Library integration initialized");
    }
    
    /**
     * Checks if the required FTB Library classes are available.
     *
     * @return true if FTB Library classes are available, false otherwise
     */
    @Override
    protected boolean checkClassAvailability() {
        try {
            // Check if FTB Library mod is loaded
            boolean modLoaded = ModList.get().isLoaded(MOD_ID);
            if (!modLoaded) {
                return false;
            }
            
            // Additional check using the FTB integration
            return ftbIntegration.isFTBLibraryLoaded();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Performs FTB Library-specific initialization.
     */
    @Override
    protected void doInitialize() {
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Initializing FTB Library integration", ""
            );
            
            // Verify the sidebar is accessible
            ftbIntegration.getSidebarExclusionAreas();
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "FTB Library integration initialization complete", ""
            );
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "FTB Library integration initialization failed: {}", 
                e.getMessage()
            );
        }
    }

    /**
     * Gets the mod's display name.
     * 
     * @return The display name of the mod
     */
    @Override
    public String getModName() {
        return "FTB Library";
    }
    
    /**
     * Adjusts a UI rectangle to avoid overlapping with FTB sidebar.
     * 
     * @param rectangle The rectangle to adjust
     * @return Adjusted rectangle that doesn't overlap with FTB sidebar
     */
    public Rect2i avoidExclusionAreas(Rect2i rectangle) {
        return ftbIntegration.avoidExclusionAreas(rectangle);
    }

    // These methods are not applicable for FTB integration as FTB Library doesn't handle ingredients
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
