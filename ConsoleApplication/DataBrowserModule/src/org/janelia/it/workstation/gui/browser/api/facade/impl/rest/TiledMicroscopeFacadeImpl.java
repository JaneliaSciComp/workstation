package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.MultiPartMediaTypes;
import org.janelia.it.jacs.model.domain.support.DomainUtils;
import org.janelia.it.jacs.model.domain.tiledMicroscope.BulkNeuronStyleUpdate;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.TiledMicroscopeFacade;
import org.perf4j.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TiledMicroscopeFacadeImpl extends RESTClientImpl implements TiledMicroscopeFacade {

    private static final Logger log = LoggerFactory.getLogger(TiledMicroscopeFacadeImpl.class);

    private RESTClientManager manager;

    public TiledMicroscopeFacadeImpl() {
        super(log);
        this.manager = RESTClientManager.getInstance();
    }
    
    @Override
    public List<String> getTmSamplePaths() throws Exception {
        Response response = manager.getMouselightEndpoint("/sampleRootPaths")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmSamplePaths")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<String>>() {});
    }

    @Override
    public void updateSamplePaths(List<String> paths) throws Exception {
        Response response = manager.getMouselightEndpoint("/sampleRootPaths")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .post(Entity.json(paths));
        if (checkBadResponse(response, "update: " + paths)) {
            throw new WebApplicationException(response);
        }
    }
    
    @Override
    public Collection<TmSample> getTmSamples() throws Exception {
        Response response = manager.getMouselightEndpoint("/sample")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmSamples")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmSample>>() {});
    }

    @Override
    public TmSample create(TmSample tmSample) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmSample);
        Response response = manager.getMouselightEndpoint("/sample")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response, "create: "+tmSample)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmSample.class);
    }

    @Override
    public TmSample update(TmSample tmSample) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmSample);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getMouselightEndpoint("/sample")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response, "update: " + tmSample)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmSample.class);
    }

    @Override
    public void remove(TmSample tmSample) throws Exception {
        Response response = manager.getMouselightEndpoint("/sample")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("sampleId", tmSample.getId())
                .request("application/json")
                .delete();
        if (checkBadResponse(response, "remove: " + tmSample)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public Collection<TmWorkspace> getTmWorkspaces() throws Exception {
        Response response = manager.getMouselightEndpoint("/workspace")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmWorkspaces")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmWorkspace>>() {});
    }

    public Collection<TmWorkspace> getTmWorkspacesForSample(Long sampleId) throws Exception {
        Response response = manager.getMouselightEndpoint("/workspace")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("sampleId", sampleId)
                .request("application/json")
                .get();
        if (checkBadResponse(response, "getTmWorkspaces")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmWorkspace>>() {});
    }
    
    @Override
    public TmWorkspace create(TmWorkspace tmWorkspace) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmWorkspace);
        Response response = manager.getMouselightEndpoint("/workspace")
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response, "create: "+tmWorkspace)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmWorkspace.class);
    }

    @Override
    public TmWorkspace update(TmWorkspace tmWorkspace) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmWorkspace);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getMouselightEndpoint("/workspace")
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response, "update: " + tmWorkspace)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmWorkspace.class);
    }

    @Override
    public void remove(TmWorkspace tmWorkspace) throws Exception {
        Response response = manager.getMouselightEndpoint("/workspace")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("workspaceId", tmWorkspace.getId())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "remove: " + tmWorkspace)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public Collection<Pair<TmNeuronMetadata,InputStream>> getWorkspaceNeuronPairs(Long workspaceId) throws Exception {
        Response response = manager.getMouselightEndpoint("/workspace/neuron")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
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

    @Override
    public TmNeuronMetadata createMetadata(TmNeuronMetadata neuronMetadata) throws Exception {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE);
        Response response = manager.getMouselightEndpoint("/workspace/neuron")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request()
                .put(Entity.entity(multiPart, multiPart.getMediaType()));
        if (checkBadResponse(response, "createMetadata: "+neuronMetadata)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmNeuronMetadata.class);
    }

    @Override
    public TmNeuronMetadata create(TmNeuronMetadata neuronMetadata, InputStream protobufStream) throws Exception {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE)
                .field("protobufBytes", protobufStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        Response response = manager.getMouselightEndpoint("/workspace/neuron")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request()
                .put(Entity.entity(multiPart, multiPart.getMediaType()));
        if (checkBadResponse(response, "create: "+neuronMetadata)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmNeuronMetadata.class);
    }

    @Override
    public TmNeuronMetadata updateMetadata(TmNeuronMetadata neuronMetadata) throws Exception {
        return update(neuronMetadata, null);
    }

    @Override
    public TmNeuronMetadata update(TmNeuronMetadata neuronMetadata, InputStream protobufStream) throws Exception {
       List<TmNeuronMetadata> list = update(Arrays.asList(Pair.of(neuronMetadata, protobufStream)));
       if (list.isEmpty()) return null;
       if (list.size()>1) log.warn("update(TmNeuronMetadata) returned more than one result.");
       return list.get(0);
    }

    @Override
    public List<TmNeuronMetadata> updateMetadata(List<TmNeuronMetadata> neuronList) throws Exception {
        List<Pair<TmNeuronMetadata,InputStream>> pairs = new ArrayList<>();
        for(TmNeuronMetadata tmNeuronMetadata : neuronList) {
            pairs.add(Pair.of(tmNeuronMetadata, (InputStream)null));
        }
        return update(pairs);
    }

    @Override
    public List<TmNeuronMetadata> update(Collection<Pair<TmNeuronMetadata,InputStream>> neuronPairs) throws Exception {
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
        
        Response response = manager.getMouselightEndpoint("/workspace/neuron")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request()
                .post(Entity.entity(multiPartEntity, MultiPartMediaTypes.MULTIPART_MIXED));
        if (checkBadResponse(response, "update: " +neuronPairs.size()+" neurons")) {
            throw new WebApplicationException(response);
        }

        List<TmNeuronMetadata> list = response.readEntity(new GenericType<List<TmNeuronMetadata>>() {});

        log.info("Updated {} neurons in {} ms",neuronPairs.size(),w.getElapsedTime());
        return list;
    }

    @Override
    public void updateNeuronStyles(BulkNeuronStyleUpdate bulkNeuronStyleUpdate) throws Exception {
        if (bulkNeuronStyleUpdate.getNeuronIds()==null || bulkNeuronStyleUpdate.getNeuronIds().isEmpty()) return;
        Response response = manager.getMouselightEndpoint("/workspace/neuronStyle")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(bulkNeuronStyleUpdate));
        if (checkBadResponse(response, "updateNeuronStyles")) {
            throw new WebApplicationException(response);
        }
    }
    
    @Override
    public void remove(TmNeuronMetadata neuronMetadata) throws Exception {
        Response response = manager.getMouselightEndpoint("/workspace/neuron")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("neuronId", neuronMetadata.getId())
                .request()
                .delete();
        if (checkBadResponse(response.getStatus(), "remove: " + neuronMetadata)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public void changeTags(List<TmNeuronMetadata> neurons, List<String> tags, boolean tagState) throws Exception {
        if (neurons.isEmpty()) return;
        List<Long> neuronIds = DomainUtils.getIds(neurons);
        Response response = manager.getMouselightEndpoint("/workspace/neuronTags")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("tags", StringUtils.join(tags, ","))
                .queryParam("tagState", tagState)
                .request(MediaType.APPLICATION_JSON)
                .post(Entity.json(neuronIds));
        if (checkBadResponse(response, "bulkEditTags")) {
            throw new WebApplicationException(response);
        }
    }
}
