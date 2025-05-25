package com.enoughfolders.integrations.jei.drag;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import java.util.List;
import com.enoughfolders.di.DependencyProvider;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget;
import com.enoughfolders.util.DebugLogger;
import com.enoughfolders.integrations.util.IntegrationUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Optional;

/**
 * Global manager for handling JEI drag and drop operations.
 */
@EventBusSubscriber(modid = EnoughFolders.MOD_ID, value = Dist.CLIENT)
/**
 * Manages drag and drop operations for JEI ingredients.
 */
public class JEIDragDropManager {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private JEIDragDropManager() {
        // Utility class should not be instantiated
    }
    
    // Keep track of the currently dragged ingredient
    private static Object currentDraggedIngredient = null;
    private static boolean isDragging = false;
    
    // Called when a mouse button is pressed (start of potential drag)
    @SubscribeEvent
    /**
     * Handles mouse press events for JEI drag and drop operations.
     * 
     * @param event The mouse button pressed event
     */
    public static void onMousePressed(ScreenEvent.MouseButtonPressed.Pre event) {
        currentDraggedIngredient = null;
        isDragging = false;
        
        // Get mouse coordinates
        double mouseX = event.getMouseX();
        double mouseY = event.getMouseY();
        
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
            "Mouse pressed at {},{} - checking for JEI draggable ingredient", mouseX, mouseY);
        
