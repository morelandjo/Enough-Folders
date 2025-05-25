package com.enoughfolders.integrations.util;

import net.minecraft.client.gui.screens.Screen;

/**
 * Utility class for stack trace analysis used by integration modules.
 */
public final class StackTraceUtils {

    private StackTraceUtils() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Checks if the current screen is transitioning to a recipe screen.
     * 
     * @param screen the current screen
     * @return true if transitioning to a recipe screen, false otherwise
     */
    
    public static boolean isTransitioningToRecipeScreen(Screen screen) {
        return isJEIRecipeTransition() || isREIRecipeTransition() || isEMIRecipeTransition();
    }

    /**
     * Checks if the current stack trace contains classes and methods associated with JEI recipe transitions.
     * 
     * @return true if JEI recipe screen transition is detected in the stack trace
     */
    public static boolean isJEIRecipeTransition() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return containsClassAndMethod(stackTrace, "mezz.jei", 
                new String[]{"show"}, 
                new String[]{"RecipesGui"});
    }

    /**
     * Checks if the current stack trace contains classes and methods associated with REI recipe transitions.
     * 
     * @return true if REI recipe screen transition is detected in the stack trace
     */
    public static boolean isREIRecipeTransition() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        
        // Add detailed logging for debugging
        boolean result = containsClassAndMethod(stackTrace, "shedaniel.rei", 
                new String[]{"show", "view", "open", "display"}, 
                new String[]{"RecipeScreen", "DisplayScreen", "ViewSearchBuilder", "ViewsScreen", "DefaultDisplayViewingScreen", "DisplayViewingScreen"});
        
        if (result) {
            System.out.println("REI Recipe Transition Detected! Stack trace:");
            for (StackTraceElement element : stackTrace) {
                if (element.getClassName().contains("shedaniel.rei")) {
                    System.out.println("  " + element.getClassName() + "." + element.getMethodName() + "()");
                }
            }
        }
        
        return result;
    }
    
    /**
     * Checks if the current stack trace contains classes and methods associated with EMI recipe transitions.
     * 
     * @return true if EMI recipe screen transition is detected in the stack trace
     */
    public static boolean isEMIRecipeTransition() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        return containsClassAndMethod(stackTrace, "dev.emi.emi", 
                new String[]{"display", "show", "view"}, 
                new String[]{"RecipeScreen", "EmiScreen"});
    }

    /**
     * Generic method to check if a stack trace contains elements matching specific patterns.
     * 
     * @param stackTrace the stack trace to analyze
     * @param packagePrefix the package prefix to look for
     * @param methodPatterns array of method name patterns to match
     * @param classPatterns array of class name patterns to match
     * @return true if the stack trace contains at least one element matching the specified patterns
     */
    public static boolean containsClassAndMethod(
            StackTraceElement[] stackTrace, 
            String packagePrefix,
            String[] methodPatterns, 
            String[] classPatterns) {
        
        for (StackTraceElement element : stackTrace) {
            String className = element.getClassName();
            String methodName = element.getMethodName();
            
            if (className.contains(packagePrefix)) {
                // Check if any method pattern matches
                boolean methodMatch = false;
                for (String methodPattern : methodPatterns) {
                    if (methodName.contains(methodPattern)) {
                        methodMatch = true;
                        break;
                    }
                }
                
                if (!methodMatch) {
                    continue;  // No method matched, skip to next element
                }
                
                // Check if any class pattern matches
                for (String classPattern : classPatterns) {
                    if (className.contains(classPattern)) {
                        return true;  // Both method and class patterns matched
                    }
                }
            }
        }
        
        return false;
    }
}
