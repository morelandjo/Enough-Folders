package com.enoughfolders.integrations.ftb.impl;

import com.enoughfolders.integrations.ftb.FTBIntegration;
import com.enoughfolders.integrations.ftb.SidebarProvider;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/**
 * Implementation class for FTB integration, responsible for checking if FTB is loaded
 * 
 * @deprecated This class is deprecated in favor of using the {@link com.enoughfolders.integrations.ftb.FTBIntegration}
 * class, which now delegates to the {@link SidebarProvider} interface. Will be removed in a future version.
 */
@Deprecated
public class FTBIntegrationImpl {
    
    /**
     * Check if FTB Library mod is loaded.
     * 
     * @return true if FTB Library is loaded
     * @deprecated Use {@link FTBIntegration#isFTBLibraryLoaded()} instead
     */
    @Deprecated
    public static boolean isFTBLibraryLoaded() {
        return FTBIntegration.isFTBLibraryLoaded();
    }

    /**
     * Gets the exclusion areas for FTB sidebar buttons.
     *
     * @return A list of rectangles representing FTB sidebar buttons areas
     * @deprecated Use {@link FTBIntegration#getSidebarExclusionAreas()} instead
     */
    @Deprecated
    public static List<Rect2i> getSidebarExclusionAreas() {
        return FTBIntegration.getSidebarExclusionAreas();
    }
    
    /**
     * Adjusts a UI rectangle to avoid overlapping with FTB sidebar.
     *
     * @param rectangle The rectangle to check and potentially adjust
     * @return The adjusted rectangle that doesn't overlap with FTB sidebar
     * @deprecated Use {@link FTBIntegration#avoidExclusionAreas(Rect2i)} instead
     */
    @Deprecated
    public static Rect2i avoidExclusionAreas(Rect2i rectangle) {
        return FTBIntegration.avoidExclusionAreas(rectangle);
    }
    
    // Private utility methods removed as they are now implemented in FTBIntegration
}
