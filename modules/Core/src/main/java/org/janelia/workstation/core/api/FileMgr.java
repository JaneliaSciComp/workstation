package org.janelia.workstation.core.api;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.concurrent.Executors;

import com.google.common.eventbus.Subscribe;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.janelia.filecacheutils.FileProxy;
import org.janelia.filecacheutils.LocalFileCache;
import org.janelia.filecacheutils.LocalFileCacheStorage;
import org.janelia.filecacheutils.LocalFileProxy;
import org.janelia.workstation.core.api.http.HttpClientProxy;
import org.janelia.workstation.core.events.Events;
import org.janelia.workstation.core.events.lifecycle.ConsolePropsLoaded;
import org.janelia.workstation.core.filecache.StorageClientMgr;
import org.janelia.workstation.core.filecache.WebDavRemoteFileRetriever;
import org.janelia.workstation.core.filecache.WebDavUploader;
import org.janelia.workstation.core.filecache.WebdavCachedFileKey;
import org.janelia.workstation.core.options.OptionConstants;
import org.janelia.workstation.core.util.ConsoleProperties;
import org.janelia.workstation.core.workers.SimpleWorker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing remote and cached file access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileMgr {

    private static final Logger log = LoggerFactory.getLogger(FileMgr.class);

    // Singleton
    private static FileMgr instance;
    public static synchronized FileMgr getFileMgr() {
        if (instance==null) {
            instance = new FileMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    public static final int MIN_FILE_CACHE_GIGABYTE_CAPACITY = 10;
    public static final int DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY = 50;
    public static final int MAX_FILE_CACHE_GIGABYTE_CAPACITY = 1000;

    private String consolePrefsDir;
    private String webdavBaseUrl;
    private int webdavMaxConnsPerHost;
    private int webdavMaxTotalConnections;
    private HttpClientProxy httpClient;
    private StorageClientMgr storageClientMgr;
    private LocalFileCacheStorage localFileCacheStorage;
    private LocalFileCache<WebdavCachedFileKey> webdavLocalFileCache;

    private FileMgr() {
    }

    @Subscribe
    public void propsLoaded(ConsolePropsLoaded event) {
        SimpleWorker.runInBackground(() -> {
            synchronized (FileMgr.this) {
                log.info("Initializing File Manager");
                this.consolePrefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
                this.webdavBaseUrl = ConsoleProperties.getString("console.webDavClient.baseUrl", null);
                this.webdavMaxConnsPerHost = ConsoleProperties.getInt("console.webDavClient.maxConnectionsPerHost", 100);
                this.webdavMaxTotalConnections = ConsoleProperties.getInt("console.webDavClient.maxTotalConnections", 100);
                log.info("Using WebDAV server: {}", webdavBaseUrl);

                MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
                HttpConnectionManagerParams managerParams = mgr.getParams();
                managerParams.setDefaultMaxConnectionsPerHost(webdavMaxConnsPerHost);
                managerParams.setMaxTotalConnections(webdavMaxTotalConnections);
                httpClient = new HttpClientProxy(new HttpClient(mgr));
                storageClientMgr = new StorageClientMgr(webdavBaseUrl, httpClient);
                setFileCacheGigabyteCapacity((Integer)
                        LocalPreferenceMgr.getInstance().getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY));
                setFileCacheDisabled(Boolean.parseBoolean(String.valueOf(
                        LocalPreferenceMgr.getInstance().getModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY))));
            }
        });
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
        return (webdavLocalFileCache != null);
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
            webdavLocalFileCache = null;
            localFileCacheStorage = null;
        } else {
            try {
                final String localCacheRoot = ConsoleProperties.getString("console.localCache.rootDirectory", consolePrefsDir);
                final long kilobyteCapacity = getFileCacheGigabyteCapacity() * 1024 * 1024;

                localFileCacheStorage = new LocalFileCacheStorage(Paths.get(localCacheRoot), kilobyteCapacity);
                webdavLocalFileCache = new LocalFileCache<>(
                        localFileCacheStorage,
                        new WebDavRemoteFileRetriever(httpClient, storageClientMgr),
                        Executors.newFixedThreadPool(4));
            } catch (IllegalStateException e) {
                webdavLocalFileCache = null;
                localFileCacheStorage = null;
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

        if (localFileCacheStorage != null) {
            updateLocalCacheStorageCapacity(gigabyteCapacity * 1024 * 1024);
        }
    }

    private void updateLocalCacheStorageCapacity(long capacityInKB) {
        if (capacityInKB > 0 && capacityInKB != localFileCacheStorage.getCapacityInKB()) {
            localFileCacheStorage.setCapacityInKB(capacityInKB);
        }
    }

    /**
     * @return the total size (in gigabytes) of all currently cached files.
     */
    public double getFileCacheGigabyteUsage() {
        double usage = 0.0;
        if (localFileCacheStorage != null) {
            final long kilobyteUsage = localFileCacheStorage.getCurrentSizeInKB();
            usage = kilobyteUsage / (1024.0 * 1024.0);
        }
        return usage;
    }

    /**
     * Removes all locally cached files.
     */
    public void clearFileCache() {
        if (localFileCacheStorage != null) {
            localFileCacheStorage.clear();
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
    public FileProxy getFile(String standardPath, boolean forceRefresh) {
        if (isFileCacheAvailable()) {
            return webdavLocalFileCache.getCachedFileEntry(new WebdavCachedFileKey(standardPath), forceRefresh);
        } else {
            // return this as a local file
            return new LocalFileProxy(standardPath);
        }
    }

    /**
     * Open an input stream for the specified standard path.
     *
     * @param standardPathName
     *            a standard system path
     * @param forceRefresh indicates if any existing cached file should be forcibly refreshed before
     *            being returned. In most cases, this should be set to false.
     *
     * @return an input stream to read the content identified by standardPathName
     */
    public InputStream getFileInputStream(String standardPathName, boolean forceRefresh) throws IOException {
        FileProxy fileProxy = getFile(standardPathName, forceRefresh);
        return fileProxy.getContentStream();
    }

}
