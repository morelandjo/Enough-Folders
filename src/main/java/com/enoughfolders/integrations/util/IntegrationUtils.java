package com.enoughfolders.integrations.util;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;

/**
 * Utility class for common geometric operations used in integration handlers.
 * This centralizes point-in-rectangle checks and other geometric utility methods
 * that were previously duplicated across JEI and REI integrations.
 */
public final class IntegrationUtils {

    private IntegrationUtils() {
        // Private constructor to prevent instantiation
    }

    /**
     * Checks if a point is within a rectangle defined by coordinates and dimensions.
     * 
     * @param x the X coordinate of the point to check
     * @param y the Y coordinate of the point to check
     * @param rectX the X coordinate of the rectangle's top-left corner
     * @param rectY the Y coordinate of the rectangle's top-left corner
     * @param width the width of the rectangle
     * @param height the height of the rectangle
     * @return true if the point is within the rectangle, false otherwise
     */
    public static boolean isPointInRect(double x, double y, int rectX, int rectY, int width, int height) {
        return x >= rectX && x < rectX + width && y >= rectY && y < rectY + height;
    }

    /**
     * Checks if a point is within the bounds of a GUI widget.
     * 
     * @param x the X coordinate of the point to check
     * @param y the Y coordinate of the point to check
     * @param widget the widget to check against
     * @return true if the point is within the widget's bounds, false otherwise
     */
    public static boolean isPointInWidget(double x, double y, AbstractWidget widget) {
        return widget.isMouseOver(x, y);
    }

    /**
     * Checks if a point is within the bounds of a GUI event listener.
     * This is useful for components that implement GuiEventListener but not AbstractWidget.
     * 
     * @param x the X coordinate of the point to check
     * @param y the Y coordinate of the point to check
     * @param listener the GUI event listener to check against
     * @return true if the point is within the listener's bounds, false otherwise
     */
    public static boolean isPointInListener(double x, double y, GuiEventListener listener) {
        return listener.isMouseOver(x, y);
    }
    
    /**
     * Calculates whether two rectangles intersect.
     * 
     * @param x1 the X coordinate of the first rectangle's top-left corner
     * @param y1 the Y coordinate of the first rectangle's top-left corner
     * @param w1 the width of the first rectangle
     * @param h1 the height of the first rectangle
     * @param x2 the X coordinate of the second rectangle's top-left corner
     * @param y2 the Y coordinate of the second rectangle's top-left corner
     * @param w2 the width of the second rectangle
     * @param h2 the height of the second rectangle
     * @return true if the rectangles intersect, false otherwise
     */
    public static boolean doRectsIntersect(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
        return x1 < x2 + w2 && x1 + w1 > x2 && y1 < y2 + h2 && y1 + h1 > y2;
    }
    
    /**
     * Checks if a point is within any rectangle object that provides getX(), getY(), getWidth(), and getHeight() methods.
     * 
     * @param <T> the type of rectangle object
     * @param x the X coordinate of the point to check
     * @param y the Y coordinate of the point to check
     * @param rect a rectangle object with position and dimension accessor methods
     * @param xGetter function to get the X coordinate from the rectangle
     * @param yGetter function to get the Y coordinate from the rectangle
     * @param widthGetter function to get the width from the rectangle
     * @param heightGetter function to get the height from the rectangle
     * @return true if the point is within the rectangle, false otherwise
     */
    public static <T> boolean isPointInRect(double x, double y, T rect, 
                                           java.util.function.ToIntFunction<T> xGetter,
                                           java.util.function.ToIntFunction<T> yGetter,
                                           java.util.function.ToIntFunction<T> widthGetter,
                                           java.util.function.ToIntFunction<T> heightGetter) {
        int rectX = xGetter.applyAsInt(rect);
        int rectY = yGetter.applyAsInt(rect);
        int width = widthGetter.applyAsInt(rect);
        int height = heightGetter.applyAsInt(rect);
        return isPointInRect(x, y, rectX, rectY, width, height);
    }
}
