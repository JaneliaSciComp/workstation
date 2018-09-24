package org.janelia.it.workstation.browser.filecache;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import com.google.common.cache.CacheLoader;
import org.apache.commons.httpclient.methods.GetMethod;
import org.janelia.it.workstation.browser.api.http.HttpClientProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class manages loading remote files into the cache.
 * It's default implementation handles loading a single file into the cache,
 * but it can be extended later for directory loads if needed.
 *
 * @author Eric Trautman
 */
public class RemoteFileCacheLoader extends CacheLoader<String, CachedFile> {
    private static final Logger LOG = LoggerFactory.getLogger(RemoteFileCacheLoader.class);
    private static final int EOF = -1;
    // Use 2Mb buffer to reduce likelihood of out of memory errors
    // when concurrent threads are loading images.
    // Most of the dynamic image files are around 1Mb.
    private static final int BUFFER_SIZE = 2 * 1024 * 1024; // 2Mb

    private final HttpClientProxy httpClient;
    private final StorageClientMgr storageClientMgr;
    private final LocalFileCache loadedCache;

    public RemoteFileCacheLoader(HttpClientProxy httpClient, StorageClientMgr storageClientMgr, LocalFileCache loadedCache) {
        this.httpClient = httpClient;
        this.storageClientMgr = storageClientMgr;
        this.loadedCache = loadedCache;
    }

    @Override
    public CachedFile load(String remoteFileName) throws Exception {
        CachedFile cachedFile;
        WebDavFile webDavFile = storageClientMgr.findFile(remoteFileName);

        // check for catastrophic case of file larger than entire cache
        final long size = webDavFile.getKilobytes();
        if (size < loadedCache.getKilobyteCapacity()) {
            final String cachedFileName;
            if (remoteFileName.startsWith("jade:///")) {
                cachedFileName = remoteFileName.substring("jade:///".length());
            } else if (remoteFileName.startsWith("jade://")) {
                cachedFileName = remoteFileName.substring("jade://".length());
            } else if (remoteFileName.startsWith("/")) {
                cachedFileName = remoteFileName.substring(1);
            } else {
                cachedFileName = remoteFileName;
            }
            // Spent a little time profiling fastest method for deriving
            // a unique name for the temp file in a multi-threaded environment.
            // The chosen Google CharMatcher method typically was 2-3 times faster
            // than java matcher replaceAll.  Also looked at a class (static)
            // synchronized counter which had similar performance to java matcher
            // but performed much worse as concurrency increased past ten threads.
            final File tempFile = new File(loadedCache.getTempDirectory(), UUID.randomUUID().toString());
            final File activeFile = new File(loadedCache.getActiveDirectory(), cachedFileName);

            cachedFile = loadRemoteFile(webDavFile, tempFile, activeFile);

        } else {
            throw new IllegalStateException(
                    size + " kilobyte file exceeds cache capacity of " +
                    loadedCache.getKilobyteCapacity());
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
        final URLProxy remoteFileUrl = webDavFile.getRemoteFileURLProxy();
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
    private File retrieveFile(URLProxy remoteFileUrl, File outputFile) throws WebDavException {
        InputStream input = null;
        FileOutputStream output = null;
        final String prototcol = remoteFileUrl.getProtocol();
        GetMethod getMethod = null;
        if (prototcol.startsWith("http")) {
            // this is the only case which if fails we need to handle the proxy error
            try {
                getMethod = new GetMethod(remoteFileUrl.toString());
                final int responseCode = httpClient.executeMethod(getMethod);
                LOG.trace("retrieveFile: {} returned for GET {}", responseCode, remoteFileUrl);
                if (responseCode != HttpServletResponse.SC_OK) {
                    throw new WebDavException(responseCode + " returned for GET " + remoteFileUrl, responseCode);
                }
                input = getMethod.getResponseBodyAsStream();
            } catch (WebDavException e) {
                remoteFileUrl.handleError(e);
                if (getMethod != null) {
                    getMethod.releaseConnection();
                }
                throw e;
            } catch (Exception e) {
                remoteFileUrl.handleError(e);
                if (getMethod != null) {
                    getMethod.releaseConnection();
                }
                throw new WebDavException(
                        "failed to open " + remoteFileUrl + " in order to write to " + outputFile.getAbsolutePath(), e);
            }
        } else {
            // use java URL library for non-http resources (e.g. file://)
            try {
                input = remoteFileUrl.openStream();
            } catch (Exception e) {
                throw new WebDavException(
                        "failed to open " + remoteFileUrl + " in order to write to " + outputFile.getAbsolutePath(), e);
            }
        }
        try {
            output = new FileOutputStream(outputFile);
            byte[] buffer = new byte[BUFFER_SIZE];
            int n;
            while (EOF != (n = input.read(buffer))) {
                output.write(buffer, 0, n);
            }
        } catch (Exception e) {
            throw new WebDavException(
                    "failed to copy " + remoteFileUrl + " to " + outputFile.getAbsolutePath(), e);
        } finally {
            if (getMethod != null) {
                getMethod.releaseConnection();
            }
            try {
                input.close();
            } catch (IOException e) {
                LOG.warn("retrieveFile: failed to close " + remoteFileUrl, e);
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
