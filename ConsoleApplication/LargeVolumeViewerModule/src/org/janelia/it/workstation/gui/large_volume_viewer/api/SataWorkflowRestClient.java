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
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata.SataDecision;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata.SataGraph;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata.SataSession;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata.SataSessionType;
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
public class SataWorkflowRestClient {

    private static final Logger log = LoggerFactory.getLogger(SataWorkflowRestClient.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("mouselight.sata.rest.url");

    private final Client client;

    public SataWorkflowRestClient() {
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

    public WebTarget getEndpoint(String suffix) {
        return client.target(REMOTE_API_URL + suffix).queryParam("subjectKey", AccessManager.getSubjectKey());
    }
    
    public List<SataGraph> getGraphs() throws Exception {
        Response response = getEndpoint("/graph/")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        checkBadResponse(response, "getGraphs");
        return response.readEntity(new GenericType<List<SataGraph>>() {});
    }

    public SataGraph getGraph(String graphId) throws Exception {
        Response response = getEndpoint("/graph/"+graphId)
                .request("application/json")
                .get();
        checkBadResponse(response, "getGraphById");
        return response.readEntity(SataGraph.class);
    }
    
    public SataGraph getLatestGraph(String samplePath) throws Exception {
        Response response = getEndpoint("/graph/findLatest")
                .queryParam("samplePath", samplePath)
                .request("application/json")
                .get();
        checkBadResponse(response, "findLatestGraph");
        return response.readEntity(SataGraph.class);
    }
    
    public SataGraph create(SataGraph graph) throws Exception {
        Response response = getEndpoint("/graph/")
                .request("application/json")
                .post(Entity.json(graph));
        checkBadResponse(response, "createGraph");
        return response.readEntity(SataGraph.class);
    }

    public SataGraph startGraphUpdate(String graphId) throws Exception {
        Response response = getEndpoint("/graph/"+graphId+"/update")
                .request("application/json")
                .post(Entity.json(null));
        checkBadResponse(response, "updateGraph");
        return response.readEntity(SataGraph.class);
    }
    
    public SataSession createSession(SataGraph graph, SataSessionType sessionType) throws Exception {
        SataSession session = new SataSession();
        session.setGraphId(graph.getId());
        session.setSessionType(sessionType.getLabel());
        Response response = getEndpoint("/session/")
                .request("application/json")
                .post(Entity.json(session));
        checkBadResponse(response, "createSession");
        return response.readEntity(SataSession.class);
    }

    public SataSession getSession(String sessionId) throws Exception {
        Response response = getEndpoint("/session/"+sessionId)
                .request("application/json")
                .get();
        checkBadResponse(response, "getSession");
        return response.readEntity(SataSession.class);
    }
    
    public SataDecision getNextDecision(String sessionId) throws Exception {
        Response response = getEndpoint("/session/"+sessionId+"/next")
                .request("application/json")
                .get();
        checkBadResponse(response, "getNextDecision");
        return response.readEntity(SataDecision.class);
    }
    
    public SataDecision updateDecision(SataDecision decision) throws Exception {
        WebTarget target = getEndpoint("/session/"+decision.getSessionId()+"/decision/"+decision.getId());
        Response response = target
                .request("application/json")
                .post(Entity.json(decision));
        checkBadResponse(response, "updateDecision");
        return response.readEntity(SataDecision.class);
    }
    
    protected void checkBadResponse(Response response, String failureError) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Problem making request for {}", failureError);
            log.error("Server responded with error code: {} {}",response.getStatus(), status);
            throw new WebApplicationException(response);
        }
    }
}
