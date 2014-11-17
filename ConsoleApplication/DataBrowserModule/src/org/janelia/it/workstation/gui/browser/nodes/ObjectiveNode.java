package org.janelia.it.workstation.gui.browser.nodes;

import java.lang.ref.WeakReference;
import java.util.List;
import org.janelia.it.jacs.model.domain.interfaces.HasFiles;

import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.api.DomainUtils;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectiveNode extends InternalNode<String> implements Has2dRepresentation {
    
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
    
    static class MyChildFactory extends ChildFactory<Object> {
    
        private final WeakReference<Sample> sampleRef;
        private final WeakReference<String> objectiveRef;
        
        public MyChildFactory(Sample sample, String objective) {
            this.sampleRef = new WeakReference<Sample>(sample);
            this.objectiveRef = new WeakReference<String>(objective);
        }
        
        @Override
        protected boolean createKeys(List<Object> list) {
            Sample sample = sampleRef.get();
            if (sample==null) return false;
            String objective = objectiveRef.get();
            if (objective==null) return false;
            ObjectiveSample objectiveSample = sample.getObjectives().get(objective);
            
            if (objectiveSample.getTiles()!=null) {
                list.add(objectiveSample.getTiles());
            }
            
            if (objectiveSample.getPipelineRuns()!=null) {
                for(SamplePipelineRun run : objectiveSample.getPipelineRuns()) {
                    list.add(run);
                }
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(Object key) {
            if (sampleRef==null) return null;
            Sample sample = sampleRef.get();
            if (objectiveRef==null) return null;
            String objective = objectiveRef.get();
            try {
                if (key instanceof PipelineResult) {
                    return new PipelineResultNode(sample, (PipelineResult)key);
                }
                else if (key instanceof List) {
                    return new ObjectiveTilesNode(sample, objective);
                }
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

    }
    
    @Override
    public String get2dImageFilepath(String role) {
        Sample sample = sampleRef.get();
        if (sample==null) return null;
        ObjectiveSample objSample = sample.getObjectives().get(getObjective());
        if (objSample==null) return null;
        SamplePipelineRun run = objSample.getLatestRun();
        HasFiles lastResult = null;
        if (run==null) return null;
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
