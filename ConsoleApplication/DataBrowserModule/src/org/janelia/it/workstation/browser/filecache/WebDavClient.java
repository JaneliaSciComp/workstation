package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Paths;

import javax.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.janelia.it.jacs.shared.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link HttpClient} wrapper for submitting WebDAV requests.
 *
 * @author Eric Trautman
 */
public class WebDavClient {

    private static final Logger LOG = LoggerFactory.getLogger(WebDavClient.class);

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  httpClient             httpClient
     *                                (e.g. /groups/...) to WebDAV URLs.
     * @throws IllegalArgumentException
     *   if the baseUrl cannot be parsed.
     */
    public WebDavClient(String baseUrl, HttpClient httpClient, ObjectMapper objectMapper) {
        this.baseUrl = validateUrl(baseUrl);
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    private String validateUrl(String urlString) {
        try {
            final URL url = new URL(urlString);
            return urlString;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("failed to parse URL: " + urlString, e);
        }
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
    public WebDavFile findStorage(String storagePath)
            throws WebDavException {
        String href = getWebdavFindUrl(storagePath, "data_storage_path");

        MultiStatusResponse[] multiStatusResponses = getResponses(href, DavConstants.DEPTH_0, 0);
        if ((multiStatusResponses == null) || (multiStatusResponses.length == 0)) {
            throw new WebDavException("empty response returned for " + storagePath);
        }
        return new WebDavFile(storagePath, multiStatusResponses[0]);
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
    public WebDavFile findFile(String remoteFileName)
            throws WebDavException {
        String href = getWebdavFindUrl(remoteFileName, "data_storage_path");

        MultiStatusResponse[] multiStatusResponses = getResponses(href, DavConstants.DEPTH_0, 0);
        if ((multiStatusResponses == null) || (multiStatusResponses.length == 0)) {
            throw new WebDavException("empty response returned for " + remoteFileName);
        }
        return new WebDavFile(remoteFileName, multiStatusResponses[0]);
    }

    /**
     * Creates a directory on the server.
     *
     * @param  directoryUrl  URL (and path) for new directory.
     *
     * @throws WebDavException
     *   if the directory cannot be created or it already exists.
     */
    RemoteLocation createDirectory(URL directoryUrl)
            throws WebDavException {
        PutMethod method = null;
        Integer responseCode = null;
        try {
            method = new PutMethod(directoryUrl.toString());

            responseCode = httpClient.executeMethod(method);
            LOG.trace("saveFile: {} returned for PUT {}", responseCode, directoryUrl);

            if (responseCode != HttpServletResponse.SC_CREATED) {
                throw new WebDavException(responseCode + " returned for PUT " + directoryUrl, responseCode);
            }
            return extractRemoteLocationFromResponse(method);
        } catch (WebDavException e) {
            throw e;
        } catch (Exception e) {
            throw new WebDavException("failed to PUT " + directoryUrl, e, responseCode);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }
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
        String storageVirtualPath = jsonResponse.get("rootPrefix").asText();
        String storageRealPath = jsonResponse.get("rootLocation").asText();
        String storageRelativePath = jsonResponse.get("nodeRelativePath").asText();

        return new RemoteLocation(Paths.get(storageVirtualPath, storageRelativePath).toString(),
                Paths.get(storageRealPath, storageRelativePath).toString(), location);
    }

    private MultiStatusResponse[] getResponses(String href, int depth, int callCount)
            throws WebDavException {
        MultiStatusResponse[] multiStatusResponses;
        PropFindMethod method = null;
        try {
            method = new PropFindMethod(href, WebDavFile.PROPERTY_NAMES, depth);
            method.addRequestHeader("Accept", "application/xml");
            method.addRequestHeader("Content-Type", "application/xml");
            final int responseCode = httpClient.executeMethod(method);
            LOG.trace("getResponses: {} returned for PROPFIND {}", responseCode, href);

            if (responseCode == HttpStatus.SC_MULTI_STATUS) {
                final MultiStatus multiStatus = method.getResponseBodyAsMultiStatus();
                multiStatusResponses = multiStatus.getResponses();
            } else if (responseCode == HttpStatus.SC_MOVED_PERMANENTLY) {
                final Header locationHeader = method.getResponseHeader("Location");
                if (locationHeader != null) {
                    final String movedHref = locationHeader.getValue();
                    if (callCount == 0) {
                        return getResponses(movedHref, depth, 1);
                    }
                }
                throw new WebDavException(responseCode + " response code returned for " + href, responseCode);
            } else if (responseCode == HttpStatus.SC_NOT_FOUND) {
                throw new WebDavException("Resource " + href + "not found (" + responseCode + ")");
            } else {
                throw new WebDavException(responseCode + " response code returned for " + href, responseCode);
            }
        } catch (WebDavException e) {
            throw e;
        } catch (Exception e) {
            throw new WebDavException("failed to retrieve WebDAV information from " + href, e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

        return multiStatusResponses;
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

    URL getDownloadFileURL(String standardPathName) {
        try {
            return new URL(baseUrl + "/storage_path/" + standardPathName);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getWebdavFindUrl(String remoteFileName, String context) {
        return baseUrl + "/" + context + "/" + remoteFileName;
    }

}
