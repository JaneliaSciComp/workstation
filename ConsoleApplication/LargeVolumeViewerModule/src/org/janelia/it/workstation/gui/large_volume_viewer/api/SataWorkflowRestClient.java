package org.janelia.it.workstation.gui.large_volume_viewer.api;

import java.io.IOException;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.gui.large_volume_viewer.api.model.sata.SataGraph;
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
        client.register(MultiPartFeature.class);
    }

    public WebTarget getEndpoint(String suffix) {
        return client.target(REMOTE_API_URL + suffix).queryParam("subjectKey", AccessManager.getSubjectKey());
    }
    
    public List<SataGraph> getGraphs() throws Exception {
        Response response = getEndpoint("/graph")
                .queryParam("subjectKey", AccessManager.getSubjectKey())
                .request("application/json")
                .get();
        checkBadResponse(response, "getGraphs");
        return response.readEntity(new GenericType<List<SataGraph>>() {});
    }
    
    protected void checkBadResponse(Response response, String failureError) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Problem making request for {}", failureError);
            // TODO: we want to print the request URI here, but I don't have time to search through the JAX-RS APIs right now
            log.error("Server responded with error code: {} {}",response.getStatus(), status);
            throw new WebApplicationException(response);
        }
    }
}
