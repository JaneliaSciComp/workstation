package org.janelia.workstation.core.api;

import java.io.File;

import com.google.common.eventbus.Subscribe;
import org.janelia.workstation.core.api.lifecycle.ConsoleState;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationClosing;
import org.janelia.workstation.core.util.BrandingConfig;
import org.janelia.workstation.core.util.SystemInfo;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.openide.modules.Places;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is the main class for the workstation client, invoked by the NetBeans Startup hook. 
 * 
 * @author Todd Safford
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ConsoleApp {

    private static final Logger LOG = LoggerFactory.getLogger(ConsoleApp.class);

    // Singleton
    private static ConsoleApp instance;
    public static synchronized ConsoleApp getConsoleApp() {
        if (instance==null) {
            instance = new ConsoleApp();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private ConsoleApp() {

    }

}
