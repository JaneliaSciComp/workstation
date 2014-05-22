package org.janelia.it.workstation.shared.util.filecache;

import com.google.common.base.CharMatcher;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URL;
import java.util.concurrent.Callable;

/**
 * This class manages loading remote files into the cache.
 * It's default implementation handles loading a single file into the cache,
 * but it can be extended later for directory loads if needed.
 *
 * @author Eric Trautman
 */
public abstract class RemoteFileLoader
        extends CacheLoader<URL, org.janelia.it.workstation.shared.util.filecache.CachedFile>
        implements Callable<org.janelia.it.workstation.shared.util.filecache.CachedFile> {
    
    private URL url;

    public RemoteFileLoader() {
        this(null);
    }

    public RemoteFileLoader(URL url) {
        this.url = url;
    }

    public abstract LocalFileCache getCache();

    @Override
    public org.janelia.it.workstation.shared.util.filecache.CachedFile call() throws Exception {
        return load(url);
    }

    @Override
    public org.janelia.it.workstation.shared.util.filecache.CachedFile load(URL url) throws Exception {
        
        org.janelia.it.workstation.shared.util.filecache.CachedFile cachedFile;

        final LocalFileCache cache = getCache();
        final org.janelia.it.workstation.shared.util.filecache.WebDavClient client = cache.getWebDavClient();

        org.janelia.it.workstation.shared.util.filecache.WebDavFile webDavFile = client.findFile(url);

        // check for catastrophic case of file larger than entire cache
        final long size = webDavFile.getKilobytes();
        if (size < cache.getKilobyteCapacity()) {

            final String urlPath = url.getPath();

            // Spent a little time profiling fastest method for deriving
            // a unique name for the temp file in a multi-threaded environment.
            // The chosen Google CharMatcher method typically was 2-3 times faster
            // than java matcher replaceAll.  Also looked at a class (static)
            // synchronized counter which had similar performance to java matcher
            // but performed much worse as concurrency increased past ten threads.
            final String uniqueTempFileName = SLASHES_CHAR_MATCHER.replaceFrom(urlPath, '-');

            final File tempFile = new File(cache.getTempDirectory(), uniqueTempFileName);
            final File activeFile = new File(cache.getActiveDirectory(), urlPath);

            cachedFile = loadRemoteFile(webDavFile, tempFile, activeFile, client);

        } else {
            throw new IllegalStateException(
                    size + " kilobyte file exceeds cache capacity of " +
                    cache.getKilobyteCapacity());
        }

        return cachedFile;
    }

    /**
     * Creates any missing parent directories for the specified file.
     *
     * @param  child  file whose parents directroies need to exist.
     *
     * @throws IllegalStateException
     *   if the parent directories do not exist and cannot be created.
     */
    public static void createParentDirectroiesIfNeccesary(File child)
            throws IllegalStateException {

        final File parent = child.getParentFile();
        if (! parent.exists()) {
            if (! parent.mkdirs()) {
                // check again for parent existence in case another thread
                // created the directory while this thread was attempting
                // to create it
                if (! parent.exists()) {
                    throw new IllegalStateException(
                            "failed to create directory " + parent.getAbsolutePath());
                }
            }
        }
    }

    /**
     * Loads the specified webDavFile locally.
     *
     * @param  webDavFile  identifies remote file to load.
     * @param  tempFile    temporary file for initial load.
     * @param  activeFile  final locally loaded file.
     * @param  client      WebDAV client instance.
     *
     * @return cached file wrapper for the locally loaded file.
     *
     * @throws IllegalArgumentException
     *   if a directory resource is specified.
     *
     * @throws IllegalStateException
     *   if the local file cannot be created.
     *
     * @throws org.janelia.it.workstation.shared.util.filecache.WebDavException
     *   if the file cannot be retrieved.
     */
    protected static org.janelia.it.workstation.shared.util.filecache.CachedFile loadRemoteFile(org.janelia.it.workstation.shared.util.filecache.WebDavFile webDavFile,
                                               File tempFile,
                                               File activeFile,
                                               org.janelia.it.workstation.shared.util.filecache.WebDavClient client)
            throws IllegalArgumentException, IllegalStateException, org.janelia.it.workstation.shared.util.filecache.WebDavException {

        org.janelia.it.workstation.shared.util.filecache.CachedFile cachedFile = new org.janelia.it.workstation.shared.util.filecache.CachedFile(webDavFile, activeFile);

        final URL remoteFileUrl = webDavFile.getUrl();

        if (webDavFile.isDirectory()) {
            throw new IllegalArgumentException(
                    "Requested load of directory " + remoteFileUrl +
                            ".  Only files may be requested.");
        }

        tempFile = client.retrieveFile(remoteFileUrl, tempFile);

        createParentDirectroiesIfNeccesary(activeFile);

        if (tempFile.renameTo(activeFile)) {
            LOG.debug("loadRemoteFile: copied {} to {}", remoteFileUrl, activeFile.getAbsolutePath());
        } else {
            if (! tempFile.delete()) {
                LOG.warn("loadRemoteFile: after move failure, failed to remove temp file {}",
                        tempFile.getAbsolutePath());
            }
            throw new IllegalStateException(
                    "failed to move " + tempFile.getAbsolutePath() +
                            " to " + activeFile.getAbsolutePath());
        }

        cachedFile.saveMetadata();

        return cachedFile;
    }

    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileLoader.class);

    private static final CharMatcher SLASHES_CHAR_MATCHER = CharMatcher.anyOf("/\\");
}
