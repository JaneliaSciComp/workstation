package org.janelia.workstation.gui.large_volume_viewer.api;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.io.ByteStreams;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
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
import org.janelia.workstation.core.api.http.HttpServiceUtils;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A web client providing access to the Tiled Microscope REST Service.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class TiledMicroscopeRestClient {

    private static final Logger LOG = LoggerFactory.getLogger(TiledMicroscopeRestClient.class);
    private final String remoteApiUrl = ConsoleProperties.getInstance().getProperty("mouselight.rest.url");
    private final String remoteStorageUrl = ConsoleProperties.getInstance().getProperty("jadestorage.rest.url");
    private final Client client;

    public TiledMicroscopeRestClient() {

        LOG.info("Using server URL: {}",remoteApiUrl);

        ClientConfig clientConfig =
                new ClientConfig()
                        .connectorProvider(new ApacheConnectorProvider())
                        .property(ClientProperties.FOLLOW_REDIRECTS, true)
                        ;

        this.client = ClientBuilder.newClient(clientConfig);
        
        JacksonJsonProvider provider = new JacksonJaxbJsonProvider();
        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) {
                LOG.error("Failed to deserialize property which does not exist in model: {}.{}",beanOrClass.getClass().getName(),propertyName);
                return true;
            }
        });

        // Add access token to every request
        ClientRequestFilter authFilter = requestContext -> {
            for (Entry<String, String> entry : HttpServiceUtils.getExtraHeaders(true).entrySet()) {
                requestContext.getHeaders().add(entry.getKey(), entry.getValue());
            }
        };
        client.register(authFilter);
        
        client.register(provider);
        client.register(MultiPartFeature.class);
    }

    private WebTarget getMouselightDataEndpoint(String suffix) {
        return getMouselightEndpoint()
                .path("mouselight/data")
                .path(suffix)
                ;
    }

    private WebTarget getMouselightEndpoint() {
        return client.target(remoteApiUrl)
                .queryParam("subjectKey", AccessManager.getSubjectKey());
    }

    List<String> getTmSamplePaths() {
        Response response = getMouselightDataEndpoint("/sampleRootPaths")
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmSamplePaths")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<String>>() {});
    }

    void updateSamplePaths(List<String> paths) {
        Response response = getMouselightDataEndpoint("/sampleRootPaths")
                .request("application/json")
                .post(Entity.json(paths));
        if (checkBadResponse(response, "update: " + paths)) {
            throw new WebApplicationException(response);
        }
    }
    
    Collection<TmSample> getTmSamples() {
        Response response = getMouselightDataEndpoint("/sample")
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmSamples")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmSample>>() {});
    }
        
    Map<String,Object> getTmSampleConstants(String samplePath) {
        Response response = getMouselightDataEndpoint("/sample/constants")
                .queryParam("samplePath", samplePath)
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmSampleConstants")) {
            LOG.error("Error getting sample constants from {}", samplePath);
            return null;
       }
        return response.readEntity(new GenericType<Map<String,Object>>() {});
    }

    public TmSample create(TmSample tmSample) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmSample);
        Response response = getMouselightDataEndpoint("/sample")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response, "create: "+tmSample)) {
            LOG.error("Error creating sample {}", tmSample);
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmSample.class);
    }

    public TmSample update(TmSample tmSample) {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmSample);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = getMouselightDataEndpoint("/sample")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response, "update: " + tmSample)) {
            LOG.error("Error updating sample {}", tmSample);
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmSample.class);
    }

    public void remove(TmSample tmSample) {
        Response response = getMouselightDataEndpoint("/sample")
                .queryParam("sampleId", tmSample.getId())
                .request("application/json")
                .delete();
        if (checkBadResponse(response, "remove: " + tmSample)) {
            throw new WebApplicationException(response);
        }
    }

    Collection<TmWorkspace> getTmWorkspaces() {
        Response response = getMouselightDataEndpoint("/workspace")
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmWorkspaces")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmWorkspace>>() {});
    }

    Collection<TmWorkspace> getTmWorkspacesForSample(Long sampleId) {
        Response response = getMouselightDataEndpoint("/workspace")
                .queryParam("sampleId", sampleId)
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmWorkspaces")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmWorkspace>>() {});
    }
    
    public TmWorkspace create(TmWorkspace tmWorkspace) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmWorkspace);
        Response response = getMouselightDataEndpoint("/workspace")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response, "create: "+tmWorkspace)) {
            throw new WebApplicationException(response);
        }
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
        Response response = getMouselightDataEndpoint("/workspace/copy")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response, "create: "+tmWorkspace)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmWorkspace.class);
    }
    
    public TmWorkspace update(TmWorkspace tmWorkspace) {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmWorkspace);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = getMouselightDataEndpoint("/workspace")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response, "update: " + tmWorkspace)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmWorkspace.class);
    }

    public void remove(TmWorkspace tmWorkspace) {
        Response response = getMouselightDataEndpoint("/workspace")
                .queryParam("workspaceId", tmWorkspace.getId())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "remove: " + tmWorkspace)) {
            throw new WebApplicationException(response);
        }
    }

    Collection<Pair<TmNeuronMetadata,InputStream>> getWorkspaceNeuronPairs(Long workspaceId) {
        Response response = getMouselightDataEndpoint("/workspace/neuron")
                .queryParam("workspaceId", workspaceId)
                .request("multipart/mixed")
                .get();
        if (checkBadResponse(response, "getWorkspaceNeuronPairs: "+workspaceId)) {
            throw new WebApplicationException(response);
        }
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
        Response response = getMouselightDataEndpoint("/workspace/neuron")
                .request()
                .put(Entity.entity(multiPart, multiPart.getMediaType()));
        if (checkBadResponse(response, "createMetadata: "+neuronMetadata)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmNeuronMetadata.class);
    }

    public TmNeuronMetadata create(TmNeuronMetadata neuronMetadata, InputStream protobufStream) {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE)
                .field("protobufBytes", protobufStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        Response response = getMouselightDataEndpoint("/workspace/neuron")
                .request()
                .put(Entity.entity(multiPart, multiPart.getMediaType()));
        if (checkBadResponse(response, "create: "+neuronMetadata)) {
            throw new WebApplicationException(response);
        }
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
        
        Response response = getMouselightDataEndpoint("/workspace/neuron")
                .request()
                .post(Entity.entity(multiPartEntity, MultiPartMediaTypes.MULTIPART_MIXED));
        if (checkBadResponse(response, "update: " +logStr)) {
            throw new WebApplicationException(response);
        }

        List<TmNeuronMetadata> list = response.readEntity(new GenericType<List<TmNeuronMetadata>>() {});
        LOG.info("Updated {} in {} ms",logStr,w.getElapsedTime());
        return list;
    }

    void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate) {
        if (bulkNeuronStyleUpdate.getNeuronIds()==null || bulkNeuronStyleUpdate.getNeuronIds().isEmpty()) return;
        Response response = getMouselightDataEndpoint("/workspace/neuronStyle")
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(bulkNeuronStyleUpdate));
        if (checkBadResponse(response, "updateNeuronStyles")) {
            throw new WebApplicationException(response);
        }
    }
    
    public void remove(TmNeuronMetadata neuronMetadata) {
        Response response = getMouselightDataEndpoint("/workspace/neuron")
                .queryParam("neuronId", neuronMetadata.getId())
                .request()
                .delete();
        if (checkBadResponse(response.getStatus(), "remove: " + neuronMetadata)) {
            throw new WebApplicationException(response);
        }
    }

    void changeTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) {
        if (neurons.isEmpty()) return;
        List<Long> neuronIds = DomainUtils.getIds(neurons);
        Response response = getMouselightDataEndpoint("/workspace/neuronTags")
                .queryParam("tags", StringUtils.join(tags, ","))
                .queryParam("tagState", tagState)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(neuronIds));
        if (checkBadResponse(response, "bulkEditTags")) {
            throw new WebApplicationException(response);
        }
    }
    
    public TmReviewTask create(TmReviewTask reviewTask) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(reviewTask);
        Response response = getMouselightDataEndpoint("/reviewtask")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response, "create: "+reviewTask)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmReviewTask.class);
    }
    
    public TmReviewTask update(TmReviewTask reviewTask) {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(reviewTask);
        Response response = getMouselightDataEndpoint("/reviewtask")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response, "update: "+reviewTask)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmReviewTask.class);
    }   
    
    public void remove(TmReviewTask reviewTask) {
        Response response = getMouselightDataEndpoint("/reviewtask")
                .queryParam("taskReviewId", reviewTask.getId())
                .request("application/json")
                .delete();
        if (checkBadResponse(response, "remove: " + reviewTask)) {
            throw new WebApplicationException(response);
        }
    }

    CoordinateToRawTransform getLvvCoordToRawTransform(String basePath) {
        Response response = client.target(getMouselightStreamURI(basePath, "volume_info"))
                .request(MediaType.APPLICATION_JSON)
                .get()
                ;
        if (checkBadResponse(response, "lvvTransform")) {
            throw new WebApplicationException(response);
        }
        RenderedVolumeMetadata renderedVolume = response.readEntity(RenderedVolumeMetadata.class);
        return new CoordinateToRawTransform(renderedVolume.getOriginVoxel(), renderedVolume.getMicromsPerVoxel(), renderedVolume.getNumZoomLevels());
    }

    byte[] getRawTextureBytes(String basePath, int[] viewerCoord, int[] dimensions, int channel) throws Exception {
        Response response = client.target(getMouselightStreamURI(basePath, "closest_raw_tile_stream"))
                .queryParam("x", viewerCoord[0])
                .queryParam("y", viewerCoord[1])
                .queryParam("z", viewerCoord[2])
                .queryParam("sx", dimensions[0])
                .queryParam("sy", dimensions[1])
                .queryParam("sz", dimensions[2])
                .queryParam("channel", channel)
                .request(MediaType.APPLICATION_OCTET_STREAM)
                .get()
                ;
        if (checkBadResponse(response, "raw image bytes")) {
            throw new WebApplicationException(response);
        }
        if (response.getStatus() == Response.Status.NO_CONTENT.getStatusCode()) {
            return null;
        } else {
            InputStream rawBytesStream = response.readEntity(InputStream.class);
            return ByteStreams.toByteArray(rawBytesStream);
        }
    }

    String getNearestChannelFilesURL(String basePath, int[] viewerCoord) {
        return getMouselightStreamURI(basePath, "closest_raw_tile_stream")
                    .resolve(String.format("?x=%d&y=%dz=%d", viewerCoord[0], viewerCoord[1], viewerCoord[2]))
                    .toString();
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

    public boolean isServerPathAvailable(String serverPath, boolean directoryOnly) {
        Response response = client.target(remoteStorageUrl)
                .path("storage_content/storage_path_redirect")
                .path(serverPath)
                .queryParam("directoryOnly", directoryOnly)
                .request()
                .head();
        int responseStatus = response.getStatus();
        return responseStatus == 200;
    }

    private boolean checkBadResponse(Response response, String failureError) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            LOG.error("Problem making request for {}", failureError);
            // TODO: we want to print the request URI here, but I don't have time to search through the JAX-RS APIs right now
            LOG.error("Server responded with error code: {} {}",response.getStatus(), status);
            return true;
        }
        return false;
    }

    private boolean checkBadResponse(int responseStatus, String failureError) {
        if (responseStatus<200 || responseStatus>=300) {
            LOG.error("ERROR RESPONSE: " + responseStatus);
            LOG.error(failureError);
            return true;
        }
        return false;
    }
}
