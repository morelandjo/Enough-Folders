package com.enoughfolders.client.gui;

import com.enoughfolders.client.data.FolderContentState;
import com.enoughfolders.client.data.NavigationControls;
import com.enoughfolders.client.data.RenderContext;
import com.enoughfolders.client.data.RenderState;
import com.enoughfolders.data.Folder;
import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Handles rendering of the folder screen components.
 */
public class FolderScreenRenderer {
    private static final ResourceLocation TEXTURE = UIConstants.FOLDER_TEXTURE;
    
    // Complete render context
    private final RenderContext context;
    
    /**
     * Creates a new folder screen renderer with the given render context.
     *
     * @param renderContext The render context containing all necessary components
     */
    public FolderScreenRenderer(RenderContext renderContext) {
        this.context = renderContext;
    }
    
    /**
     * Sets the position and dimensions for rendering.
     *
     * @param leftPos Left position
     * @param topPos Top position
     * @param width Width of the container
     * @param height Height of the container
     */
    public void setPositionAndDimensions(int leftPos, int topPos, int width, int height) {
        context.setPositionAndDimensions(leftPos, topPos, width, height);
    }
    
    /**
     * Updates the height.
     *
     * @param height New height
     */
    public void updateHeight(int height) {
        context.updateHeight(height);
    }
        
    /**
     * Renders the folder screen using data objects for parameter organization.
     *
     * @param renderState The current rendering state
     * @param contentState The current folder content state
     * @param controls The navigation controls
     */
    public void render(
            RenderState renderState,
            FolderContentState contentState,
            NavigationControls controls) {
        
        GuiGraphics graphics = renderState.getGraphics();
        int mouseX = renderState.getMouseX();
        int mouseY = renderState.getMouseY();
        float partialTick = renderState.getPartialTick();
        
        renderBackground(graphics);
        
        // Render folder buttons
        for (Button button : contentState.getFolderButtons()) {
            button.render(graphics, mouseX, mouseY, partialTick);
            
            // Render folder icon and text on top of the standard button
            renderFolderIconAndText(graphics, button);
        }
        
        DebugLogger.debugValue(DebugLogger.Category.RENDERING, "Rendered {} folder buttons", 
            contentState.getFolderButtons().size());
        
        // Render add folder button
        renderAddFolderButton(graphics, mouseX, mouseY, partialTick, controls.getAddFolderButton());
        
        // Render active folder content if there is one
        context.getFolderManager().getActiveFolder().ifPresent(folder -> {
            DebugLogger.debugValue(DebugLogger.Category.RENDERING, "Rendering active folder: {}", folder.getName());
            
            int verticalOffset = contentState.isAddingFolder() ? 20 : 0; // INPUT_FIELD_HEIGHT
            
            if (contentState.getFolderRowsCount() > 1) {
                verticalOffset += (contentState.getFolderRowsCount() - 1) * 27; // FOLDER_ROW_HEIGHT
            }
            
            String name = folder.getTruncatedName();
            graphics.drawString(
                    context.getParentScreen().getMinecraft().font, 
                    name, 
                    context.getLeftPos() + 5, 
                    context.getTopPos() + UIConstants.FOLDER_AREA_HEIGHT + 17 + verticalOffset, 
                    0xFFFFFF
            );
            
            Button deleteButton = controls.getDeleteButton();
            deleteButton.setPosition(context.getLeftPos() + context.getWidth() - 25, context.getTopPos() + UIConstants.FOLDER_AREA_HEIGHT + 12 + verticalOffset);
            
            renderDeleteButton(graphics, mouseX, mouseY, partialTick, deleteButton);
            
            Button prevPageButton = controls.getPrevPageButton();
            Button nextPageButton = controls.getNextPageButton();
            
            prevPageButton.render(graphics, mouseX, mouseY, partialTick);
            nextPageButton.render(graphics, mouseX, mouseY, partialTick);
            
            // Render page count
            String pageText = (contentState.getCurrentPage() + 1) + "/" + contentState.getTotalPages();
            int pageTextWidth = context.getParentScreen().getMinecraft().font.width(pageText);
            
            int centerX = context.getLeftPos() + (context.getWidth() - pageTextWidth) / 2;
            int centerY = prevPageButton.getY() + prevPageButton.getHeight() / 2 - 4;
            
            graphics.drawString(
                    context.getParentScreen().getMinecraft().font,
                    pageText,
                    centerX,
                    centerY,
                    0xFFFFFF
            );
            
            // Render ingredient slots
            for (IngredientSlot slot : contentState.getIngredientSlots()) {
                slot.render(graphics, mouseX, mouseY);
            }
            
            DebugLogger.debugValue(DebugLogger.Category.RENDERING, "Rendered {} ingredient slots", 
                contentState.getIngredientSlots().size());
        });
        
        // Render folder name input if adding a folder
        if (contentState.isAddingFolder()) {
            controls.getNewFolderNameInput().render(graphics, mouseX, mouseY, partialTick);
        }
        
        // Render tooltips
        renderTooltips(graphics, mouseX, mouseY, contentState.getFolderButtons());
    }
    
