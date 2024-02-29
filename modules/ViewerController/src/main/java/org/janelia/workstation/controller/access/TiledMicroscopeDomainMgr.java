package org.janelia.workstation.controller.access;

import com.google.common.eventbus.Subscribe;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObjectComparator;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.model.security.Subject;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
import org.janelia.model.domain.tiledMicroscope.BoundingBox3d;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Stream;

/**
 * Singleton for managing the Tiled Microscope Domain Model and related data access.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TiledMicroscopeDomainMgr {
    private static final int NUM_PARALLEL_NEURONSTREAMS = 4;
    private static final Logger LOG = LoggerFactory.getLogger(TiledMicroscopeDomainMgr.class);

    // Singleton
    private static TiledMicroscopeDomainMgr instance;

    public static TiledMicroscopeDomainMgr getDomainMgr() {
        if (instance==null) {
            instance = new TiledMicroscopeDomainMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private TiledMicroscopeRestClient client = new TiledMicroscopeRestClient();

    private TiledMicroscopeDomainMgr() {
    }

    @Subscribe
    public synchronized void propsLoaded(ConsolePropsLoaded event) {
        LOG.info("Re-initializing TM Domain Manager");
        try {
            this.client = new TiledMicroscopeRestClient();
        }
        catch (Exception e) {
            FrameworkAccess.handleException(e);
        }
    }

    private DomainModel getModel() {
        return DomainMgr.getDomainMgr().getModel();
    }

    public TmSample getSample(Long sampleId) throws Exception {
        LOG.debug("getSample(sampleId={})",sampleId);
        return getModel().getDomainObject(TmSample.class, sampleId);
    }

    public TmSample getSample(TmWorkspace workspace) throws Exception {
        LOG.debug("getSample({})",workspace);
        return getSample(workspace.getSampleRef().getTargetId());
    }

    public TmSample createSample(String name, String filepath, String ktxPath, String zarrPath) throws Exception {
        LOG.debug("createTiledMicroscopeSample(name={}, filepath={})", name, filepath);
        Map<String,Object> constants = client.getTmSampleConstants(filepath);
        if (constants != null) {
            TreeNode tmSampleFolder = getModel().getDefaultWorkspaceFolder(DomainConstants.NAME_TM_SAMPLE_FOLDER, true);

            TmSample sample = new TmSample();
            sample.setOwnerKey(AccessManager.getSubjectKey());
            sample.setName(name);
            DomainUtils.setFilepath(sample, FileType.LargeVolumeOctree, filepath);
            if (ktxPath != null) {
                DomainUtils.setFilepath(sample, FileType.LargeVolumeKTX, ktxPath);
            }
            if (zarrPath != null) {
                DomainUtils.setFilepath(sample, FileType.LargeVolumeZarr, zarrPath);
            }
            // call out to server to get origin/scaling information
            TmSample persistedSample = save(sample);

            // Server should have put the sample in the Samples root folder. Refresh the Samples folder to show it in the explorer.
            getModel().invalidate(tmSampleFolder);

            return persistedSample;
        } else {
            LOG.error("Problem creating sample; no sample constants available for sample {} at {}.", name, filepath);
            return null;
        }
    }

    public TmSample save(TmSample sample) throws Exception {
        LOG.debug("save({})",sample);
        TmSample canonicalObject;
        synchronized (this) {
            canonicalObject = getModel().putOrUpdate(sample.getId()==null ? client.create(sample) : client.update(sample));
        }
        if (sample.getId()==null) {
            getModel().notifyDomainObjectCreated(canonicalObject);
        }
        else {
            getModel().notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(TmSample sample) throws Exception {
        LOG.debug("remove({})",sample);
        client.remove(sample);
        getModel().notifyDomainObjectRemoved(sample);
    }

    public List<TmWorkspace> getWorkspacesSortedByCurrentPrincipal(Long sampleId) throws Exception {
        Collection<TmWorkspace> workspaces = client.getTmWorkspacesForSample(sampleId);
        List<TmWorkspace> canonicalObjects = DomainMgr.getDomainMgr().getModel().putOrUpdate(workspaces, false);
        // Sorting this when the data is queried is a bit tricky
        // because the DomainObjectComparator returns objects owned by the current principal first
        // Also I would prefer to make this more obvious in the server call that the sorting is taking place (goinac).
        Collections.sort(canonicalObjects, new DomainObjectComparator(AccessManager.getSubjectKey()));
        return canonicalObjects;
    }

    public TmWorkspace getWorkspace(Long workspaceId) throws Exception {
        LOG.debug("getWorkspace(workspaceId={})",workspaceId);
        TmWorkspace workspace = getModel().getDomainObject(TmWorkspace.class, workspaceId);
        if (workspace==null) {
            throw new Exception("Workspace with id="+workspaceId+" does not exist");
        }
        return workspace;
    }

    public void createWorkspaceBoundingBoxes(Long workspaceId, List<BoundingBox3d> boundingBoxes) throws Exception {
        LOG.debug("createWorkspaceBoundingBoxes(workspaceId={})",workspaceId);
        client.createBoundingBoxes(workspaceId, boundingBoxes);
    }

    public Collection<BoundingBox3d> getWorkspaceBoundingBoxes(Long workspaceId) throws Exception {
        LOG.debug("getWorkspaceBoundingBoxes(workspaceId={})",workspaceId);
        return client.getWorkspaceBoundingBoxes(workspaceId);
    }

    public void createOperationLog (Long sampleId, Long workspaceId,  Long neuronId, TmOperation.Activity activity,
                                    String timestamp, Long elapsedTime, String subjectKey) {
        LOG.debug("createOperationLog(activity={}, timestamp={}, elapsedTime={})",activity, timestamp, elapsedTime);
        client.createOperationLog(sampleId, workspaceId, neuronId, activity, timestamp, elapsedTime, subjectKey);
    }

    public TmWorkspace createWorkspace(Long sampleId, String name) throws Exception {
        LOG.debug("createWorkspace(sampleId={}, name={})", sampleId, name);
        TmSample sample = getSample(sampleId);
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+sampleId);
        }

        TreeNode defaultWorkspaceFolder = getModel().getDefaultWorkspaceFolder(DomainConstants.NAME_TM_WORKSPACE_FOLDER, true);

        TmWorkspace workspace = new TmWorkspace();
        workspace.setOwnerKey(AccessManager.getSubjectKey());
        workspace.setName(name);
        workspace.setSampleRef(Reference.createFor(TmSample.class, sampleId));
        workspace = save(workspace);

        // Server should have put the workspace in the Workspaces root folder. Refresh the Workspaces folder to show it in the explorer.
        getModel().invalidate(defaultWorkspaceFolder);

        // Also invalidate the sample, so that the Explorer tree can be updated 
        getModel().invalidate(sample);

        return workspace;
    }

    public TmWorkspace copyWorkspace(TmWorkspace workspace, String name, String assignOwner) throws Exception {
        LOG.debug("copyWorkspace(workspace={}, name={}, neuronOwner={})", workspace, name, assignOwner);
        TmSample sample = getSample(workspace.getSampleId());
        if (sample==null) {
            throw new IllegalArgumentException("TM sample does not exist: "+workspace.getSampleId());
        }

        TreeNode defaultWorkspaceFolder = getModel().getDefaultWorkspaceFolder(DomainConstants.NAME_TM_WORKSPACE_FOLDER, true);

        TmWorkspace workspaceCopy = client.copy(workspace, name, assignOwner);

        // Server should have put the new workspace in the Workspaces root folder. Refresh the Workspaces folder to show it in the explorer.
        getModel().invalidate(defaultWorkspaceFolder);

        // Also invalidate the sample, so that the Explorer tree can be updated 
        getModel().invalidate(sample);

        return workspaceCopy;
    }

    public TmWorkspace save(TmWorkspace workspace) throws Exception {
        LOG.debug("save({})", workspace);
        TmWorkspace canonicalObject;
        synchronized (this) {
            canonicalObject = getModel().putOrUpdate(workspace.getId()==null ? client.create(workspace) : client.update(workspace));
        }
        if (workspace.getId()==null) {
            getModel().notifyDomainObjectCreated(canonicalObject);
        }
        else {
            getModel().notifyDomainObjectChanged(canonicalObject);
        }
        return canonicalObject;
    }

    public void remove(TmWorkspace workspace) throws Exception {
        LOG.debug("remove({})", workspace);
        client.remove(workspace);
        getModel().notifyDomainObjectRemoved(workspace);
    }

    class RetrieveNeuronsTask extends RecursiveTask<Stream<TmNeuronMetadata>> {
        double defaultLength = 50000.0;
        int start;
        int end;
        long workspaceId;

        RetrieveNeuronsTask(long workspaceId,
                            int start, int end) {
            this.start = start;
            this.end = end;
            this.workspaceId = workspaceId;
        }

        @Override
        public Stream<TmNeuronMetadata> compute() {
            if ((end-start)>defaultLength) {
                List<RetrieveNeuronsTask> tasks = new ArrayList<>();
                // break up into defaultLength chunks
                int numChunks = (int)Math.ceil((end-start)/defaultLength);
                for (int i=0; i<numChunks; i++) {
                    int newStart = i*(int)defaultLength;
                    int newEnd = newStart + (int)defaultLength;
                    if ((newStart+defaultLength)>end) {
                        newEnd = end;
                    }
                    RetrieveNeuronsTask task = new RetrieveNeuronsTask(workspaceId, newStart,
                            newEnd);
                    tasks.add(task);
                    task.fork();
                }

                return tasks.stream().flatMap(subtask -> subtask.join());
            } else {
                LOG.info("Retrieving results - Neuron block: {} - {}", start, end);
                return client.getWorkspaceNeurons(workspaceId, start, end - start).stream();
            }
        }
    }

    public List<TmNeuronMetadata> getNeurons(List<Long> neuronIds, TmWorkspace workspace) {
        LOG.debug("getNeurons(workspaceId={})",workspace.getId());
        List<TmNeuronMetadata> neurons = client.getNeuronSet(neuronIds, workspace);
        return neurons;
    }

    public Stream<TmNeuronMetadata> streamWorkspaceNeurons(Long workspaceId) {
        LOG.debug("getWorkspaceNeurons(workspaceId={})",workspaceId);
        long neuronCount = client.getWorkspaceNeuronCount(workspaceId);
        ForkJoinPool pool = new ForkJoinPool(NUM_PARALLEL_NEURONSTREAMS);
        Stream<TmNeuronMetadata> neurons = pool.invoke(new RetrieveNeuronsTask(workspaceId, 0, (int)neuronCount));
        return neurons;
    }

    public TmNeuronMetadata createWithId(TmNeuronMetadata neuronMetadata) throws Exception {
        LOG.debug("save({})", neuronMetadata);
        TmNeuronMetadata savedMetadata;
        savedMetadata = client.createNeuron(neuronMetadata);
        getModel().notifyDomainObjectCreated(savedMetadata);
        return savedMetadata;
    }

    public TmReviewTask save(TmReviewTask reviewTask) throws Exception {
        LOG.debug("save({})", reviewTask);
        if (reviewTask.getId()==null) {
            reviewTask = client.create(reviewTask);
        } else {
            reviewTask = client.update(reviewTask);
        }
        getModel().notifyDomainObjectChanged(reviewTask);
        return reviewTask;
    }

    public void remove(TmReviewTask reviewTask) throws Exception {
        LOG.debug("remove({})", reviewTask);
        client.remove(reviewTask);
        getModel().notifyDomainObjectRemoved(reviewTask);
    }

    public void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate, Long workspaceId) throws Exception {
        client.updateNeuronStyles(bulkNeuronStyleUpdate, workspaceId);
    }

    public List<TmReviewTask> getReviewTasks() throws Exception {
        LOG.debug("getReviewTasks()");
        List<TmReviewTask> reviewTasks = getModel().getAllDomainObjectsByClass(TmReviewTask.class);
        return reviewTasks;
    }

    public void bulkEditNeuronTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) throws Exception {
        LOG.debug("bulkEditTags({})", neurons);
        client.changeTags(neurons, tags, tagState);
    }

    public CoordinateToRawTransform getCoordToRawTransform(String basePath) throws Exception {
        LOG.debug("getCoordToRawTransform({})", basePath);
        return client.getLvvCoordToRawTransform(basePath);
    }

    public byte[] getRawTextureBytes(String basePath, int[] viewerCoord, int[] dimensions, int channel) throws Exception {
        LOG.debug("getTextureBytes({}, viewerCoord={}, dimensions={})", basePath, viewerCoord, dimensions);
        return client.getRawTextureBytes(basePath, viewerCoord, dimensions, channel);
    }

    public String getNearestChannelFilesURL(String basePath, int[] viewerCoord) {
        LOG.debug("getNearestChannelFilesURL({}, viewerCoord={})", basePath, viewerCoord);
        return client.getNearestChannelFilesURL(basePath, viewerCoord);
    }

    public TmNeuronMetadata createNeuron(TmNeuronMetadata neuronMetadata) throws Exception {
        LOG.debug("createNeuron({})", neuronMetadata);
        if (neuronMetadata.getId()!=null) {
            throw new IllegalStateException("Attempt to create neuron which already has a GUID: "+neuronMetadata.getId());
        }
        TmNeuronMetadata savedMetadata = client.createNeuron(neuronMetadata);
        return savedMetadata;
    }

    public TmNeuronMetadata updateNeuron(TmNeuronMetadata neuronMetadata) throws Exception {
        LOG.debug("updateNeuron({})", neuronMetadata);
        if (neuronMetadata.getId()==null) {
            throw new IllegalStateException("Attempt to update neuron which does not have a GUID");
        }
        client.updateNeuron(neuronMetadata);
        // We assume that the neuron data was saved on the server, but it only returns metadata for efficiency. We
        // already have the data, so let's copy it over into the new object.
        // exchanger.copyNeuronData(neuronMetadata, savedMetadata);
        return neuronMetadata;
    }

    public TmNeuronMetadata changeOwnership(TmNeuronMetadata neuronMetadata, String targetUser) throws Exception {
        LOG.debug("changeOwnership({})", neuronMetadata);
        if (neuronMetadata.getId()==null) {
            throw new IllegalStateException("Attempt to update neuron which does not have a GUID");
        }
        TmNeuronMetadata savedMetadata = client.changeOwnership(neuronMetadata, targetUser);
        // We assume that the neuron data was saved on the server, but it only returns metadata for efficiency. We
        // already have the data, so let's copy it over into the new object.
        // exchanger.copyNeuronData(neuronMetadata, savedMetadata);
        return savedMetadata;
    }

    public TmNeuronMetadata removeNeuron(TmNeuronMetadata tmNeuron) throws Exception {
        LOG.debug("removeNeuron({})", tmNeuron);
        client.removeNeuron(tmNeuron);
        return tmNeuron;
    }
}
