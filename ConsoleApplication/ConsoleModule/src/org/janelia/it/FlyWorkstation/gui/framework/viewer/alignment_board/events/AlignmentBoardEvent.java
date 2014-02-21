package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events;

import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

/**
 * An even concerned a single alignment board.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public abstract class AlignmentBoardEvent { 
    
    private final AlignmentBoardContext alignmentBoardContext;
    
	public AlignmentBoardEvent(AlignmentBoardContext alignmentBoardContext) {
		this.alignmentBoardContext = alignmentBoardContext;		
	}
	
	public AlignmentBoardContext getAlignmentBoardContext() {
	    return alignmentBoardContext;
	}
}
