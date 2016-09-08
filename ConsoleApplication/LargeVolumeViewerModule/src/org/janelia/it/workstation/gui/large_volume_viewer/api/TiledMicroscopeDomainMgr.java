package org.janelia.it.workstation.gui.large_volume_viewer.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing the Tiled Microscope Domain Model and related data access.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TiledMicroscopeDomainMgr {

    private static final Logger log = LoggerFactory.getLogger(TiledMicroscopeDomainMgr.class);

    // Singleton
    private static TiledMicroscopeDomainMgr instance;

    public static TiledMicroscopeDomainMgr getDomainMgr() {
        if (instance==null) {
            instance = new TiledMicroscopeDomainMgr();
        }
        return instance;
    }

    private final DomainModel model = DomainMgr.getDomainMgr().getModel();

    public TmSample getSample(Long sampleId) throws Exception {
        log.debug("getSample(sampleId={})",sampleId);
        TmSample sample = model.getDomainObject(TmSample.class, sampleId);
        if (sample==null) {
            throw new Exception("Sample with id="+sampleId+" does not exist");
        }
        return sample;
    }

    public TmSample getSample(TmWorkspace workspace) throws Exception {
        log.debug("getSample({})",workspace);
        return getSample(workspace.getSampleRef().getTargetId());
    }

    public Collection<TmSample> getTmSamples() throws Exception {
        log.debug("getTmSamples()");
        return model.getTmSamples();
    }

    public TmSample createTiledMicroscopeSample(String name, String filepath) throws Exception {
        log.debug("createTiledMicroscopeSample(name={}, filepath={})", name, filepath);
        TmSample sample = new TmSample();
        sample.setOwnerKey(SessionMgr.getSubjectKey());
        sample.setName(name);
        sample.setFilepath(filepath);
        sample = save(sample);
        return sample;
    }

    public TmSample save(TmSample sample) throws Exception {
        log.debug("save({})",sample);
        return model.save(sample);
    }

    public void remove(TmSample sample) throws Exception {
        log.debug("remove({})",sample);
        model.remove(sample);
    }

    public TmWorkspace getWorkspace(Long workspaceId) throws Exception {
        log.debug("getWorkspace(workspaceId={})",workspaceId);
        TmWorkspace workspace = model.getDomainObject(TmWorkspace.class, workspaceId);
        if (workspace==null) {
            throw new Exception("Workspace with id="+workspaceId+" does not exist");
        }
        return workspace;
    }

    public Collection<TmWorkspace> getTmWorkspaces() throws Exception {
        log.debug("getTmWorkspaces()");
        return model.getTmWorkspaces();
    }

    public TmWorkspace createTiledMicroscopeWorkspace(Long sampleId, String name) throws Exception {
        log.debug("createTiledMicroscopeWorkspace(sampleId={}, name={})", sampleId, name);
        TmSample sample = getSample(sampleId);
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+sampleId);
        }
        TmWorkspace workspace = new TmWorkspace();
        workspace.setOwnerKey(SessionMgr.getSubjectKey());
        workspace.setName(name);
        workspace.setSampleRef(Reference.createFor(TmSample.class, sampleId));
        workspace = save(workspace);
        return workspace;
    }

    public TmWorkspace save(TmWorkspace workspace) throws Exception {
        log.debug("save({})", workspace);
        return model.save(workspace);
    }

    public void remove(TmWorkspace workspace) throws Exception {
        log.debug("remove({})", workspace);
        model.remove(workspace);
    }
    
    public List<TmNeuronMetadata> getWorkspaceNeurons(Long workspaceId) throws Exception {
        log.debug("getWorkspaceNeurons(workspaceId={})",workspaceId);
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        List<TmNeuronMetadata> neurons = new ArrayList<>();
        for(Pair<TmNeuronMetadata,InputStream> pair : model.getWorkspaceNeuronPairs(workspaceId)) {
            TmNeuronMetadata neuronMetadata = pair.getLeft();
            exchanger.deserializeNeuron(pair.getRight(), neuronMetadata);
            log.info("Got neuron {} with payload '{}'", neuronMetadata.getId(), neuronMetadata);
            neurons.add(neuronMetadata);
        }

        log.info("Loaded {} neurons for workspace {}", neurons.size(), workspaceId);
        return neurons;
    }

    public TmNeuronMetadata saveMetadata(TmNeuronMetadata neuronMetadata) throws Exception {
        log.debug("save({})", neuronMetadata);
        if (neuronMetadata.getId()==null) {
            return model.createMetadata(neuronMetadata);
        }
        else {
            return model.updateMetadata(neuronMetadata);
        }
    }

    public TmNeuronMetadata save(TmNeuronMetadata neuronMetadata) throws Exception {
        log.debug("save({})", neuronMetadata);
        log.info("save: "+neuronMetadata);
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        InputStream protobufStream = new ByteArrayInputStream(exchanger.serializeNeuron(neuronMetadata));
        TmNeuronMetadata metadata;
        if (neuronMetadata.getId()==null) {
            metadata = model.create(neuronMetadata, protobufStream);
        }
        else {
            metadata = model.update(neuronMetadata, protobufStream);
        }
        // We assume that the neuron data was saved on the server, but it only returns metadata for efficiency. We
        // already have the data, so let's copy it over into the new object.
        exchanger.copyNeuronData(neuronMetadata, metadata);
        return metadata;
    }

    public void remove(TmNeuronMetadata tmNeuron) throws Exception {
        log.debug("remove({})", tmNeuron);
        TmNeuronMetadata neuronMetadata = new TmNeuronMetadata();
        neuronMetadata.setId(tmNeuron.getId());
        model.remove(neuronMetadata);
    }

    public List<String> getTmSamplePaths() throws Exception {
        return model.getTmSamplePaths();
    }

    public void setTmSamplePaths(List<String> paths) throws Exception {
        model.setTmSamplePaths(paths);
    }
}
