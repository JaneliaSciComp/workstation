package org.janelia.it.workstation.gui.browser.events.lifecycle;

import org.janelia.it.jacs.model.domain.Subject;

/**
 * The running user has changed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class RunAsEvent extends SessionEvent {

    public RunAsEvent(Subject subject) {
        super(subject);
    }
}
