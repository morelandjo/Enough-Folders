package com.enoughfolders.integrations.emi.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.api.FolderTarget;
import com.enoughfolders.integrations.api.FolderTargetStub;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.emi.gui.handlers.EMIFolderIngredientHandler;
import com.enoughfolders.integrations.util.StackTraceUtils;
import com.enoughfolders.util.DebugLogger;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Integration for EMI (Enough Modding Interface) mod.
 */
public class EMIIntegration implements ModIntegration, RecipeViewingIntegration {
    
    private boolean initialized = false;
    private boolean available = false;
    
    /**
     * Creates a new EMI integration instance.
     */
    public EMIIntegration() {
        try {
            // Check if EMI classes are available
            Class.forName("dev.emi.emi.api.EmiApi");
            Class.forName("dev.emi.emi.api.stack.EmiStack");
            Class.forName("dev.emi.emi.api.stack.EmiIngredient");
            available = true;
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI integration initialized successfully", ""
            );
        } catch (ClassNotFoundException e) {
            available = false;
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI classes not found, integration disabled: {}", 
                e.getMessage()
            );
        }
    }
    
    @Override
    public String getModName() {
        return "EMI";
    }
    
    @Override
    public boolean isAvailable() {
        return available;
    }
    
    @Override
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        if (!available) {
            return Optional.empty();
        }
        
        try {
            return EMIIngredientManager.getIngredientFromStored(storedIngredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting StoredIngredient to EMI ingredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        if (!available) {
            return Optional.empty();
        }
        
        try {
            return EMIIngredientManager.storeIngredient(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error converting EMI ingredient to StoredIngredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    @Override
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        if (!available) {
            return Optional.empty();
        }
        
        try {
            return EMIIngredientManager.getItemStackForDisplay(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting ItemStack for EMI ingredient: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    /**
     * Gets the ingredient currently under the mouse cursor in EMI.
     * This method is used by keybind handlers to detect which ingredient to add to folders.
     * 
     * @return Optional containing the ingredient under mouse, or empty if none
     */
    public Optional<Object> getIngredientUnderMouse() {
        if (!available || !initialized) {
            return Optional.empty();
        }
        
        try {
            Object hoveredIngredient = EMIRecipeManager.getHoveredIngredient();
            return Optional.ofNullable(hoveredIngredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting ingredient under mouse: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    @Override
    public void initialize() {
        if (!available || initialized) {
            return;
        }
        
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Initializing EMI integration", ""
            );
            
            // Initialize EMI-specific components
            EMIIngredientManager.initialize();
            EMIRecipeManager.initialize();
            
            // Register EMI plugin
            EMIPlugin.register();
            
            initialized = true;
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI integration initialization complete", ""
            );
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to initialize EMI integration", e);
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI integration initialization failed: {}", 
                e.getMessage()
            );
        }
    }
    
    @Override
    public void renderIngredient(net.minecraft.client.gui.GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        if (!available) {
            return;
        }

        try {
            // Try to convert the stored ingredient back to an EMI ingredient
            Optional<?> ingredientOpt = getIngredientFromStored(ingredient);
            if (ingredientOpt.isEmpty()) {
                return;
            }

            // Get the item stack for display
            Optional<ItemStack> itemStackOpt = getItemStackForDisplay(ingredientOpt.get());
            if (itemStackOpt.isPresent()) {
                ItemStack itemStack = itemStackOpt.get();
                
                // Draw the item
                graphics.renderItem(itemStack, x, y);
                
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "Successfully rendered EMI ingredient at " + x + "," + y);
            } else {
                DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                    "Failed to get ItemStack for rendering EMI ingredient");
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to render EMI ingredient", e);
            DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
                "Exception details: {}", e.getMessage());
        }
    }
    
    @Override
    public void registerDragAndDrop() {
        if (!available || !initialized) {
            return;
        }
        
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI drag and drop registration skipped (functionality removed)", ""
            );
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI drag and drop registration failed: {}", 
                e.getMessage()
            );
        }
    }
    
    // Drag and drop functionality has been removed
    // Recipe viewing and integration functionality preserved
    
    @Override
    public String getDisplayName() {
        return "EMI";
    }
    
    // RecipeViewingIntegration implementation
    
    @Override
    public void showRecipes(Object ingredient) {
        if (!available) {
            return;
        }
        
        try {
            EMIRecipeManager.showRecipes(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing EMI recipes: {}", 
                e.getMessage()
            );
        }
    }
    
    @Override
    public void showUses(Object ingredient) {
        if (!available) {
            return;
        }
        
        try {
            EMIRecipeManager.showUses(ingredient);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error showing EMI uses: {}", 
                e.getMessage()
            );
        }
    }
    
    @Override
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        if (!available) {
            return;
        }
        
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Connecting EMI to folder screen", ""
            );
            
            // Create handlers for this folder screen
            new EMIFolderIngredientHandler(folderScreen, containerScreen);
            
            // Store the folder screen for later use
            saveLastFolderScreen(folderScreen);
            
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "EMI folder screen connection complete", ""
            );
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error connecting EMI to folder screen: {}", 
                e.getMessage()
            );
        }
    }
    
    @Override
    public void saveLastFolderScreen(FolderScreen folderScreen) {
        if (!available) {
            return;
        }
        
        try {
            EMIRecipeManager.saveLastFolderScreen(folderScreen);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error saving EMI folder screen: {}", 
                e.getMessage()
            );
        }
    }
    
    @Override
    public void clearLastFolderScreen() {
        if (!available) {
            return;
        }
        
        try {
            EMIRecipeManager.clearLastFolderScreen();
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error clearing EMI folder screen: {}", 
                e.getMessage()
            );
        }
    }
    
    @Override
    public Optional<FolderScreen> getLastFolderScreen() {
        if (!available) {
            return Optional.empty();
        }
        
        try {
            return EMIRecipeManager.getLastFolderScreen();
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error getting EMI folder screen: {}", 
                e.getMessage()
            );
            return Optional.empty();
        }
    }
    
    @Override
    public boolean isRecipeScreen(Screen screen) {
        if (!available) {
            return false;
        }
        
        try {
            return EMIRecipeManager.isRecipeScreen(screen);
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error checking EMI recipe screen: {}", 
                e.getMessage()
            );
            return false;
        }
    }
    
    @Override
    public boolean isTransitioningToRecipeScreen(Screen screen) {
        if (!available) {
            return false;
        }
        
        try {
            return StackTraceUtils.isEMIRecipeTransition();
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error checking EMI recipe transition: {}", 
                e.getMessage()
            );
            return false;
        }
    }
    
    @Override
    public List<? extends FolderTarget> createFolderTargets(List<FolderButton> folderButtons) {
        if (!available) {
            return new ArrayList<>();
        }
        
        try {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Creating EMI folder target stubs for {} buttons (drag functionality disabled)", 
                folderButtons.size()
            );
            
            // Return stub targets that disable drag functionality
            List<FolderTargetStub> targets = new ArrayList<>();
            for (FolderButton button : folderButtons) {
                targets.add(new FolderTargetStub(button.getFolder()));
            }
            return targets;
        } catch (Exception e) {
            DebugLogger.debugValue(
                DebugLogger.Category.INTEGRATION,
                "Error creating EMI folder targets: {}", 
                e.getMessage()
            );
            return new ArrayList<>();
        }
    }
    
    /**
     * Checks if EMI mod is loaded and available.
     * 
     * @return true if EMI is loaded, false otherwise
     */
    public static boolean isEMILoaded() {
        try {
            Class.forName("dev.emi.emi.api.EmiApi");
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }
    
    /**
     * Gets the installed version of EMI.
     * 
     * @return the EMI version string, or "unknown" if EMI is not loaded
     */
    public static String getEMIVersion() {
        if (!isEMILoaded()) {
            return "Not loaded";
        }
            return "Available";
    }
}
