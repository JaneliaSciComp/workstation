package org.janelia.workstation.gui.task_workflow;

/**
 *
 * @author schauderd
 */


import java.awt.Image;
import java.util.List;

import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;
import org.openide.util.lookup.Lookups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 */
public class ReviewListNode extends AbstractNode
{
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    
    public ReviewListNode(String name, List<ReviewGroup> groupList) {
        super(Children.create(new ReviewListChildNodeFactory(groupList), true), Lookups.singleton(groupList));
        updateDisplayName(name);
       
    }
    
    private void updateDisplayName(String name) {
        setDisplayName(name); 
    }
    
     private static class ReviewListChildNodeFactory extends ChildFactory<ReviewGroup>
    {
        private List<ReviewGroup> groupList;
        int index=0;
        
        public ReviewListChildNodeFactory(List<ReviewGroup> group) {
            this.groupList = group;
        }

        @Override
        protected boolean createKeys(List toPopulate)
        {
            toPopulate.addAll(groupList);
            return true;
        }

        @Override
        protected Node createNodeForKey(ReviewGroup key) {            
            return new ReviewGroupNode(key, index++);
        }
    }
    
    @Override
    public Image getIcon(int type) {
        return ImageUtilities.loadImage("org/janelia/horta/images/neuron1.png");
    }
    
    @Override
    public Image getOpenedIcon(int i) {
        return getIcon(i);
    }
}


