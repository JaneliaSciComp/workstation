package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutionException;

import javax.servlet.http.HttpServletResponse;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.InputStreamRequestEntity;
import org.apache.commons.lang3.StringUtils;
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
 * {@link WebDavClient} manager.
 */
public class WebDavClientMgr {

    private static final Logger LOG = LoggerFactory.getLogger(WebDavClientMgr.class);
    private static final int STORAGE_PATH_PREFIX_COMPS_COUNT = 2;

    private static final Cache<String, WebDavClient> WEBDAV_AGENTS_CACHE = CacheBuilder.newBuilder()
            .maximumSize(10)
            .build();

    private final HttpClient httpClient;
    private final WebDavClient masterWebDavInstance;

    /**
     * Constructs a client with default authentication credentials.
     *
     * @param  httpClient             httpClient
     *                                (e.g. /groups/...) to WebDAV URLs.
     * @throws IllegalArgumentException
     *   if the baseUrl cannot be parsed.
     */
    public WebDavClientMgr(String baseUrl, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.masterWebDavInstance = new WebDavClient(validateUrl(baseUrl), httpClient);
    }

    private String validateUrl(String urlString) {
        try {
            final URL url = new URL(urlString);
            return urlString;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("failed to parse URL: " + urlString, e);
        }
    }

    public URL getDownloadFileURL(String standardPathName) {
        WebDavClient webDavClient = getWebDavClientForStandardPath(standardPathName);
        return webDavClient.getDownloadFileURL(standardPathName);
    }

    private WebDavClient getWebDavClientForStandardPath(String standardPathName) {
        Path standardPath = Paths.get(standardPathName);
        int nPathComponentIndex = standardPath.getNameCount();
        Path storagePathPrefix;
        if (nPathComponentIndex < STORAGE_PATH_PREFIX_COMPS_COUNT) {
            storagePathPrefix = standardPath;
        } else {
            if (standardPath.getRoot() == null) {
                storagePathPrefix = standardPath.subpath(0, STORAGE_PATH_PREFIX_COMPS_COUNT);
            } else {
                storagePathPrefix = standardPath.getRoot().resolve(standardPath.subpath(0, STORAGE_PATH_PREFIX_COMPS_COUNT));
            }
        }
        try {
            String storageKey = storagePathPrefix.toString();
            return WEBDAV_AGENTS_CACHE.get(storageKey, () -> {
                WebDavFile webDavFile = findWebDavFileStorage(storageKey);
                return new WebDavClient(webDavFile.getRemoteFileUrl().toString(), httpClient);
            });
        } catch (ExecutionException e) {
            throw new IllegalStateException(e);
        }
    }

    private WebDavFile findWebDavFileStorage(String storagePathPrefix) throws WebDavException {
        return masterWebDavInstance.findStorage(storagePathPrefix);
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
    WebDavFile findFile(String remoteFileName)
            throws WebDavException {
        WebDavClient webDavClient = getWebDavClientForStandardPath(remoteFileName);
        return webDavClient.findFile(remoteFileName);
    }

    String createStorage(String storageName) {
        // !!!!!
        return null; // FIXME
    }

    String createStorageDirectory(String storageName) {
        // !!!!!!!!!
        return null; // FIXME
    }

    String uploadFile(Path file) {
        return null; // FIXME
    }

    String createDirectory(Path dir) {
        return null; // FIXME
    }

}
