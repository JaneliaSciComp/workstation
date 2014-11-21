package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.lang.ref.WeakReference;
import java.util.List;
import org.janelia.it.jacs.model.domain.compartments.Compartment;
import org.janelia.it.jacs.model.domain.compartments.CompartmentSet;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.util.Icons;

import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CompartmentSetNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(CompartmentSetNode.class);
    
    public CompartmentSetNode(TreeNodeChildFactory parentChildFactory, CompartmentSet compartmentSet) throws Exception {
        super(parentChildFactory, Children.create(new CompartmentSetNode.MyChildFactory(compartmentSet), true), compartmentSet);
    }
    
    public CompartmentSet getCompartmentSet() {
        return (CompartmentSet)getDomainObject();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("matrix.png").getImage();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getCompartmentSet().getName();
    }
    
    static class MyChildFactory extends ChildFactory<Compartment> {
    
        private final WeakReference<CompartmentSet> compartmentSetRef;
        
        public MyChildFactory(CompartmentSet compartmentSetRef) {
            this.compartmentSetRef = new WeakReference<CompartmentSet>(compartmentSetRef);
        }
        
        @Override
        protected boolean createKeys(List<Compartment> list) {
            CompartmentSet compartmentSet = compartmentSetRef.get();
            if (compartmentSet==null) return false;
            
            if (compartmentSet.getCompartments()!=null) {
                list.addAll(compartmentSet.getCompartments());
            }
            
            return true;
        }

        @Override
        protected Node createNodeForKey(Compartment key) {
            try {
                return new CompartmentNode(key);
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

    }
}
