package org.janelia.it.workstation.gui.alignment_board.events;

import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;

/**
 * The given alignment board has just been closed.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentBoardCloseEvent extends AlignmentBoardEvent {
    
	public AlignmentBoardCloseEvent(AlignmentBoardContext alignmentBoardContext) {
		super(alignmentBoardContext);		
	}
}
