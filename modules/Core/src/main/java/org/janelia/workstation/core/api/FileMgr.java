package org.janelia.workstation.core.api;

import java.io.File;
import java.io.FileNotFoundException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.filecache.LocalFileCache;
import org.janelia.workstation.core.filecache.URLProxy;
import org.janelia.workstation.core.filecache.StorageClientMgr;
import org.janelia.workstation.core.filecache.WebDavUploader;
import org.janelia.workstation.core.options.OptionConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing remote and cached file access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileMgr {

    private static final Logger log = LoggerFactory.getLogger(FileMgr.class);
    private static final String JACS_WEBDAV_BASE_URL = "http://jacs-webdav.int.janelia.org/Webdav";

    private static final String CONSOLE_PREFS_DIR = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    private static final String WEBDAV_BASE_URL = ConsoleProperties.getString("console.webDavClient.baseUrl", JACS_WEBDAV_BASE_URL);
    private static final int WEBDAV_MAX_CONNS_PER_HOST = ConsoleProperties.getInt("console.webDavClient.maxConnectionsPerHost", 100);
    private static final int WEBDAV_MAX_TOTAL_CONNECTIONS = ConsoleProperties.getInt("console.webDavClient.maxTotalConnections", 100);

    // Singleton
    private static FileMgr instance;
    public static synchronized FileMgr getFileMgr() {
        if (instance==null) {
            instance = new FileMgr();
        }
        return instance;
    }

    public static final int MIN_FILE_CACHE_GIGABYTE_CAPACITY = 10;
    public static final int DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY = 50;
    public static final int MAX_FILE_CACHE_GIGABYTE_CAPACITY = 1000;

    private final HttpClientProxy httpClient;
    private final StorageClientMgr storageClientMgr;
    private LocalFileCache localFileCache;

    private FileMgr() {
        
        log.info("Initializing File Manager");

        log.info("Using WebDAV server: {}", WEBDAV_BASE_URL);

        MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
        HttpConnectionManagerParams managerParams = mgr.getParams();
        managerParams.setDefaultMaxConnectionsPerHost(WEBDAV_MAX_CONNS_PER_HOST);
        managerParams.setMaxTotalConnections(WEBDAV_MAX_TOTAL_CONNECTIONS);
        httpClient = new HttpClientProxy(new HttpClient(mgr));
        storageClientMgr = new StorageClientMgr(WEBDAV_BASE_URL, httpClient);

        setFileCacheGigabyteCapacity((Integer)
                LocalPreferenceMgr.getInstance().getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY));
        setFileCacheDisabled(Boolean.parseBoolean(String.valueOf(
                LocalPreferenceMgr.getInstance().getModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY))));
    }

    public HttpClientProxy getHttpClient() {
        return httpClient;
    }

    public WebDavUploader getFileUploader() {
        return new WebDavUploader(storageClientMgr);
    }

    /**
     * @return true if a local file cache is available for this session; otherwise false.
     */
    public boolean isFileCacheAvailable() {
        return (localFileCache != null);
    }

    /**
     * Enables or disables the local file cache and
     * saves the setting as a session preference.
     *
     * @param isDisabled if true, cache will be disabled;
     * otherwise cache will be enabled.
     */
    public final void setFileCacheDisabled(boolean isDisabled) {
        LocalPreferenceMgr.getInstance().setModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY, isDisabled);

        if (isDisabled) {
            log.warn("disabling local cache");
            localFileCache = null;
        } else {
            try {
                final String localCacheRoot = ConsoleProperties.getString("console.localCache.rootDirectory", CONSOLE_PREFS_DIR);
                final long kilobyteCapacity = getFileCacheGigabyteCapacity() * 1024 * 1024;

                localFileCache = new LocalFileCache(new File(localCacheRoot), kilobyteCapacity, null, httpClient, storageClientMgr);
            }
            catch (IllegalStateException e) {
                localFileCache = null;
                log.error("disabling local cache after initialization failure", e);
            }
        }
    }

    /**
     * @return the maximum number of gigabytes to store in the local file cache.
     */
    public int getFileCacheGigabyteCapacity() {
        return (Integer) LocalPreferenceMgr.getInstance().getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
    }

    /**
     * Sets the local file cache capacity and saves the setting as a session preference.
     *
     * @param gigabyteCapacity cache capacity in gigabytes.
     */
    public final void setFileCacheGigabyteCapacity(Integer gigabyteCapacity) {

        if (gigabyteCapacity == null) {
            gigabyteCapacity = DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY;
        }
        else if (gigabyteCapacity < MIN_FILE_CACHE_GIGABYTE_CAPACITY) {
            gigabyteCapacity = MIN_FILE_CACHE_GIGABYTE_CAPACITY;
        }
        else if (gigabyteCapacity > MAX_FILE_CACHE_GIGABYTE_CAPACITY) {
            gigabyteCapacity = MAX_FILE_CACHE_GIGABYTE_CAPACITY;
        }

        LocalPreferenceMgr.getInstance().setModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY,
                gigabyteCapacity);

        if (isFileCacheAvailable()) {
            final long kilobyteCapacity = gigabyteCapacity * 1024 * 1024;
            if (kilobyteCapacity != localFileCache.getKilobyteCapacity()) {
                localFileCache.setKilobyteCapacity(kilobyteCapacity);
            }
        }
    }

    /**
     * @return the total size (in gigabytes) of all currently cached files.
     */
    public double getFileCacheGigabyteUsage() {
        double usage = 0.0;
        if (isFileCacheAvailable()) {
            final long kilobyteUsage = localFileCache.getNumberOfKilobytes();
            usage = kilobyteUsage / (1024.0 * 1024.0);
        }
        return usage;
    }

    /**
     * Removes all locally cached files.
     */
    public void clearFileCache() {
        if (isFileCacheAvailable()) {
            localFileCache.clear();
        }
    }

    /**
     * If local caching is enabled, this method will synchronously cache
     * the requested system file (as needed) and return the cached file.
     * If local caching is disabled, null is returned.
     *
     * @param standardPath the standard system path for the file.
     *
     * @param forceRefresh indicates if any existing cached file
     * should be forcibly refreshed before
     * being returned. In most cases, this
     * should be set to false.
     *
     * @return an accessible file for the specified path or
     * null if caching is disabled or the file cannot be cached.
     */
    public File getFile(String standardPath, boolean forceRefresh) {
        File file = null;
        if (isFileCacheAvailable()) {
            try {
                file = localFileCache.getFile(standardPath, forceRefresh);
            } catch (FileNotFoundException e) {
                log.warn("File does not exist: " + standardPath, e);
            } catch (Exception e) {
                if ("No space left on device".equals(e.getMessage())) {
                    FrameworkImplProvider.handleException("No space left on disk", e);
                } else {
                    log.error("Failed to retrieve " + standardPath + " from local cache", e);
                }
            }
        } else {
            log.warn("Local file cache is not available");
        }

        return file;
    }

    /**
     * Get the URL for a standard path. It may be a local URL, if the file has
     * been cached, or a remote URL on the WebDAV server. It might even be a
     * mounted location, if WebDAV is disabled.
     *
     * @param standardPathName
     *            a standard system path
     * @param cacheAsync flag to cache the file if caching is available
     *
     * @return an accessible URL for the specified path
     */
    public URLProxy getURL(String standardPathName, boolean cacheAsync) throws FileNotFoundException {
        return isFileCacheAvailable()
                ? localFileCache.getEffectiveUrl(standardPathName, cacheAsync)
                : storageClientMgr.getDownloadFileURL(standardPathName);
    }

}
