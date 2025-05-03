package org.atlis.common.util;

import java.util.Map;
import org.atlis.common.model.Entity;

/**
 * Utility class containing very relevant information that is constantly used
 * throughout the game
 * @author smokey
 */
public class Constants {
    
    public static final String HOST = "127.0.0.1";
    
    public static final int PORT = 5050;

    public static final byte LOGIN_SUCCESS = 0x02;
    
    public static final byte LOGIN_FAILURE_INVALID_CREDENTIALS = 0x03;   
    
    public static final byte PLEASE_REGISTER = 0x04;
    
    public static final int BUFFER_SIZE = 1024;
    
    public static final Map<String, Integer> DIRECTION_MAP = Map.of(
        "north", 0,
        "south", 1,
        "east",  2,
        "west",  3
    );

    public static final String FONT = "Calibri";
    
    /**
     * The title of the game
     */
    public static final String GAME_TITLE = "Atlis";
    
    /** 
     * The rate at which actions take place i.e. attacks, 
     * skill actions, etc
     */
    public static final long PARSING_INTERVAL = 250;
    
    /**
     * The rate the game screen repaints at
     */
    public static final long PAINT_INTERVAL = 50; 
    
    /**
     * The size of a given region in pixels
     */
    public static final int REGION_SIZE = 768;
    
    /**
     * The margin that the server draws the players current
     * location outside what is actually visible on screen
     * to avoid seeing the edge of the map when walking around
     */
    public static final double DRAW_MARGIN = 1.6666666D;
    
    /**
     * The rate at which the logs automatically dump to text files
     * within the cache
     */
    public static final long LOG_DUMP_INTERVAL = 60000;
    
    /**
     * The date format for logging and chat, im sure there will 
     * be other uses in the future
     */
    public static final String DATE_FORMAT = "h:mm a - MMM dd yyyy";
    
    /**
     * The directory in which the cache is stored
     */
    public static final String CACHE_DIR = 
            "C:/Users/spark/OneDrive/Documents/NetBeansProjects/2D Game/cache";
    
    /**
     * The default width and height the game will open at 
     */
    public static final int DEFAULT_SCREEN_WIDTH = 1024, 
            DEFAULT_SCREEN_HEIGHT = 800; 
     
    public static final int START_X = -100, START_Y = -40;
    
    public static final int WALK_SPEED = 3;
    public static final int RUN_SPEED = 5;
}