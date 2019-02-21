package org.janelia.it.workstation.browser.api.web;

import com.google.common.base.Preconditions;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.janelia.it.workstation.browser.api.http.RESTClientBase;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import java.util.List;

/**
 * RESTful client for invoking an async service.
 */
public class JadeServiceClient extends RESTClientBase {

    private static final Logger LOG = LoggerFactory.getLogger(JadeServiceClient.class);
    private static final String JADE_BASE_URL = ConsoleProperties.getString("jadestorage.rest.url");

    private static class JadeResults<T> {
        @JsonProperty
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<T> resultList;
    }

    private static class JadeStorageVolume {
        @JsonProperty
        private String storageServiceURL;
    }

    private final ObjectMapper objectMapper;
    private final Client httpClient;

    public JadeServiceClient() {
        super(LOG);
        Preconditions.checkArgument(JADE_BASE_URL != null && JADE_BASE_URL.trim().length() > 0);
        objectMapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        httpClient = createHttpClient(objectMapper);
    }
    
    public String findStorageURL(String storagePath) {
        Preconditions.checkArgument(storagePath != null && storagePath.trim().length() > 0);
        WebTarget target = httpClient.target(JADE_BASE_URL)
                .path("storage_volumes")
                .queryParam("dataStoragePath", storagePath);
        Response response = target.request()
                .header("Authorization", "Bearer " + getAccessToken())
                .get();
        int responseStatus = response.getStatus();
        if (responseStatus != Response.Status.OK.getStatusCode()) {
            LOG.error("Request to {} returned with status {}", target, responseStatus);
            throw new IllegalStateException("Request to " + target.getUri() + " returned with status " + responseStatus);
        }
        JadeResults<JadeStorageVolume> storageContentResults = response.readEntity(new GenericType<JadeResults<JadeStorageVolume>>(){});
        if (storageContentResults.resultList.size() < 0) {
            throw new IllegalArgumentException("No storage volume found for " + storagePath + " from querying " + target.getUri());
        }
        return storageContentResults.resultList.get(0).storageServiceURL;
    }

    public InputStream streamContent(String serverURL, String dataPath) {
        Preconditions.checkArgument(serverURL != null && serverURL.trim().length() > 0);
        Preconditions.checkArgument(dataPath != null && dataPath.trim().length() > 0);
        WebTarget target = httpClient.target(serverURL)
                .path("agent_storage")
                .path("storage_path/data_content")
                .path(dataPath)
                ;
        Response response = target.request()
                .header("Authorization", "Bearer " + getAccessToken())
                .get();
        int responseStatus = response.getStatus();
        if (responseStatus != Response.Status.OK.getStatusCode()) {
            LOG.error("Request to {} returned with status {}", target, responseStatus);
            throw new IllegalStateException("Request to " + target.getUri() + " returned with status " + responseStatus);
        }
        return response.readEntity(InputStream.class);
    }
}
