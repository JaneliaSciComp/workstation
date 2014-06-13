package org.janelia.it.workstation.model.domain;

import org.janelia.it.workstation.model.entity.RootedEntity;

public class AlignedEntityWrapper extends EntityWrapper {

    private AlignmentContext alignmentContext;
    
    public AlignedEntityWrapper(RootedEntity entity) {
        super(entity);
    }

    public AlignmentContext getAlignmentContext() {
        return alignmentContext;
    }

    protected void setAlignmentContext(AlignmentContext alignmentContext) {
        this.alignmentContext = alignmentContext;
    }
}
