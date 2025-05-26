package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import net.minecraft.resources.ResourceLocation;

/**
 * Centralized class for UI constants.
 */
public final class UIConstants {
    // Prevent instantiation
    private UIConstants() {}

    /** The texture resource location for folder UI elements */
    public static final ResourceLocation FOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(EnoughFolders.MOD_ID, "textures/gui/folders.png");
    
    /** The width of a folder button in pixels */
    public static final int FOLDER_WIDTH = 18;
    
    /** The height of a folder button in pixels */
    public static final int FOLDER_HEIGHT = 18;
    
    /** The height of a folder row including spacing in pixels */
    public static final int FOLDER_ROW_HEIGHT = 27;
    
    /** The spacing between folder columns in pixels */
    public static final int FOLDER_COLUMN_SPACING = 4;
    
    /** The total height of the folder area in pixels */
    public static final int FOLDER_AREA_HEIGHT = 22;

    /** The size of each ingredient slot in pixels */
    public static final int INGREDIENT_SLOT_SIZE = 18;
    
    /** The spacing between ingredient slots in pixels */
    public static final int INGREDIENT_SPACING = 0;
    
    /** The number of rows in the ingredient grid */
    public static final int INGREDIENT_ROWS = 4;

    /** The height of input fields in pixels */
    public static final int INPUT_FIELD_HEIGHT = 20;

    /** The width reduction applied when JEI integration is active */
    public static final int JEI_WIDTH_REDUCTION = 10;

    /** The width of pagination buttons in pixels */
    public static final int PAGE_BUTTON_WIDTH = 16;
    
    /** The height of pagination buttons in pixels */
    public static final int PAGE_BUTTON_HEIGHT = 16;

    /** The default background color in ARGB format */
    public static final int DEFAULT_BACKGROUND_COLOR = 0xF0100010;
    
    /** The starting color for default border gradients in ARGB format */
    public static final int DEFAULT_BORDER_COLOR_START = 0x5000FF00;
    
    /** The ending color for default border gradients in ARGB format */
    public static final int DEFAULT_BORDER_COLOR_END = 0x50FFFF00;
}
