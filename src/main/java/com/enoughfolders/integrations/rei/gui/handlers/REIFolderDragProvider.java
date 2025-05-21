package com.enoughfolders.integrations.rei.gui.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.client.gui.IngredientSlot;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.integrations.rei.gui.targets.REIFolderTarget;
import com.enoughfolders.util.DebugLogger;

import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.gui.drag.DraggableStack;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackProvider;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor;
import me.shedaniel.rei.api.client.gui.drag.DraggableStackVisitor.BoundsProvider;
import me.shedaniel.rei.api.client.gui.drag.DraggedAcceptorResult;
import me.shedaniel.rei.api.client.gui.drag.DraggingContext;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.screen.ScreenRegistry;
import me.shedaniel.rei.api.common.entry.EntryStack;
import me.shedaniel.rei.forge.REIPluginClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Implements both drag-from-folder and drag-to-folder functionality for REI integration.
 * This unified provider handles all REI drag-and-drop operations for EnoughFolders.
 */
@OnlyIn(Dist.CLIENT)
@REIPluginClient
public class REIFolderDragProvider implements REIClientPlugin {

    private final DragProviderImpl dragProvider = new DragProviderImpl();
    private final DragVisitorImpl dragVisitor = new DragVisitorImpl();
    
    @Override
    public void registerScreens(ScreenRegistry registry) {
        try {
            // Enable REI integration debug logging in console
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Registering REI folder drag-and-drop handlers");
            
            // Register our handlers for both drag providers and visitors
            registry.registerDraggableStackProvider(dragProvider);
            registry.registerDraggableStackVisitor(dragVisitor);
            
            // Log success using debug logger
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, 
                "Successfully registered REI folder drag-and-drop handler");
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to register REI folder drag handler: {}", e.getMessage());
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Failed to register REI folder drag handler: {}", e.getMessage());
        }
    }
    
    /**
     * Implementation of DraggableStackProvider - allows dragging items from folders to REI
     */
    private static class DragProviderImpl implements DraggableStackProvider<Screen> {
        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                return false;
            }
            
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
            // Get the folder screen associated with this container
            Optional<FolderScreen> folderScreenOpt = com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen);
            boolean result = folderScreenOpt.isPresent();
            
            // Log more detailed info for debugging
            if (result) {
                DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION,
                    "DragProviderImpl handling screen: {}", screen.getClass().getSimpleName());
            }
            
            return result;
        }
        
        @Override
        public DraggableStack getHoveredStack(DraggingContext<Screen> context, double mouseX, double mouseY) {
            if (!(context.getScreen() instanceof AbstractContainerScreen<?>)) {
                return null;
            }
            
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) context.getScreen();
            
            // Get the folder screen associated with this container
            Optional<FolderScreen> folderScreenOpt = com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(screen);
            if (folderScreenOpt.isEmpty()) {
                return null;
            }
            
            FolderScreen folderScreen = folderScreenOpt.get();
            
            // Only proceed if the folder screen is visible
            if (!folderScreen.isVisible(mouseX, mouseY)) {
                return null;
            }
            
            // Get REI integration
            Optional<REIIntegration> reiIntegration = IntegrationRegistry.getIntegration(REIIntegration.class);
            if (reiIntegration.isEmpty() || !reiIntegration.get().isAvailable()) {
                return null;
            }
            
            // Check if mouse is over an ingredient slot
            for (IngredientSlot slot : folderScreen.getIngredientSlots()) {
                if (mouseX >= slot.getX() && mouseX < slot.getX() + slot.getWidth() &&
                    mouseY >= slot.getY() && mouseY < slot.getY() + slot.getHeight() && 
                    slot.hasIngredient()) {
                    
                    DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                        "Mouse over ingredient slot at {},{}", mouseX, mouseY);
                    
                    var storedIngredient = slot.getStoredIngredient();
                    if (storedIngredient != null) {
                        // Convert to REI EntryStack
                        Optional<?> reiIngredient = reiIntegration.get().getIngredientFromStored(storedIngredient);
                        if (reiIngredient.isPresent() && reiIngredient.get() instanceof EntryStack<?> entryStack) {
                            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION,
                                "Creating draggable stack for folder ingredient");
                                
                            return new DraggableStack() {
                                public EntryStack<?> getStack() {
                                    return entryStack;
                                }
                                
                                public Rectangle getBounds() {
                                    return new Rectangle(slot.getX(), slot.getY(), slot.getWidth(), slot.getHeight());
                                }
                                
                                public void drag() {
                                    // Handle the start of dragging if needed
                                    DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION,
                                        "Started dragging item from folder");
                                }
                            };
                        }
                    }
                }
            }
            
            return null;
        }
    }
    
    /**
     * Implementation of DraggableStackVisitor - allows dropping items from REI into folders
     */
    private static class DragVisitorImpl implements DraggableStackVisitor<Screen> {
        @Override
        public <R extends Screen> boolean isHandingScreen(R screen) {
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                return false;
            }
            
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
            // Get the folder screen associated with this container
            Optional<FolderScreen> folderScreenOpt = com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen);
            boolean result = folderScreenOpt.isPresent();
            
            // Log more detailed info for debugging
            if (result) {
                DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION,
                    "DragVisitorImpl handling screen: {}", screen.getClass().getSimpleName());
            }
            
            return result;
        }
        
        @Override
        public DraggedAcceptorResult acceptDraggedStack(DraggingContext<Screen> context, DraggableStack stack) {
            Screen screen = context.getScreen();
            if (!(screen instanceof AbstractContainerScreen<?>)) {
                return DraggedAcceptorResult.PASS;
            }
            
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) screen;
            
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                    "REI draggable stack visitor called for screen: {}, stack: {}",
                    screen.getClass().getSimpleName(),
                    stack.getStack().getType());
            
            // Get the folder screen associated with this container
            Optional<FolderScreen> folderScreenOpt = com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen);
            if (folderScreenOpt.isEmpty()) {
                return DraggedAcceptorResult.PASS;
            }
            
            FolderScreen folderScreen = folderScreenOpt.get();
            
            // Get current mouse position
            Point mousePos = context.getCurrentPosition();
            if (mousePos == null) {
                // If no position is available, fall back to Minecraft's mouse handler
                double mouseX = Minecraft.getInstance().mouseHandler.xpos();
                double mouseY = Minecraft.getInstance().mouseHandler.ypos();
                
                // Only proceed if the folder screen is visible
                if (!folderScreen.isVisible(mouseX, mouseY)) {
                    return DraggedAcceptorResult.PASS;
                }
            }
            
            // Check if dragging over content area
            var contentArea = folderScreen.getContentDropArea();
            Rectangle contentRectangle = new Rectangle(contentArea.getX(), contentArea.getY(), 
                                                    contentArea.getWidth(), contentArea.getHeight());
            
            // Get the entire folder UI area
            var entireFolderArea = folderScreen.getEntireFolderArea();
            Rectangle entireFolderRectangle = new Rectangle(entireFolderArea.getX(), entireFolderArea.getY(),
                                                    entireFolderArea.getWidth(), entireFolderArea.getHeight());
            
            // Get the stack center position from the context or fallback to the mouse position
            double stackCenterX, stackCenterY;
            Rectangle currentBounds = context.getCurrentBounds();
            if (currentBounds != null) {
                stackCenterX = currentBounds.getCenterX();
                stackCenterY = currentBounds.getCenterY();
            } else if (mousePos != null) {
                stackCenterX = mousePos.getX();
                stackCenterY = mousePos.getY();
            } else {
                stackCenterX = Minecraft.getInstance().mouseHandler.xpos();
                stackCenterY = Minecraft.getInstance().mouseHandler.ypos();
            }
            
            // Log both the content area bounds and the mouse position for debugging
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                "Content area bounds: x={}, y={}, width={}, height={}", 
                contentArea.getX(), contentArea.getY(), contentArea.getWidth(), contentArea.getHeight());
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                "Entire folder area bounds: x={}, y={}, width={}, height={}", 
                entireFolderArea.getX(), entireFolderArea.getY(), entireFolderArea.getWidth(), entireFolderArea.getHeight());
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                "Stack center position: x={}, y={}", stackCenterX, stackCenterY);
            
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                "Stack center position: {},{}", 
                stackCenterX, stackCenterY);
            
            boolean inContentArea = contentRectangle.contains(stackCenterX, stackCenterY);
            boolean inEntireFolderArea = entireFolderRectangle.contains(stackCenterX, stackCenterY);
            
            DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                "Is in content area: {}, Is in entire folder area: {}", inContentArea, inEntireFolderArea);
            
            // First check if dragging over specific folder targets
            for (REIFolderTarget target : folderScreen.getREIFolderTargets()) {
                boolean inTargetArea = target.isPointInTarget(stackCenterX, stackCenterY);
                
                if (inTargetArea) {
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                            "Draggable stack over folder target: {}", target.getFolder().getName());
                    
                    if (stack.getStack().isEmpty()) {
                        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Stack is empty, passing");
                        return DraggedAcceptorResult.PASS;
                    }
                    
                    // Handle drop on folder target
                    processDroppedStack(stack.getStack(), target.getFolder(), folderScreen);
                    return DraggedAcceptorResult.CONSUMED;
                }
            }
            
            // Then check if over content area
            if (inContentArea) {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Draggable stack over content area");
                
                if (stack.getStack().isEmpty()) {
                    DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Stack is empty, passing");
                    return DraggedAcceptorResult.PASS;
                }
                
                // Handle drop on content area
                EnoughFolders.getInstance().getFolderManager().getActiveFolder().ifPresent(folder -> {
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                        "Processing dropped stack for active folder: {}", folder.getName());
                    // Convert REI ingredient to stored format
                    processDroppedStack(stack.getStack(), folder, folderScreen);
                });
                
                return DraggedAcceptorResult.CONSUMED;
            }
            
            // Finally check if over the entire folder area and there's an active folder
            if (inEntireFolderArea) {
                Optional<Folder> activeFolder = EnoughFolders.getInstance().getFolderManager().getActiveFolder();
                if (activeFolder.isPresent()) {
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION,
                        "Draggable stack over entire folder area with active folder: {}", 
                        activeFolder.get().getName());
                    
                    if (stack.getStack().isEmpty()) {
                        DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Stack is empty, passing");
                        return DraggedAcceptorResult.PASS;
                    }
                    
                    // Handle drop on entire folder area
                    DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                        "Processing dropped stack for active folder: {}", activeFolder.get().getName());
                    processDroppedStack(stack.getStack(), activeFolder.get(), folderScreen);
                    return DraggedAcceptorResult.CONSUMED;
                }
            }
            
            return DraggedAcceptorResult.PASS;
        }
        
        @Override
        public Stream<BoundsProvider> getDraggableAcceptingBounds(DraggingContext<Screen> context, DraggableStack stack) {
            if (!(context.getScreen() instanceof AbstractContainerScreen<?>)) {
                return Stream.empty();
            }
            
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) context.getScreen();
            
            // Get the folder screen associated with this container
            Optional<FolderScreen> folderScreenOpt = com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(screen);
            if (folderScreenOpt.isEmpty()) {
                return Stream.empty();
            }
            
            FolderScreen folderScreen = folderScreenOpt.get();
            
            // Only proceed if the folder screen is visible at current mouse position
            Point mousePos = context.getCurrentPosition();
            double mouseX, mouseY;
            if (mousePos != null) {
                mouseX = mousePos.getX();
                mouseY = mousePos.getY();
            } else {
                mouseX = Minecraft.getInstance().mouseHandler.xpos();
                mouseY = Minecraft.getInstance().mouseHandler.ypos();
            }
            
            if (!folderScreen.isVisible(mouseX, mouseY)) {
                return Stream.empty();
            }
            
            DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "Getting drag accepting bounds");
            
            List<Rectangle> bounds = new ArrayList<>();
            
            // Get active folder if any
            Optional<Folder> activeFolder = EnoughFolders.getInstance().getFolderManager().getActiveFolder();
            
            // Add the entire folder area if there's an active folder
            if (activeFolder.isPresent()) {
                var entireFolderArea = folderScreen.getEntireFolderArea();
                Rectangle entireRect = new Rectangle(entireFolderArea.getX(), entireFolderArea.getY(), 
                                                entireFolderArea.getWidth(), entireFolderArea.getHeight());
                bounds.add(entireRect);
                
                DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                    "Added entire folder area bounds: {}x{} at {},{}", 
                    entireRect.width, entireRect.height, entireRect.x, entireRect.y);
                
                // Also add content area for more precise highlighting
                var contentArea = folderScreen.getContentDropArea();
                Rectangle contentRect = new Rectangle(contentArea.getX(), contentArea.getY(), 
                                                contentArea.getWidth(), contentArea.getHeight());
                bounds.add(contentRect);
                
                DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                    "Added content area bounds: {}x{} at {},{}", 
                    contentRect.width, contentRect.height, contentRect.x, contentRect.y);
            }
            
            // Add all folder target bounds
            for (REIFolderTarget target : folderScreen.getREIFolderTargets()) {
                Rectangle rect = new Rectangle(target.getX(), target.getY(), 
                                            target.getWidth(), target.getHeight());
                bounds.add(rect);
                
                DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                    "Added folder target bounds for {}: {}x{} at {},{}", 
                    target.getFolder().getName(), rect.width, rect.height, rect.x, rect.y);
            }
            
            if (bounds.isEmpty()) {
                DebugLogger.debug(DebugLogger.Category.REI_INTEGRATION, "No accepting bounds found");
                return Stream.empty();
            }
            
            DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION, 
                "Returning {} drag accepting bounds", bounds.size());
            
            return Stream.of(BoundsProvider.ofRectangles(bounds));
        }
    }
    
    /**
     * Helper method to process a dropped stack and add it to a folder
     */
    private static void processDroppedStack(EntryStack<?> entryStack, Folder folder, FolderScreen folderScreen) {
        // Get REI integration
        DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
            "Processing dropped REI stack of type '{}' for folder: '{}'", 
            entryStack.getType(), folder.getName());
            
        IntegrationRegistry.getIntegration(REIIntegration.class).ifPresent(integration -> {
            // Convert REI ingredient to stored format
            integration.storeIngredient(entryStack).ifPresent(ingredient -> {
                EnoughFolders.getInstance().getFolderManager().addIngredient(folder, ingredient);
                folderScreen.onIngredientAdded();
                
                DebugLogger.debugValues(DebugLogger.Category.REI_INTEGRATION,
                    "Added ingredient to folder: {}, type: {}", 
                    folder.getName(), entryStack.getType());
            });
        });
    }
}
