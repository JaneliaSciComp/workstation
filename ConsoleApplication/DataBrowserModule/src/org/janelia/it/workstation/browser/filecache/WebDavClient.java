package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
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

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  httpClient             httpClient
     *                                (e.g. /groups/...) to WebDAV URLs.
     * @throws IllegalArgumentException
     *   if the baseUrl cannot be parsed.
     */
    public WebDavClient(String baseUrl, HttpClient httpClient) {
        this.baseUrl = validateUrl(baseUrl);
        this.httpClient = httpClient;
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
     * @param  storagePathPrefix storage key
     *
     * @return WebDAV information for the storage.
     *
     * @throws WebDavException
     *   if the storage information cannot be retrieved.
     */
    public WebDavFile findStorage(String storagePathPrefix)
            throws WebDavException {
        String href = getWebdavFindUrl(storagePathPrefix, "storagePrefix");

        MultiStatusResponse[] multiStatusResponses = getResponses(href, DavConstants.DEPTH_0, 0);
        if ((multiStatusResponses == null) || (multiStatusResponses.length == 0)) {
            throw new WebDavException("empty response returned for " + storagePathPrefix);
        }
        return new WebDavFile(storagePathPrefix, multiStatusResponses[0]);
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
        String href = getWebdavFindUrl(remoteFileName, "dataStoragePath");

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
    void createDirectory(URL directoryUrl)
            throws WebDavException {

        MkColMethod method = null;
        Integer responseCode = null;

        try {
            method = new MkColMethod(directoryUrl.toString());

            responseCode = httpClient.executeMethod(method);
            LOG.trace("createDirectory: {} returned for MKCOL {}", responseCode, directoryUrl);

            if (responseCode != HttpServletResponse.SC_CREATED) {
                throw new WebDavException(responseCode + " returned for MKCOL " + directoryUrl,
                        responseCode);
            }
        } catch (WebDavException e) {
            throw e;
        } catch (Exception e) {
            throw new WebDavException("failed to MKCOL " + directoryUrl, e, responseCode);
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
    void saveFile(URL url, File file)
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
            saveFile(url, fileStream);
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
    private void saveFile(URL url, InputStream fileStream)
            throws WebDavException {

        PutMethod method = null;
        Integer responseCode = null;

        try {
            method = new PutMethod(url.toString());
            method.setRequestEntity(new InputStreamRequestEntity(fileStream));

            responseCode = httpClient.executeMethod(method);
            LOG.trace("saveFile: {} returned for PUT {}", responseCode, url);

            if ((responseCode != HttpServletResponse.SC_CREATED) &&
                    (responseCode != HttpServletResponse.SC_NO_CONTENT)) {
                throw new WebDavException(responseCode + " returned for PUT " + url,
                        responseCode);
            }
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

    URL getDownloadFileURL(String standardPathName) {
        try {
            return new URL(baseUrl + "/path/" + standardPathName);
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getWebdavFindUrl(String remoteFileName, String context) {
        return baseUrl + "/" + context + "/" + remoteFileName;
    }
}
