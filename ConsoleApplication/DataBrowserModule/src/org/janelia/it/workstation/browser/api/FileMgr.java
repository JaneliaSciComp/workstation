package org.janelia.it.workstation.browser.api;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import org.janelia.it.jacs.integration.FrameworkImplProvider;
import org.janelia.it.workstation.browser.ConsoleApp;
import org.janelia.it.workstation.browser.filecache.LocalFileCache;
import org.janelia.it.workstation.browser.filecache.WebDavClient;
import org.janelia.it.workstation.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.browser.util.ConsoleProperties;
import org.janelia.it.workstation.browser.util.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing remote and cached file access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileMgr {

    private static final Logger log = LoggerFactory.getLogger(FileMgr.class);
    
    private static final String webdavBaseUrl = ConsoleProperties.getString("console.webDavClient.baseUrl", WebDavClient.JACS_WEBDAV_BASE_URL);
    
    // Singleton
    private static FileMgr instance;
    public static synchronized FileMgr getFileMgr() {
        if (instance==null) {
            instance = new FileMgr();
        }
        return instance;
    }
    
    private final String prefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    
    public static final int MIN_FILE_CACHE_GIGABYTE_CAPACITY = 10;
    public static final int DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY = 50;
    public static final int MAX_FILE_CACHE_GIGABYTE_CAPACITY = 1000;
    
    private final WebDavClient webDavClient;
    private LocalFileCache localFileCache; 

    private FileMgr() {
        
        log.info("Initializing File Manager");

        log.info("Using WebDAV server: {}",webdavBaseUrl);
        // TODO: most of this initialization should be done in a background thread, to improve start-up time for the Workstation
        this.webDavClient = new WebDavClient(
                webdavBaseUrl,
                ConsoleProperties.getInt("console.webDavClient.maxConnectionsPerHost", 100),
                ConsoleProperties.getInt("console.webDavClient.maxTotalConnections", 100));
        
        setFileCacheGigabyteCapacity((Integer) 
                FrameworkImplProvider.getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY));
        setFileCacheDisabled(Boolean.parseBoolean(String.valueOf(
                FrameworkImplProvider.getModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY))));        
        
        Integer tmpCache = (Integer) FrameworkImplProvider.getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
        if (null != tmpCache) {
            PropertyConfigurator.getProperties().setProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, tmpCache.toString());
        }
    }

    /**
     * @return the session client for issuing WebDAV requests.
     */
    public WebDavClient getWebDavClient() {
        return webDavClient;
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

        FrameworkImplProvider.setModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY, isDisabled);

        if (isDisabled) {
            log.warn("disabling local cache");
            localFileCache = null;
        }
        else {
            try {
                final String localCacheRoot
                        = ConsoleProperties.getString("console.localCache.rootDirectory",
                                prefsDir);
                final long kilobyteCapacity = getFileCacheGigabyteCapacity() * 1024 * 1024;

                localFileCache = new LocalFileCache(new File(localCacheRoot),
                        kilobyteCapacity,
                        webDavClient,
                        null);
            }
            catch (IllegalStateException e) {
                localFileCache = null;
                log.error("disabling local cache after initialization failure", e);
            }
        }
    }

    /**
     * @return the session local file cache instance or null if a cache is not available.
     */
    public LocalFileCache getFileCache() {
        return localFileCache;
    }

    /**
     * @return the maximum number of gigabytes to store in the local file cache.
     */
    public int getFileCacheGigabyteCapacity() {
        return (Integer) FrameworkImplProvider.getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
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

        FrameworkImplProvider.setModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY,
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
    public static File getCachedFile(String standardPath,
            boolean forceRefresh) {

        FileMgr mgr = getFileMgr();

        File file = null;
        if (mgr.isFileCacheAvailable()) {
            final LocalFileCache cache = mgr.getFileCache();
            final WebDavClient client = mgr.getWebDavClient();
            try {
                final URL url = client.getWebDavUrl(standardPath);
                file = cache.getFile(url, forceRefresh);
            }
            catch (FileNotFoundException e) {
                log.warn("File does not exist: " + standardPath, e);
            }
            catch (IOException e) {
                if ("No space left on device".equals(e.getMessage())) {
                    FrameworkImplProvider.handleExceptionQuietly("No space left on disk", e);
                }
                else {
                    log.error("Failed to retrieve " + standardPath + " from local cache", e);
                }
            }
            catch (Exception e) {
                log.error("Failed to retrieve " + standardPath + " from local cache", e);
            }
        }
        else {
            log.warn("Local file cache is not available");
        }

        return file;
    }

    /**
     * Get the URL for a standard path. It may be a local URL, if the file has
     * been cached, or a remote URL on the WebDAV server. It might even be a
     * mounted location, if WebDAV is disabled.
     *
     * @param standardPath
     *            a standard system path
     * @return an accessible URL for the specified path
     */
    public static URL getURL(String standardPath) {
        return getURL(standardPath, true);
    }

    public static URL getURL(String standardPath, boolean cacheAsync) {
        try {
            FileMgr mgr = getFileMgr();
            WebDavClient client = mgr.getWebDavClient();
            URL remoteFileUrl = client.getWebDavUrl(standardPath);
            LocalFileCache cache = mgr.getFileCache();
            return mgr.isFileCacheAvailable() ? cache.getEffectiveUrl(remoteFileUrl, cacheAsync) : remoteFileUrl;
        }
        catch (MalformedURLException e) {
            ConsoleApp.handleException(e);
            return null;
        }
    }
}
