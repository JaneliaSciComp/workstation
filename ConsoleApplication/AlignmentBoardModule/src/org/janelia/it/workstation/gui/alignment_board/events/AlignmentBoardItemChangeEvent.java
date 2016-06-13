package org.janelia.it.workstation.gui.alignment_board.events;

import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;

/**
 * An item on an open alignment board has changed in some way.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class AlignmentBoardItemChangeEvent extends AlignmentBoardEvent {
   
    public enum ChangeType {
        Added,
        Removed,
        VisibilityChange,
        OverlapFilter,
        ColorChange,
		NameChange,
        FilterLevelChange
    }
    
    private final AlignmentBoardItem domainObject;
    private final ChangeType changeType;
    
	public AlignmentBoardItemChangeEvent(AlignmentBoardContext alignmentBoardContext, AlignmentBoardItem item, ChangeType changeType) {
		super(alignmentBoardContext);		
		this.domainObject = item;
        this.changeType = changeType;
	}
    
    public AlignmentBoardItem getItem() {
        return domainObject;
    }

    public ChangeType getChangeType() {
        return changeType;
    }
}
