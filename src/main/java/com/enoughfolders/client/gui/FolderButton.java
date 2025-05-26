package com.enoughfolders.client.gui;

import com.enoughfolders.data.Folder;
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
}
