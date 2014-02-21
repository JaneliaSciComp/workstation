package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events;

import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

/**
 * An item on an open alignment board has changed in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentBoardItemRemoveEvent extends AlignmentBoardItemChangeEvent {
    
    private Integer previousOrderIndex;
    
	public AlignmentBoardItemRemoveEvent(AlignmentBoardContext alignmentBoardContext, AlignedItem alignedItem, Integer previousOrderIndex) {
		super(alignmentBoardContext, alignedItem, ChangeType.Removed);		
		this.previousOrderIndex = previousOrderIndex;
	}

    public Integer getPreviousOrderIndex() {
        return previousOrderIndex;
    }	
}
