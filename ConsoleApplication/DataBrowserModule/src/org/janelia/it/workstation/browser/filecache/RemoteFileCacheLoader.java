package org.janelia.it.workstation.browser.filecache;

import com.google.common.base.CharMatcher;
import com.google.common.cache.CacheLoader;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import javax.servlet.http.HttpServletResponse;

/**
 * This class manages loading remote files into the cache.
 * It's default implementation handles loading a single file into the cache,
 * but it can be extended later for directory loads if needed.
 *
 * @author Eric Trautman
 */
public class RemoteFileCacheLoader extends CacheLoader<String, CachedFile> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileCacheLoader.class);
    private static final CharMatcher SLASHES_CHAR_MATCHER = CharMatcher.anyOf("/\\");
    private static final int EOF = -1;
    // Use 2Mb buffer to reduce likelihood of out of memory errors
    // when concurrent threads are loading images.
    // Most of the dynamic image files are around 1Mb.
    private static final int BUFFER_SIZE = 2 * 1024 * 1024; // 2Mb

    private final HttpClient httpClient;
    private final WebDavClientMgr webDavClientMgr;
    private final long maxCacheableFileSizeInKb;
    private final File loadCompletedDir;
    private final File loadInProgressDir;

    public RemoteFileCacheLoader(HttpClient httpClient, WebDavClientMgr webDavClientMgr,
                                 long maxCacheableFileSizeInKb,
                                 File loadCompletedDir,
                                 File loadInProgressDir) {
        this.httpClient = httpClient;
        this.webDavClientMgr = webDavClientMgr;
        this.maxCacheableFileSizeInKb = maxCacheableFileSizeInKb;
        this.loadCompletedDir = loadCompletedDir;
        this.loadInProgressDir = loadInProgressDir;
    }

    @Override
    public CachedFile load(String remoteFileName) throws Exception {
        CachedFile cachedFile;
        WebDavFile webDavFile = webDavClientMgr.findFile(remoteFileName);

        // check for catastrophic case of file larger than entire cache
        final long size = webDavFile.getKilobytes();
        if (size < maxCacheableFileSizeInKb) {

            final String urlPath = webDavFile.getRemoteFileUrl().getPath();

            // Spent a little time profiling fastest method for deriving
            // a unique name for the temp file in a multi-threaded environment.
            // The chosen Google CharMatcher method typically was 2-3 times faster
            // than java matcher replaceAll.  Also looked at a class (static)
            // synchronized counter which had similar performance to java matcher
            // but performed much worse as concurrency increased past ten threads.
            final String uniqueTempFileName = SLASHES_CHAR_MATCHER.replaceFrom(urlPath, '-');

            final File tempFile = new File(loadInProgressDir, uniqueTempFileName);
            final File activeFile = new File(loadCompletedDir, urlPath);

            cachedFile = loadRemoteFile(webDavFile, tempFile, activeFile);

        } else {
            throw new IllegalStateException(
                    size + " kilobyte file exceeds cache capacity of " +
                    maxCacheableFileSizeInKb);
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
    private void createParentDirectroiesIfNeccesary(File child)
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
     *
     * @return cached file wrapper for the locally loaded file.
     *
     * @throws IllegalArgumentException
     *   if a directory resource is specified.
     *
     * @throws IllegalStateException
     *   if the local file cannot be created.
     *
     * @throws WebDavException
     *   if the file cannot be retrieved.
     */
    private CachedFile loadRemoteFile(WebDavFile webDavFile,
                                      File tempFile,
                                      File activeFile)
            throws IllegalArgumentException, IllegalStateException, WebDavException {
        CachedFile cachedFile = new CachedFile(webDavFile, activeFile);
        final URL remoteFileUrl = webDavFile.getRemoteFileUrl();
        if (webDavFile.isDirectory()) {
            throw new IllegalArgumentException(
                    "Requested load of directory " + remoteFileUrl +
                            ".  Only files may be requested.");
        }
        tempFile = retrieveFile(remoteFileUrl, tempFile);

        createParentDirectroiesIfNeccesary(activeFile);

        if (activeFile.exists()) {
            LOG.debug("loadRemoteFile: active file exists, and will be deleted: {}", activeFile.getAbsolutePath());
            if (!activeFile.delete()) {
                LOG.warn("loadRemoteFile: before move, failed to remove active file {}", activeFile.getAbsolutePath());
            }
        }
        
        if (tempFile.renameTo(activeFile)) {
            LOG.debug("loadRemoteFile: copied {} to {}", remoteFileUrl, activeFile.getAbsolutePath());
        } else {
            if (! tempFile.delete()) {
                LOG.warn("loadRemoteFile: after move failure, failed to remove temp file {}", tempFile.getAbsolutePath());
            }
            throw new IllegalStateException(
                    "failed to move " + tempFile.getAbsolutePath() +
                            " to " + activeFile.getAbsolutePath());
        }

        cachedFile.saveMetadata();

        return cachedFile;
    }

    /**
     * Retrieves the file identified by the URL and writes its contents to the
     * specified output file.
     *
     * @param  remoteFileUrl  file URL to retrieve.
     * @param  outputFile     local file in which to store contents.
     *
     * @return the specified output file.
     *
     * @throws WebDavException
     *   if the file cannot be retrieved.
     */
    private File retrieveFile(URL remoteFileUrl, File outputFile) throws WebDavException {
        InputStream input = null;
        FileOutputStream output = null;
        GetMethod getMethod = null;
        try {
            final String prototcol = remoteFileUrl.getProtocol();
            if (prototcol.startsWith("http")) {
                getMethod = new GetMethod(remoteFileUrl.toString());
                final int responseCode = httpClient.executeMethod(getMethod);
                LOG.trace("retrieveFile: {} returned for GET {}", responseCode, remoteFileUrl);
                if (responseCode != HttpServletResponse.SC_OK) {
                    throw new WebDavException(responseCode + " returned for GET " + remoteFileUrl,
                            responseCode);
                }
                input = getMethod.getResponseBodyAsStream();
            } else {
                // use java URL library for non-http resources (e.g. file://)
                input = remoteFileUrl.openStream();
            }
            output = new FileOutputStream(outputFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
        } catch (Throwable t) {
            throw new WebDavException(
                    "failed to copy " + remoteFileUrl + " to " + outputFile.getAbsolutePath(), t);
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOG.warn("retrieveFile: failed to close " + remoteFileUrl, e);
                }
            }
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                    LOG.warn("retrieveFile: failed to close " + outputFile.getAbsolutePath(), e);
                }
            }
        }
        return outputFile;
    }

}
