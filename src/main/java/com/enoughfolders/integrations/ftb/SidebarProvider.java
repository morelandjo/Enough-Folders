package com.enoughfolders.integrations.ftb;

import net.minecraft.client.renderer.Rect2i;
import java.util.Optional;

/**
 * Interface for accessing FTB sidebar information.
 */
public interface SidebarProvider {
    /**
     * Checks if the FTB Library sidebar is available.
     *
     * @return True if the sidebar is available
     */
    boolean isSidebarAvailable();
    
    /**
     * Gets the rectangle representing the FTB sidebar, if it exists.
     *
     * @return Optional containing the sidebar rectangle, or empty if not available
     */
    Optional<Rect2i> getSidebarArea();
}
