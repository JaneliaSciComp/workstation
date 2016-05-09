package org.janelia.it.workstation.gui.alignment_board.events;

import org.janelia.it.jacs.model.domain.DomainObject;
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
        FilterLevelChange
    }
    
    private final DomainObject domainObject;
    private final ChangeType changeType;
    
	public AlignmentBoardItemChangeEvent(AlignmentBoardContext alignmentBoardContext, DomainObject domainObject, ChangeType changeType) {
		super(alignmentBoardContext);		
		this.domainObject = domainObject;
        this.changeType = changeType;
	}
    
    public DomainObject getDomainObject() {
        return domainObject;
    }

    public ChangeType getChangeType() {
        return changeType;
    }
}
