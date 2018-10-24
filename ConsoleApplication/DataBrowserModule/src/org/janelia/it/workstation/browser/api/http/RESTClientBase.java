package org.janelia.it.workstation.browser.api.http;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.janelia.it.workstation.browser.api.AccessManager;
import org.slf4j.Logger;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

public class RESTClientBase {

    protected final Logger log;
    
    protected RESTClientBase(Logger log) {
        this.log = log;
    }

    protected boolean checkBadResponse(Response response, String failureError) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Problem making request for {}", failureError);
            // TODO: we want to print the request URI here, but I don't have time to search through the JAX-RS APIs right now
            log.error("Server responded with error code: {} {}",response.getStatus(), status);
            return true;
        }
        return false;
    }

    protected boolean checkBadResponse(int responseStatus, String failureError) {
        if (responseStatus<200 || responseStatus>=300) {
            log.error("ERROR RESPONSE: " + responseStatus);
            log.error(failureError);
            return true;
        }
        return false;
    }
    
    protected void checkBadResponse(WebTarget target, Response response) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Request for {} returned {} {}", target.getUri(), responseStatus, status);
            throw new WebApplicationException(response);
        }
    }

    protected Client createHttpClient(ObjectMapper objectMapper) {
        JacksonJaxbJsonProvider jacksonProvider = new JacksonJaxbJsonProvider();
        jacksonProvider.setMapper(objectMapper);
        ClientConfig clientConfig = new ClientConfig()
                .register(jacksonProvider);

        return ClientBuilder.newBuilder()
                .withConfig(clientConfig)
                .build();
    }

    protected String getAccessToken() {
        return AccessManager.getAccessManager().getToken();
    }
}
