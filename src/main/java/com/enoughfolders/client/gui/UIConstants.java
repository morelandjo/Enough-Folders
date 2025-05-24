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

    // Common texture resources
    public static final ResourceLocation FOLDER_TEXTURE = ResourceLocation.fromNamespaceAndPath(EnoughFolders.MOD_ID, "textures/gui/folders.png");

    // Folder button dimensions
    public static final int FOLDER_WIDTH = 18;
    public static final int FOLDER_HEIGHT = 18;
    public static final int FOLDER_ROW_HEIGHT = 27;
    public static final int FOLDER_COLUMN_SPACING = 4;
    public static final int FOLDER_AREA_HEIGHT = 22;

    // Ingredient grid constants
    public static final int INGREDIENT_SLOT_SIZE = 18;
    public static final int INGREDIENT_SPACING = 0;
    public static final int INGREDIENT_ROWS = 4;

    // Input field dimensions
    public static final int INPUT_FIELD_HEIGHT = 20;

    // Integration-specific constants
    public static final int JEI_WIDTH_REDUCTION = 10;

    // Pagination controls
    public static final int PAGE_BUTTON_WIDTH = 16;
    public static final int PAGE_BUTTON_HEIGHT = 16;

    // Background color constants
    public static final int DEFAULT_BACKGROUND_COLOR = 0xF0100010;
    public static final int DEFAULT_BORDER_COLOR_START = 0x5000FF00;
    public static final int DEFAULT_BORDER_COLOR_END = 0x50FFFF00;
}
