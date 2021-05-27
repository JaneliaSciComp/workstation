package org.janelia.workstation.core.api.lifecycle;

import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ApplicationClosing;
import org.janelia.workstation.core.model.local.LocalMongoService;
import org.openide.modules.OnStop;

/**
 * This hook declares itself to be called when NetBeans RCP/Platform decides
 * to exit (in response to users clicking an 'Exit' button).
 *
 * @author fosterl
 */
@OnStop
public class Shutdown implements Runnable {
    public void run() {
        LocalMongoService.cleanupConnection();
        Events.getInstance().postOnEventBus(new ApplicationClosing());
    }
}
