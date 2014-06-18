package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import org.janelia.it.jacs.model.domain.NeuronSeparation;
import org.janelia.it.jacs.model.domain.PipelineResult;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.SampleProcessingResult;
import org.janelia.it.workstation.gui.browser.children.NeuronNodeFactory;
import org.janelia.it.workstation.gui.browser.children.ResultChildFactory;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.BeanNode;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineResultNode extends BeanNode<PipelineResult> {
    
    private final static Logger log = LoggerFactory.getLogger(PipelineResultNode.class);
    
    private final Sample sample;
    
    public PipelineResultNode(Sample sample, PipelineResult result) throws Exception {
        super(result);
        this.sample = sample;
        if (getBean() instanceof NeuronSeparation) {
            setChildren(Children.create(new NeuronNodeFactory(sample, (NeuronSeparation)getBean()), true));   
        }
        else {
            setChildren(Children.create(new ResultChildFactory(sample, (PipelineResult)getBean()), true));
        } 
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("folder_image.png").getImage();
    }
    
    @Override
    public String getHtmlDisplayName() {
        PipelineResult result = getBean();
        if (getBean() != null) {
            String name = "Result";
            if (result instanceof NeuronSeparation) {
                 name = "Neuron Separation";
            }
            else if (result instanceof SampleProcessingResult) {
                name = "Sample Processing";
            }
            else if (result instanceof SampleAlignmentResult) {
                SampleAlignmentResult alignment = (SampleAlignmentResult)result;
                name = "Sample Alignment";
            }
            
            return "<font color='!textText'>" + name + "</font>" +
                    " <font color='#957D47'><i>" + getBean().getCreationDate() + "</i></font>";
        } else {
            return null;
        }
    }
    
}
