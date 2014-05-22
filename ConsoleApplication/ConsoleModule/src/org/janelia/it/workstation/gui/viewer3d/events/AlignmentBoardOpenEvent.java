package org.janelia.it.workstation.gui.viewer3d.events;

/**
 * The given alignment board has just been opened.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentBoardOpenEvent extends AlignmentBoardEvent {
    
	public AlignmentBoardOpenEvent(org.janelia.it.workstation.model.viewer.AlignmentBoardContext alignmentBoardContext) {
		super(alignmentBoardContext);		
	}
}
