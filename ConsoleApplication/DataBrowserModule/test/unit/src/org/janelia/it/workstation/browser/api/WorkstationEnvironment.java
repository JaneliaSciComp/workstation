package org.janelia.it.workstation.browser.api;

import org.janelia.it.workstation.browser.util.ConsoleProperties;

/**
 * Helps mock out the Workstation well enough to carry out tests. Logs in, etc.
 * Created by fosterl on 1/27/14.
 */
public class WorkstationEnvironment {
    public void invoke() {
        // Need to mock the browser environment.
        // Prime the tool-specific properties before the Session is invoked
        ConsoleProperties.load();
        AccessManager.getAccessManager().loginUsingSavedCredentials();
    }
}

