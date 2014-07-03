package org.janelia.it.workstation.gui.browser.nodes.children;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.janelia.it.jacs.model.domain.DomainObject;
import org.janelia.it.jacs.model.domain.sample.NeuronFragment;
import org.janelia.it.jacs.model.domain.sample.NeuronSeparation;
import org.janelia.it.jacs.model.domain.sample.ObjectiveSample;
import org.janelia.it.jacs.model.domain.sample.PipelineResult;
import org.janelia.it.jacs.model.domain.sample.Sample;
import org.janelia.it.jacs.model.domain.sample.SamplePipelineRun;
import org.janelia.it.workstation.gui.browser.api.DomainDAO;
import org.janelia.it.workstation.gui.browser.components.DomainExplorerTopComponent;
import org.janelia.it.workstation.gui.browser.nodes.NeuronFragmentNode;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class NeuronNodeFactory extends ChildFactory<NeuronFragment> {

    private final static Logger log = LoggerFactory.getLogger(NeuronNodeFactory.class);

    private final WeakReference<Sample> sampleRef;
    private final WeakReference<NeuronSeparation> separationRef;

    public NeuronNodeFactory(Sample sample) {
        this.sampleRef = new WeakReference<Sample>(sample);
        this.separationRef = null;
    }
    
    public NeuronNodeFactory(Sample sampleRef, NeuronSeparation separation) {
        this.sampleRef = new WeakReference<Sample>(sampleRef);
        this.separationRef = new WeakReference<NeuronSeparation>(separation);
    }

    @Override
    protected boolean createKeys(List<NeuronFragment> list) {
        Sample sample = sampleRef.get();
        if (sample==null) return false;
        
        if (separationRef!=null) {
            NeuronSeparation separation = separationRef.get();
            if (separation==null) return false;
            DomainDAO dao = DomainExplorerTopComponent.getDao();
            if (separation.getFragmentsReference()!=null) {
                for(DomainObject object : dao.getDomainObjects(SessionMgr.getSubjectKey(), separation.getFragmentsReference())) {
                    if (object instanceof NeuronFragment) {
                        list.add((NeuronFragment)object);
                    }
                    else {
                        log.warn("Fragments reference contains non-fragment: "+object.getId());
                    }
                }
            }
        }
        else {
            DomainDAO dao = DomainExplorerTopComponent.getDao();
            Long separationId = null;
            if (sample.getObjectives() != null) {
                List<String> objectives = new ArrayList<String>(sample.getObjectives().keySet());
                Collections.sort(objectives);
                String objective = objectives.get(0);
                ObjectiveSample objectiveSample = sample.getObjectives().get(objective);
                SamplePipelineRun run = objectiveSample.getLatestRun();
                if (run != null && run.getResults() != null) {
                    for (PipelineResult result : run.getResults()) {
                        NeuronSeparation separation = result.getLatestSeparationResult();
                        if (separation != null) {
                            separationId = separation.getFragmentsReference().getReferenceId();
                        }
                    }
                    List<NeuronFragment> fragments = dao.getNeuronFragmentsBySeparationId(SessionMgr.getSubjectKey(), separationId);
                    for (NeuronFragment obj : fragments) {
                        if (obj != null) {
                            list.add(obj);
                        }
                    }
                }
            }
        }
        return true;
    }

    @Override
    protected Node createNodeForKey(NeuronFragment key) {
        Sample sample = sampleRef.get();
        if (sample==null) return null;
        try {
            return new NeuronFragmentNode(this, key);
        }
        catch (Exception e) {
            log.error("Error creating node for key " + key, e);
        }
        return null;
    }
}
