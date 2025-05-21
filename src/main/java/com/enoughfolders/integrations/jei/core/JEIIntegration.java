package com.enoughfolders.integrations.jei.core;

import com.enoughfolders.EnoughFolders;
import com.enoughfolders.client.gui.FolderButton;
import com.enoughfolders.client.gui.FolderScreen;
import com.enoughfolders.data.Folder;
import com.enoughfolders.data.StoredIngredient;
import com.enoughfolders.integrations.ModIntegration;
import com.enoughfolders.integrations.api.IngredientDragProvider;
import com.enoughfolders.integrations.api.RecipeViewingIntegration;
import com.enoughfolders.integrations.jei.gui.handlers.JEIRecipeGuiHandler;
import com.enoughfolders.util.DebugLogger;

import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.ingredients.IIngredientHelper;
import mezz.jei.api.ingredients.IIngredientType;
import mezz.jei.api.ingredients.IIngredientTypeWithSubtypes;
import mezz.jei.api.ingredients.ITypedIngredient;
import mezz.jei.api.ingredients.subtypes.UidContext;
import mezz.jei.api.runtime.IJeiRuntime;
import mezz.jei.api.runtime.IRecipesGui;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.IFocus;
import mezz.jei.api.recipe.IFocusFactory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

/**
 * Integration with JEI mod.
 */
public class JEIIntegration implements ModIntegration, IngredientDragProvider, RecipeViewingIntegration {
    /**
     * JEI mod identifier
     */
    private static final String MOD_ID = "jei";
    
    /**
     * The JEI runtime, obtained from the JEIPlugin
     */
    private IJeiRuntime jeiRuntime;

    /**
     * Currently tracked dragged ingredient
     */
    private Object currentDraggedObject = null;
    
    /**
     * Whether an ingredient is currently being dragged
     */
    private boolean isDragging = false;

    /**
     * Gets the display name of this mod integration.
     *
     * @return The display name of the integration
     */
    @Override
    public String getModName() {
        return "JEI";
    }
    
    /**
     * Checks if JEI is available in the current environment.
     *
     * @return true if JEI mod is loaded, false otherwise
     */
    @Override
    public boolean isAvailable() {
        return ModList.get().isLoaded(MOD_ID);
    }
    
