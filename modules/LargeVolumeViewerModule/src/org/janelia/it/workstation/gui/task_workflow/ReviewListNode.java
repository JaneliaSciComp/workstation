package org.janelia.it.workstation.gui.task_workflow;

/**
 *
 * @author schauderd
 */


import java.awt.Color;
import java.awt.Image;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Observable;
import java.util.Observer;
import org.janelia.console.viewerapi.ObservableInterface;
import org.janelia.console.viewerapi.model.NeuronSet;
import org.janelia.console.viewerapi.model.VantageInterface;
import org.janelia.console.viewerapi.model.HortaMetaWorkspace;
import org.janelia.console.viewerapi.model.NeuronModel;
import org.openide.ErrorManager;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Exceptions;
import org.openide.util.ImageUtilities;
import org.openide.util.datatransfer.PasteType;
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


