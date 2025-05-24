// Import UIConstants class
package com.enoughfolders.client.gui;

import com.enoughfolders.EnoughFolders;
import net.minecraft.resources.ResourceLocation;

/**
 * Centralized class for UI-related constants used across the client GUI components.
 * This class helps reduce duplication and ensures consistency across the UI.
 */
public final class UIConstants {
    // Prevent instantiation
    private UIConstants() {}

    // Common texture resources    /** Main texture resource for folder UI elements */
    public static final ResourceLocation FOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(EnoughFolders.MOD_ID, "textures/gui/folders.png");
    
    // Folder button dimensions
    /** Width of folder buttons in pixels */
    public static final int FOLDER_WIDTH = 18;
    
    /** Height of folder buttons in pixels */
    public static final int FOLDER_HEIGHT = 18;
    
    /** Height of a row of folders including spacing in pixels */
    public static final int FOLDER_ROW_HEIGHT = 27;
    
    /** Horizontal spacing between folder buttons in pixels */
    public static final int FOLDER_COLUMN_SPACING = 4;
    
    /** Height of the folder area section in pixels */
    public static final int FOLDER_AREA_HEIGHT = 22;

    // Ingredient grid constants
    /** Size of ingredient slots in pixels */
    public static final int INGREDIENT_SLOT_SIZE = 18;
    
    /** Spacing between ingredients in pixels */
    public static final int INGREDIENT_SPACING = 0;
    
    /** Maximum number of ingredient rows to display */
    public static final int INGREDIENT_ROWS = 4;

    // Input field dimensions
    /** Height of text input fields in pixels */
    public static final int INPUT_FIELD_HEIGHT = 20;

    // Integration-specific constants
    /** Width reduction for JEI compatibility in pixels */
    public static final int JEI_WIDTH_REDUCTION = 10;

    // Pagination controls
    /** Width of page navigation buttons in pixels */
    public static final int PAGE_BUTTON_WIDTH = 16;
    
    /** Height of page navigation buttons in pixels */
    public static final int PAGE_BUTTON_HEIGHT = 16;

    // Background color constants
    /** Default background color for UI elements (ARGB) */
    public static final int DEFAULT_BACKGROUND_COLOR = 0xF0100010;
    
    /** Start color for border gradient (ARGB) */
    public static final int DEFAULT_BORDER_COLOR_START = 0x5000FF00;
    
    /** End color for border gradient (ARGB) */
    public static final int DEFAULT_BORDER_COLOR_END = 0x50FFFF00;
}
