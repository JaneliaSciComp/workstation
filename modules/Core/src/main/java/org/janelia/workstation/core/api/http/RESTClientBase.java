package org.janelia.workstation.core.api.http;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.janelia.workstation.core.api.AccessManager;
import org.janelia.workstation.core.api.exceptions.RemoteServiceException;
import org.slf4j.Logger;

/**
 * Base class for RESTful clients which provides some utility methods.
 */
public class RESTClientBase {

    protected final Logger log;
    
    protected RESTClientBase(Logger log) {
        this.log = log;
    }

    protected void checkBadResponse(WebTarget target, Response response) {
        int responseStatus = response.getStatus();
        Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (responseStatus<200 || responseStatus>=300) {
            log.error("Request for {} returned {} {}", target.getUri(), responseStatus, status);
            throw new RemoteServiceException("Remote service returned "+response+" response");
        }
    }

    protected String getAccessToken() {
        return AccessManager.getAccessManager().getToken();
    }
}
