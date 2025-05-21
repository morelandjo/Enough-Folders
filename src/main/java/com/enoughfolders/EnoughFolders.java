package com.enoughfolders;

import com.enoughfolders.client.event.ClientEventHandler;
import com.enoughfolders.client.input.KeyBindings;
import com.enoughfolders.client.input.KeyHandler;
import com.enoughfolders.data.FolderManager;
import com.enoughfolders.util.DebugConfig;
import com.enoughfolders.util.DebugLogger;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Main mod class for Enough Folders.
 */
@Mod(EnoughFolders.MOD_ID)
public class EnoughFolders {
    /** 
     * The mod ID used for registration, configs, and resources 
     */
    public static final String MOD_ID = "enoughfolders";
    
    /**
     * Logger instance for the mod
     */
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);
    
    /**
     * Singleton instance of the mod
     */
    private static EnoughFolders instance;
    
    /**
     * Manager for all folders data and operations
     */
    private FolderManager folderManager;

    /**
     * Constructs the main mod instance.
     *
     * @param modEventBus The mod-specific event bus for registration events
     */
    public EnoughFolders(IEventBus modEventBus) {
        instance = this;
        
        // We only need to register client-side components
        if (FMLEnvironment.dist == Dist.CLIENT) {
            // Keep this as a main log entry since it's important initialization info
            LOGGER.info("Initializing Enough Folders client components");
            
            // Initialize debug configuration
            DebugConfig.load();
            
            // Initialize the folder manager
            folderManager = new FolderManager();
            DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Folder manager created");
            
            // Initialize client event handler
            ClientEventHandler.initialize();
            DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Client event handler initialized");
            
            // Register key mapping registration handler to the mod event bus
            modEventBus.register(KeyBindings.class);
            
            // Initialize key bindings
            KeyBindings.init();
            DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Key bindings initialized");
            
            // Initialize key handler
            KeyHandler.init();
            DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Key handler initialized");
            
            // Register the command registration event handler
            NeoForge.EVENT_BUS.register(this);
        }
        
        // Keep this as a main log entry
        LOGGER.info("Enough Folders initialized");
        DebugLogger.debug(DebugLogger.Category.INITIALIZATION, "Mod initialization complete");
    }
    
    /**
     * Event handler for registering commands.
     * 
     * @param event The command registration event
     */
    @SubscribeEvent(priority = EventPriority.NORMAL)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        // Command registration code (if any) goes here
    }

    /**
     * Gets the singleton instance of the mod.
     * 
     * @return The singleton instance of the EnoughFolders mod
     */
    public static EnoughFolders getInstance() {
        return instance;
    }

    /**
     * Gets the folder manager.
     * 
     * @return The folder manager instance
     */
    public FolderManager getFolderManager() {
        DebugLogger.debug(DebugLogger.Category.FOLDER_MANAGER, 
            "getFolderManager called from " + Thread.currentThread().getStackTrace()[2].getClassName());
        return folderManager;
    }
}
