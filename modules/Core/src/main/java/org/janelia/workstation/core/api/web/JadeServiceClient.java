package org.janelia.workstation.core.api.web;

import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Optional;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

import com.google.common.base.Preconditions;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.janelia.jacsstorage.clients.api.JadeResults;
import org.janelia.jacsstorage.clients.api.JadeStorageAttributes;
import org.janelia.jacsstorage.clients.api.JadeStorageVolume;
import org.janelia.jacsstorage.clients.api.http.HttpClientProvider;
import org.janelia.jacsstorage.clients.api.rendering.JadeBasedDataLocation;
import org.janelia.rendering.Streamable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * RESTful client for invoking an async service.
 */
public class JadeServiceClient {

    private static final Logger LOG = LoggerFactory.getLogger(JadeServiceClient.class);

    private final String jadeURL; // jade master node URL
    private final HttpClientProvider clientProvider;

    public JadeServiceClient(String jadeURL, HttpClientProvider clientProvider) {
        Preconditions.checkArgument(StringUtils.isNotBlank(jadeURL));
        this.jadeURL = jadeURL;
        this.clientProvider = clientProvider;
    }

    public Optional<String> findStorageURL(String storagePath) {
        Preconditions.checkArgument(storagePath != null && storagePath.trim().length() > 0);
        Client httpClient = clientProvider.getClient();
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
            return storageContentResults.getResultList().stream().findFirst().map(JadeStorageVolume::getStorageServiceURL);
        } finally {
            httpClient.close();
        }
    }

    public Optional<JadeBasedDataLocation> findDataLocation(String storagePathParam, JadeStorageAttributes storageAttributes) {
        Preconditions.checkArgument(storagePathParam != null && storagePathParam.trim().length() > 0);
        String storagePath = RegExUtils.replaceFirst(StringUtils.replaceChars(storagePathParam, '\\', '/'), "^((.+:)?/+)+", "/");
        Client httpClient = clientProvider.getClient();
        try {
            LOG.debug("Lookup storage for {}", storagePath);
            WebTarget target = httpClient.target(jadeURL)
                    .path("storage_volumes")
                    .queryParam("dataStoragePath", storagePath);
            Response response = createRequest(target, storageAttributes).get();
            int responseStatus = response.getStatus();
            if (responseStatus != Response.Status.OK.getStatusCode()) {
                LOG.error("Request to {} returned with status {}", target, responseStatus);
                return Optional.empty();
            }
            JadeResults<JadeStorageVolume> storageContentResults = response.readEntity(new GenericType<JadeResults<JadeStorageVolume>>() {
            });
            return storageContentResults.getResultList().stream().findFirst()
                    .map(jadeVolume -> {
                        String renderedVolumePath;
                        if (StringUtils.startsWith(storagePath, jadeVolume.getStorageVirtualPath())) {
                            renderedVolumePath = Paths.get(jadeVolume.getStorageVirtualPath()).relativize(Paths.get(storagePath)).toString();
                        } else {
                            renderedVolumePath = Paths.get(jadeVolume.getBaseStorageRootDir()).relativize(Paths.get(storagePath)).toString();
                        }
                        LOG.info("Create JADE volume location with URLs {}, {} and volume path {}", jadeVolume.getStorageServiceURL(), jadeVolume.getVolumeStorageURI(), renderedVolumePath);
                        return new JadeBasedDataLocation(
                                jadeVolume.getStorageServiceURL(),
                                jadeVolume.getVolumeStorageURI(),
                                renderedVolumePath,
                                null,
                                null,
                                storageAttributes);
                    })
                    ;
        } finally {
            httpClient.close();
        }

    }

    public Streamable<InputStream> streamContent(String serverURL, String dataPath, JadeStorageAttributes storageAttributes) {
        Preconditions.checkArgument(serverURL != null && serverURL.trim().length() > 0);
        Preconditions.checkArgument(dataPath != null && dataPath.trim().length() > 0);
        Client httpClient = clientProvider.getClient();
        try {
            WebTarget target = httpClient.target(serverURL)
                    .path("agent_storage/storage_path/data_content")
                    .path(dataPath);
            LOG.info("Streaming tile from {}", target);
            Response response = createRequest(target, storageAttributes).get();
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

    public boolean checkStoragePath(String storagePath, JadeStorageAttributes storageAttributes) {
        Preconditions.checkArgument(storagePath != null && storagePath.trim().length() > 0);
        Client httpClient = clientProvider.getClient();
        try {
            LOG.debug("Check if storage path exists {}", storagePath);
            WebTarget target = httpClient.target(jadeURL)
                    .path("storage_content/storage_path_redirect")
                    .path(storagePath);
            Response response = createRequest(target, storageAttributes).head();
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

    private Invocation.Builder createRequest(WebTarget target, JadeStorageAttributes storageAttributes) {
        Invocation.Builder requestBuilder = target.request();
        for (String storageAttribute : storageAttributes.getAttributeNames()) {
            requestBuilder = requestBuilder.header(
                    storageAttribute,
                    storageAttributes.getAttributeValue(storageAttribute)
            );
        }
        return requestBuilder;
    }
}
