package org.janelia.it.FlyWorkstation.gui.framework.viewer.alignment_board;

import javax.swing.tree.TreeModel;

import org.janelia.it.FlyWorkstation.model.viewer.AlignedItem;
import org.janelia.it.FlyWorkstation.model.viewer.AlignmentBoardContext;

/**
 * A tree model for the alignment board Layers panel. 
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class SampleTreeModel implements TreeModel {
    
    private AlignmentBoardContext alignmentBoardContext;
    
    public SampleTreeModel(AlignmentBoardContext alignmentBoardContext) {
        this.alignmentBoardContext = alignmentBoardContext;
    }

    @Override
    public void addTreeModelListener(javax.swing.event.TreeModelListener l) {
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
    public void removeTreeModelListener(javax.swing.event.TreeModelListener l) {
    }

    @Override
    public void valueForPathChanged(javax.swing.tree.TreePath path, Object newValue) {
    }
}