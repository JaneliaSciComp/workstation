package org.janelia.it.workstation.gui.browser.children;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.List;
import org.janelia.it.jacs.model.domain.Sample;
import org.janelia.it.workstation.gui.browser.nodes.ObjectiveNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class ObjectiveNodeFactory extends ChildFactory<String> {

    private final static Logger log = LoggerFactory.getLogger(ObjectiveNodeFactory.class);
    
    private final WeakReference<Sample> sampleRef;
    
    public ObjectiveNodeFactory(Sample sample) {
        this.sampleRef = new WeakReference<Sample>(sample);
    }
    
    @Override
    protected boolean createKeys(List<String> list) {
        Sample sample = sampleRef.get();
        if (sample==null) return false;
        for(String objective : sample.getObjectives().keySet()) {
            list.add(objective);
        }
        Collections.sort(list);
        return true;
    }

    @Override
    protected Node createNodeForKey(String key) {
        Sample sample = sampleRef.get();
        if (sample==null) return null;
        try {
            return new ObjectiveNode(sample, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
}