        // Store the current dragged ingredient from JEI
        Optional<JEIIntegration> jeiIntegration = DependencyProvider.get(JEIIntegration.class);
        jeiIntegration.ifPresent(integration -> {
            Optional<Object> draggedIngredient = integration.getDraggedIngredient();
            if (draggedIngredient.isPresent()) {
                Object ingredient = draggedIngredient.get();
                currentDraggedIngredient = ingredient;
                isDragging = true;
                DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                    "JEI DRAG START: Ingredient type: {}", ingredient.getClass().getSimpleName());
                
                // Log active UI information
                Screen currentScreen = Minecraft.getInstance().screen;
                if (currentScreen != null) {
                    DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                        "Current screen: {}", currentScreen.getClass().getSimpleName());
                }
                
                // Check if we're hovering over a folder button
                checkFolderButtonsUnderCursor(mouseX, mouseY);
            } else {
                DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "No JEI ingredient found to drag");
            }
        });
    }
    
    // Helper method to log if cursor is over any folder button
    private static void checkFolderButtonsUnderCursor(double mouseX, double mouseY) {
        // Check in recipe gui
        JEIRecipeGuiHandler.getLastFolderScreen().ifPresent(folderScreen -> {
            if (folderScreen.isVisible(mouseX, mouseY)) {
                List<FolderButtonTarget> jeiTargets = folderScreen.getJEIFolderTargets();
                for (FolderButtonTarget target : jeiTargets) {
                    if (IntegrationUtils.isPointInRect(mouseX, mouseY, 
                            target.getArea().getX(), 
                            target.getArea().getY(),
                            target.getArea().getWidth(),
                            target.getArea().getHeight())) {
                        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                            "Mouse is over folder button: {} at drag start", target.getFolder().getName());
                    }
                }
            }
        });
        
        // Check in inventory
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen) {
            ClientEventHandler.getFolderScreen(containerScreen)
                .ifPresent(folderScreen -> {
                    if (folderScreen.isVisible(mouseX, mouseY)) {
                        List<FolderButtonTarget> jeiTargets = folderScreen.getJEIFolderTargets();
                        for (FolderButtonTarget target : jeiTargets) {
                            if (IntegrationUtils.isPointInRect(mouseX, mouseY, 
                                    target.getArea().getX(), 
                                    target.getArea().getY(),
                                    target.getArea().getWidth(),
                                    target.getArea().getHeight())) {
                                DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, 
                                    "Mouse is over inventory folder button: {} at drag start", 
                                    target.getFolder().getName());
                            }
                        }
                    }
                });
        }
    }
    
    // Called when the mouse is released (end of potential drag)
    @SubscribeEvent
    /**
     * Handles mouse release events for JEI drag and drop operations.
     * 
     * @param event The mouse button released event
     */
    /**
     * Handles mouse release events for JEI drag and drop operations.
     * 
     * @param event The mouse button released event
     */
    public static void onMouseReleased(ScreenEvent.MouseButtonReleased.Pre event) {
        if (isDragging && currentDraggedIngredient != null) {
            // Get the coordinates
            double mouseX = event.getMouseX();
            double mouseY = event.getMouseY();
            
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                "JEIDragDropManager detected drag end at {},{} with ingredient: {}", 
                mouseX, mouseY, currentDraggedIngredient.getClass().getSimpleName());
            
            // Check if we're in a recipe screen
            Screen currentScreen = Minecraft.getInstance().screen;
            boolean isRecipeScreen = false;
            
            try {
                isRecipeScreen = currentScreen != null && 
                    Class.forName("mezz.jei.api.runtime.IRecipesGui").isAssignableFrom(currentScreen.getClass());
            } catch (ClassNotFoundException e) {
                // Ignore - JEI API not available
            }
            
            // If we're in a recipe screen, check if mouse is over folder UI
            if (isRecipeScreen) {
                handleRecipeScreenDrop(event, mouseX, mouseY);
            } else {
                // If we're not in a recipe screen, check if we're in any inventory screen with a folder overlay
                handleInventoryScreenDrop(event, mouseX, mouseY, currentScreen);
            }
        }
        
        // Reset drag state
        isDragging = false;
        currentDraggedIngredient = null;
    }

    // Handle drop in a recipe screen context
    private static void handleRecipeScreenDrop(ScreenEvent.MouseButtonReleased.Pre event, double mouseX, double mouseY) {
        JEIRecipeGuiHandler.getLastFolderScreen().ifPresent(folderScreen -> {
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION, 
                "Checking drop on folder screen at {},{}", mouseX, mouseY);
            
            if (!folderScreen.isVisible(mouseX, mouseY)) {
                return;
            }
            
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Drop detected inside folder UI area");
            
            // Check for drops on folder buttons first
            boolean handled = handleDropOnFolderButtons(event, mouseX, mouseY, folderScreen);
            
            // If not handled by button drops, check content area drops
            if (!handled) {
                handled = handleContentAreaDrop(event, mouseX, mouseY, folderScreen);
            }
        });
    }

    // Handle drop in an inventory screen context
    private static void handleInventoryScreenDrop(ScreenEvent.MouseButtonReleased.Pre event, 
                                                double mouseX, double mouseY, Screen currentScreen) {
        if (!(currentScreen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> containerScreen)) {
            return;
        }
        
        ClientEventHandler.getFolderScreen(containerScreen).ifPresent(folderScreen -> {
            DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
                "Checking drop on inventory folder screen at {},{}", mouseX, mouseY);
            
            if (!folderScreen.isVisible(mouseX, mouseY)) {
                return;
            }
            
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Drop detected inside inventory folder UI area");
            
            // Check for drops on folder buttons first
            boolean handled = handleDropOnFolderButtons(event, mouseX, mouseY, folderScreen);
            
            // If not handled by button drops, check content area drops
            if (!handled) {
                handled = handleContentAreaDrop(event, mouseX, mouseY, folderScreen);
            }
        });
    }

    // Handle drops on folder buttons
    private static boolean handleDropOnFolderButtons(ScreenEvent.MouseButtonReleased.Pre event, 
                                               double mouseX, double mouseY, 
                                               FolderScreen folderScreen) {
        for (FolderButton button : folderScreen.getFolderButtons()) {
            if (button.tryHandleDrop((int)mouseX, (int)mouseY)) {
                folderScreen.onIngredientAdded();
                event.setCanceled(true);
                return true;
            }
        }
        return false;
    }
    
    // Handle drops on the content area
    private static boolean handleContentAreaDrop(ScreenEvent.MouseButtonReleased.Pre event,
                                          double mouseX, double mouseY,
                                          FolderScreen folderScreen) {
        // Check if drop is in content area
        if (IntegrationUtils.isPointInRect(mouseX, mouseY, 
                folderScreen.getContentDropArea().getX(), 
                folderScreen.getContentDropArea().getY(), 
                folderScreen.getContentDropArea().getWidth(), 
                folderScreen.getContentDropArea().getHeight())) {
            
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Drop detected in folder content area");
            
            // Get the active folder
            EnoughFolders.getInstance().getFolderManager().getActiveFolder().ifPresent(folder -> {
                // Get JEI integration
                DependencyProvider.get(JEIIntegration.class).ifPresent(integration -> {
                    // Convert JEI ingredient to stored format
                    integration.storeIngredient(currentDraggedIngredient).ifPresent(ingredient -> {
                        EnoughFolders.getInstance().getFolderManager().addIngredient(folder, ingredient);
                        folderScreen.onIngredientAdded();
                        event.setCanceled(true);
                    });
                });
            });
            return true;
        }
        return false;
    }
}