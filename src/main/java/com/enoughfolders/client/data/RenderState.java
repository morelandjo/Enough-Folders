package com.enoughfolders.client.data;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Data object containing the basic rendering state parameters.
 * Groups commonly used rendering parameters to simplify method signatures.
 */
public class RenderState {
    private final GuiGraphics graphics;
    private final int mouseX;
    private final int mouseY;
    private final float partialTick;

    /**
     * Creates a new render state.
     * 
     * @param graphics The graphics context
     * @param mouseX The current mouse X position
     * @param mouseY The current mouse Y position
     * @param partialTick The partial tick time
     */
    public RenderState(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        this.graphics = graphics;
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        this.partialTick = partialTick;
    }

    /**
     * Gets the graphics context.
     * 
     * @return The graphics context
     */
    public GuiGraphics getGraphics() {
        return graphics;
    }

    /**
     * Gets the mouse X position.
     * 
     * @return The mouse X position
     */
    public int getMouseX() {
        return mouseX;
    }

    /**
     * Gets the mouse Y position.
     * 
     * @return The mouse Y position
     */
    public int getMouseY() {
        return mouseY;
    }

    /**
     * Gets the partial tick time.
     * 
     * @return The partial tick time
     */
    public float getPartialTick() {
        return partialTick;
    }
    
    /**
     * Checks if the mouse is hovering over the specified area.
     * 
     * @param x The x position of the area
     * @param y The y position of the area
     * @param width The width of the area
     * @param height The height of the area
     * @return True if the mouse is hovering over the area, false otherwise
     */
    public boolean isHovering(int x, int y, int width, int height) {
        return mouseX >= x && mouseX < x + width && 
               mouseY >= y && mouseY < y + height;
    }
}
