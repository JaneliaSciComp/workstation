package org.janelia.it.workstation.browser.api.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.lang3.StringUtils;
import org.janelia.it.workstation.browser.api.exceptions.AuthenticationException;
import org.janelia.it.workstation.browser.api.exceptions.ServiceException;
import org.janelia.it.workstation.browser.api.facade.impl.rest.RESTClientImpl;
import org.janelia.it.workstation.browser.api.http.RestJsonClientManager;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTful client for invoking an async service.
 */
public class AsyncServiceClient extends RESTClientImpl {

    private static final Logger log = LoggerFactory.getLogger(AsyncServiceClient.class);

    private static final String REMOTE_API_URL = ConsoleProperties.getInstance().getProperty("async.rest.url");

    private WebTarget service;

    public AsyncServiceClient() {
        this(REMOTE_API_URL);
    }

    public AsyncServiceClient(String serverUrl) {
        super(log);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, false);
        log.info("Using server URL: {}",serverUrl);
    }
    
    public Long invokeService(String serviceName,
                                List<String> serviceArgs,
                                String processingLocation,
                                Map<String, String> serviceResources) throws ServiceException {
        AsyncServiceData body = new AsyncServiceData();
        if (serviceArgs != null) {
            body.serviceArgs.addAll(serviceArgs);
        }
        if (serviceResources != null) {
            body.resources.putAll(serviceResources);
        }
        if (StringUtils.isNotBlank(processingLocation)) {
            body.processingLocation = processingLocation;
        }
        WebTarget target = service.path("async-services").path(serviceName);
        Response response = target
                .request("application/json")
                .post(Entity.json(body));
        if (response.getStatus() != 201) {
            throw new ServiceException("Service " + serviceName + " returned status "+response.getStatus());
        }
        
        AsyncServiceData data = response.readEntity(AsyncServiceData.class);
        if (data != null) {
            if (data.id == null) {
                throw new ServiceException("Service returned status OK, but an empty service ID");
            }
            return data.id;
        } else {
            throw new ServiceException("Service returned status OK, but an empty response");
        }
    }

    public String getServiceStatus(Long serviceId) throws ServiceException {
        WebTarget target = service.path("services").path(serviceId.toString());
        Response response = target
                .request("application/json")
                .get();
        if (response.getStatus() != 200) {
            throw new ServiceException("Service " + serviceId + " returned status " + response.getStatus());
        }

        AsyncServiceData data = response.readEntity(AsyncServiceData.class);
        if (data != null) {
            if (data.state == null) {
                throw new ServiceException("Service returned status OK, but an empty service state");
            }
            return data.state;
        } else {
            throw new ServiceException("Service returned status OK, but an empty response");
        }
    }

    /**
     * Serializes into the JSON expected by the AuthenticationService as input.
     */
    private static class AsyncServiceData {
        private Long id;
        private String processingLocation;
        private String state;
        private List<String> serviceArgs = new ArrayList<>();
        private Map<String, String> resources = new HashMap<>();
    }

}
