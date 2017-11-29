package org.janelia.it.workstation.browser.api.lifecycle;

/**
 * Static class for keeping track of the console's startup state. Certain things (logging, reporting 
 * exceptions, etc.) can only happen once a given state has been reached. 
 * 
 * This class is as basic as possible, to avoid all kinds of possible race conditions and other startup 
 * dependency issues. In particular, it does not import any other classes, like the EventBus, or any 
 * logging frameworks.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConsoleState {

    public static int INITIAL = 0; 
    public static int STARTING_SESSION = 100;
    public static int WINDOW_SHOWN = 200;
    
    private static int currState = INITIAL;

    public static int getCurrState() {
        return currState;
    }

    public static void setCurrState(int currState) {
        ConsoleState.currState = currState;
    }
}
