package org.janelia.it.workstation.gui.large_volume_viewer.api;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwDecision;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwGraph;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwSession;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.dtw.DtwSessionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;

/**
 * A web client providing access to the Semi-Automated Tracing API REST Service.
 *
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class DirectedTracingWorkflowRestClient {

    private static final Logger log = LoggerFactory.getLogger(DirectedTracingWorkflowRestClient.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("mouselight.dtw.rest.url");

    private final Client client;

    public DirectedTracingWorkflowRestClient() {
        log.info("Using server URL: {}",REMOTE_API_URL);
        JacksonJsonProvider provider = new JacksonJaxbJsonProvider();
        ObjectMapper mapper = provider.locateMapper(Object.class, MediaType.APPLICATION_JSON_TYPE);
        mapper.addHandler(new DeserializationProblemHandler() {
            @Override
            public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser jp, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException, JsonProcessingException {
                log.error("Failed to deserialize property which does not exist in model: {}.{}",beanOrClass.getClass().getName(),propertyName);
                return true;
            }
        });
        this.client = ClientBuilder.newClient();
        client.register(provider);
    }

    private WebTarget getEndpoint(String suffix) {
        return client.target(REMOTE_API_URL + suffix).queryParam("subjectKey", AccessManager.getSubjectKey());
    }
    
    public List<DtwGraph> getGraphs() throws Exception {
        WebTarget endpoint = getEndpoint("/graph/");
        Response response = endpoint
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        checkBadResponse(endpoint, response);
        return response.readEntity(new GenericType<List<DtwGraph>>() {});
    }

    public DtwGraph getGraph(String graphId) throws Exception {
        WebTarget endpoint = getEndpoint("/graph/"+graphId);
        Response response = endpoint
                .request("application/json")
                .get();
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwGraph.class);
    }
    
    public DtwGraph getLatestGraph(String samplePath) throws Exception {
        WebTarget endpoint = getEndpoint("/graph/findLatest");
        Response response = endpoint
                .queryParam("samplePath", samplePath)
                .request("application/json")
                .get();
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwGraph.class);
    }
    
    public DtwGraph create(DtwGraph graph) throws Exception {
        WebTarget endpoint = getEndpoint("/graph/");
        Response response = endpoint
                .request("application/json")
                .post(Entity.json(graph));
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwGraph.class);
    }

    public DtwGraph startGraphUpdate(String graphId) throws Exception {
        WebTarget endpoint = getEndpoint("/graph/"+graphId+"/update");
        Response response = endpoint
                .request("application/json")
                .post(Entity.json(null));
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwGraph.class);
    }
    
    public DtwSession createSession(DtwGraph graph, DtwSessionType sessionType) throws Exception {
        DtwSession session = new DtwSession();
        session.setGraphId(graph.getId());
        session.setSessionType(sessionType.getLabel());
        WebTarget endpoint = getEndpoint("/session/");
        Response response = endpoint
                .request("application/json")
                .post(Entity.json(session));
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwSession.class);
    }

    public DtwSession getSession(String sessionId) throws Exception {
        WebTarget endpoint = getEndpoint("/session/"+sessionId);
        Response response = endpoint
                .request("application/json")
                .get();
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwSession.class);
    }
    
    public DtwDecision getNextDecision(String sessionId) throws Exception {
        WebTarget endpoint = getEndpoint("/session/"+sessionId+"/next");
        Response response = endpoint
                .request("application/json")
                .get();
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwDecision.class);
    }
    
    public DtwDecision updateDecision(DtwDecision decision) throws Exception {
        WebTarget endpoint = getEndpoint("/session/"+decision.getSessionId()+"/decision/"+decision.getId());
        Response response = endpoint
                .request("application/json")
                .post(Entity.json(decision));
        checkBadResponse(endpoint, response);
        return response.readEntity(DtwDecision.class);
    }
    
    private void checkBadResponse(WebTarget endpoint, Response response) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Problem making request for: {}", endpoint.getUri());
            log.error("Server responded with error code: {} {}",response.getStatus(), status);
            throw new WebApplicationException(response);
        }
    }
}
