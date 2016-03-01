package org.janelia.it.workstation.gui.browser.events.lifecycle;

import org.janelia.it.jacs.model.domain.Subject;

/**
 * A new user has logged in.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class LoginEvent extends SessionEvent {

    public LoginEvent(Subject subject) {
        super(subject);
    }
}
