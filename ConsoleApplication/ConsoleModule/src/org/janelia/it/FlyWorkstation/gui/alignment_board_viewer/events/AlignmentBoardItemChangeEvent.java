package org.janelia.it.FlyWorkstation.gui.alignment_board_viewer.events;

import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

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
        ColorChange,
        FilterLevelChange
    }
    
    private final AlignedItem alignedItem;
    private final ChangeType changeType;
    
	public AlignmentBoardItemChangeEvent(AlignmentBoardContext alignmentBoardContext, AlignedItem alignedItem, ChangeType changeType) {
		super(alignmentBoardContext);		
		this.alignedItem = alignedItem;
        this.changeType = changeType;
	}

    public AlignedItem getAlignedItem() {
        return alignedItem;
    }

    public ChangeType getChangeType() {
        return changeType;
    }
}
