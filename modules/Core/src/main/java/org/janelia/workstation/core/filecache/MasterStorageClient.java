package org.janelia.workstation.core.filecache;

import java.io.FileNotFoundException;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HttpClient} wrapper for submitting WebDAV requests.
 *
 * @author Eric Trautman
 */
class MasterStorageClient extends AbstractStorageClient {

    private static final Logger LOG = LoggerFactory.getLogger(MasterStorageClient.class);

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  httpClient             httpClient
     *                                (e.g. /groups/...) to WebDAV URLs.
     * @throws IllegalArgumentException
     *   if the baseUrl cannot be parsed.
     */
    MasterStorageClient(String baseUrl, HttpClientProxy httpClient, ObjectMapper objectMapper) {
        super(baseUrl, httpClient, objectMapper);
    }

    /**
     * Finds information about the storage using the storage path prefix
     *
     * @param  storagePath storage key
     *
     * @return WebDAV information for the storage.
     *
     * @throws WebDavException
     *   if the storage information cannot be retrieved.
     */
    WebDavStorage findStorage(String storagePath) throws WebDavException, FileNotFoundException {
        MultiStatusResponse[] multiStatusResponses = StorageClientResponseHelper.getResponses(
                httpClient,
                StorageClientResponseHelper.getStorageLookupURL(baseUrl, "data_storage_path", storagePath),
                DavConstants.DEPTH_0,
                0
        );
        return new WebDavStorage(storagePath, multiStatusResponses[0]);
    }

    String createStorage(String storageName, String storageContext, String storageTags) {
        return createStorageForResource(getCreateStorageURL(storageName, "DATA_DIRECTORY"), storageContext, storageTags);
    }

    private String getCreateStorageURL(String storageName, String storageType) {
        return baseUrl + "/storage/" + storageName + "/format/" + storageType;
    }

    private String createStorageForResource(String resourceURI, String storageContext, String storageTags) {
        MkColMethod method = null;
        Integer responseCode = null;
        try {
            method = new MkColMethod(resourceURI);
            if (storageTags != null) {
                method.addRequestHeader("storageTags", storageTags);
            }
            if (storageContext != null) {
                method.addRequestHeader("pathPrefix", storageContext);
            }
            responseCode = httpClient.executeMethod(method);
            LOG.trace("createDirectory: {} returned for MKCOL {}", resourceURI, responseCode);

            if (responseCode != HttpServletResponse.SC_CREATED) {
                String response = method.getResponseBodyAsString();
                throw new WebDavException(responseCode + " returned for MKCOL " + resourceURI + ": " + response, responseCode);
            }
            final Header locationHeader = method.getResponseHeader("Location");
            if (locationHeader == null) {
                throw new WebDavException("No location header returned for " + resourceURI, responseCode);
            }
            String location = locationHeader.getValue();
            if (StringUtils.isBlank(location)) {
                throw new WebDavException("No location value set in the header returned for " + resourceURI, responseCode);
            }
            return location;
        } catch (WebDavException e) {
            throw e;
        } catch (Exception e) {
            throw new WebDavException("failed to MKCOL " + resourceURI, e, responseCode);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

}
