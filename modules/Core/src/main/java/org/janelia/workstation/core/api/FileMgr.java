package org.janelia.workstation.core.api;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executors;

import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

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
import org.janelia.workstation.core.filecache.WebDavFileKeyProxySupplier;
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

    private static final Logger LOG = LoggerFactory.getLogger(FileMgr.class);
    private static final int DEFAULT_FILE_CACHE_CONCURRENCY = 8;

    // Singleton
    private static FileMgr instance;
    public static synchronized FileMgr getFileMgr() {
        if (instance==null) {
            instance = new FileMgr();
            Events.getInstance().registerOnEventBus(instance);
        }
        return instance;
    }

    private String webdavBaseUrl;
    private int webdavMaxConnsPerHost;
    private int webdavMaxTotalConnections;
    private HttpClientProxy httpClient;
    private StorageClientMgr storageClientMgr;
    private LocalFileCache<WebdavCachedFileKey> webdavLocalFileCache;

    private FileMgr() {
    }

    @Subscribe
    public void propsLoaded(ConsolePropsLoaded event) {
        SimpleWorker.runInBackground(() -> {
            synchronized (FileMgr.this) {
                LOG.info("Initializing File Manager");
                this.webdavBaseUrl = ConsoleProperties.getString("console.webDavClient.baseUrl", null);
                this.webdavMaxConnsPerHost = ConsoleProperties.getInt("console.webDavClient.maxConnectionsPerHost", 100);
                this.webdavMaxTotalConnections = ConsoleProperties.getInt("console.webDavClient.maxTotalConnections", 100);
                LOG.info("Using WebDAV server: {}", webdavBaseUrl);

                MultiThreadedHttpConnectionManager mgr = new MultiThreadedHttpConnectionManager();
                HttpConnectionManagerParams managerParams = mgr.getParams();
                managerParams.setDefaultMaxConnectionsPerHost(webdavMaxConnsPerHost);
                managerParams.setMaxTotalConnections(webdavMaxTotalConnections);
                httpClient = new HttpClientProxy(new HttpClient(mgr));
                storageClientMgr = new StorageClientMgr(webdavBaseUrl, httpClient);
                webdavLocalFileCache = new LocalFileCache<>(
                        LocalCacheMgr.getInstance().getLocalFileCacheStorage(),
                        DEFAULT_FILE_CACHE_CONCURRENCY,
                        new WebDavFileKeyProxySupplier(httpClient, storageClientMgr),
                        Executors.newFixedThreadPool(4,
                                new ThreadFactoryBuilder()
                                        .setNameFormat("CacheEvictor-%d")
                                        .setDaemon(true).build()),
                        Executors.newFixedThreadPool(DEFAULT_FILE_CACHE_CONCURRENCY,
                                new ThreadFactoryBuilder()
                                        .setNameFormat("LocalCachedFileWriter-%d")
                                        .setDaemon(true).build())
                );
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
        return webdavLocalFileCache.getCachedFileEntry(new WebdavCachedFileKey(standardPath), forceRefresh);
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
    public InputStream openFileInputStream(String standardPathName, boolean forceRefresh) throws IOException {
        FileProxy fileProxy = getFile(standardPathName, forceRefresh);
        return fileProxy.openContentStream();
    }

}
