package com.enoughfolders.client.gui;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;

/**
 * A button that handles mouse interactions but doesn't render any visual elements.
 * Used for creating invisible interaction areas that are rendered by custom rendering code.
 */
public class InvisibleButton extends Button {
    
    /**
     * Creates a new invisible button.
     *
     * @param x The x position of the button
     * @param y The y position of the button
     * @param width The width of the button
     * @param height The height of the button
     * @param message The button's message (not rendered)
     * @param onPress The action to perform when the button is pressed
     */
    public InvisibleButton(int x, int y, int width, int height, Component message, OnPress onPress) {
        super(x, y, width, height, message, onPress, DEFAULT_NARRATION);
    }
    
    /**
     * Override renderWidget to do nothing - this button is invisible.
     * Mouse interaction detection still works through the inherited methods.
     */
    @Override
    public void renderWidget(@Nonnull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        // Render nothing - this button is invisible
        // But mouse interaction detection still works through isMouseOver() and onClick()
    }
}
