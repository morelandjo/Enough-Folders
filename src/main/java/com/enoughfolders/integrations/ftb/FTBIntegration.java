package com.enoughfolders.integrations.ftb;

import com.enoughfolders.util.DebugLogger;
import net.minecraft.client.renderer.Rect2i;

import java.util.Collections;
import java.util.List;

/**
 * Utility class for FTB integration
 * 
 */
/**
 * Integration with FTB mods for EnoughFolders.
 */
public class FTBIntegration {
    
    /**
     * Creates a new FTB integration instance.
     */
    public FTBIntegration() {
        // Default constructor
    }
    
    // Use the SidebarProvider interface from the provider-based implementation
    private static final SidebarProvider sidebarProvider = SidebarProviderFactory.create();
    
    /**
     * Check if FTB Library mod is loaded.
     * 
     * @return true if FTB Library is loaded
     */
    public static boolean isFTBLibraryLoaded() {
        boolean isAvailable = sidebarProvider.isSidebarAvailable();
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "FTB check - sidebar available: {}", isAvailable);
        return isAvailable;
    }

    /**
     * Gets the exclusion areas for FTB sidebar buttons.
     *
     * @return A list of rectangles representing FTB sidebar buttons areas
     */
    public static List<Rect2i> getSidebarExclusionAreas() {
        boolean ftbLoaded = isFTBLibraryLoaded();
        DebugLogger.debugValue(DebugLogger.Category.INTEGRATION, 
            "FTB getSidebarExclusionAreas - FTB Library loaded: {}", ftbLoaded);
        
        if (!ftbLoaded) {
            DebugLogger.debug(DebugLogger.Category.INTEGRATION, 
                "FTB Library not loaded, returning empty exclusion areas");
            return Collections.emptyList();
        }
        
        // Get the sidebar area from the provider
        return sidebarProvider.getSidebarArea()
            .map(rect -> {
                DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                    "Found FTB sidebar area: {}x{} at position {}x{}",
                    rect.getWidth(), rect.getHeight(), rect.getX(), rect.getY());
                return Collections.singletonList(rect);
            })
            .orElse(Collections.emptyList());
    }
    
    /**
     * Adjusts a UI rectangle to avoid overlapping with FTB sidebar.
     *
     * @param rectangle The rectangle to check and potentially adjust
     * @return The adjusted rectangle that doesn't overlap with FTB sidebar
     */
    public static Rect2i avoidExclusionAreas(Rect2i rectangle) {
        boolean ftbLoaded = isFTBLibraryLoaded();
        if (!ftbLoaded) {
            return rectangle;
        }
        
        List<Rect2i> exclusionAreas = getSidebarExclusionAreas();
        if (exclusionAreas.isEmpty()) {
            return rectangle;
        }
        
        // Create a copy of the rectangle that we can modify
        Rect2i adjustedRect = new Rect2i(
            rectangle.getX(), rectangle.getY(), 
            rectangle.getWidth(), rectangle.getHeight()
        );
        
        for (Rect2i exclusionArea : exclusionAreas) {
            // Check if our rectangle overlaps with the exclusion area
            if (rectanglesOverlap(adjustedRect, exclusionArea)) {
                // Calculate overlap percentage
                double overlapPercentage = calculateOverlapPercentage(adjustedRect, exclusionArea);
                
                if (overlapPercentage > 0.05) { // More than 5% overlap
                    DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                        "FTB sidebar overlap detected ({}%), adjusting position", 
                        Math.round(overlapPercentage * 100));
                    
                    // Move our rectangle down below the exclusion area
                    int newY = exclusionArea.getY() + exclusionArea.getHeight() + 5; // 5px padding
                    adjustedRect = new Rect2i(
                        adjustedRect.getX(), newY, 
                        adjustedRect.getWidth(), adjustedRect.getHeight()
                    );
                    
                    DebugLogger.debugValues(DebugLogger.Category.INTEGRATION,
                        "Adjusted Y position from {} to {}", rectangle.getY(), newY);
                }
            }
        }
        
        return adjustedRect;
    }
    
    /**
     * Checks if two rectangles overlap.
     *
     * @param rect1 The first rectangle
     * @param rect2 The second rectangle
     * @return True if the rectangles overlap, false otherwise
     */
    private static boolean rectanglesOverlap(Rect2i rect1, Rect2i rect2) {
        return rect1.getX() < rect2.getX() + rect2.getWidth() &&
               rect1.getX() + rect1.getWidth() > rect2.getX() &&
               rect1.getY() < rect2.getY() + rect2.getHeight() &&
               rect1.getY() + rect1.getHeight() > rect2.getY();
    }
    
    /**
     * Calculates the percentage of rect1 that overlaps with rect2.
     *
     * @param rect1 The first rectangle
     * @param rect2 The second rectangle
     * @return The percentage of rect1 that overlaps with rect2 (0.0 to 1.0)
     */
    private static double calculateOverlapPercentage(Rect2i rect1, Rect2i rect2) {
        // Calculate overlap dimensions
        int xOverlap = Math.max(0, Math.min(rect1.getX() + rect1.getWidth(), rect2.getX() + rect2.getWidth()) - 
                                 Math.max(rect1.getX(), rect2.getX()));
        
        int yOverlap = Math.max(0, Math.min(rect1.getY() + rect1.getHeight(), rect2.getY() + rect2.getHeight()) - 
                                 Math.max(rect1.getY(), rect2.getY()));
        
        int overlapArea = xOverlap * yOverlap;
        int rect1Area = rect1.getWidth() * rect1.getHeight();
        
        // Return percentage of rect1 that overlaps with rect2
        return rect1Area > 0 ? (double) overlapArea / rect1Area : 0;
    }
}
