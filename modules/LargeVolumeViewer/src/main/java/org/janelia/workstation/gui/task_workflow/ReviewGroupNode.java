package org.janelia.workstation.gui.task_workflow;

/**
 *
 * @author schauderd
 */


import java.awt.Image;
import java.util.List;

import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ReviewGroupNode extends AbstractNode
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private ReviewGroup group;
    
    public ReviewGroupNode(ReviewGroup reviewGroup, int index) {
        super(Children.create(new ReviewGroupNodeFactory(reviewGroup), true), Lookups.singleton(reviewGroup));
        group = reviewGroup;
        updateDisplayName(index);
       
    }
    
    private void updateDisplayName(int index) {
        setDisplayName("Branch" + index); //  (" + workspace.getNeuronSets().size() + " neurons)");
    }
    
     private static class ReviewGroupNodeFactory extends ChildFactory<ReviewPoint>
    {
        private List<ReviewPoint> pointList;
        
        public ReviewGroupNodeFactory(ReviewGroup group) {
            this.pointList = group.getPointList();
        }

        @Override
        protected boolean createKeys(List<ReviewPoint> toPopulate)
        {
            toPopulate.addAll(pointList);
            return true;
        }

        @Override
        protected Node createNodeForKey(ReviewPoint key) {
            return new ReviewPointNode(key);
        }
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/VertexBranch2.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
    
    public boolean getReviewed() {
        return group.isReviewed();
    }
     
    public void setReviewed(boolean reviewed) {
        group.setReviewed(reviewed);
    }
    
    @Override 
    protected Sheet createSheet() { 
        Sheet sheet = Sheet.createDefault(); 
        Sheet.Set set = Sheet.createPropertiesSet(); 
        try { 
            PropertySupport.Reflection reviewProp = new PropertySupport.Reflection(this, boolean.class, "reviewed");
            reviewProp.setPropertyEditorClass(ReviewGroupPropertyEditor.class);            
            set.put(reviewProp);
        } 
        catch (NoSuchMethodException ex) {
            ErrorManager.getDefault(); 
        } 
        sheet.put(set); 
        return sheet; 
    }

}

