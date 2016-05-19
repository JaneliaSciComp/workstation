package org.janelia.it.workstation.gui.alignment_board.events;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;

/**
 * An item on an open alignment board has changed in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentBoardItemRemoveEvent extends AlignmentBoardItemChangeEvent {
    
    private Integer previousOrderIndex;
    
	public AlignmentBoardItemRemoveEvent(AlignmentBoardContext alignmentBoardContext, AlignmentBoardItem domainObject, Integer previousOrderIndex) {
		super(alignmentBoardContext, domainObject, ChangeType.Removed);		
		this.previousOrderIndex = previousOrderIndex;
	}

    public Integer getPreviousOrderIndex() {
        return previousOrderIndex;
    }	
}
