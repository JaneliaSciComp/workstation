package org.janelia.it.workstation.gui.large_volume_viewer.api;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.janelia.it.jacs.model.domain.DomainConstants;
import org.janelia.it.jacs.model.domain.Reference;
import org.janelia.it.jacs.model.domain.tiledMicroscope.BulkNeuronStyleUpdate;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmProtobufExchanger;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmDirectedSession;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.model.domain.workspace.TreeNode;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.RawFileInfo;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.api.DomainMgr;
import org.janelia.it.workstation.browser.api.DomainModel;
import org.janelia.it.workstation.browser.model.DomainObjectComparator;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwDecision;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwGraph;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwGraphStatus;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwSession;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwSessionType;
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

    private final TiledMicroscopeRestClient client;
    private final DirectedTracingWorkflowRestClient sataClient;
    
    private TiledMicroscopeDomainMgr() {
        client = new TiledMicroscopeRestClient();
        sataClient = new DirectedTracingWorkflowRestClient();
    }
    
    private final DomainModel model = DomainMgr.getDomainMgr().getModel();

    // *****************************************************************************************************************
    // SAMPLES
    // *****************************************************************************************************************

    public List<String> getSamplePaths() throws Exception {
        return client.getTmSamplePaths();
    }

    public void setSamplePaths(List<String> paths) throws Exception {
        client.updateSamplePaths(paths);
    }
    
    public TmSample getSample(Long sampleId) throws Exception {
        log.debug("getSample(sampleId={})",sampleId);
        return model.getDomainObject(TmSample.class, sampleId);
    }

    public TmSample getSample(TmWorkspace workspace) throws Exception {
        log.debug("getSample({})",workspace);
        return getSample(workspace.getSampleRef().getTargetId());
    }

    public TmSample createSample(String name, String filepath) throws Exception {
        log.debug("createTiledMicroscopeSample(name={}, filepath={})", name, filepath);
        TmSample sample = new TmSample();
        sample.setOwnerKey(AccessManager.getSubjectKey());
        sample.setName(name);
        sample.setFilepath(filepath);
        sample = save(sample);

        // Server should have put the sample in the Samples root folder. Refresh the Samples folder to show it in the explorer.
        TreeNode folder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_TM_SAMPLE_FOLDER, true);
        model.invalidate(folder);
        
        return sample;
    }

    public TmSample save(TmSample sample) throws Exception {
        log.debug("save({})",sample);
        TmSample canonicalObject;
        synchronized (this) {
            canonicalObject = model.putOrUpdate(sample.getId()==null ? client.create(sample) : client.update(sample));
        }
        if (sample.getId()==null) {
            model.notifyDomainObjectCreated(canonicalObject);
        }
        else {
            model.notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(TmSample sample) throws Exception {
        log.debug("remove({})",sample);
        client.remove(sample);
        model.notifyDomainObjectRemoved(sample);
    }

    // *****************************************************************************************************************
    // WORKSPACES
    // *****************************************************************************************************************

    public List<TmWorkspace> getWorkspaces(Long sampleId) throws Exception {
        Collection<TmWorkspace> workspaces = client.getTmWorkspacesForSample(sampleId);
        List<TmWorkspace> canonicalObjects = DomainMgr.getDomainMgr().getModel().putOrUpdate(workspaces, false);
        Collections.sort(canonicalObjects, new DomainObjectComparator());
        return canonicalObjects;
    }

    public TmWorkspace getWorkspace(Long workspaceId) throws Exception {
        log.debug("getWorkspace(workspaceId={})",workspaceId);
        TmWorkspace workspace = model.getDomainObject(TmWorkspace.class, workspaceId);
        if (workspace==null) {
            throw new Exception("Workspace with id="+workspaceId+" does not exist");
        }
        return workspace;
    }
    
    public TmWorkspace createWorkspace(Long sampleId, String name) throws Exception {
        log.debug("createWorkspace(sampleId={}, name={})", sampleId, name);
        TmSample sample = getSample(sampleId);
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+sampleId);
        }
        
        Reference sampleRef = Reference.createFor(TmSample.class, sampleId);
        
        TmWorkspace workspace = new TmWorkspace();
        workspace.setOwnerKey(AccessManager.getSubjectKey());
        workspace.setName(name);
        workspace.setSampleRef(sampleRef);
        workspace = save(workspace);
        
        // Server should have put the workspace in the Workspaces root folder. Refresh the Workspaces folder to show it in the explorer.
        TreeNode folder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_TM_WORKSPACE_FOLDER, true);
        model.invalidate(folder);
        
        // Also invalidate the sample, so that the Explorer tree can be updated 
        model.invalidate(sample);
        
        return workspace;
    }

    public TmWorkspace copyWorkspace(TmWorkspace workspace, String name) throws Exception {
        log.debug("copyWorkspace(workspace={}, name={})", workspace, name);
        
        TmWorkspace workspaceCopy = client.copy(workspace, name);
        
        // Server should have put the new workspace in the Workspaces root folder. Refresh the Workspaces folder to show it in the explorer.
        TreeNode folder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_TM_WORKSPACE_FOLDER, true);
        model.invalidate(folder);
        
        // Also invalidate the sample, so that the Explorer tree can be updated 
        TmSample sample = getSample(workspace.getSampleId());
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+workspace.getSampleId());
        }
        
        model.invalidate(sample);
        
        return workspaceCopy;
    }

    public TmWorkspace save(TmWorkspace workspace) throws Exception {
        log.debug("save({})", workspace);
        TmWorkspace canonicalObject;
        synchronized (this) {
            canonicalObject = model.putOrUpdate(workspace.getId()==null ? client.create(workspace) : client.update(workspace));
        }
        if (workspace.getId()==null) {
            model.notifyDomainObjectCreated(canonicalObject);
        }
        else {
            model.notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(TmWorkspace workspace) throws Exception {
        log.debug("remove({})", workspace);
        client.remove(workspace);
        model.notifyDomainObjectRemoved(workspace);
    }

    // *****************************************************************************************************************
    // NEURONS
    // *****************************************************************************************************************

    public List<TmNeuronMetadata> getWorkspaceNeurons(Long workspaceId) throws Exception {
        log.debug("getWorkspaceNeurons(workspaceId={})",workspaceId);
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        List<TmNeuronMetadata> neurons = new ArrayList<>();
        for(Pair<TmNeuronMetadata,InputStream> pair : client.getWorkspaceNeuronPairs(workspaceId)) {
            TmNeuronMetadata neuronMetadata = pair.getLeft();
            exchanger.deserializeNeuron(pair.getRight(), neuronMetadata);
            log.trace("Got neuron {} with payload '{}'", neuronMetadata.getId(), neuronMetadata);
            neurons.add(neuronMetadata);
        }
        log.trace("Loaded {} neurons for workspace {}", neurons.size(), workspaceId);
        return neurons;
    }

    public TmNeuronMetadata saveMetadata(TmNeuronMetadata neuronMetadata) throws Exception {
        log.debug("save({})", neuronMetadata);
        TmNeuronMetadata savedMetadata;
        if (neuronMetadata.getId()==null) {
            savedMetadata = client.createMetadata(neuronMetadata);
            model.notifyDomainObjectCreated(savedMetadata);
        }
        else {
            savedMetadata = client.updateMetadata(neuronMetadata);
            model.notifyDomainObjectChanged(savedMetadata);
        }
        return savedMetadata;
    }

    public List<TmNeuronMetadata> saveMetadata(List<TmNeuronMetadata> neuronList) throws Exception {
        log.debug("save({})", neuronList);
        for(TmNeuronMetadata tmNeuronMetadata : neuronList) {
            if (tmNeuronMetadata.getId()==null) {
                throw new IllegalArgumentException("Bulk neuron creation is currently unsupported");
            }
        }
        List<TmNeuronMetadata> updatedMetadata = client.updateMetadata(neuronList);
        for(TmNeuronMetadata tmNeuronMetadata : updatedMetadata) {
            model.notifyDomainObjectChanged(tmNeuronMetadata);
        }
        return updatedMetadata;
    }
    
    public TmNeuronMetadata save(TmNeuronMetadata neuronMetadata) throws Exception {
        log.debug("save({})", neuronMetadata);
        TmProtobufExchanger exchanger = new TmProtobufExchanger();
        InputStream protobufStream = new ByteArrayInputStream(exchanger.serializeNeuron(neuronMetadata));
        TmNeuronMetadata savedMetadata;
        if (neuronMetadata.getId()==null) {
            savedMetadata = client.create(neuronMetadata, protobufStream);
            model.notifyDomainObjectCreated(savedMetadata);
        }
        else {
            savedMetadata = client.update(neuronMetadata, protobufStream);
            model.notifyDomainObjectChanged(savedMetadata);
        }
        // We assume that the neuron data was saved on the server, but it only returns metadata for efficiency. We
        // already have the data, so let's copy it over into the new object.
        exchanger.copyNeuronData(neuronMetadata, savedMetadata);
        return savedMetadata;
    }
    
    public void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate) throws Exception {
        client.updateNeuronStyles(bulkNeuronStyleUpdate);
    }
    
    public void remove(TmNeuronMetadata tmNeuron) throws Exception {
        log.debug("remove({})", tmNeuron);
        TmNeuronMetadata neuronMetadata = new TmNeuronMetadata();
        neuronMetadata.setId(tmNeuron.getId());
        client.remove(neuronMetadata);
        model.notifyDomainObjectRemoved(neuronMetadata);
    }

    public void bulkEditNeuronTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) throws Exception {
        log.debug("bulkEditTags({})", neurons);
        client.changeTags(neurons, tags, tagState);
    }

    // *****************************************************************************************************************
    // SESSIONS
    // *****************************************************************************************************************

    public List<TmDirectedSession> getSessions(Long sampleId) throws Exception {
        Collection<TmDirectedSession> sessions = client.getTmSessionsForSample(sampleId);
        List<TmDirectedSession> canonicalObjects = DomainMgr.getDomainMgr().getModel().putOrUpdate(sessions, false);
        Collections.sort(canonicalObjects, new DomainObjectComparator());
        return canonicalObjects;
    }

    public TmDirectedSession getSession(Long sessionId) throws Exception {
        log.debug("getSession(sessionId={})",sessionId);
        TmDirectedSession session = model.getDomainObject(TmDirectedSession.class, sessionId);
        if (session==null) {
            throw new Exception("Session with id="+sessionId+" does not exist");
        }
        return session;
    }
    
    public TmSample getSample(TmDirectedSession session) throws Exception {
        log.debug("getSample({})",session);
        return getSample(session.getSampleRef().getTargetId());
    }
    
    public TmDirectedSession createSession(Long sampleId, String name) throws Exception {
        log.debug("createSession(sampleId={}, name={})", sampleId, name);
        TmSample sample = getSample(sampleId);
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+sampleId);
        }
        
        Reference sampleRef = Reference.createFor(TmSample.class, sampleId);
        
        // Get or create graph in SATA
        DtwGraph graph = sataClient.getLatestGraph(sample.getFilepath());
        if (graph==null || graph.getId()==null) {
            graph = new DtwGraph();
            graph.setSamplePath(sample.getFilepath());
            graph = sataClient.create(graph);
            log.info("Created graph with id="+graph.getId());
        }
        
        log.info("Creating session for graph with id="+graph.getId());
        
        // Create new session in SATA
        DtwSession session = sataClient.createSession(graph, DtwSessionType.AffinityLearning);
        
        // Create session tracking object in JACS
        TmDirectedSession tmSession = new TmDirectedSession();
        tmSession.setOwnerKey(AccessManager.getSubjectKey());
        tmSession.setName(name);
        tmSession.setSampleRef(sampleRef);
        tmSession.setExternalSessionId(session.getId());
        tmSession = save(tmSession);
        
        // Cache the SATA session for later use
        sessionMapping.put(tmSession, session);
        
        // Server should have put the session in the Sessions root folder. Refresh the Sessions folder to show it in the explorer.
        TreeNode folder = model.getDefaultWorkspaceFolder(DomainConstants.NAME_TM_SESSIONS_FOLDER, true);
        model.invalidate(folder);
        
        // Also invalidate the sample, so that the Explorer tree can be updated 
        model.invalidate(sample);
        
        return tmSession;
    }
    
    private final Map<TmDirectedSession, DtwSession> sessionMapping = new HashMap<>();
    private DtwSession getSataSession(TmDirectedSession tmSession) throws Exception {
        DtwSession sataSession = sessionMapping.get(tmSession);
        if (sataSession==null) {
            log.info("Session not cached, attempting to retrieve from SATA");
            sataSession = sataClient.getSession(tmSession.getExternalSessionId());
            sessionMapping.put(tmSession, sataSession);
        }
        return sataSession;
    }
    
    public DtwDecision getNextDecision(TmDirectedSession tmSession) throws Exception {
        log.debug("getNextDecision(sessionId={})", tmSession.getExternalSessionId());
        return sataClient.getNextDecision(tmSession.getExternalSessionId());
    }
    
    public DtwDecision saveDecision(DtwDecision decision) throws Exception {
        log.debug("saveDecision(decisionId={})", decision.getId());
        return sataClient.updateDecision(decision);
    }
    
    public void startGraphUpdate(TmDirectedSession tmSession) throws Exception {
        log.debug("startGraphUpdate(sessionId={})", tmSession.getExternalSessionId());
        DtwSession session = getSataSession(tmSession);
        sataClient.startGraphUpdate(session.getGraphId());
    }

    public DtwGraphStatus getGraphStatus(TmDirectedSession tmSession) throws Exception {
        log.debug("getGraphStatus(sessionId={})", tmSession.getExternalSessionId());
        DtwSession session = getSataSession(tmSession);
        DtwGraph graph = sataClient.getGraph(session.getGraphId());
        if (graph!=null) {
            return graph.getStatus();
        }
        return DtwGraphStatus.Unknown;
    }
    
    public TmDirectedSession save(TmDirectedSession session) throws Exception {
        log.debug("save({})", session);
        TmDirectedSession canonicalObject;
        synchronized (this) {
            canonicalObject = model.putOrUpdate(session.getId()==null ? client.create(session) : client.update(session));
        }
        if (session.getId()==null) {
            model.notifyDomainObjectCreated(canonicalObject);
        }
        else {
            model.notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(TmDirectedSession session) throws Exception {
        log.debug("remove({})", session);
        client.remove(session);
        model.notifyDomainObjectRemoved(session);
    }

    // *****************************************************************************************************************
    // BYTES
    // *****************************************************************************************************************

    public CoordinateToRawTransform getCoordToRawTransform(String basePath) throws Exception {
        log.debug("getCoordToRawTransform({})", basePath);
        return DomainMgr.getDomainMgr().getLegacyFacade().getLvvCoordToRawTransform(basePath);
    }

    public Map<Integer, byte[]> getTextureBytes(String basePath, int[] viewerCoord, int[] dimensions) throws Exception {
        log.debug("getTextureBytes({}, viewerCoord={}, dimensions={})", basePath, viewerCoord, dimensions);
        return DomainMgr.getDomainMgr().getLegacyFacade().getTextureBytes(basePath, viewerCoord, dimensions);
    }

    public RawFileInfo getNearestChannelFiles(String basePath, int[] viewerCoord) throws Exception {
        log.debug("getNearestChannelFiles({}, viewerCoord={})", basePath, viewerCoord);
        return DomainMgr.getDomainMgr().getLegacyFacade().getNearestChannelFiles(basePath, viewerCoord);
    }
}
