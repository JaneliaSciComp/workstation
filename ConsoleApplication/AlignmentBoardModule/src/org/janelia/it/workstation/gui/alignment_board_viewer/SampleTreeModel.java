package org.janelia.it.workstation.gui.alignment_board_viewer;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.janelia.it.workstation.api.entity_model.management.ModelMgr;
import org.janelia.it.workstation.gui.alignment_board.ab_mgr.AlignmentBoardMgr;
import org.janelia.it.workstation.gui.viewer3d.events.AlignmentBoardItemChangeEvent;
import org.janelia.it.workstation.model.domain.EntityWrapper;
import org.janelia.it.workstation.model.viewer.AlignedItem;
import org.janelia.it.workstation.model.viewer.AlignmentBoardContext;
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

    private AlignmentBoardContext alignmentBoardContext;
    
    private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
    
    public SampleTreeModel(AlignmentBoardContext alignmentBoardContext) {
        this.alignmentBoardContext = alignmentBoardContext;
        log.debug("Creating SampleTreeModel for alignment board context id={}",alignmentBoardContext.getId());
    }

    @Override
    public Object getChild(Object parent, int index) {
        Object child = null;
        AlignedItem item = (AlignedItem)parent;
        if ( item != null ) {
            if ( item.getChildren() != null ) {
                List<EntityWrapper> nonExcludedChildren = getNonExcludedChildren( item );
                if ( index == nonExcludedChildren.size() ) {
                    // Special case: the warning blurb.
                    int count = item.getChildren().size() - nonExcludedChildren.size();
                    child = new String( count + ITEMS_OMITTED_MENU_ITEM_TEXT );
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
        if ( parent instanceof AlignedItem ) {
            AlignedItem item = (AlignedItem)parent;
            if (item != null) {
                if ( item.getChildren() != null ) {
                    List<EntityWrapper> nonExcludedChildren = getNonExcludedChildren( item );
                    count = nonExcludedChildren.size();
                    // Anything actually excluded -> add one more item.
                    if ( nonExcludedChildren.size() < item.getChildren().size() ) {
                        count ++;
                    }
                }
            }
        }
        log.debug("{} has {} children", parent, count);
        return count;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        AlignedItem item = (AlignedItem)parent;
        int index = 0;
        if (item != null) {
            if ( item.getChildren() != null ) {
                List<EntityWrapper> nonExcludedChildren = getNonExcludedChildren( item );
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

    private List<EntityWrapper> getNonExcludedChildren( AlignedItem item ) {
        List<EntityWrapper> rtnVal = new ArrayList<EntityWrapper>();
        if ( item.getChildren() != null ) {
            // Pushing to new connection to avoid concurrent modification.
            for ( EntityWrapper nextChild: new ArrayList<EntityWrapper>( item.getChildren() ) ) {
                if ( nextChild instanceof AlignedItem ) {
                    AlignedItem nextItem = (AlignedItem)nextChild;
                    if ( AlignedItem.InclusionStatus.In.equals( nextItem.getInclusionStatus() ) ) {
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