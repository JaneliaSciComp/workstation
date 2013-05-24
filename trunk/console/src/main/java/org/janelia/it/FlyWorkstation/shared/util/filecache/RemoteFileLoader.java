package org.janelia.it.FlyWorkstation.shared.util.filecache;

import com.google.common.base.CharMatcher;
import com.google.common.cache.CacheLoader;

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
        extends CacheLoader<URL, CachedFile>
        implements Callable<CachedFile> {

    private URL url;

    public RemoteFileLoader() {
        this(null);
    }

    public RemoteFileLoader(URL url) {
        this.url = url;
    }

    public abstract LocalFileCache getCache();

    @Override
    public CachedFile call() throws Exception {
        return load(url);
    }

    @Override
    public CachedFile load(URL url) throws Exception {

        CachedFile cachedFile;

        final LocalFileCache cache = getCache();
        final WebDavClient client = cache.getWebDavClient();

        WebDavFile webDavFile = client.findFile(url);

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

            cachedFile = new CachedFile(webDavFile, activeFile);
            cachedFile.loadRemoteFile(tempFile);

        } else {
            throw new IllegalStateException(
                    size + " kilobyte file exceeds cache capacity of " +
                    cache.getKilobyteCapacity());
        }

        return cachedFile;
    }

    private static final CharMatcher SLASHES_CHAR_MATCHER = CharMatcher.anyOf("/\\");
}
