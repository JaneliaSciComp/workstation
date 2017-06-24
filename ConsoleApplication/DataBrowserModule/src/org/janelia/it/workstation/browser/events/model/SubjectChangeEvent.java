package org.janelia.it.workstation.browser.events.model;

import org.janelia.it.jacs.model.domain.Subject;

/**
 * A user or group has changed. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SubjectChangeEvent {

    private Subject subject;
    
    public SubjectChangeEvent(Subject subject) {
        this.subject = subject;
    }
           
    public Subject getSubject() {
        return subject;
    }
}
