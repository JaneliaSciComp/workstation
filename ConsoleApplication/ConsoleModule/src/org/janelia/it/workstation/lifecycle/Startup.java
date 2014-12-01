package org.janelia.it.workstation.lifecycle;

import org.janelia.it.workstation.gui.application.ConsoleApp;
import org.openide.modules.OnStart;

import java.security.ProtectionDomain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is run at startup.
 *
 * @author fosterl
 */
@OnStart
public class Startup implements Runnable {

    private static final Logger log = LoggerFactory.getLogger(Startup.class);

    static {
        try {
            setSystemProperties();
        }
        catch (Exception ex) {
            log.error("Failed to initialize starting-hook.", ex);
        }
    }

    private static void setSystemProperties() {
        log.info("Java version: "+System.getProperty("java.version"));
        ProtectionDomain pd = ConsoleApp.class.getProtectionDomain();
        log.debug("Code Source: "+pd.getCodeSource().getLocation());
        System.setProperty("apple.laf.useScreenMenuBar", "false");
//        System.setProperty("com.apple.mrj.application.apple.menu.about.name",
//                           ConsoleProperties.getString("console.Title"));
        System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");
    }
    
    public void run() {
        java.util.logging.Logger.getLogger("").addHandler(new NBExceptionHandler());
        ConsoleApp.newBrowser();
    }
}
