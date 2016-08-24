package org.janelia.it.workstation.gui.large_volume_viewer.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.domain.workspace.Workspace;
import org.janelia.it.workstation.gui.browser.api.DomainMgr;
import org.janelia.it.workstation.gui.browser.api.DomainModel;
import org.janelia.it.workstation.gui.browser.events.Events;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing the Tiled Microscope Domain Model and related data access.
 * 
 * Translates in Mongo-land domain objects to the existing TM classes.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TiledMicroscopeDomainMgr {

    private static final Logger log = LoggerFactory.getLogger(TiledMicroscopeDomainMgr.class);

    // name of entity that holds our workspaces
    public static final String WORKSPACES_FOLDER_NAME = "Workspaces";

    // Singleton
    private static TiledMicroscopeDomainMgr instance;

    public static TiledMicroscopeDomainMgr getDomainMgr() {
        if (instance==null) {
            instance = new TiledMicroscopeDomainMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private final DomainModel model = DomainMgr.getDomainMgr().getModel();

    public TmSample getSample(Long sampleId) throws Exception {
        log.debug("getSample(sampleId={})",sampleId);
        return model.getDomainObject(TmSample.class, sampleId);
    }

    public TmSample getSample(TmWorkspace workspace) throws Exception {
        log.debug("getSample({})",workspace);
        return getSample(workspace.getSampleRef().getTargetId());
    }

    public Collection<TmSample> getTmSamples() throws Exception {
        log.debug("getTmSamples()");
        return model.getTmSamples();
    }

    public TmSample save(TmSample sample) throws Exception {
        log.debug("save({})",sample);
        return model.save(sample);
    }

    public void remove(TmSample sample) throws Exception {
        log.debug("remove({})",sample);
        model.remove(sample);
    }

    public TreeNode getOrCreateWorkspacesFolder() throws Exception {
        log.debug("getOrCreateWorkspacesFolder()");
        Workspace defaultWorkspace = model.getDefaultWorkspace();
        TreeNode folder = DomainUtils.findObjectByTypeAndName(model.getDomainObjects(defaultWorkspace.getChildren()), TreeNode.class, WORKSPACES_FOLDER_NAME);
        if (folder==null) {
            TreeNode workspacesNode = new TreeNode();
            workspacesNode.setName(WORKSPACES_FOLDER_NAME);
            workspacesNode = model.create(workspacesNode);
            model.addChild(defaultWorkspace, workspacesNode);
        }
        return folder;
    }

    public TmWorkspace getWorkspace(Long workspaceId) throws Exception {
        log.debug("getWorkspace(workspaceId={})",workspaceId);
        return model.getDomainObject(TmWorkspace.class, workspaceId);
    }

    public Collection<TmWorkspace> getTmWorkspaces() throws Exception {
        log.debug("getTmWorkspaces()");
        return model.getTmWorkspaces();
    }

    public TmWorkspace save(TmWorkspace workspace) throws Exception {
        log.debug("save({})", workspace);
        return model.save(workspace);
    }

    public void remove(TmWorkspace workspace) throws Exception {
        log.debug("remove({})", workspace);
        model.remove(workspace);
    }

    public TmWorkspace createTiledMicroscopeWorkspace(Long sampleId, String name) throws Exception {
        log.debug("createTiledMicroscopeWorkspace(sampleId={}, name={})", sampleId, name);
        TreeNode workspaceRoot = getOrCreateWorkspacesFolder();
        TmSample sample = getSample(sampleId);
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+sampleId);
        }
        TmWorkspace workspace = new TmWorkspace();
        workspace.setOwnerKey(SessionMgr.getSubjectKey());
        workspace.setName(name);
        workspace.setSampleRef(Reference.createFor(TmSample.class, sampleId));
        workspace = save(workspace);
        model.addChild(workspaceRoot, workspace);
        return workspace;
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
            throw new IllegalArgumentException("Cannot save neuron metadata without id");
        }
        else {
            model.update(neuronMetadata);
        }

        return neuronMetadata;
    }

    public TmNeuronMetadata save(TmNeuronMetadata neuronMetadata) throws Exception {
        log.debug("saveMetadata({})", neuronMetadata);
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        byte[] protobufBytes = exchanger.serializeNeuron(neuronMetadata);

        if (neuronMetadata.getId()==null) {
            model.create(neuronMetadata, new ByteArrayInputStream(protobufBytes));
        }
        else {
            model.update(neuronMetadata, new ByteArrayInputStream(protobufBytes));
        }

        return neuronMetadata;
    }

    public void remove(TmNeuronMetadata tmNeuron) throws Exception {
        log.debug("remove({})", tmNeuron);
        TmNeuronMetadata neuronMetadata = new TmNeuronMetadata();
        neuronMetadata.setId(tmNeuron.getId());
        model.remove(neuronMetadata);
    }
}
