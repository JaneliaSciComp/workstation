package org.janelia.it.workstation.gui.browser.nodes;

import java.awt.Image;
import java.lang.ref.WeakReference;
import java.util.List;
import org.janelia.it.jacs.model.domain.ObjectiveSample;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.jacs.model.domain.SamplePipelineRun;
import org.janelia.it.workstation.gui.util.Icons;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectiveNode extends InternalNode<String> {
    
    private final static Logger log = LoggerFactory.getLogger(ObjectiveNode.class);
    
    private final WeakReference<Sample> sampleRef;
    
    public ObjectiveNode(Sample sample, String objective) throws Exception {
        super(objective);
        this.sampleRef = new WeakReference<Sample>(sample);
        setChildren(Children.create(new ObjectiveNode.MyChildFactory(), true));
    }
    
    public String getObjective() {
        return getBean();
    }
    
    @Override
    public String getPrimaryLabel() {
        return getObjective();
    }
    
    @Override
    public Image getIcon(int type) {
        return Icons.getIcon("page_white.png").getImage();
    }
    
    public class MyChildFactory extends ChildFactory<SamplePipelineRun> {
    
        @Override
        protected boolean createKeys(List<SamplePipelineRun> list) {
            if (sampleRef==null) return false;
            Sample sample = sampleRef.get();
            if (sample==null) return false;
            ObjectiveSample objectiveSample = sample.getObjectives().get(getObjective());
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
