package org.janelia.it.workstation.browser.api.lifecycle;

import org.janelia.it.workstation.browser.events.Events;
import org.janelia.it.workstation.browser.events.lifecycle.ApplicationClosing;
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
        Events.getInstance().postOnEventBus(new ApplicationClosing());
    }
}
