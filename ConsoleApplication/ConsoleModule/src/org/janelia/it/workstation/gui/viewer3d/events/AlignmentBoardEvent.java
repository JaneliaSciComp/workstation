package org.janelia.it.workstation.gui.viewer3d.events;

/**
 * An even concerned a single alignment board.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AlignmentBoardEvent { 
    
    private final org.janelia.it.workstation.model.viewer.AlignmentBoardContext alignmentBoardContext;
    
	public AlignmentBoardEvent(org.janelia.it.workstation.model.viewer.AlignmentBoardContext alignmentBoardContext) {
		this.alignmentBoardContext = alignmentBoardContext;		
	}
	
	public org.janelia.it.workstation.model.viewer.AlignmentBoardContext getAlignmentBoardContext() {
	    return alignmentBoardContext;
	}
}
