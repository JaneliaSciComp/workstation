package org.janelia.it.workstation.browser.api.facade.impl.rest;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;

public class RESTClientImpl {

    protected final RESTClientManager manager;
    protected final Logger log;

    protected RESTClientImpl(Logger log) {
        this(log, RESTClientManager.getInstance());
    }
    
    protected RESTClientImpl(Logger log, RESTClientManager manager) {
        this.log = log;
        this.manager = manager;
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
    
    protected boolean checkBadResponse(WebTarget target, Response response) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Request for {} returned {}", target.getUri(), responseStatus);
        }
        return false;
    }
    
}
