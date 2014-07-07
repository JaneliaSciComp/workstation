package org.janelia.it.workstation.gui.browser.nodes;

import java.lang.ref.WeakReference;

import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleAlignmentResult;
import org.janelia.it.jacs.model.domain.sample.SampleCellCountingResult;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.jacs.model.domain.sample.SampleProcessingResult;
import org.janelia.it.workstation.gui.browser.nodes.children.NeuronNodeFactory;
import org.janelia.it.workstation.gui.browser.nodes.children.ResultChildFactory;
import org.openide.nodes.Children;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PipelineResultNode extends InternalNode<PipelineResult> {
    
    private final static Logger log = LoggerFactory.getLogger(PipelineResultNode.class);
    
    private final WeakReference<Sample> sampleRef;
    
    public PipelineResultNode(Sample sample, PipelineResult result) throws Exception {
        super(result);
        this.sampleRef = new WeakReference<Sample>(sample);
        if (getBean() instanceof NeuronSeparation) {
            setChildren(Children.create(new NeuronNodeFactory(sample, (NeuronSeparation)getBean()), true));   
        }
        else {
            setChildren(Children.create(new ResultChildFactory(sample, (PipelineResult)getBean()), true));
        } 
    }
    
    private PipelineResult getPipelineResult() {
        return (PipelineResult)getBean();
    }
    
    @Override
    public String getPrimaryLabel() {
        String name = "Result";
        PipelineResult result = getPipelineResult();
        if (getBean() != null) {
            if (result instanceof SamplePipelineRun) {
                SamplePipelineRun run = (SamplePipelineRun)result;
                name = run.getName();
            }
            else if (result instanceof NeuronSeparation) {
                name = "Neuron Separation";
            }
            else if (result instanceof SampleProcessingResult) {
                name = "Sample Processing";
            }
            else if (result instanceof SampleCellCountingResult) {
                SampleCellCountingResult cellCountingResult = (SampleCellCountingResult)result;
                name = cellCountingResult.getName();
            }
            else if (result instanceof SampleAlignmentResult) {
                SampleAlignmentResult alignment = (SampleAlignmentResult)result;
                name = alignment.getName();
            }
            else {
                name = result.getClass().getName();
            }
        }
        return name;
    }
    
    @Override
    public String getSecondaryLabel() {
        return getBean().getCreationDate()+"";
    }
}
