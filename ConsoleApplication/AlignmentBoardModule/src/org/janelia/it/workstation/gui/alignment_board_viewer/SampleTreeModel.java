package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.AlignmentBoardContext;
import org.janelia.it.jacs.model.domain.gui.alignment_board.AlignmentBoardItem;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.alignment_board.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.model.viewer.AlignedItem.InclusionStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A tree model for the alignment board Layers panel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTreeModel implements TreeModel {

    private static final Logger log = LoggerFactory.getLogger(SampleTreeModel.class);
    public static final String ITEMS_OMITTED_MENU_ITEM_TEXT = " items below cutoff";

    private final AlignmentBoardContext alignmentBoardContext;
    
    private final List<TreeModelListener> listeners = new ArrayList<>();
    
    public SampleTreeModel(AlignmentBoardContext alignmentBoardContext) {
        this.alignmentBoardContext = alignmentBoardContext;
        log.debug("Creating SampleTreeModel for alignment board context id={}",alignmentBoardContext.getAlignmentBoard().getId());
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object child = null;
        AlignmentBoardItem item = (AlignmentBoardItem)parent;
        if ( item != null ) {
            if ( item.getChildren() != null ) {
                List<AlignmentBoardItem> nonExcludedChildren = getNonExcludedChildren( item );
                if ( index == nonExcludedChildren.size() ) {
                    // Special case: the warning blurb.
                    int count = item.getChildren().size() - nonExcludedChildren.size();
                    child = count + ITEMS_OMITTED_MENU_ITEM_TEXT;
                }
                else if ( index < nonExcludedChildren.size() ) {
                    child = nonExcludedChildren.get( index );
                }
                else {
                    // Calling tree is working from an obsolete child count.
                    new Thread( new Runnable() {
                        public void run() {
                            AlignmentBoardItemChangeEvent event = 
                                new AlignmentBoardItemChangeEvent(
                                    AlignmentBoardMgr.getInstance().getLayersPanel()
                                            .getAlignmentBoardContext(),
                                    null, 
                                    AlignmentBoardItemChangeEvent.ChangeType
                                            .FilterLevelChange
                            );
                            ModelMgr.getModelMgr().postOnEventBus(event);
                        }
                    }).start();
                }
            }
        }
        log.debug(index+"th child of {} is {}",parent, child);
        
        if ( child == null ) {
            return ""; // Change to empty child.
        }
        return child;
    }

    @Override
    public int getChildCount(Object parent) {
        int count = 0;
        if ( parent != null  &&   parent instanceof AlignmentBoardItem ) {
            AlignmentBoardItem item = (AlignmentBoardItem) parent;
            if (item.getChildren() != null) {
                List<AlignmentBoardItem> nonExcludedChildren = getNonExcludedChildren(item);
                count = nonExcludedChildren.size();
                // Anything actually excluded -> add one more item.
                if (nonExcludedChildren.size() < item.getChildren().size()) {
                    count++;
                }
            }
        }
        log.debug("{} has {} children", parent, count);
        return count;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        AlignmentBoardItem item = (AlignmentBoardItem)parent;
        int index = 0;
        if (item != null) {
            if ( item.getChildren() != null ) {
                List<AlignmentBoardItem> nonExcludedChildren = getNonExcludedChildren( item );
                index = nonExcludedChildren.indexOf( child );
                if ( index == -1  &&  ("" + child).endsWith( ITEMS_OMITTED_MENU_ITEM_TEXT ) ) {
                    return nonExcludedChildren.size();
                }
            }
        }
        log.debug("{} is {}th child of "+parent, child, index);
        return index;
    }

    @Override
    public Object getRoot() {
        return alignmentBoardContext;
    }

    @Override
    public boolean isLeaf(Object node) {
        return getChildCount(node)==0;
    }

    @Override
    public void addTreeModelListener(TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(TreeModelListener l) {
        listeners.remove(l);
    }
    
    @Override
    public void valueForPathChanged(TreePath path, Object newValue) {
        throw new AssertionError("This method should never be called");
    }

    private List<AlignmentBoardItem> getNonExcludedChildren( AlignmentBoardItem item ) {
        List<AlignmentBoardItem> rtnVal = new ArrayList<>();
        if ( item.getChildren() != null ) {
            // Pushing to new connection to avoid concurrent modification.
            for ( AlignmentBoardItem nextChild: new ArrayList<>( item.getChildren() ) ) {
                if ( nextChild instanceof AlignmentBoardItem ) {
                    AlignmentBoardItem nextItem = (AlignmentBoardItem)nextChild;
                    if ( InclusionStatus.In == InclusionStatus.get(nextItem.getInclusionStatus()) ) {
                        rtnVal.add( nextChild );
                    }
                }
                else {
                    // No exclusion based on type of child.  Non-aligned items always have presence.
                    rtnVal.add( nextChild );
                }

            }
        }
        return rtnVal;
    }
}