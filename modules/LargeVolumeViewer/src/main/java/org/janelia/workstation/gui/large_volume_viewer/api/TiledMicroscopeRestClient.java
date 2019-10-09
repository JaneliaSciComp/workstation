package org.janelia.workstation.gui.large_volume_viewer.api;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.google.common.io.ByteStreams;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartMediaTypes;
import org.janelia.it.jacs.model.user_data.tiledMicroscope.CoordinateToRawTransform;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.model.domain.DomainUtils;
import org.janelia.model.domain.tiledMicroscope.BulkNeuronStyleUpdate;
import org.janelia.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.model.domain.tiledMicroscope.TmReviewTask;
import org.janelia.model.domain.tiledMicroscope.TmSample;
import org.janelia.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.rendering.RenderedVolumeMetadata;
import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
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
    private WebTarget service;
    private String remoteApiUrl;
    private String remoteStorageUrl;

    public TiledMicroscopeRestClient() {
        this(ConsoleProperties.getInstance().getProperty("mouselight.rest.url"),
                ConsoleProperties.getInstance().getProperty("jadestorage.rest.url"));
    }

    public TiledMicroscopeRestClient(String remoteApiUrl, String remoteStorageUrl) {
        super(LOG);
        LOG.info("Using server URL: {}",remoteApiUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(remoteApiUrl, true);
        // TODO: convert the methods using these variables to reuse targets instead
        this.remoteApiUrl = remoteApiUrl;
        this.remoteStorageUrl = remoteStorageUrl;
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

    List<String> getTmSamplePaths() {
        WebTarget target = getMouselightDataEndpoint("/sampleRootPaths");
        Response response = target
                .request("application/json")
                .get();
        checkBadResponse(target, response);
        return response.readEntity(new GenericType<List<String>>() {});
    }

    void updateSamplePaths(List<String> paths) {
        WebTarget target = getMouselightDataEndpoint("/sampleRootPaths");
        Response response = target
                .request("application/json")
                .post(Entity.json(paths));
        checkBadResponse(target, response);
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

    Collection<Pair<TmNeuronMetadata,InputStream>> getWorkspaceNeuronPairs(Long workspaceId, long offset, int length) {
        WebTarget target = getMouselightDataEndpoint("/workspace/neuron")
                .queryParam("workspaceId", workspaceId)
                .queryParam("offset", offset)
                .queryParam("length", length)
                ;

        Response response = target
                .request("multipart/mixed")
                .get();
        checkBadResponse(target, response);
        MultiPart multipart = response.readEntity(MultiPart.class);
        List<Pair<TmNeuronMetadata,InputStream>> neurons = new ArrayList<>();
        BodyPart jsonPart = null;
        for(BodyPart bodyPart : multipart.getBodyParts()) {
            if (jsonPart==null) {
                // Every other part is a JSON representation
                jsonPart = bodyPart;
            }
            else  {
                // Every other part is the protobuf byte representation
                TmNeuronMetadata neuron = jsonPart.getEntityAs(TmNeuronMetadata.class);
                InputStream protobufStream = bodyPart.getEntityAs(InputStream.class);
                neurons.add(Pair.of(neuron, protobufStream));
                // Get ready to read the next pair
                jsonPart = null;
            }
        }
        return neurons;
    }

    TmNeuronMetadata createMetadata(TmNeuronMetadata neuronMetadata) {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE);
        WebTarget target = getMouselightDataEndpoint("/workspace/neuron");
        Response response = target
                .request()
                .put(Entity.entity(multiPart, multiPart.getMediaType()));
        checkBadResponse(target, response);
        return response.readEntity(TmNeuronMetadata.class);
    }

    public TmNeuronMetadata create(TmNeuronMetadata neuronMetadata, InputStream protobufStream) {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE)
                .field("protobufBytes", protobufStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        WebTarget target = getMouselightDataEndpoint("/workspace/neuron");
        Response response = target
                .request()
                .put(Entity.entity(multiPart, multiPart.getMediaType()));
        checkBadResponse(target, response);
        return response.readEntity(TmNeuronMetadata.class);
    }

    TmNeuronMetadata updateMetadata(TmNeuronMetadata neuronMetadata) {
        return update(neuronMetadata, null);
    }

    public TmNeuronMetadata update(TmNeuronMetadata neuronMetadata, InputStream protobufStream) {
       List<TmNeuronMetadata> list = update(Arrays.asList(Pair.of(neuronMetadata, protobufStream)));
       if (list.isEmpty()) return null;
       if (list.size()>1) LOG.warn("update(TmNeuronMetadata) returned more than one result.");
       return list.get(0);
    }

    List<TmNeuronMetadata> updateMetadata(List<TmNeuronMetadata> neuronList) {
        List<Pair<TmNeuronMetadata,InputStream>> pairs = new ArrayList<>();
        for(TmNeuronMetadata tmNeuronMetadata : neuronList) {
            pairs.add(Pair.of(tmNeuronMetadata, null));
        }
        return update(pairs);
    }

    public List<TmNeuronMetadata> update(Collection<Pair<TmNeuronMetadata,InputStream>> neuronPairs) {
        StopWatch w = new StopWatch();
        if (neuronPairs.isEmpty()) return Collections.emptyList();
        MultiPart multiPartEntity = new MultiPart();
        for (Pair<TmNeuronMetadata, InputStream> neuronPair : neuronPairs) {
            multiPartEntity.bodyPart(new BodyPart(neuronPair.getLeft(), MediaType.APPLICATION_JSON_TYPE));
            if (neuronPair.getRight()!=null) {
                multiPartEntity.bodyPart(new BodyPart(neuronPair.getRight(), MediaType.APPLICATION_OCTET_STREAM_TYPE));
            }
            else {
                multiPartEntity.bodyPart(new BodyPart("", MediaType.TEXT_PLAIN_TYPE));
            }
        }
        
        String logStr;
        if (neuronPairs.size()==1) {
            TmNeuronMetadata neuron = neuronPairs.iterator().next().getLeft();
            logStr = neuron==null?"null neuron":neuron.toString();
        }
        else {
            logStr = neuronPairs.size()+" neurons";
        }

        WebTarget target = getMouselightDataEndpoint("/workspace/neuron");
        Response response = target
                .request()
                .post(Entity.entity(multiPartEntity, MultiPartMediaTypes.MULTIPART_MIXED));
        checkBadResponse(target, response);

        List<TmNeuronMetadata> list = response.readEntity(new GenericType<List<TmNeuronMetadata>>() {});
        LOG.info("Updated {} in {} ms",logStr,w.getElapsedTime());
        return list;
    }

    void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate) {
        if (bulkNeuronStyleUpdate.getNeuronIds()==null || bulkNeuronStyleUpdate.getNeuronIds().isEmpty()) return;
        WebTarget target = getMouselightDataEndpoint("/workspace/neuronStyle");
        Response response = target
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(bulkNeuronStyleUpdate));
        checkBadResponse(target, response);
    }
    
    public void remove(TmNeuronMetadata neuronMetadata) {
        WebTarget target = getMouselightDataEndpoint("/workspace/neuron")
                .queryParam("neuronId", neuronMetadata.getId());
        Response response = target
                .request()
                .delete();
        checkBadResponse(target, response);
    }

    void changeTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) {
        if (neurons.isEmpty()) return;
        List<Long> neuronIds = DomainUtils.getIds(neurons);
        WebTarget target = getMouselightDataEndpoint("/workspace/neuronTags")
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
