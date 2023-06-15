package org.janelia.workstation.controller.access;

import java.io.InputStream;
import java.net.URI;
import java.util.*;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.ByteStreams;

import org.apache.commons.lang3.StringUtils;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.model.domain.dto.DomainQuery;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.tiledMicroscope.BulkNeuronStyleUpdate;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmReviewTask;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.exceptions.RemoteServiceException;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.integration.util.FrameworkAccess;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A web client providing access to the Tiled Microscope REST Service.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TiledMicroscopeRestClient extends RESTClientBase {

    private static final Logger LOG = LoggerFactory.getLogger(TiledMicroscopeRestClient.class);

    private final WebTarget service;
    private final String remoteApiUrl;
    private final String remoteStorageUrl;

    public TiledMicroscopeRestClient() {
        super(LOG);
        this.remoteApiUrl = ConsoleProperties.getInstance().getProperty("mouselight.rest.url");
        this.remoteStorageUrl = ConsoleProperties.getInstance().getProperty("jadestorage.rest.url");
        LOG.info("Using server URL: {}", this.remoteApiUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(this.remoteApiUrl, true);
        // TODO: convert the methods using these variables to reuse targets instead
    }

    private WebTarget getMouselightDataEndpoint(String suffix) {
        return getMouselightEndpoint()
                .path("mouselight/data")
                .path(suffix);
    }

    private WebTarget getMouselightEndpoint() {
        return service.queryParam("subjectKey", AccessManager.getSubjectKey());
    }

    private URI getMouselightStreamURI(String basePath, String suffix) {
        String suffixPath = suffix.endsWith("/") ? suffix : suffix + "/";
        if (basePath.startsWith("http://") || basePath.startsWith("https://")) {
            return URI.create(basePath)
                    .resolve(suffixPath);
        } else {
            return URI.create(remoteApiUrl)
                    .resolve("mouselight/")
                    .resolve(suffixPath)
                    .resolve(basePath);
        }
    }

    Collection<TmSample> getTmSamples() {
        WebTarget target = getMouselightDataEndpoint("/sample");
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<TmSample>>() {});
    }
        
    Map<String,Object> getTmSampleConstants(String samplePath) {
        WebTarget target = getMouselightDataEndpoint("/sample/constants")
                .queryParam("samplePath", samplePath);
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<Map<String,Object>>() {});
    }

    public TmSample create(TmSample tmSample) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmSample);
        WebTarget target = getMouselightDataEndpoint("/sample");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmSample.class);
    }

    public TmSample update(TmSample tmSample) {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmSample);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = getMouselightDataEndpoint("/sample");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmSample.class);
    }

    public void remove(TmSample tmSample) {
        WebTarget target = getMouselightDataEndpoint("/sample")
                .queryParam("sampleId", tmSample.getId());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
    }

    Collection<TmWorkspace> getTmWorkspaces() {
        WebTarget target = getMouselightDataEndpoint("/workspace");
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<TmWorkspace>>() {});
    }

    Collection<TmWorkspace> getTmWorkspacesForSample(Long sampleId) {
        WebTarget target = getMouselightDataEndpoint("/workspace")
                .queryParam("sampleId", sampleId);
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<TmWorkspace>>() {});
    }
    
    public TmWorkspace create(TmWorkspace tmWorkspace) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmWorkspace);
        WebTarget target = getMouselightDataEndpoint("/workspace");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmWorkspace.class);
    }

    public TmWorkspace copy(TmWorkspace tmWorkspace, String name, String assignOwner) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmWorkspace);
        query.setPropertyName("name");
        query.setPropertyValue(name);
        // overload objectType in domain query for assign owner subject key
        if (assignOwner!=null) {
            query.setObjectType(assignOwner);
        }
        WebTarget target = getMouselightDataEndpoint("/workspace/copy");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmWorkspace.class);
    }
    
    public TmWorkspace update(TmWorkspace tmWorkspace) {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmWorkspace);
        query.setSubjectKey(AccessManager.getSubjectKey());
        WebTarget target = getMouselightDataEndpoint("/workspace");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmWorkspace.class);
    }

    public void remove(TmWorkspace tmWorkspace) {
        WebTarget target = getMouselightDataEndpoint("/workspace")
                .queryParam("workspaceId", tmWorkspace.getId());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
    }

    public Long getWorkspaceNeuronCount(Long workspaceId) {
        WebTarget target = getMouselightDataEndpoint("/neurons/totals")
                .queryParam("workspaceId", workspaceId)
                .queryParam("subjectKey", AccessManager.getSubjectKey());

        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(Long.class);
    }

    Collection<TmNeuronMetadata> getWorkspaceNeurons(Long workspaceId, long offset, int length) {
        try {
            List<TmNeuronMetadata> neuronList = new ArrayList<>();
            WebTarget target = getMouselightDataEndpoint("/workspace/neuron")
                    .queryParam("workspaceId", workspaceId)
                    .queryParam("offset", offset)
                    .queryParam("length", length);
            ObjectMapper mapper = new ObjectMapper();
            InputStream is = target
                    .request("application/octet-stream")
                    .get(InputStream.class);
            JsonFactory factory = new JsonFactory();
            factory.setCodec(mapper);
            JsonParser parser  = factory.createParser(is);
            Iterator<TmNeuronMetadata> neurons = parser.readValuesAs(TmNeuronMetadata.class);
            while(neurons.hasNext()) {
                neuronList.add(neurons.next());
            }
            return neuronList;

        } catch (Exception e) {
            FrameworkAccess.handleException(e);
            LOG.error ("Problems parsing the neuron stream from the server for workspace id {}",workspaceId);
            throw new RemoteServiceException("Client had problems processing Neuron Server Stream");
        }
    }

    public TmNeuronMetadata create(TmNeuronMetadata neuronMetadata) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(neuronMetadata);
        WebTarget target = getMouselightDataEndpoint("/workspace/neuron");
        Response response = target
                .request()
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmNeuronMetadata.class);
    }

    public TmNeuronMetadata update(TmNeuronMetadata neuronMetadata) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(neuronMetadata);
        WebTarget target = getMouselightDataEndpoint("/workspace/neuron");
        Response response = target
                .request()
                .post(Entity.json(query));
        checkBadResponse(target, response);
        LOG.info("Updated {} - {}",neuronMetadata.getName(), neuronMetadata.getId());
        return response.readEntity(TmNeuronMetadata.class);
    }

    public List<TmNeuronMetadata> update(Collection<TmNeuronMetadata> neurons) {
        StopWatch w = new StopWatch();
        if (neurons.isEmpty()) return Collections.emptyList();

        WebTarget target = getMouselightDataEndpoint("/workspace/neuron");
        Response response = target
                .request()
                .post(Entity.json(neurons));
        checkBadResponse(target, response);

        List<TmNeuronMetadata> list = response.readEntity(new GenericType<List<TmNeuronMetadata>>() {});
        LOG.info("Updated {} in {} ms",neurons.toString(),w.getElapsedTime());
        return list;
    }

    void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate, Long workspaceId) {
        if (bulkNeuronStyleUpdate.getNeuronIds()==null || bulkNeuronStyleUpdate.getNeuronIds().isEmpty()) return;
        WebTarget target = getMouselightDataEndpoint("/workspace/neuronStyle")
                .queryParam("workspaceId", workspaceId);
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(bulkNeuronStyleUpdate));
        checkBadResponse(target, response);
    }
    
    public void remove(TmNeuronMetadata neuronMetadata) {
        WebTarget target = getMouselightDataEndpoint("/workspace/neuron")
                .queryParam("workspaceId", neuronMetadata.getWorkspaceId())
                .queryParam("neuronId", neuronMetadata.getId())
                .queryParam("isLarge", neuronMetadata.isLargeNeuron());
        Response response = target
                .request()
                .delete();
        checkBadResponse(target, response);
    }

    //==================================================================================================================
    // NEW API for shared data
    //==================================================================================================================

    public TmNeuronMetadata createNeuron(TmNeuronMetadata neuronMetadata) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(neuronMetadata);
        WebTarget target = getMouselightDataEndpoint("/shared/workspace/neuron");
        Response response = target
                .request()
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmNeuronMetadata.class);
    }

    public TmNeuronMetadata updateNeuron(TmNeuronMetadata neuronMetadata) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(neuronMetadata);
        WebTarget target = getMouselightDataEndpoint("/shared/workspace/neuron");
        Response response = target
                .request()
                .post(Entity.json(query));
        checkBadResponse(target, response);
        LOG.info("Updated {} - {}",neuronMetadata.getName(), neuronMetadata.getId());
        return response.readEntity(TmNeuronMetadata.class);
    }

    public TmNeuronMetadata changeOwnership(TmNeuronMetadata neuronMetadata, String targetUser) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(neuronMetadata);
        WebTarget target = getMouselightDataEndpoint("/shared/workspace/neuron/ownership")
            .queryParam("targetUser", targetUser);
        Response response = target
                .request()
                .post(Entity.json(query));
        checkBadResponse(target, response);
        LOG.info("Changed ownership of {} ({}) to {}",neuronMetadata.getName(), neuronMetadata.getId(), targetUser);
        return response.readEntity(TmNeuronMetadata.class);
    }

    public void removeNeuron(TmNeuronMetadata neuronMetadata) {
        WebTarget target = getMouselightDataEndpoint("/shared/workspace/neuron")
                .queryParam("workspaceId", neuronMetadata.getWorkspaceId())
                .queryParam("neuronId", neuronMetadata.getId())
                .queryParam("isLarge", neuronMetadata.isLargeNeuron());
        Response response = target
                .request()
                .delete();
        checkBadResponse(target, response);
    }

    //==================================================================================================================
    void changeTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) {
        if (neurons.isEmpty()) return;
        List<Long> neuronIds = DomainUtils.getIds(neurons);
        Long workspaceId = neurons.get(0).getWorkspaceId();
        WebTarget target = getMouselightDataEndpoint("/workspace/neuronTags")
                .queryParam("workspaceId", workspaceId)
                .queryParam("tags", StringUtils.join(tags, ","))
                .queryParam("tagState", tagState);
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(neuronIds));
        checkBadResponse(target, response);
    }
    
    public TmReviewTask create(TmReviewTask reviewTask) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(reviewTask);
        WebTarget target = getMouselightDataEndpoint("/reviewtask");
        Response response = target
                .request("application/json")
                .put(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmReviewTask.class);
    }
    
    public TmReviewTask update(TmReviewTask reviewTask) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(reviewTask);
        WebTarget target = getMouselightDataEndpoint("/reviewtask");
        Response response = target
                .request("application/json")
                .post(Entity.json(query));
        checkBadResponse(target, response);
        return response.readEntity(TmReviewTask.class);
    }   
    
    public void remove(TmReviewTask reviewTask) {
        WebTarget target = getMouselightDataEndpoint("/reviewtask")
                .queryParam("taskReviewId", reviewTask.getId());
        Response response = target
                .request("application/json")
                .delete();
        checkBadResponse(target, response);
    }

    CoordinateToRawTransform getLvvCoordToRawTransform(String basePath) {
        Client client = RestJsonClientManager.getInstance().getHttpClient(true);
        WebTarget target = client.target(getMouselightStreamURI(basePath, "volume_info"));
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .get();
        checkBadResponse(target, response);
        RenderedVolumeMetadata renderedVolume = response.readEntity(RenderedVolumeMetadata.class);
        return new CoordinateToRawTransform(renderedVolume.getOriginVoxel(), renderedVolume.getMicromsPerVoxel(), renderedVolume.getNumZoomLevels());
    }

    byte[] getRawTextureBytes(String basePath, int[] viewerCoord, int[] dimensions, int channel) throws Exception {
        Client client = RestJsonClientManager.getInstance().getHttpClient(true);
        WebTarget target = client.target(getMouselightStreamURI(basePath, "closest_raw_tile_stream"))
                .queryParam("x", viewerCoord[0])
                .queryParam("y", viewerCoord[1])
                .queryParam("z", viewerCoord[2])
                .queryParam("sx", dimensions[0])
                .queryParam("sy", dimensions[1])
                .queryParam("sz", dimensions[2])
                .queryParam("channel", channel);
        Response response = target
                .request(MediaType.APPLICATION_OCTET_STREAM)
                .get()
                ;
        checkBadResponse(target, response);
        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return null;
        } else {
            InputStream rawBytesStream = response.readEntity(InputStream.class);
            return ByteStreams.toByteArray(rawBytesStream);
        }
    }

    String getNearestChannelFilesURL(String basePath, int[] viewerCoord) {
        return getMouselightStreamURI(basePath, "closest_raw_tile_stream")
                    .resolve(String.format("?x=%d&y=%d&z=%d", viewerCoord[0], viewerCoord[1], viewerCoord[2]))
                    .toString();
    }

    public boolean isServerPathAvailable(String serverPath, boolean directoryOnly) {
        Client client = RestJsonClientManager.getInstance().getHttpClient(true);
        Response response = client.target(remoteStorageUrl)
                .path("storage_content/storage_path_redirect")
                .path(serverPath)
                .queryParam("directoryOnly", directoryOnly)
                .request()
                .head();
        int responseStatus = response.getStatus();
        return responseStatus == 200;
    }
}
