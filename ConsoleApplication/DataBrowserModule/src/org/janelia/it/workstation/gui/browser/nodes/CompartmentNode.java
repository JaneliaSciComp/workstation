package org.janelia.it.workstation.gui.browser.nodes;

import org.janelia.it.jacs.model.domain.compartments.Compartment;

import org.openide.nodes.Children;

public class CompartmentNode extends InternalNode<Compartment> {
    
    public CompartmentNode(Compartment compartment) throws Exception {
        super(Children.LEAF, compartment);
    }
    
    public Compartment getCompartment() {
        return (Compartment)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getCompartment().getName();
    }
    
    @Override
    public String getSecondaryLabel() {
        return getCompartment().getCode();
    }
}
