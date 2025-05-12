package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.IntegrationRegistry;
import com.enoughfolders.integrations.jei.core.JEIIntegration;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import javax.annotation.Nonnull;
import java.util.Optional;

/**
 * Button that represents a folder in the GUI.
 */
public class FolderButton extends Button {
    /**
     * Resource location for the folder button textures
     */
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(EnoughFolders.MOD_ID, "textures/gui/folders.png");
    
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
        
        // Updated sprite coordinates to match the original implementation
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
                getY() + height + 2,
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
        
        IntegrationRegistry.getIntegration(JEIIntegration.class).ifPresent(jei -> {
            JEIIntegration jeiInt = (JEIIntegration) jei;
            jeiInt.getDraggedIngredient().ifPresent(ingredient -> {
                EnoughFolders.LOGGER.debug("Folder button '{}' highlighting for drag at {},{}", 
                    folder.getName(), mouseX, mouseY);
                
                int highlightColor = 0x80FFFFFF;
                graphics.fill(getX() - 1, getY() - 1, getX() + width + 1, getY() + height + 1, highlightColor);
            });
        });
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
     * Checks if the button is currently being hovered over by the mouse.
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
     * Tries to handle a JEI ingredient drop on this folder button.
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
        
        Optional<JEIIntegration> jeiIntegration = IntegrationRegistry.getIntegration(JEIIntegration.class);
        if (jeiIntegration.isEmpty()) {
            return false;
        }
        
        JEIIntegration jei = jeiIntegration.get();
        Optional<Object> draggedIngredient = jei.getDraggedIngredient();
        
        if (draggedIngredient.isEmpty()) {
            return false;
        }
        
        EnoughFolders.LOGGER.info("Drop detected on folder button: {}", folder.getName());
        
        // Convert JEI ingredient to stored format
        Optional<StoredIngredient> storedIngredient = jei.storeIngredient(draggedIngredient.get());
        
        if (storedIngredient.isEmpty()) {
            return false;
        }
        
        // Add ingredient to folder
        EnoughFolders.getInstance().getFolderManager().addIngredient(folder, storedIngredient.get());
        EnoughFolders.LOGGER.info("Added ingredient to folder: {}", folder.getName());
        return true;
    }
}
