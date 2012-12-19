package org.janelia.it.FlyWorkstation.shared.util.filecache;

import org.apache.commons.httpclient.*;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.http.HttpStatus;
import org.apache.jackrabbit.webdav.DavConstants;
import org.apache.jackrabbit.webdav.MultiStatus;
import org.apache.jackrabbit.webdav.MultiStatusResponse;
import org.apache.jackrabbit.webdav.client.methods.PropFindMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link HttpClient} wrapper for submitting WebDAV requests.
 *
 * @author Eric Trautman
 */
public class WebDavClient {

    private HttpClient httpClient;

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  maxConnectionsPerHost  the default maximum number of connections
     *                                allowed for a given host config.
     * @param  maxTotalConnections    the maximum number of connections allowed.
     */
    public WebDavClient(int maxConnectionsPerHost,
                        int maxTotalConnections) {
        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(maxConnectionsPerHost); // default is 2
        managerParams.setMaxTotalConnections(maxTotalConnections);            // default is 20
        this.httpClient = new HttpClient(mgr);

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

        final HttpState clientState = this.httpClient.getState();
        final Credentials savedCredentials =
                clientState.getCredentials(AuthScope.ANY);
        if (savedCredentials == null) {
            LOG.warn("<init>: no credentials saved for WebDAV requests");
        }

    }

    /**
     * Constructs a client using the specified (already configured) {@link HttpClient}.
     *
     * @param  httpClient  client for issuing WebDAV requests.
     */
    protected WebDavClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Sets the default credentials for all requests.
     *
     * @param  credentials  default credentials.
     */
    public void setCredentials(UsernamePasswordCredentials credentials) {
        LOG.info("setCredentials: entry, userName={}",
                 credentials.getUserName());
        final HttpState clientState = this.httpClient.getState();
        clientState.setCredentials(AuthScope.ANY, credentials);
    }

    /**
     * Finds all files immediately within the specified directory
     * (but does not recurse into sub-directories).
     *
     * @param  url  directory URL.
     *
     * @return list of immediate files in the directory.
     *
     * @throws WebDavRetrievalException
     *   if the directory information cannot be retrieved.
     */
    public List<WebDavFile> findImmediateInternalFiles(URL url)
            throws WebDavRetrievalException {
        return findInternalFiles(url, DavConstants.DEPTH_1);
    }

    /**
     * Finds all files within the specified directory
     * including those in sub-directories.
     *
     * @param  url  directory URL.
     *
     * @return list of all files in the directory or its children.
     *
     * @throws WebDavRetrievalException
     *   if the directory information cannot be retrieved.
     */
    public List<WebDavFile> findAllInternalFiles(URL url)
            throws WebDavRetrievalException {
        return findInternalFiles(url, DavConstants.DEPTH_INFINITY);
    }

    private List<WebDavFile> findInternalFiles(URL url,
                                               int depth)
            throws WebDavRetrievalException {

        List<WebDavFile> webDavFileList = new ArrayList<WebDavFile>(1024);
        PropFindMethod method = null;
        try {
            final String href = url.toString();

            if (! href.endsWith("/")) {
                throw new IllegalArgumentException(
                        "URL '" + href +
                        "' must end with a '/' to be used for directory retrieval");
            }

            // TODO: handle 403, unauthorized by reload creds and then retry

            method = new PropFindMethod(href, WebDavFile.PROPERTY_NAMES, depth);
            final int responseCode = httpClient.executeMethod(method);
            if (responseCode == HttpStatus.SC_MULTI_STATUS) {
                MultiStatus multiStatus = method.getResponseBodyAsMultiStatus();

                WebDavFile webDavFile;
                for (MultiStatusResponse msr : multiStatus.getResponses()) {
                    webDavFile = new WebDavFile(url, msr);
                    if (! webDavFile.isDirectory()) {
                        webDavFileList.add(webDavFile);
                    }
                }
            } else {
                LOG.warn("find: response code " + responseCode + " returned for " + url);
            }
        } catch (Exception e) {
            throw new WebDavRetrievalException(
                    "failed to retrieve WebDAV information for " + url, e);
        } finally {
            if (method != null) {
                method.releaseConnection();
            }
        }

        return webDavFileList;
    }

    private static final Logger LOG = LoggerFactory.getLogger(WebDavClient.class);
}
