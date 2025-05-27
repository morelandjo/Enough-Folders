package com.enoughfolders.integrations.rei.handlers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.event.ClientEventHandler;
import net.minecraft.client.gui.components.Button;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.di.DependencyProvider;
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
 * Handles transfer operations between REI and folder screens.
 */
public class REITransferHandler implements TransferHandler {
    
    /**
     * Creates a new REI transfer handler.
     */
    public REITransferHandler() {
        // Default constructor
    }
    
    @Override
    public double getPriority() {
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
        for (Button button : folderScreen.getFolderButtons()) {
            if (IntegrationUtils.isPointInRect(mouseX, mouseY, 
                    button.getX(), 
                    button.getY(), 
                    button.getWidth(), 
                    button.getHeight())) {
                
                // Get the folder for this button
                var folder = folderScreen.getFolderForButton(button);
                if (folder == null) continue;
                
                // Use the REI integration to convert and add the ingredient
                DependencyProvider.get(REIIntegration.class).ifPresent(integration -> {
                    integration.storeIngredient(entryStack).ifPresent(ingredient -> {
                        EnoughFolders.getInstance().getFolderManager().addIngredient(folder, ingredient);
                        folderScreen.onIngredientAdded();
                    });
                });
                
                return Result.createSuccessful()
                    .tooltip(Component.translatable("enoughfolders.message.ingredient_added_to_folder", 
                        folder.getName()));
            }
        }
        
        return Result.createNotApplicable();
    }
}
