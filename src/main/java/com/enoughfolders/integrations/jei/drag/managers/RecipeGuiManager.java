package com.enoughfolders.integrations.jei.drag.managers;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;

import mezz.jei.api.runtime.IRecipesGui;
import net.minecraft.client.gui.screens.Screen;

import java.util.Optional;

/**
 * Manages interaction with folder UI on JEI recipe screens.
 */
public class RecipeGuiManager {
    /**
     * Handle mouse clicks on the folder UI when viewing JEI recipes
     * @param screen The current JEI recipe screen
     * @param mouseX Mouse X position
     * @param mouseY Mouse Y position
     * @param button Mouse button (0=left, 1=right)
     * @return true if the click was handled
     */
    public static boolean handleMouseClick(Screen screen, double mouseX, double mouseY, int button) {
        Optional<FolderScreen> folderScreenOpt = JEIRecipeGuiHandler.getLastFolderScreen();
        
        if (folderScreenOpt.isPresent() && screen instanceof IRecipesGui) {
            FolderScreen folderScreen = folderScreenOpt.get();
            if (folderScreen.isVisible(mouseX, mouseY)) {
                boolean handled = folderScreen.mouseClicked(mouseX, mouseY, button);
                if (handled) {
                    EnoughFolders.LOGGER.debug("Handled mouse click in folder UI on JEI recipe screen");
                }
                return handled;
            }
        }
        return false;
    }
}