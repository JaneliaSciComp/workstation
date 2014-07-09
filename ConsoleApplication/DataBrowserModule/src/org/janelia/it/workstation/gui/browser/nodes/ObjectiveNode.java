package org.janelia.it.workstation.gui.browser.nodes;

import java.lang.ref.WeakReference;
import java.util.List;

import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectiveNode extends InternalNode<String> {
    
    private final static Logger log = LoggerFactory.getLogger(ObjectiveNode.class);
    
    private final WeakReference<Sample> sampleRef;
    
    public ObjectiveNode(Sample sample, String objective) throws Exception {
        super(Children.create(new ObjectiveNode.MyChildFactory(sample, objective), true), objective);
        this.sampleRef = new WeakReference<Sample>(sample);
    }
    
    public String getObjective() {
        return (String)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getObjective();
    }
    
    static class MyChildFactory extends ChildFactory<SamplePipelineRun> {
    
        private final WeakReference<Sample> sampleRef;
        private final WeakReference<String> objective;
        
        public MyChildFactory(Sample sample, String objective) {
            this.sampleRef = new WeakReference<Sample>(sample);
            this.objective = new WeakReference<String>(objective);
        }
        
        @Override
        protected boolean createKeys(List<SamplePipelineRun> list) {
            if (sampleRef==null) return false;
            Sample sample = sampleRef.get();
            if (sample==null) return false;
            ObjectiveSample objectiveSample = sample.getObjectives().get(objective.get());
            if (objectiveSample.getPipelineRuns()!=null) {
                for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                    list.add(run);
                }
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(SamplePipelineRun key) {
            if (sampleRef==null) return null;
            Sample sample = sampleRef.get();
            try {
                return new PipelineResultNode(sample, key);
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

    }
}