    /**
     * Renders the semi-transparent background of the folder screen.
     *
     * @param graphics The graphics context to render with
     */
    private void renderBackground(GuiGraphics graphics) {
        int leftPos = context.getLeftPos();
        int topPos = context.getTopPos();
        int width = context.getWidth();
        int height = context.getHeight();
        
        DebugLogger.debugValues(DebugLogger.Category.RENDERING, 
            "FolderScreen.renderBackground at position: {},{} with dimensions: {}x{}", 
            leftPos, topPos, width, height);
        
        graphics.fill(leftPos, topPos, leftPos + width, topPos + height, 0x80404040);
    }
    
    /**
     * Renders tooltips for elements under the mouse cursor.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param folderButtons The folder buttons
     */
    private void renderTooltips(GuiGraphics graphics, int mouseX, int mouseY, List<Button> folderButtons) {
        for (Button button : folderButtons) {
            if (button.isMouseOver(mouseX, mouseY)) {
                // Get the folder for this button from the button manager
                Folder folder = context.getButtonManager().getFolderForButton(button);
                if (folder != null) {
                    graphics.renderTooltip(
                            context.getParentScreen().getMinecraft().font, 
                            Component.literal(folder.getName()),
                            mouseX, 
                            mouseY
                    );
                }
            }
        }
    }
    
    /**
     * Renders the add folder button and textures.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     * @param addFolderButton The add folder button
     */
    private void renderAddFolderButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Button addFolderButton) {
        DebugLogger.debug(DebugLogger.Category.RENDERING, "Rendering add folder button using sprite sheet");
        
        int x = addFolderButton.getX();
        int y = addFolderButton.getY();
        int width = 16;
        int height = 16;
        
        boolean isHovered = mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
        
        int textureU = 0;
        int textureV = isHovered ? 16 : 0;
        
        graphics.blit(TEXTURE, x, y, textureU, textureV, width, height, 64, 64);
        
        DebugLogger.debugValues(DebugLogger.Category.RENDERING, 
            "Add button drawn at {},{} using texture at {},{} with size {}x{}", 
            x, y, textureU, textureV, width, height);
    }

    /**
     * Renders the delete folder button and textures.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     * @param deleteButton The delete button
     */
    private void renderDeleteButton(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, Button deleteButton) {
        DebugLogger.debug(DebugLogger.Category.RENDERING, "Rendering delete button using sprite sheet");
        
        int x = deleteButton.getX();
        int y = deleteButton.getY();
        int width = 16;
        int height = 16;
        
        // Delete button uses the same texture whether hovered or not
        int textureU = 16;
        int textureV = 0;
        
        graphics.blit(TEXTURE, x, y, textureU, textureV, width, height, 64, 64);
        
        DebugLogger.debugValues(DebugLogger.Category.RENDERING,
            "Delete button drawn at {},{} using texture at {},{}", 
            x, y, textureU, textureV);
    }

    /**
     * Renders the folder icon and text over a standard button.
     *
     * @param graphics The graphics context to render with
     * @param button The button to render the folder graphics on
     */
    private void renderFolderIconAndText(GuiGraphics graphics, Button button) {
        Folder folder = context.getButtonManager().getFolderForButton(button);
        if (folder == null) {
            return;
        }
        
        // Calculate icon position to center it on the button
        int iconX = button.getX() + (button.getWidth() - 16) / 2;
        int iconY = button.getY() + (button.getHeight() - 16) / 2;
        
        // Determine texture coordinates based on folder state
        int textureU, textureV;
        
        if (folder.isActive()) {
            textureU = button.isHovered() ? 16 : 0;
            textureV = 48;
        } else {
            textureU = button.isHovered() ? 16 : 0;
            textureV = 32;
        }
        
        // Render the folder icon (16x16 icon in a 64x64 texture atlas)
        graphics.blit(UIConstants.FOLDER_TEXTURE, iconX, iconY, textureU, textureV, 16, 16, 64, 64);
        
        // Draw folder name below the button
        String shortName = folder.getShortName();
        int textX = button.getX() + (button.getWidth() - context.getParentScreen().getMinecraft().font.width(shortName)) / 2;
        int textY = button.getY() + button.getHeight() + 2;
        graphics.drawString(
                context.getParentScreen().getMinecraft().font, 
                shortName,
                textX,
                textY,
                0xFFFFFF
        );
    }

    /**
     * Renders the folder screen using only the RenderContext.
     *
     * @param graphics The graphics context to render with
     * @param mouseX The mouse x position
     * @param mouseY The mouse y position
     * @param partialTick The partial tick time
     * @param isAddingFolder Whether a new folder is being added
     * @param controls The navigation controls
     */
    public void renderWithContext(
            GuiGraphics graphics, 
            int mouseX, 
            int mouseY, 
            float partialTick,
            boolean isAddingFolder,
            NavigationControls controls) {
        
        // Create render state
        RenderState renderState = new RenderState(graphics, mouseX, mouseY, partialTick);
        
        // Create content state from context components
        FolderContentState contentState = new FolderContentState(
            isAddingFolder,
            context.getGridManager().getCurrentPage(),
            context.getGridManager().getTotalPages(),
            context.getButtonManager().getFolderRowsCount(),
            context.getButtonManager().getFolderButtons(),
            context.getGridManager().getIngredientSlots()
        );
        
        // Delegate to the regular render method
        render(renderState, contentState, controls);
    }
}
