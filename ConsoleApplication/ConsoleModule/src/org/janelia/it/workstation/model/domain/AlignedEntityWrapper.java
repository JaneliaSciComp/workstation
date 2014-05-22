package org.janelia.it.workstation.model.domain;

import org.janelia.it.workstation.model.entity.RootedEntity;

public class AlignedEntityWrapper extends EntityWrapper {

    private org.janelia.it.workstation.model.domain.AlignmentContext alignmentContext;
    
    public AlignedEntityWrapper(RootedEntity entity) {
        super(entity);
    }

    public org.janelia.it.workstation.model.domain.AlignmentContext getAlignmentContext() {
        return alignmentContext;
    }

    protected void setAlignmentContext(org.janelia.it.workstation.model.domain.AlignmentContext alignmentContext) {
        this.alignmentContext = alignmentContext;
    }
}
