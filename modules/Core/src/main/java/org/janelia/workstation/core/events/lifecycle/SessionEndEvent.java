package org.janelia.workstation.core.events.lifecycle;

import org.janelia.model.security.Subject;

/**
 * The user session has ended. This can happen if the application is closed, or if 
 * an admin chooses to "run as" a new user.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SessionEndEvent extends SessionEvent {

    public SessionEndEvent(Subject subject) {
        super(subject);
    }
}
