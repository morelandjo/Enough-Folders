// Import UIConstants class
package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;

/**
 * Button that represents a folder in the GUI.
 */
public class FolderButton extends Button {
    /**
     * Resource location for the folder button textures
     */
    private static final ResourceLocation TEXTURE = UIConstants.FOLDER_TEXTURE;
    
    /**
     * The folder that this button represents
     */
    private final Folder folder;
    
    /**
     * Creates a new folder button.
     *
     * @param x The x position of the button
     * @param y The y position of the button
     * @param width The width of the button
     * @param height The height of the button
     * @param folder The folder that this button represents
     * @param onPress The action to perform when the button is pressed
     */
    public FolderButton(int x, int y, int width, int height, Folder folder, OnPress onPress) {
        super(x, y, width, height, Component.literal(folder.getShortName()), onPress, DEFAULT_NARRATION);
        this.folder = folder;
    }
    
    /**
     * Renders the folder button.
     *
     * @param guiGraphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     */
    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        RenderSystem.setShaderTexture(0, TEXTURE);
        
        int textureU, textureV;
        
        if (folder.isActive()) {
            textureU = isHovered() ? 16 : 0;
            textureV = 48;
        } else {
            textureU = isHovered() ? 16 : 0;
            textureV = 32;
        }
        
        // The texture coordinates are for a 16x16 icon in a 64x64 texture atlas
        guiGraphics.blit(TEXTURE, getX(), getY(), textureU, textureV, 16, 16, 64, 64);
          // Draw folder name
        String shortName = folder.getShortName();
        guiGraphics.drawString(
                Minecraft.getInstance().font, 
                shortName,
                getX() + (width - Minecraft.getInstance().font.width(shortName)) / 2,
                getY() + height,
                0xFFFFFF
        );
        
        // Highlight if an ingredient is being dragged over
        highlightForDrag(guiGraphics, mouseX, mouseY);
    }
    
    /**
     * Highlights the folder button if an ingredient is being dragged over it.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     */
    private void highlightForDrag(GuiGraphics graphics, int mouseX, int mouseY) {
        if (!isPointInButton(mouseX, mouseY)) {
            return;
        }
        
        // Check all drag providers for any dragged ingredients
        for (var provider : IntegrationRegistry.getAllDragProviders()) {
            if (provider.isAvailable() && provider.isIngredientBeingDragged()) {
                EnoughFolders.LOGGER.debug("Folder button '{}' highlighting for {} drag at {},{}", 
                    folder.getName(), provider.getDisplayName(), mouseX, mouseY);
                
                int highlightColor = 0x80FFFFFF;
                graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, highlightColor);
                
                break;
            }
        }
    }
    
    /**
     * Gets the folder that this button represents.
     *
     * @return The folder object
     */
    public Folder getFolder() {
        return folder;
    }
    
    /**
     * Checks if a point is within the button's bounds.
     *
     * @param mouseX The X coordinate to check
     * @param mouseY The Y coordinate to check
     * @return True if the point is within the button
     */
    public boolean isPointInButton(int mouseX, int mouseY) {
        return mouseX >= getX() && mouseX < getX() + width && 
               mouseY >= getY() && mouseY < getY() + height;
    }
    
    /**
     * Checks if the button is currently being hovered over.
     * 
     * @return True if the mouse is hovering over this button
     */
    @Override
    public boolean isHovered() {
        return super.isHovered();
    }
    
    /**
     * Triggers the button's click action.
     */
    public void onClick() {
        onPress.onPress(this);
    }
    
    /**
     * Tries to handle an ingredient drop on this folder button.
     *
     * @param mouseX The mouse X coordinate
     * @param mouseY The mouse Y coordinate
     * @return True if a drop was handled, false otherwise
     */
    public boolean tryHandleDrop(int mouseX, int mouseY) {
        if (!isPointInButton(mouseX, mouseY)) {
            return false;
        }
        
        EnoughFolders.LOGGER.debug("Attempting drop on folder button: {}", folder.getName());
        
        // Check for any available integration with drag and drop capability
        for (var integration : IntegrationRegistry.getAllDragProviders()) {
            if (integration.isAvailable() && integration.isIngredientBeingDragged()) {
                EnoughFolders.LOGGER.info("Handling drop from {} on folder button: {}", 
                    integration.getDisplayName(), folder.getName());
                
                // We can still use the folder-based method for the integration API
                // since we already know which specific folder to target
                boolean success = integration.handleIngredientDrop(folder);
                if (success) {
                    EnoughFolders.LOGGER.info("Successfully added ingredient from {} to folder: {}", 
                        integration.getDisplayName(), folder.getName());
                    return true;
                }
            }
        }
        
        return false;
    }
}
