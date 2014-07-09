package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;

import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.nodes.children.NeuronNodeFactory;
import org.janelia.it.workstation.gui.browser.nodes.children.ObjectiveNodeFactory;
import org.janelia.it.workstation.gui.browser.nodes.children.TreeNodeChildFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SampleNode extends DomainObjectNode {
    
    private final static Logger log = LoggerFactory.getLogger(SampleNode.class);
    
    public SampleNode(TreeNodeChildFactory parentChildFactory, Sample sample) throws Exception {
        super(parentChildFactory, 
                DomainExplorerTopComponent.isShowNeurons()
                        ?Children.create(new NeuronNodeFactory(sample), true)
                        :Children.create(new ObjectiveNodeFactory(sample), true), sample);
    }
    
    private Sample getSample() {
        return (Sample)getDomainObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSample().getName();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("beaker.png").getImage();
    }
}
