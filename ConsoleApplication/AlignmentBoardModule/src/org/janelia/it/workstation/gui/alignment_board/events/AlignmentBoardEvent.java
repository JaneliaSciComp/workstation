package org.janelia.it.workstation.gui.alignment_board.events;

import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;

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
