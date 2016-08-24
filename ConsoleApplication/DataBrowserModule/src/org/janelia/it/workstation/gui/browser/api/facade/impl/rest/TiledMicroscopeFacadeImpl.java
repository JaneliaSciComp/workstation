package org.janelia.it.workstation.gui.browser.api.facade.impl.rest;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.tuple.Pair;
import org.glassfish.jersey.media.multipart.BodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmNeuronMetadata;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmSample;
import org.janelia.it.jacs.model.domain.tiledMicroscope.TmWorkspace;
import org.janelia.it.jacs.shared.utils.DomainQuery;
import org.janelia.it.workstation.gui.browser.api.AccessManager;
import org.janelia.it.workstation.gui.browser.api.facade.interfaces.TiledMicroscopeFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TiledMicroscopeFacadeImpl extends RESTClientImpl implements TiledMicroscopeFacade {

    private static final Logger log = LoggerFactory.getLogger(RESTClientManager.class);

    private RESTClientManager manager;

    public TiledMicroscopeFacadeImpl() {
        this.manager = RESTClientManager.getInstance();
    }

    @Override
    public Collection<TmSample> getTmSamples() throws Exception {
        Response response = manager.getTmSampleEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getTmSamples from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmSample>>() {});
    }

    @Override
    public TmSample create(TmSample tmSample) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmSample);
        Response response = manager.getTmSampleEndpoint()
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createTmSample from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmSample.class);
    }

    @Override
    public TmSample update(TmSample tmSample) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmSample);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getTmSampleEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateTmSample to server: " + tmSample)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmSample.class);
    }

    @Override
    public void remove(TmSample tmSample) throws Exception {
        Response response = manager.getTmSampleEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("tmSampleId", tmSample.getId())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeTmSample from server: " + tmSample)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public Collection<TmWorkspace> getTmWorkspaces() throws Exception {
        Response response = manager.getTmWorkspaceEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getTmWorkspaces from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(new GenericType<List<TmWorkspace>>() {});
    }

    @Override
    public TmWorkspace create(TmWorkspace tmWorkspace) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setSubjectKey(AccessManager.getSubjectKey());
        query.setDomainObject(tmWorkspace);
        Response response = manager.getTmWorkspaceEndpoint()
                .request("application/json")
                .put(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request createTmWorkspace from server")) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmWorkspace.class);
    }

    @Override
    public TmWorkspace update(TmWorkspace tmWorkspace) throws Exception {
        DomainQuery query = new DomainQuery();
        query.setDomainObject(tmWorkspace);
        query.setSubjectKey(AccessManager.getSubjectKey());
        Response response = manager.getTmWorkspaceEndpoint()
                .request("application/json")
                .post(Entity.json(query));
        if (checkBadResponse(response.getStatus(), "problem making request updateTmWorkspace to server: " + tmWorkspace)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmWorkspace.class);
    }

    @Override
    public void remove(TmWorkspace tmWorkspace) throws Exception {
        Response response = manager.getTmWorkspaceEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("tmWorkspaceId", tmWorkspace.getId())
                .request("application/json")
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeTmWorkspace from server: " + tmWorkspace)) {
            throw new WebApplicationException(response);
        }
    }

    @Override
    public Collection<Pair<TmNeuronMetadata,InputStream>> getWorkspaceNeuronPairs(Long workspaceId) throws Exception {
        Response response = manager.getTmNeuronEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("workspaceId", workspaceId)
                .request("multipart/mixed")
                .get();
        if (checkBadResponse(response.getStatus(), "problem making request getWorkspaceNeurons from server: "+response.getLocation())) {
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
    public TmNeuronMetadata create(TmNeuronMetadata neuronMetadata, InputStream protobufStream) throws Exception {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE)
                .field("protobufBytes", protobufStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        Response response = manager.getTmNeuronEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request()
                .put(Entity.entity(multiPart, multiPart.getMediaType()));
        if (checkBadResponse(response.getStatus(), "problem making request createTmNeuron from server: "+neuronMetadata)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmNeuronMetadata.class);
    }

    @Override
    public TmNeuronMetadata updateMetadata(TmNeuronMetadata neuronMetadata) throws Exception {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE);
        Response response = manager.getTmNeuronEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request()
                .post(Entity.entity(multiPart, multiPart.getMediaType()));
        if (checkBadResponse(response.getStatus(), "problem making request updateTmNeuronMetadata to server: " + neuronMetadata)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmNeuronMetadata.class);
    }

    @Override
    public TmNeuronMetadata update(TmNeuronMetadata neuronMetadata, InputStream protobufStream) throws Exception {
        FormDataMultiPart multiPart = new FormDataMultiPart()
                .field("neuronMetadata", neuronMetadata, MediaType.APPLICATION_JSON_TYPE)
                .field("protobufBytes", protobufStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        Response response = manager.getTmNeuronEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request()
                .post(Entity.entity(multiPart, multiPart.getMediaType()));
        if (checkBadResponse(response.getStatus(), "problem making request updateTmNeuron to server: " + neuronMetadata)) {
            throw new WebApplicationException(response);
        }
        return response.readEntity(TmNeuronMetadata.class);
    }

    @Override
    public void remove(TmNeuronMetadata neuronMetadata) throws Exception {
        Response response = manager.getTmNeuronEndpoint()
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .queryParam("neuronId", neuronMetadata.getId())
                .request()
                .delete();
        if (checkBadResponse(response.getStatus(), "problem making request removeTmNeuron from server: " + neuronMetadata)) {
            throw new WebApplicationException(response);
        }
    }
}
