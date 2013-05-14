package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.MkColMethod;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.apache.jackrabbit.webdav.client.methods.PutMethod;
import org.apache.jackrabbit.webdav.property.DavPropertyName;
import org.apache.jackrabbit.webdav.property.DavPropertyNameSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * {@link HttpClient} wrapper for submitting WebDAV requests.
 *
 * @author Eric Trautman
 */
public class WebDavClient {

    public static final String JACS_WEBDAV_BASE_URL = "http://jacs.int.janelia.org/WebDAV";

    private String baseUrl;
    private HttpClient httpClient;
    private String uploadClientHostAddress;
    private String uploadClientStartTimestamp;
    private long uploadCount;
    private String userName;

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  baseUrl                base URL for converting standard paths
     *                                (e.g. /groups/...) to WebDAV URLs.
     * @param  maxConnectionsPerHost  the default maximum number of connections
     *                                allowed for a given host config.
     * @param  maxTotalConnections    the maximum number of connections allowed.
     */
    public WebDavClient(String baseUrl,
                        int maxConnectionsPerHost,
                        int maxTotalConnections) {
        this.baseUrl = baseUrl;
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(maxConnectionsPerHost); // default is 2
        managerParams.setMaxTotalConnections(maxTotalConnections);            // default is 20
        this.httpClient = new HttpClient(mgr);

        setCredentialsUsingAuthenticator();

        final HttpState clientState = this.httpClient.getState();
        final Credentials savedCredentials =
                clientState.getCredentials(AuthScope.ANY);
        if (savedCredentials == null) {
            LOG.warn("<init>: no credentials saved for WebDAV requests");
        }

        InetAddress address;
        try {
            address = InetAddress.getLocalHost();
            this.uploadClientHostAddress = address.getHostAddress();
        } catch (UnknownHostException e) {
            this.uploadClientHostAddress = "unknown";
            LOG.warn("failed to derive client host address, ignoring error", e);
        }

        final SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss-SSS");
        this.uploadClientStartTimestamp = sdf.format(new Date());

        this.userName = "anonymous";
        this.uploadCount = 0;
    }

    /**
     * Constructs an empty client for testing.
     */
    protected WebDavClient() {
        this.baseUrl = "file:";
        this.httpClient = null;
    }

    /**
     * @return the HTTP client instance used for issuing all WebDAV requests.
     */
    public HttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * Returns a WebDAV URL for the specified path.
     *
     * @param  standardPath  standard path for the file.
     *
     * @return corresponding WebDAV URL.
     *
     * @throws MalformedURLException
     *   if the URL cannot be constructed.
     */
    public URL getWebDavUrl(String standardPath) throws MalformedURLException {
        return new URL(baseUrl + standardPath);
    }

    /**
     * @param  rootUploadPath  root path for all uploads
     *                         (e.g. '/groups/scicomp/jacsData/upload').
     *
     * @return a "very-likely" unique path under the specified root upload path.
     */
    public String getUniqueUploadDirectoryPath(String rootUploadPath) {
        StringBuilder path = new StringBuilder(128);
        path.append(rootUploadPath);
        if (! rootUploadPath.endsWith("/")) {
            path.append('/');
        }
        path.append(uploadClientStartTimestamp);
        path.append("__");
        path.append(uploadClientHostAddress);
        path.append("__");
        path.append(userName);
        path.append("__");
        path.append(incrementUploadCount());
        path.append('/');
        return path.toString();
    }

    /**
     * Sets the default credentials for all requests.
     *
     * @param  credentials  default credentials.
     */
    public void setCredentials(UsernamePasswordCredentials credentials) {
        userName = credentials.getUserName();
        LOG.info("setCredentials: entry, userName={}", userName);
        final HttpState clientState = this.httpClient.getState();
        clientState.setCredentials(AuthScope.ANY, credentials);
    }

    /**
     * Uses the {@link Authenticator} default credentials as the
     * default credentials for all WebDAV requests.
     */
    public void setCredentialsUsingAuthenticator() {

        PasswordAuthentication defaultAuthentication =
                Authenticator.requestPasswordAuthentication(
                        null,
                        null,
                        -1,
                        null,
                        null,
                        null);

        if (defaultAuthentication != null) {
            final String userName = defaultAuthentication.getUserName();
            final char[] password = defaultAuthentication.getPassword();
            if ((userName != null) && (password != null)) {

                final UsernamePasswordCredentials credentials =
                        new UsernamePasswordCredentials(userName,
                                String.valueOf(password));
                setCredentials(credentials);
            }
        }
    }

    /**
     * Finds information about the specified file.
     *
     * @param  url  file URL.
     *
     * @return WebDAV information for the specified file.
     *
     * @throws WebDavException
     *   if the file information cannot be retrieved.
     */
    public WebDavFile findFile(URL url)
            throws WebDavException {

        final String href = url.toString();
        MultiStatusResponse[] multiStatusResponses = getResponses(href, DavConstants.DEPTH_0, 0);
        if ((multiStatusResponses == null) || (multiStatusResponses.length == 0)) {
            throw new WebDavException("empty response returned for " + href);
        }
        return new WebDavFile(url, multiStatusResponses[0]);
    }

    /**
     * @param  url  file or directory URL.
     *              Note that this URL does not need to include a trailing slash.
     *
     * @return true if the specified URL identifies a resource on the remote server,
     *         otherwise false.
     *
     * @throws WebDavException
     *   if the information cannot be retrieved.
     */
    public boolean isAvailable(URL url)
            throws WebDavException {
        final String href = url.toString();
        final int responseCode = getResourceTypeResponseCode(href);
        return ((responseCode == HttpStatus.SC_MULTI_STATUS) ||
                 (responseCode == HttpStatus.SC_MOVED_PERMANENTLY));
    }

