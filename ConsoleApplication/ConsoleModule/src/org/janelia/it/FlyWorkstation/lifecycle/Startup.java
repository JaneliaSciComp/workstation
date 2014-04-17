package org.janelia.it.FlyWorkstation.lifecycle;

import org.janelia.it.FlyWorkstation.gui.application.ConsoleApp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.openide.modules.OnStart;

/**
 * This is run at startup.
 * @author fosterl
 */
@OnStart
public class Startup implements Runnable {
    private static Logger logger = LoggerFactory.getLogger(Startup.class);
    
    static {
        try {
            setSystemProperties();
        } catch ( Exception ex ) {
            ex.printStackTrace();
            logger.error("Failed to initialize starting-hook.");
        }
    }
    private static void setSystemProperties() {
        logger.info("Java version: " + System.getProperty("java.version"));
        java.security.ProtectionDomain pd = ConsoleApp.class.getProtectionDomain();
        logger.debug("Code Source: " + pd.getCodeSource().getLocation());
        System.setProperty("apple.laf.useScreenMenuBar", "false");
//        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
//                           ConsoleProperties.getString("console.Title"));
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
    }

    
    public void run() {
        ConsoleApp.newBrowser();
    }
}
