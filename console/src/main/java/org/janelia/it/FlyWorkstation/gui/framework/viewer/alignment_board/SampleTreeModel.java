package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import java.util.ArrayList;
import java.util.List;

import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;

import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events.AlignmentBoardItemChangeEvent.ChangeType;
import org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board.events.AlignmentBoardItemRemoveEvent;
import org.janelia.it.FlyWorkstation.model.domain.EntityWrapper;
import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.Subscribe;

/**
 * A tree model for the alignment board Layers panel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTreeModel implements TreeModel {

    private static final Logger log = LoggerFactory.getLogger(SampleTreeModel.class);
    
    private AlignmentBoardContext alignmentBoardContext;
    
    private List<TreeModelListener> listeners = new ArrayList<TreeModelListener>();
    
    public SampleTreeModel(AlignmentBoardContext alignmentBoardContext) {
        this.alignmentBoardContext = alignmentBoardContext;
    }

    @Override
    public Object getChild(Object parent, int index) {
        AlignedItem item = (AlignedItem)parent;
        if (item==null) return null;
        Object child = item.getChildren()==null?0:item.getChildren().get(index);
        return child;
    }

    @Override
    public int getChildCount(Object parent) {
        AlignedItem item = (AlignedItem)parent;
        if (item==null) return 0;
        int count = item.getChildren()==null?0:item.getChildren().size();
        return count;
    }

    @Override
    public int getIndexOfChild(Object parent, Object child) {
        AlignedItem item = (AlignedItem)parent;
        if (item==null) return 0;
        int index = item.getChildren()==null?0:item.getChildren().indexOf(child);
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
    public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
        listeners.add(l);
    }

    @Override
    public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
        listeners.remove(l);
    }
    
    @Override
    public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
        throw new AssertionError("This method should never be called");
    }
}