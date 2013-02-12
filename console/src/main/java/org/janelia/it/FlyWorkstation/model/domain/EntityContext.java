package org.janelia.it.FlyWorkstation.model.domain;

/**
 * A context in which wrapped entity trees are populated. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class EntityContext {
    
    private AlignmentContext alignmentContext;

    public AlignmentContext getAlignmentContext() {
        return alignmentContext;
    }

    public void setAlignmentContext(AlignmentContext alignmentContext) {
        this.alignmentContext = alignmentContext;
    }
}
