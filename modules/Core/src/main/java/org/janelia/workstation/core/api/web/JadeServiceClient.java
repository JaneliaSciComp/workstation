package org.janelia.workstation.core.api.web;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.google.common.base.Preconditions;
import org.janelia.workstation.core.api.http.RESTClientBase;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.List;

/**
 * RESTful client for invoking an async service.
 */
public class JadeServiceClient extends RESTClientBase {

    private static final Logger LOG = LoggerFactory.getLogger(JadeServiceClient.class);

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    private static class JadeResults<T> {
        @JsonProperty
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<T> resultList;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonAutoDetect(
            fieldVisibility = JsonAutoDetect.Visibility.ANY,
            setterVisibility = JsonAutoDetect.Visibility.NONE
    )
    private static class JadeStorageVolume {
        @JsonProperty
        private String storageServiceURL;
    }

    private final String jadeBaseUrl;
    private final ObjectMapper objectMapper;
    private final Client httpClient;

    public JadeServiceClient() {
        super(LOG);
        this.jadeBaseUrl = ConsoleProperties.getString("jadestorage.rest.url");
        Preconditions.checkArgument(jadeBaseUrl != null && jadeBaseUrl.trim().length() > 0);
        this.objectMapper = new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
                ;
        this.httpClient = createHttpClient(objectMapper);
    }
    
    public String findStorageURL(String storagePath) {
        Preconditions.checkArgument(storagePath != null && storagePath.trim().length() > 0);
        WebTarget target = httpClient.target(jadeBaseUrl)
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
                .path("agent_storage/storage_path/data_content")
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
