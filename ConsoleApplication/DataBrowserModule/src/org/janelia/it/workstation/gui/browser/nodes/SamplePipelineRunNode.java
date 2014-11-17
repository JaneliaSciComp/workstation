package org.janelia.it.workstation.gui.browser.nodes;

import java.lang.ref.WeakReference;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;

import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.janelia.it.workstation.gui.browser.nodes.children.ResultChildFactory;
import org.openide.nodes.Children;

public class SamplePipelineRunNode extends InternalNode<SamplePipelineRun> {
    
    private final WeakReference<Sample> sampleRef;
    
    public SamplePipelineRunNode(Sample sample, SamplePipelineRun run) throws Exception {
        super(DomainUtils.isEmpty(run.getResults())?Children.LEAF:Children.create(new ResultChildFactory(sample, run), true), run);
        this.sampleRef = new WeakReference<Sample>(sample);
    }
    
    public SamplePipelineRun getSamplePipelineRun() {
        return (SamplePipelineRun)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getSamplePipelineRun().getName();
    }
    
    @Override
    public String getSecondaryLabel() {
        return getSamplePipelineRun().getCreationDate()+"";
    }
    
    @Override
    public String get2dImageFilepath(String role) {
        SamplePipelineRun run = getSamplePipelineRun();
        HasFiles lastResult = null;
        for(PipelineResult result : run.getResults()) {
            if (result instanceof HasFiles) {
                lastResult = (HasFiles)result;
            }
        }
        if (lastResult!=null) {
            return DomainUtils.get2dImageFilepath(lastResult, role);
        }
        return null;
    }
}
