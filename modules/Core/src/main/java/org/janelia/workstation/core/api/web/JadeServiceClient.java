package org.janelia.workstation.core.api.web;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.rendering.JADEBasedDataLocation;
import org.janelia.rendering.Streamable;
import org.janelia.rendering.utils.ClientProxy;
import org.janelia.rendering.utils.HttpClientProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTful client for invoking an async service.
 */
public class JadeServiceClient {

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
        private String id;
        @JsonProperty
        private String storageServiceURL;
        @JsonProperty
        private String baseStorageRootDir;
        @JsonProperty
        private String storageVirtualPath;

        String getVolumeStorageURI() {
            try {
                return UriBuilder.fromUri(new URI(storageServiceURL)).path("agent_storage/storage_volume").path(id).build().toString();
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

    }

    private final String jadeURL; // jade master node URL
    private final HttpClientProvider httpClientProvider;

    public JadeServiceClient(String jadeURL, HttpClientProvider httpClientProvider) {
        Preconditions.checkArgument(StringUtils.isNotBlank(jadeURL));
        this.jadeURL = jadeURL;
        this.httpClientProvider = httpClientProvider;
    }

    public Optional<String> findStorageURL(String storagePath) {
        Preconditions.checkArgument(storagePath != null && storagePath.trim().length() > 0);
        ClientProxy httpClient = getHttpClient();
        try {
            LOG.debug("Lookup storage for {}", storagePath);
            WebTarget target = httpClient.target(jadeURL)
                    .path("storage_volumes")
                    .queryParam("dataStoragePath", storagePath);
            Response response = target.request()
                    .get();
            int responseStatus = response.getStatus();
            if (responseStatus != Response.Status.OK.getStatusCode()) {
                LOG.error("Request to {} returned with status {}", target, responseStatus);
                return Optional.empty();
            }
            JadeResults<JadeStorageVolume> storageContentResults = response.readEntity(new GenericType<JadeResults<JadeStorageVolume>>() {
            });
            return storageContentResults.resultList.stream().findFirst().map(jadeVolume -> jadeVolume.storageServiceURL);
        } finally {
            httpClient.close();
        }
    }

    public Optional<JADEBasedDataLocation> findDataLocation(String storagePathParam) {
        Preconditions.checkArgument(storagePathParam != null && storagePathParam.trim().length() > 0);
        String storagePath = RegExUtils.replaceFirst(StringUtils.replaceChars(storagePathParam, '\\', '/'), "^((.+:)?/+)+", "/");
        ClientProxy httpClient = getHttpClient();
        try {
            LOG.debug("Lookup storage for {}", storagePath);
            WebTarget target = httpClient.target(jadeURL)
                    .path("storage_volumes")
                    .queryParam("dataStoragePath", storagePath);
            Response response = target.request()
                    .get();
            int responseStatus = response.getStatus();
            if (responseStatus != Response.Status.OK.getStatusCode()) {
                LOG.error("Request to {} returned with status {}", target, responseStatus);
                return Optional.empty();
            }
            JadeResults<JadeStorageVolume> storageContentResults = response.readEntity(new GenericType<JadeResults<JadeStorageVolume>>() {
            });
            return storageContentResults.resultList.stream().findFirst()
                    .map(jadeVolume -> {
                        String renderedVolumePath;
                        if (storagePath.startsWith(jadeVolume.storageVirtualPath)) {
                            renderedVolumePath = Paths.get(jadeVolume.storageVirtualPath).relativize(Paths.get(storagePath)).toString();
                        } else {
                            renderedVolumePath = Paths.get(jadeVolume.baseStorageRootDir).relativize(Paths.get(storagePath)).toString();
                        }
                        LOG.info("Create JADE volume location with URLs {}, {} and volume path {}", jadeVolume.storageServiceURL, jadeVolume.getVolumeStorageURI(), renderedVolumePath);
                        return new JADEBasedDataLocation(
                                jadeVolume.storageServiceURL,
                                jadeVolume.getVolumeStorageURI(),
                                renderedVolumePath,
                                null,
                                null,
                                httpClientProvider);
                    })
                    ;
        } finally {
            httpClient.close();
        }

    }

    public Streamable<InputStream> streamContent(String serverURL, String dataPath) {
        Preconditions.checkArgument(serverURL != null && serverURL.trim().length() > 0);
        Preconditions.checkArgument(dataPath != null && dataPath.trim().length() > 0);
        ClientProxy httpClient = getHttpClient();
        try {
            WebTarget target = httpClient.target(serverURL)
                    .path("agent_storage/storage_path/data_content")
                    .path(dataPath);
            LOG.info("Streaming tile from {}", target);
            Response response = target.request()
                    .get();
            int responseStatus = response.getStatus();
            if (responseStatus == Response.Status.OK.getStatusCode()) {
                InputStream is = (InputStream) response.getEntity();
                return Streamable.of(is, response.getLength());
            } else {
                LOG.warn("Request to {} in order to get {} returned with status {}", target, dataPath, responseStatus);
                return Streamable.empty();
            }
        } finally {
            httpClient.close();
        }
    }

    public boolean checkStoragePath(String storagePath) {
        Preconditions.checkArgument(storagePath != null && storagePath.trim().length() > 0);
        ClientProxy httpClient = getHttpClient();
        try {
            LOG.debug("Check if storage path exists {}", storagePath);
            WebTarget target = httpClient.target(jadeURL)
                    .path("storage_content/storage_path_redirect")
                    .path(storagePath);
            Response response = target.request()
                    .head();
            int responseStatus = response.getStatus();
            if (responseStatus != Response.Status.OK.getStatusCode()) {
                LOG.error("Request to {} returned with status {}", target, responseStatus);
                return false;
            } else {
                return true;
            }
        } finally {
            httpClient.close();
        }
    }

    private ClientProxy getHttpClient() {
        return httpClientProvider.getClient();
    }

}
