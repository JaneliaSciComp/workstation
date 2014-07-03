package org.janelia.it.workstation.gui.browser.nodes.children;

import java.lang.ref.WeakReference;
import java.util.List;

import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.workstation.gui.browser.nodes.PipelineResultNode;
import org.janelia.it.workstation.gui.browser.nodes.children.NeuronNodeFactory;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResultChildFactory extends ChildFactory<PipelineResult> {

    private final static Logger log = LoggerFactory.getLogger(NeuronNodeFactory.class);

    private final WeakReference<Sample> sampleRef;
    private final WeakReference<PipelineResult> pipelineResultRef;

    public ResultChildFactory(Sample sample, PipelineResult pipelineResultRef) {
        this.sampleRef = new WeakReference<Sample>(sample);
        this.pipelineResultRef = new WeakReference<PipelineResult>(pipelineResultRef);
    }
    
    @Override
    protected boolean createKeys(List<PipelineResult> list) {
        PipelineResult result = pipelineResultRef.get();
        if (result==null) return false;
        if (result.getResults() != null) {
            list.addAll(result.getResults());
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(PipelineResult key) {
        Sample sample = sampleRef.get();
        if (sample==null) return null;
        try {
            return new PipelineResultNode(sample, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
}