    /**
     * @param  url  file or directory URL.
     *              Note that this URL does not need to include a trailing slash.
     *
     * @return true if the specified URL identifies a directory, otherwise false.
     *
     * @throws WebDavException
     *   if the information cannot be retrieved or the URL identifies a non-existent file.
     */
    public boolean isDirectory(URL url)
            throws WebDavException {
        final WebDavFile webDavFile = findFile(url);
        return webDavFile.isDirectory();
    }

    /**
     * Finds all files immediately within the specified directory
     * (but does not recurse into sub-directories).
     *
     * @param  directoryUrl  directory URL.
     *
     * @return list of immediate files in the directory.
     *
     * @throws WebDavException
     *   if the directory information cannot be retrieved.
     */
    public List<WebDavFile> findImmediateInternalFiles(URL directoryUrl)
            throws WebDavException {
        return findInternalFiles(directoryUrl, DavConstants.DEPTH_1);
    }

    /**
     * Finds all files within the specified directory
     * including those in sub-directories.
     *
     * @param  directoryUrl  directory URL.
     *
     * @return list of all files in the directory or its children.
     *
     * @throws WebDavException
     *   if the directory information cannot be retrieved.
     */
    public List<WebDavFile> findAllInternalFiles(URL directoryUrl)
            throws WebDavException {
        return findInternalFiles(directoryUrl, DavConstants.DEPTH_INFINITY);
    }

    /**
     * @param  directoryUrl  directory URL.
     *
     * @return true if the remote directory can be read; otherwise false.
     */
    public boolean canReadDirectory(URL directoryUrl) {
        boolean canRead = false;
        try {
            findFile(directoryUrl);
            canRead = true;
        } catch (WebDavException e) {
            LOG.error("failed to access " + directoryUrl, e);
        }
        return canRead;
    }

    /**
     * Creates a directory on the server.
     *
     * @param  directoryUrl  URL (and path) for new directory.
     *
     * @throws WebDavException
     *   if the directory cannot be created or it already exists.
     */
    public void createDirectory(URL directoryUrl)
            throws WebDavException {

        MkColMethod method = null;
        Integer responseCode = null;

        try {
            method = new MkColMethod(directoryUrl.toString());

            responseCode = httpClient.executeMethod(method);

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
    public void saveFile(URL url,
                         File file)
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
                LOG.warn("failed to close input stream for " + file.getAbsolutePath() +
                        ", ignoring exception", e);
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
    protected void saveFile(URL url,
                            InputStream fileStream)
            throws WebDavException {

        PutMethod method = null;
        Integer responseCode = null;

        try {
            method = new PutMethod(url.toString());
            method.setRequestEntity(new InputStreamRequestEntity(fileStream));

            responseCode = httpClient.executeMethod(method);

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

    private List<WebDavFile> findInternalFiles(URL url,
                                               int depth)
            throws WebDavException {

        final String href = url.toString();
        List<WebDavFile> webDavFileList = new ArrayList<WebDavFile>(1024);
        MultiStatusResponse[] multiStatusResponses = getResponses(href, depth, 0);
        if ((multiStatusResponses == null) || (multiStatusResponses.length == 0)) {
            throw new WebDavException("empty response returned for " + href);
        }
        WebDavFile webDavFile;
        for (MultiStatusResponse msr : multiStatusResponses) {
            webDavFile = new WebDavFile(url, msr);
            if (! webDavFile.isDirectory()) {
                webDavFileList.add(webDavFile);
            }
        }
        return webDavFileList;
    }

    private MultiStatusResponse[] getResponses(String href,
                                               int depth,
                                               int callCount)
            throws WebDavException {

        MultiStatusResponse[] multiStatusResponses = null;
        PropFindMethod method = null;
        try {
            method = new PropFindMethod(href,
                                        WebDavFile.PROPERTY_NAMES,
                                        depth);

            final int responseCode = httpClient.executeMethod(method);

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
                throw new WebDavException(responseCode + " response code returned for " + href,
                                          responseCode);
            } else {
                throw new WebDavException(responseCode + " response code returned for " + href,
                                          responseCode);
            }

        } catch (WebDavException e) {
            throw e;
        } catch (Exception e) {
            throw new WebDavException("failed to retrieve WebDAV information for " + href, e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

        return multiStatusResponses;
    }

    private int getResourceTypeResponseCode(String href)
            throws WebDavException {

        int responseCode;

        PropFindMethod method = null;
        try {
            method = new PropFindMethod(href,
                                        RESOURCE_TYPE_PROPERTY_ONLY,
                                        DavConstants.DEPTH_0);
            responseCode = httpClient.executeMethod(method);
        } catch (Exception e) {
            throw new WebDavException("failed to retrieve WebDAV information for " + href, e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

        return responseCode;
    }

    private synchronized long incrementUploadCount() {
        uploadCount++;
        return uploadCount;
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebDavClient.class);

    /**
     * The set of WebDAV properties required to populate a
     * {@link WebDavFile} instance.
     */
    private static final DavPropertyNameSet RESOURCE_TYPE_PROPERTY_ONLY;
    static {
        DavPropertyNameSet nameSet = new DavPropertyNameSet();
        nameSet.add(DavPropertyName.RESOURCETYPE);
        RESOURCE_TYPE_PROPERTY_ONLY = nameSet;
    }

}
