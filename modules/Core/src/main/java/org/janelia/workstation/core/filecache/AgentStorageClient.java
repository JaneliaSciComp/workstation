package org.janelia.workstation.core.filecache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Consumer;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.janelia.workstation.core.util.PathUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HttpClient} wrapper for submitting WebDAV requests.
 *
 * @author Eric Trautman
 */
class AgentStorageClient extends AbstractStorageClient {

    private static final Logger LOG = LoggerFactory.getLogger(AgentStorageClient.class);

    private final Consumer<Throwable> connectionErrorHandler;

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  httpClient             httpClient
     *                                (e.g. /groups/...) to WebDAV URLs.
     * @throws IllegalArgumentException
     *   if the baseUrl cannot be parsed.
     */
    AgentStorageClient(String baseUrl, HttpClientProxy httpClient, ObjectMapper objectMapper, Consumer<Throwable> connectionErrorHandler) {
        super(baseUrl, httpClient, objectMapper);
        this.connectionErrorHandler = connectionErrorHandler;
    }

    /**
     * Finds information about the specified file.
     *
     * @param  remoteFileName  file's remote reference name.
     *
     * @return WebDAV information for the specified file.
     *
     * @throws WebDavException
     *   if the file information cannot be retrieved.
     */
    WebDavFile findFile(String remoteFileName) throws WebDavException, FileNotFoundException {
        MultiStatusResponse[] multiStatusResponses = StorageClientResponseHelper.getResponses(
                httpClient,
                StorageClientResponseHelper.getStorageLookupURL(baseUrl, "data_storage_path", remoteFileName),
                DavConstants.DEPTH_0,
                0
        );
        return new WebDavFile(remoteFileName, multiStatusResponses[0], connectionErrorHandler);
    }

    /**
     * Saves the specified file to the server.
     *
     * @param  url   server URL for the new file.
     * @param  file  file to save.
     *
     * @throws IllegalArgumentException
     *   if the file cannot be read.
     *
     * @throws WebDavException
     *   if the save fails for any other reason.
     */
    RemoteLocation saveFile(URL url, File file)
            throws IllegalArgumentException, WebDavException {
        InputStream fileStream;
        if (file == null) {
            throw new IllegalArgumentException("file must be defined");
        }
        try {
            fileStream = new FileInputStream(file);
        } catch (Exception e) {
            throw new IllegalArgumentException("failed to open stream for " + file.getAbsolutePath(), e);
        }
        try {
            return saveFile(url, fileStream);
        } finally {
            try {
                fileStream.close();
            } catch (IOException e) {
                LOG.warn("failed to close input stream for " + file.getAbsolutePath() + ", ignoring exception", e);
            }
        }
    }

    /**
     * Save the specified stream to the server.
     *
     * This method is protected (instead of private) so that it can
     * be used for testing.
     *
     * @param  url         server URL for the new file.
     * @param  fileStream  file contents to save.
     *
     * @throws WebDavException
     *   if the save fails for any reason.
     */
    private RemoteLocation saveFile(URL url, InputStream fileStream)
            throws WebDavException {

        PutMethod method = null;
        Integer responseCode = null;

        try {
            method = new PutMethod(url.toString());
            method.setRequestEntity(new InputStreamRequestEntity(fileStream));

            responseCode = httpClient.executeMethod(method);
            LOG.trace("saveFile: {} returned for PUT {}", responseCode, url);

            if (responseCode != HttpServletResponse.SC_CREATED) {
                throw new WebDavException(responseCode + " returned for PUT " + url,
                        responseCode);
            }
            return extractRemoteLocationFromResponse(method);
        } catch (WebDavException e) {
            throw e;
        } catch (Exception e) {
            throw new WebDavException("failed to PUT " + url, e, responseCode);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
    }

    private RemoteLocation extractRemoteLocationFromResponse(HttpMethod method) throws IOException {
        JsonNode jsonResponse = objectMapper.readTree(method.getResponseBodyAsStream());
        final Header locationHeader = method.getResponseHeader("Location");
        String location = null;
        if (locationHeader != null) {
            location = locationHeader.getValue();
        }
        String storageVirtualPath = jsonResponse.get("storageRootPathURI").asText();
        String storageRealPath = jsonResponse.get("storageRootLocation").asText();
        String storageRelativePath = jsonResponse.get("nodeRelativePath").asText();        
        String virtualFilePath = PathUtil.getStandardPath(storageVirtualPath+"/"+storageRelativePath);
        String realFilePath = PathUtil.getStandardPath(storageRealPath+"/"+storageRelativePath);
        return new RemoteLocation(virtualFilePath, realFilePath, location);
    }

    URL getUploadFileURL(String storageLocation) {
        try {
            return new URL(baseUrl + "/data_content/" + (StringUtils.isBlank(storageLocation) ? "" : storageLocation));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    URLProxy getDownloadFileURL(String standardPathName) {
        try {
            return new URLProxy(new URL(baseUrl + "/storage_path/data_content/" + standardPathName), connectionErrorHandler);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

}
