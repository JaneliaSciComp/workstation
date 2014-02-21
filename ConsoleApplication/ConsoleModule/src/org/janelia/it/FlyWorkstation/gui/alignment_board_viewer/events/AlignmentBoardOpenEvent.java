package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.events;

import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

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
