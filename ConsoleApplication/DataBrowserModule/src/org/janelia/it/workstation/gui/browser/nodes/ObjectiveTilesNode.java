package org.janelia.it.workstation.gui.browser.nodes;

import java.lang.ref.WeakReference;
import java.util.List;

import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SampleTile;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ObjectiveTilesNode extends InternalNode<String> {
    
    private final static Logger log = LoggerFactory.getLogger(ObjectiveTilesNode.class);
    
    private final WeakReference<Sample> sampleRef;
    
    public ObjectiveTilesNode(Sample sample, String objective) throws Exception {
        super(Children.create(new ObjectiveTilesNode.MyChildFactory(sample, objective), true), objective);
        this.sampleRef = new WeakReference<Sample>(sample);
    }
    
    public String getObjective() {
        return (String)getObject();
    }
    
    @Override
    public String getPrimaryLabel() {
        return "Tiles";
    }
    
    static class MyChildFactory extends ChildFactory<SampleTile> {
    
        private final WeakReference<Sample> sampleRef;
        private final WeakReference<String> objectiveRef;
        
        public MyChildFactory(Sample sample, String objective) {
            this.sampleRef = new WeakReference<Sample>(sample);
            this.objectiveRef = new WeakReference<String>(objective);
        }
        
        @Override
        protected boolean createKeys(List<SampleTile> list) {
            Sample sample = sampleRef.get();
            if (sample==null) return false;
            String objective = objectiveRef.get();
            if (objective==null) return false;
            ObjectiveSample objectiveSample = sample.getObjectives().get(objective);
            if (objectiveSample.getTiles()!=null) {
                list.addAll(objectiveSample.getTiles());
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(SampleTile key) {
            Sample sample = sampleRef.get();
            if (sample==null) return null;
            try {
                return new ObjectiveTileNode(sample, key);
            }
            catch (Exception e) {
                log.error("Error creating node for key " + key, e);
            }
            return null;
        }

    }
}
