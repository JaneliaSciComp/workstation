package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import org.janelia.it.jacs.model.domain.NeuronFragment;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;

public class NeuronFragmentNode extends DomainObjectNode {
    
    public NeuronFragmentNode(ChildFactory parentChildFactory, NeuronFragment neuronFragment) throws Exception {
        super(parentChildFactory, neuronFragment);
    }
    
    private NeuronFragment getNeuronFragment() {
        return (NeuronFragment)getBean();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("brick.png").getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        if (getBean() != null) {
            return "<font color='!Label.foreground'>Neuron Fragment " + getNeuronFragment().getNumber() + "</font>" +
                    " <font color='#957D47'><i>" + getNeuronFragment().getOwnerKey() + "</i></font>";
        } else {
            return null;
        }
    }
}
