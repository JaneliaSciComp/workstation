package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events;

import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

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
