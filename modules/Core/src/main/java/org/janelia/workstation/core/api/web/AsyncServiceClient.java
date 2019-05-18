package org.janelia.workstation.core.api.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.janelia.workstation.core.api.exceptions.ServiceException;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.api.http.RestJsonClientManager;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * RESTful client for invoking an async service.
 */
public class AsyncServiceClient extends RESTClientBase {

    private static final Logger log = LoggerFactory.getLogger(AsyncServiceClient.class);

    private WebTarget service;
    private ObjectMapper objectMapper;

    public AsyncServiceClient() {
        this(ConsoleProperties.getInstance().getProperty("async.rest.url"));
    }

    public AsyncServiceClient(String serverUrl) {
        super(log);
        log.info("Using server URL: {}",serverUrl);
        this.service = RestJsonClientManager.getInstance().getTarget(serverUrl, true);
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    
    public Long invokeService(String serviceName,
                              List<String> serviceArgs,
                              String processingLocation,
                              Map<String, String> serviceResources) throws ServiceException {
        AsyncServiceData body = new AsyncServiceData();
        if (serviceArgs != null) {
            body.args.addAll(serviceArgs);
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
        AsyncServiceData data = readServiceData(response);
        if (data != null) {
            if (data.serviceId == null) {
                throw new ServiceException("Service returned status OK, but an empty service ID");
            }
            return Long.valueOf(data.serviceId);
        } else {
            throw new ServiceException("Service returned status OK, but an empty response");
        }
    }

    private AsyncServiceData readServiceData(Response response) {
        String strData = response.readEntity(String.class);
        try {
            return objectMapper.readValue(strData, AsyncServiceData.class);
        } catch (IOException e) {
            throw new ServiceException(e.toString());
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
        AsyncServiceData data = readServiceData(response);
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
    public static class AsyncServiceData {
        private String serviceId;
        private String processingLocation;
        private String state;
        private String ownerKey;
        private List<String> args = new ArrayList<>();
        private Map<String, String> resources = new HashMap<>();

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }

        public String getProcessingLocation() {
            return processingLocation;
        }

        public void setProcessingLocation(String processingLocation) {
            this.processingLocation = processingLocation;
        }

        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public String getOwnerKey() {
            return ownerKey;
        }

        public void setOwnerKey(String ownerKey) {
            this.ownerKey = ownerKey;
        }

        public List<String> getArgs() {
            return args;
        }

        public void setArgs(List<String> args) {
            this.args = args;
        }

        public Map<String, String> getResources() {
            return resources;
        }

        public void setResources(Map<String, String> resources) {
            this.resources = resources;
        }
    }

}