    /**
     * Converts a StoredIngredient back to its original JEI ingredient object.
     *
     * @param storedIngredient The stored ingredient to convert
     * @return Optional containing the original ingredient object, or empty if conversion failed
     */
    @Override
    public Optional<?> getIngredientFromStored(StoredIngredient storedIngredient) {
        if (jeiRuntime == null) {
            return Optional.empty();
        }
        
        try {
            String typeName = storedIngredient.getType();
            String value = storedIngredient.getValue();
            
            for (IIngredientType<?> type : jeiRuntime.getIngredientManager().getRegisteredIngredientTypes()) {
                if (type.getIngredientClass().getName().equals(typeName)) {
                    @SuppressWarnings({"deprecation", "unchecked"})
                    Optional<? extends ITypedIngredient<?>> typedIngredient = 
                            jeiRuntime.getIngredientManager().getTypedIngredientByUid((IIngredientType) type, value);
                    
                    if (typedIngredient.isPresent()) {
                        return Optional.ofNullable(typedIngredient.get().getIngredient());
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get ingredient from stored data", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Converts a JEI ingredient object into a StoredIngredient for persistence.
     *
     * @param ingredient The JEI ingredient object to convert
     * @return Optional containing the StoredIngredient, or empty if conversion failed
     */
    @Override
    public Optional<StoredIngredient> storeIngredient(Object ingredient) {
        if (jeiRuntime == null) {
            return Optional.empty();
        }
        
        try {
            Optional<? extends ITypedIngredient<?>> optTypedIngredient = jeiRuntime.getIngredientManager()
                    .createTypedIngredient(ingredient);
            
            if (optTypedIngredient.isPresent()) {
                ITypedIngredient<?> typedIngredient = optTypedIngredient.get();
                IIngredientType<?> ingredientType = typedIngredient.getType();
                IIngredientHelper<Object> helper = getHelperForType(ingredientType);
                
                if (helper != null) {
                    String typeClass = ingredientType.getIngredientClass().getName();
                    Object uid = helper.getUid(ingredient, UidContext.Ingredient);
                    
                    return Optional.of(new StoredIngredient(typeClass, uid.toString()));
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to store ingredient", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets an ItemStack that can be used to visually represent the ingredient.
     *
     * @param ingredient The ingredient to get an ItemStack for
     * @return Optional containing the ItemStack, or empty if conversion failed
     */
    @Override
    public Optional<ItemStack> getItemStackForDisplay(Object ingredient) {
        if (jeiRuntime == null) {
            return Optional.empty();
        }
        
        try {
            if (ingredient instanceof ItemStack itemStack) {
                return Optional.of(itemStack);
            }
            
            Optional<? extends ITypedIngredient<?>> optTypedIngredient = jeiRuntime.getIngredientManager()
                    .createTypedIngredient(ingredient);
            
            if (optTypedIngredient.isPresent()) {
                ITypedIngredient<?> typedIngredient = optTypedIngredient.get();
                IIngredientHelper<Object> helper = getHelperForType(typedIngredient.getType());
                if (helper != null) {
                    return Optional.ofNullable(helper.getCheatItemStack(ingredient));
                }
                
                if (typedIngredient.getType() instanceof IIngredientTypeWithSubtypes) {
                    if (typedIngredient.getIngredient() instanceof ItemStack) {
                        return Optional.of((ItemStack) typedIngredient.getIngredient());
                    }
                    
                    @SuppressWarnings("unchecked")
                    IIngredientTypeWithSubtypes<Item, ItemStack> itemType = 
                            (IIngredientTypeWithSubtypes<Item, ItemStack>) VanillaTypes.ITEM_STACK;
                    
                    if (ingredient instanceof Item item) {
                        return Optional.of(itemType.getDefaultIngredient(item));
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get ItemStack for display", e);
        }
        
        return Optional.empty();
    }
    
    /**
     * Renders a stored ingredient in the GUI.
     *
     * @param graphics The graphics context to render with
     * @param ingredient The stored ingredient to render
     * @param x The x position to render at
     * @param y The y position to render at
     * @param width The width of the rendering area
     * @param height The height of the rendering area
     */
    @Override
    public void renderIngredient(GuiGraphics graphics, StoredIngredient ingredient, int x, int y, int width, int height) {
        if (jeiRuntime == null) {
            return;
        }

        try {
            Optional<?> ingredientOpt = getIngredientFromStored(ingredient);
            if (ingredientOpt.isEmpty()) {
                return;
            }

            Object ingredientObj = ingredientOpt.get();
            
            for (IIngredientType<?> type : jeiRuntime.getIngredientManager().getRegisteredIngredientTypes()) {
                if (type.getIngredientClass().isInstance(ingredientObj)) {
                    @SuppressWarnings("unchecked")
                    var renderer = jeiRuntime.getIngredientManager().getIngredientRenderer((IIngredientType<Object>)type);
                    
                    if (renderer != null) {
                        renderer.render(graphics, ingredientObj, x, y);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to render ingredient", e);
        }
    }
    
    /**
     * Shows recipes for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show recipes for
     */
    public void showRecipes(Object ingredient) {
        if (jeiRuntime == null) {
            EnoughFolders.LOGGER.error("Cannot show recipes: JEI runtime is not available");
            return;
        }
        
        try {
            saveCurrentFolderScreen();
            
            IRecipesGui recipesGui = jeiRuntime.getRecipesGui();
            if (recipesGui != null) {
                Optional<? extends ITypedIngredient<?>> typedIngredient = 
                    jeiRuntime.getIngredientManager().createTypedIngredient(ingredient);
                
                if (typedIngredient.isPresent()) {
                    IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();
                    
                    @SuppressWarnings("unchecked")
                    IFocus<?> focus = focusFactory.createFocus(
                        RecipeIngredientRole.OUTPUT,
                        (ITypedIngredient) typedIngredient.get()
                    );
                    
                    recipesGui.show(focus);
                    EnoughFolders.LOGGER.debug("Successfully showed recipes for ingredient");
                } else {
                    EnoughFolders.LOGGER.error("Failed to create typed ingredient for showing recipes");
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing recipes for ingredient", e);
        }
    }
    
    /**
     * Shows usages for the provided ingredient in the JEI recipe GUI.
     *
     * @param ingredient The ingredient to show usages for
     */
    public void showUses(Object ingredient) {
        if (jeiRuntime == null) {
            EnoughFolders.LOGGER.error("Cannot show uses: JEI runtime is not available");
            return;
        }
        
        try {
            saveCurrentFolderScreen();
            
            IRecipesGui recipesGui = jeiRuntime.getRecipesGui();
            if (recipesGui != null) {
                Optional<? extends ITypedIngredient<?>> typedIngredient = 
                    jeiRuntime.getIngredientManager().createTypedIngredient(ingredient);
                
                if (typedIngredient.isPresent()) {
                    IFocusFactory focusFactory = jeiRuntime.getJeiHelpers().getFocusFactory();
                    
                    @SuppressWarnings("unchecked")
                    IFocus<?> focus = focusFactory.createFocus(
                        RecipeIngredientRole.INPUT,
                        (ITypedIngredient) typedIngredient.get()
                    );
                    
                    recipesGui.show(focus);
                    EnoughFolders.LOGGER.debug("Successfully showed uses for ingredient");
                } else {
                    EnoughFolders.LOGGER.error("Failed to create typed ingredient for showing uses");
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error showing uses for ingredient", e);
        }
    }
    
    /**
     * Saves the current folder screen so it can be displayed on recipe screens.
     */
    private void saveCurrentFolderScreen() {
        Screen currentScreen = Minecraft.getInstance().screen;
        if (currentScreen instanceof AbstractContainerScreen<?>) {
            AbstractContainerScreen<?> containerScreen = (AbstractContainerScreen<?>) currentScreen;
            com.enoughfolders.client.event.ClientEventHandler.getFolderScreen(containerScreen)
                .ifPresent(folderScreen -> {
                    // Force a reinit of the folder screen before saving it
                    int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
                    int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
                    folderScreen.init(screenWidth, screenHeight);
                    
                    JEIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
                    EnoughFolders.LOGGER.debug("Saved folder screen for recipe/usage view");
                });
        }
    }
    
    /**
     * Helper method to get the ingredient helper for a specific ingredient type.
     *
     * @param <T> The type of ingredient
     * @param type The ingredient type class
     * @return The ingredient helper, or null if not found
     */
    @SuppressWarnings("unchecked")
    private <T> IIngredientHelper<T> getHelperForType(IIngredientType<?> type) {
        try {
            return (IIngredientHelper<T>) jeiRuntime.getIngredientManager().getIngredientHelper((IIngredientType<T>) type);
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Failed to get ingredient helper for type: " + type, e);
            return null;
        }
    }
    
    /**
     * Initializes the JEI integration.
     */
    @Override
    public void initialize() {
        EnoughFolders.LOGGER.info("Initializing JEI integration");
    }
    
    /**
     * Sets the JEI runtime reference.
     *
     * @param jeiRuntime The JEI runtime instance
     */
    public void setJeiRuntime(IJeiRuntime jeiRuntime) {
        this.jeiRuntime = jeiRuntime;
        EnoughFolders.LOGGER.info("JEI Runtime available, integration active");
    }
    
    /**
     * Gets the JEI runtime reference.
     *
     * @return Optional containing the JEI runtime, or empty if not available
     */
    public Optional<IJeiRuntime> getJeiRuntime() {
        return Optional.ofNullable(jeiRuntime);
    }
    
    /**
     * Checks if the JEI recipe GUI is currently open.
     *
     * @return true if the JEI recipe GUI is the current screen
     */
    public boolean isRecipeGuiOpen() {
        if (jeiRuntime == null) {
            return false;
        }
        
        try {
            IRecipesGui recipesGui = jeiRuntime.getRecipesGui();
            if (recipesGui == null) {
                return false;
            }
            
            // Check if the recipe GUI is the current screen
            Screen currentScreen = Minecraft.getInstance().screen;
            return currentScreen != null && currentScreen.equals(recipesGui);
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error checking if JEI recipe GUI is open", e);
            return false;
        }
    }
    
    /**
     * Gets the ingredient currently being dragged in JEI, if any.
     *
     * @return Optional containing the dragged ingredient, or empty if none is being dragged
     */
    public Optional<Object> getDraggedIngredient() {
        // If we're tracking a dragged object, return that first
        if (isDragging && currentDraggedObject != null) {
            EnoughFolders.LOGGER.info("Returning tracked dragged ingredient: {}", 
                    currentDraggedObject.getClass().getSimpleName());
            return Optional.of(currentDraggedObject);
        }
        
        // Otherwise, try to get it from JEI directly
        if (jeiRuntime == null) {
            EnoughFolders.LOGGER.debug("JEI runtime not available");
            return Optional.empty();
        }
        
        try {
            // Try to get the dragged ingredient from JEI's GhostIngredientDrag
            Optional<Object> draggedIngredient = getJeiDraggedItem();
            draggedIngredient.ifPresent(ingredient -> 
                EnoughFolders.LOGGER.info("Got JEI dragged ingredient: {}", 
                    ingredient.getClass().getSimpleName()));
            return draggedIngredient;
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting dragged ingredient", e);
            return Optional.empty();
        }
    }
    
    /**
     * Sets the current dragged object.
     *
     * @param ingredient The ingredient being dragged
     */
    public void setCurrentDraggedObject(Object ingredient) {
        this.currentDraggedObject = ingredient;
        this.isDragging = true;
        EnoughFolders.LOGGER.info("Set current dragged object: {}", 
                ingredient != null ? ingredient.getClass().getSimpleName() : "null");
    }
    
    /**
     * Clears the current dragged object.
     */
    public void clearCurrentDraggedObject() {
        this.currentDraggedObject = null;
        this.isDragging = false;
        EnoughFolders.LOGGER.info("Cleared current dragged object");
    }

    /**
     * Helper method to get the dragged ingredient directly from JEI.
     *
     * @return Optional containing the dragged ingredient, or empty if none is found
     */
    private Optional<Object> getJeiDraggedItem() {
        try {
            // Try to get access to JEI's drag manager
            Optional<Object> dragManagerOpt = getJeiDragManager();
            if (dragManagerOpt.isEmpty()) {
                return Optional.empty();
            }
            
            Object dragManager = dragManagerOpt.get();
            EnoughFolders.LOGGER.debug("Found JEI drag manager: {}", dragManager.getClass().getName());
            
            // Try to get the dragged ingredient
            Optional<Object> ingredientOpt = getDraggedIngredientFromManager(dragManager);
            if (ingredientOpt.isPresent()) {
                Object ingredient = ingredientOpt.get();
                EnoughFolders.LOGGER.debug("Found JEI dragged ingredient: {}", ingredient.getClass().getName());
                
                // If it's an ITypedIngredient, extract the actual ingredient
                if (ingredient instanceof ITypedIngredient<?> typedIngredient) {
                    return Optional.ofNullable(typedIngredient.getIngredient());
                }
                
                return Optional.of(ingredient);
            }
            
            return Optional.empty();
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error getting JEI dragged ingredient", e);
            return Optional.empty();
        }
    }
    
    /**
     * Gets JEI's GhostIngredientDrag manager through various means.
     *
     * @return Optional containing the drag manager, or empty if not found
     */
    private Optional<Object> getJeiDragManager() {
        // First try through minecraft GUI screen if JEI overlay is active
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.screen != null) {
            try {
                // JEI often adds its drag manager as a listener to GUI screens
                Class<?> screenClass = minecraft.screen.getClass();
                Field listenersField = null;
                
                // Look for any field that might contain listeners
                for (Field field : screenClass.getDeclaredFields()) {
                    if (field.getType().isAssignableFrom(java.util.List.class)) {
                        field.setAccessible(true);
                        java.util.List<?> listeners = (java.util.List<?>) field.get(minecraft.screen);
                        
                        if (listeners != null) {
                            for (Object listener : listeners) {
                                if (listener != null && listener.getClass().getSimpleName().contains("GhostIngredientDrag")) {
                                    return Optional.of(listener);
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                EnoughFolders.LOGGER.debug("Error finding drag manager in screen: {}", e.getMessage());
            }
        }
        
        return Optional.empty();
    }
    
    /**
     * Gets the dragged ingredient from JEI's drag manager.
     *
     * @param dragManager The JEI drag manager object
     * @return Optional containing the dragged ingredient, or empty if none is found
     */
    private Optional<Object> getDraggedIngredientFromManager(Object dragManager) {
        try {
            // Try to find and invoke the getter method for the dragged ingredient
            for (Method method : dragManager.getClass().getDeclaredMethods()) {
                if (method.getParameterCount() == 0 && 
                    (method.getName().contains("get") || method.getName().contains("drag")) && 
                    !method.getReturnType().equals(void.class)) {
                    
                    method.setAccessible(true);
                    Object result = method.invoke(dragManager);
                    
                    // If result is Optional, unwrap it
                    if (result instanceof Optional<?> opt) {
                        if (opt.isPresent()) {
                            return Optional.of(opt.get());
                        }
                    } 
                    // If result is directly an ingredient
                    else if (result != null && 
                             (result instanceof ITypedIngredient<?> || 
                              !result.getClass().getName().contains("java.lang"))) {
                        return Optional.of(result);
                    }
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Error extracting ingredient from drag manager: {}", e.getMessage());
        }
        
        return Optional.empty();
    }
    
    /**
     * Processes a drop of the currently dragged ingredient onto a folder.
     * 
     * @param folder The folder to add the ingredient to
     * @return True if the drop was successful, false otherwise
     */
    @Override
    public boolean handleIngredientDrop(Folder folder) {
        Optional<Object> draggedIngredient = getDraggedIngredient();
        if (draggedIngredient.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "No ingredient is being dragged");
            return false;
        }
        
        Object ingredient = draggedIngredient.get();
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "Processing JEI dragged ingredient drop for folder: {}", folder.getName());
        
        // Convert ingredient to StoredIngredient
        Optional<StoredIngredient> storedIngredient = storeIngredient(ingredient);
        if (storedIngredient.isEmpty()) {
            DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Failed to convert ingredient to StoredIngredient");
            return false;
        }
        
        // Add ingredient to folder
        EnoughFolders.getInstance().getFolderManager().addIngredient(folder, storedIngredient.get());
        
        // Clear the dragged ingredient so it doesn't get processed again
        clearCurrentDraggedObject();
        
        DebugLogger.debugValues(DebugLogger.Category.JEI_INTEGRATION,
            "Successfully added JEI ingredient to folder: {}", folder.getName());
        return true;
    }
    
    /**
     * Gets the display name of the integration.
     * 
     * @return The display name
     */
    @Override
    public String getDisplayName() {
        return "JEI";
    }
    
    /**
     * Connect a folder screen to JEI for recipe viewing.
     * 
     * @param folderScreen The folder screen to connect
     * @param containerScreen The container screen
     */
    @Override
    public void connectToFolderScreen(FolderScreen folderScreen, AbstractContainerScreen<?> containerScreen) {
        try {
            // Create handler if available
            if (isAvailable()) {
                // Connect the folder screen to JEI functionality
                folderScreen.registerIngredientClickHandler((slot, button, shift, ctrl) -> 
                    handleIngredientClick(slot, button, shift, ctrl));
                
                EnoughFolders.LOGGER.debug("Connected folder screen to JEI");
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Could not connect folder to JEI: {}", e.getMessage());
        }
    }
    
    /**
     * Save a folder screen to be used during recipe GUI navigation.
     * 
     * @param folderScreen The folder screen to save
     */
    @Override
    public void saveLastFolderScreen(FolderScreen folderScreen) {
        JEIRecipeGuiHandler.saveLastFolderScreen(folderScreen);
    }
    
    /**
     * Clear the saved folder screen when no longer needed.
     */
    @Override
    public void clearLastFolderScreen() {
        JEIRecipeGuiHandler.clearLastFolderScreen();
    }
    
    /**
     * Get the last folder screen saved for recipe GUI navigation.
     * 
     * @return Optional containing the folder screen if available
     */
    @Override
    public Optional<FolderScreen> getLastFolderScreen() {
        return JEIRecipeGuiHandler.getLastFolderScreen();
    }
    
    /**
     * Check if the given screen is a recipe screen for this integration.
     * 
     * @param screen The screen to check
     * @return True if it's a recipe screen for this integration, false otherwise
     */
    @Override
    public boolean isRecipeScreen(Screen screen) {
        if (screen == null || !isAvailable()) {
            return false;
        }
        
        try {
            // Check if the screen is a JEI IRecipesGui
            Class<?> recipesGuiClass = Class.forName("mezz.jei.api.runtime.IRecipesGui");
            return recipesGuiClass.isInstance(screen);
        } catch (ClassNotFoundException e) {
            // JEI is not installed or class not found
            return false;
        }
    }
    
    /**
     * Check if the screen being closed is transitioning to a recipe screen for this integration.
     * 
     * @param screen The screen that's being closed
     * @return True if we're transitioning to a recipe screen, false otherwise
     */
    @Override
    public boolean isTransitioningToRecipeScreen(Screen screen) {
        if (!isAvailable()) {
            return false;
        }
        
        try {
            // We need to check if this closure is due to JEI opening a recipe view
            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
            for (StackTraceElement element : stackTrace) {
                // Check if JEI recipe classes are in the stack trace
                if (element.getClassName().contains("mezz.jei") && 
                    (element.getMethodName().contains("show") || 
                     element.getClassName().contains("RecipesGui"))) {
                    return true;
                }
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.debug("Error checking for JEI recipe transition: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Handle a click on an ingredient slot in a folder.
     *
     * @param slot The ingredient slot that was clicked
     * @param button The mouse button used (0 = left, 1 = right)
     * @param shift Whether shift was held
     * @param ctrl Whether ctrl was held
     * @return true if the click was handled, false otherwise
     */
    public boolean handleIngredientClick(com.enoughfolders.client.gui.IngredientSlot slot, int button, 
                                          boolean shift, boolean ctrl) {
        try {
            if (!isAvailable() || jeiRuntime == null) {
                return false;
            }
            
            // Get the stored ingredient from the slot
            StoredIngredient storedIngredient = slot.getIngredient();
            if (storedIngredient == null) {
                return false;
            }
            
            // Convert stored ingredient to JEI ingredient
            Optional<?> ingredientOpt = getIngredientFromStored(storedIngredient);
            if (ingredientOpt.isEmpty()) {
                return false;
            }
            
            Object ingredient = ingredientOpt.get();
            
            // Determine action based on mouse button and modifiers
            if (button == 0) {
                // Left click - show recipes
                showRecipes(ingredient);
                return true;
            } else if (button == 1) {
                // Right click - show uses
                showUses(ingredient);
                return true;
            }
        } catch (Exception e) {
            EnoughFolders.LOGGER.error("Error handling ingredient click: {}", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Creates folder targets that can be used for ingredient drops from JEI.
     * 
     * @param folderButtons The list of folder buttons to create targets for
     * @return A list of folder targets compatible with JEI
     */
    @Override
    public List<com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget> createFolderTargets(
            List<FolderButton> folderButtons) {
        EnoughFolders.LOGGER.debug("Creating JEI folder targets - Number of folder buttons available: {}", 
            folderButtons.size());
        DebugLogger.debug(DebugLogger.Category.JEI_INTEGRATION, "Getting JEI folder targets");
        
        List<com.enoughfolders.integrations.jei.gui.targets.FolderButtonTarget> targets = 
            com.enoughfolders.integrations.jei.gui.targets.JEIFolderTargetFactory
                .getInstance()
                .createTargets(folderButtons);
        
        DebugLogger.debugValue(DebugLogger.Category.JEI_INTEGRATION, "Created {} JEI folder targets", targets.size());
        return targets;
    }
}
