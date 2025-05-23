package com.enoughfolders.integrations.rei.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.rei.core.REIIntegration;
import com.enoughfolders.util.DebugLogger;
import com.enoughfolders.integrations.util.IntegrationUtils;
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.entry.EntryStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Handles drag and drop operations from REI to EnoughFolders using the TransferHandler API.
 */
public class REITransferHandler implements TransferHandler {
    
    @Override
    public double getPriority() {
        // Use a high priority to ensure our handler gets called first
        return 100.0;
    }
    
    @Override
    public ApplicabilityResult checkApplicable(Context context) {
        AbstractContainerScreen<?> containerScreen = context.getContainerScreen();
        if (containerScreen == null) {
            return ApplicabilityResult.createNotApplicable();
        }
        
        // Get mouse coordinates
        double mouseX = Minecraft.getInstance().mouseHandler.xpos() * 
            (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / 
            (double)Minecraft.getInstance().getWindow().getScreenWidth();
        
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() * 
            (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / 
            (double)Minecraft.getInstance().getWindow().getScreenHeight();
        
        // We should handle if the folder screen is visible
        Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(containerScreen);
        if (folderScreenOpt.isEmpty() || !folderScreenOpt.get().isVisible(mouseX, mouseY)) {
            return ApplicabilityResult.createNotApplicable();
        }
        
        return ApplicabilityResult.createApplicable();
    }
    
    @Override
    public Result handle(Context context) {
        DebugLogger.debugValue(DebugLogger.Category.REI_INTEGRATION,
            "REI transfer handler called for display: {}", 
            context.getDisplay().getCategoryIdentifier().toString());
        
        // If not actually crafting (just hovering), don't do anything yet
        if (!context.isActuallyCrafting()) {
            return Result.createNotApplicable();
        }
        
        // Get mouse coordinates
        double mouseX = Minecraft.getInstance().mouseHandler.xpos() * 
            (double)Minecraft.getInstance().getWindow().getGuiScaledWidth() / 
            (double)Minecraft.getInstance().getWindow().getScreenWidth();
        
        double mouseY = Minecraft.getInstance().mouseHandler.ypos() * 
            (double)Minecraft.getInstance().getWindow().getGuiScaledHeight() / 
            (double)Minecraft.getInstance().getWindow().getScreenHeight();
        
        // Get folder screen
        AbstractContainerScreen<?> containerScreen = context.getContainerScreen();
        Optional<FolderScreen> folderScreenOpt = ClientEventHandler.getFolderScreen(containerScreen);
        if (folderScreenOpt.isEmpty() || !folderScreenOpt.get().isVisible(mouseX, mouseY)) {
            return Result.createNotApplicable();
        }
        
        FolderScreen folderScreen = folderScreenOpt.get();
        
        // Get the first input from the display
        if (context.getDisplay().getInputEntries().isEmpty() || 
            context.getDisplay().getInputEntries().get(0).isEmpty()) {
            return Result.createNotApplicable();
        }
        
        EntryIngredient firstInput = context.getDisplay().getInputEntries().get(0);
        EntryStack<?> entryStack = firstInput.get(0);
        
        // Check if drop is on a folder button
        for (FolderButton button : folderScreen.getFolderButtons()) {
            if (IntegrationUtils.isPointInRect(mouseX, mouseY, 
                    button.getX(), 
                    button.getY(), 
                    button.getWidth(), 
                    button.getHeight())) {
                
                // Use the REI integration to convert and add the ingredient
                IntegrationRegistry.getIntegration(REIIntegration.class).ifPresent(integration -> {
                    integration.storeIngredient(entryStack).ifPresent(ingredient -> {
                        EnoughFolders.getInstance().getFolderManager().addIngredient(button.getFolder(), ingredient);
                        folderScreen.onIngredientAdded();
                    });
                });
                
                return Result.createSuccessful()
                    .tooltip(Component.translatable("enoughfolders.message.ingredient_added_to_folder", 
                        button.getFolder().getName()));
            }
        }
        
        // Check if drop is in the content area
        if (IntegrationUtils.isPointInRect(mouseX, mouseY, 
                folderScreen.getContentDropArea().getX(), 
                folderScreen.getContentDropArea().getY(), 
                folderScreen.getContentDropArea().getWidth(), 
                folderScreen.getContentDropArea().getHeight())) {
            
            // Get the active folder
            Optional<Folder> activeFolder = EnoughFolders.getInstance().getFolderManager().getActiveFolder();
            if (activeFolder.isPresent()) {
                IntegrationRegistry.getIntegration(REIIntegration.class).ifPresent(integration -> {
                    integration.storeIngredient(entryStack).ifPresent(ingredient -> {
                        EnoughFolders.getInstance().getFolderManager().addIngredient(activeFolder.get(), ingredient);
                        folderScreen.onIngredientAdded();
                    });
                });
                
                return Result.createSuccessful()
                    .tooltip(Component.translatable("enoughfolders.message.ingredient_added_to_folder", 
                        activeFolder.get().getName()));
            }
        }
        
        return Result.createNotApplicable();
    }
}
