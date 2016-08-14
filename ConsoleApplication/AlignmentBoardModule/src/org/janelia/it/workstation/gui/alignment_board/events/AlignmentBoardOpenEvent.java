package org.janelia.it.workstation.gui.alignment_board.events;

import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;

/**
 * The given alignment board has just been opened.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentBoardOpenEvent extends AlignmentBoardEvent {
    
	public AlignmentBoardOpenEvent(AlignmentBoardContext alignmentBoardContext) {
		super(alignmentBoardContext);		
	}
}
