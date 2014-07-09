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
        super((result instanceof NeuronSeparation)
                ?Children.create(new NeuronNodeFactory(sample, (NeuronSeparation)result), true)
                :Children.create(new ResultChildFactory(sample, (PipelineResult)result), true), result);
        this.sampleRef = new WeakReference<Sample>(sample);
    }
    
    private PipelineResult getPipelineResult() {
        return (PipelineResult)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        String name = "Result";
        PipelineResult result = getPipelineResult();
        if (getObject() != null) {
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
        return getObject().getCreationDate()+"";
    }
}
