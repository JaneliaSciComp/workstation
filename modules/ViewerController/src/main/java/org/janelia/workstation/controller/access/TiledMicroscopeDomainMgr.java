package org.janelia.workstation.controller.access;

import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.model.domain.DomainConstants;
import org.janelia.model.domain.DomainObjectComparator;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.Reference;
import org.janelia.model.domain.enums.FileType;
import org.janelia.model.domain.tiledMicroscope.*;
import org.janelia.model.domain.workspace.TreeNode;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.DomainMgr;
import org.janelia.workstation.core.api.DomainModel;
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
public interface TiledMicroscopeDomainMgr {
    public DomainModel getModel();
    public List<String> getSamplePaths() throws Exception;
    public void setSamplePaths(List<String> paths) throws Exception;
    public TmSample getSample(Long sampleId) throws Exception;
    public TmSample getSample(TmWorkspace workspace) throws Exception;
    public TmSample createSample(String name, String filepath, String ktxPath, String rawPath) throws Exception;
    public TmSample save(TmSample sample) throws Exception;
    public void remove(TmSample sample) throws Exception;
    public List<TmWorkspace> getWorkspacesSortedByCurrentPrincipal(Long sampleId) throws Exception;
    public TmWorkspace getWorkspace(Long workspaceId) throws Exception;
    public TmWorkspace createWorkspace(Long sampleId, String name) throws Exception;
    public TmWorkspace copyWorkspace(TmWorkspace workspace, String name, String assignOwner) throws Exception;
    public TmWorkspace save(TmWorkspace workspace) throws Exception;
    public void remove(TmWorkspace workspace) throws Exception;
    public Stream<TmNeuronMetadata> streamWorkspaceNeurons(Long workspaceId);
    public TmNeuronMetadata saveMetadata(TmNeuronMetadata neuronMetadata) throws Exception;
    public List<TmNeuronMetadata> saveMetadata(List<TmNeuronMetadata> neuronList) throws Exception;
    public TmNeuronMetadata createWithId(TmNeuronMetadata neuronMetadata) throws Exception;
    public TmNeuronMetadata save(TmNeuronMetadata neuronMetadata) throws Exception;
    public TmReviewTask save(TmReviewTask reviewTask) throws Exception;
    public void remove(TmReviewTask reviewTask) throws Exception;
    public void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate, Long workspaceId) throws Exception;
    public void remove(TmNeuronMetadata tmNeuron) throws Exception;
    public List<TmReviewTask> getReviewTasks() throws Exception;
    public void bulkEditNeuronTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) throws Exception;
    public CoordinateToRawTransform getCoordToRawTransform(String basePath) throws Exception;
    public byte[] getRawTextureBytes(String basePath, int[] viewerCoord, int[] dimensions, int channel) throws Exception;
    public String getNearestChannelFilesURL(String basePath, int[] viewerCoord);
}
