package org.janelia.it.workstation.gui.browser.api;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

import org.janelia.it.workstation.gui.browser.ConsoleApp;
import org.janelia.it.workstation.gui.browser.filecache.LocalFileCache;
import org.janelia.it.workstation.gui.browser.filecache.WebDavClient;
import org.janelia.it.workstation.gui.browser.gui.options.OptionConstants;
import org.janelia.it.workstation.gui.browser.util.ConsoleProperties;
import org.janelia.it.workstation.gui.browser.util.PropertyConfigurator;
import org.janelia.it.workstation.gui.framework.session_mgr.SessionMgr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Singleton for managing remote and cached file access.
 * 
 * @author <a href="mailto:rokickik@janelia.hhmi.org">Konrad Rokicki</a>
 */
public class FileMgr {

    // Singleton
    private static final FileMgr instance = new FileMgr();
    public static FileMgr getFileMgr() {
        return instance;
    }
    
    private static final Logger log = LoggerFactory.getLogger(FileMgr.class);

    private String prefsDir = System.getProperty("user.home") + ConsoleProperties.getString("Console.Home.Path");
    
    public static final int MIN_FILE_CACHE_GIGABYTE_CAPACITY = 10;
    public static final int DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY = 50;
    public static final int MAX_FILE_CACHE_GIGABYTE_CAPACITY = 1000;
    
    private WebDavClient webDavClient;
    private LocalFileCache localFileCache; 

    private FileMgr() {

        setFileCacheGigabyteCapacity((Integer) 
                SessionMgr.getSessionMgr().getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY));
        setFileCacheDisabled(Boolean.parseBoolean(String.valueOf(
                SessionMgr.getSessionMgr().getModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY))));        
        
        Integer tmpCache = (Integer) ConsoleApp.getConsoleApp().getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
        if (null != tmpCache) {
            PropertyConfigurator.getProperties().setProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY, tmpCache.toString());
        }
        
        webDavClient = new WebDavClient(
                ConsoleProperties.getString("console.webDavClient.baseUrl",
                        WebDavClient.JACS_WEBDAV_BASE_URL),
                ConsoleProperties.getInt("console.webDavClient.maxConnectionsPerHost", 100),
                ConsoleProperties.getInt("console.webDavClient.maxTotalConnections", 100));
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
    public void setFileCacheDisabled(boolean isDisabled) {

        SessionMgr.getSessionMgr().setModelProperty(OptionConstants.FILE_CACHE_DISABLED_PROPERTY, isDisabled);

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
            catch (Exception e) {
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
        return (Integer) SessionMgr.getSessionMgr().getModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY);
    }

    /**
     * Sets the local file cache capacity and saves the setting as a session preference.
     *
     * @param gigabyteCapacity cache capacity in gigabytes.
     */
    public void setFileCacheGigabyteCapacity(Integer gigabyteCapacity) {

        if (gigabyteCapacity == null) {
            gigabyteCapacity = DEFAULT_FILE_CACHE_GIGABYTE_CAPACITY;
        }
        else if (gigabyteCapacity < MIN_FILE_CACHE_GIGABYTE_CAPACITY) {
            gigabyteCapacity = MIN_FILE_CACHE_GIGABYTE_CAPACITY;
        }
        else if (gigabyteCapacity > MAX_FILE_CACHE_GIGABYTE_CAPACITY) {
            gigabyteCapacity = MAX_FILE_CACHE_GIGABYTE_CAPACITY;
        }

        SessionMgr.getSessionMgr().setModelProperty(OptionConstants.FILE_CACHE_GIGABYTE_CAPACITY_PROPERTY,
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
     * Get the URL for a standard path. It may be a local URL, if the file has been cached, or a remote
     * URL on the WebDAV server. It might even be a mounted location, if WebDAV is disabled.
     *
     * @param standardPath a standard system path
     * @return an accessible URL for the specified path
     */
    public static URL getURL(String standardPath) {
        try {
            FileMgr mgr = getFileMgr();
            WebDavClient client = mgr.getWebDavClient();
            URL remoteFileUrl = client.getWebDavUrl(standardPath);
            LocalFileCache cache = mgr.getFileCache();
            return mgr.isFileCacheAvailable() ? cache.getEffectiveUrl(remoteFileUrl) : remoteFileUrl;
        }
        catch (MalformedURLException e) {
            SessionMgr.getSessionMgr().handleException(e);
            return null;
        }
    }
}
