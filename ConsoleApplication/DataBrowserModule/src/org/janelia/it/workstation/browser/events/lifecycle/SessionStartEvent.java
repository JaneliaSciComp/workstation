package org.janelia.it.workstation.browser.events.lifecycle;

import org.janelia.model.security.Subject;

/**
 * The user session has started. This can happen when the application is opened, 
 * or when an admin chooses to "run as" a new user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionStartEvent extends SessionEvent {

    public SessionStartEvent(Subject subject) {
        super(subject);
    }
}
