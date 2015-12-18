package org.janelia.it.workstation.gui.browser.events.selection;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;

public class PipelineResultSelectionEvent extends DomainObjectSelectionEvent {

    private PipelineResult result;
    
    public PipelineResultSelectionEvent(Object source, DomainObject domainObject, PipelineResult result) {
        super(source, domainObject, true, true);
    }

    public PipelineResult getResult() {
        return result;
    }
}
